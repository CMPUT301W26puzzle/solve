package com.example.eventparticipation;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for reading and updating entrant notifications.
 */
public class NotificationRepository {

    public interface NotificationListener {
        void onNotificationsChanged(List<NotificationItem> items);
        void onError(Exception e);
    }

    private final FirebaseFirestore db;

    public NotificationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public NotificationRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public ListenerRegistration listenForNotifications(String entrantId, NotificationListener listener) {
        return getNotificationCollection(entrantId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    List<NotificationItem> items = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            items.add(fromDocument(doc));
                        }
                    }
                    listener.onNotificationsChanged(items);
                });
    }

    public Task<Void> markAsRead(String entrantId, String notificationId) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || notificationId == null || notificationId.trim().isEmpty()) {
            return Tasks.forResult(null);
        }

        return getNotificationCollection(entrantId)
                .document(notificationId)
                .update("unread", false);
    }

    public Task<Void> acceptInvitation(String entrantId, NotificationItem item) {
        if (item == null || item.getEventId() == null || item.getId() == null) {
            return Tasks.forException(new IllegalArgumentException("Missing notification data"));
        }

        DocumentReference eventRef = db.collection("events").document(item.getEventId());
        DocumentReference waitRef = eventRef.collection("waitingList").document(entrantId);
        DocumentReference notificationRef = getNotificationCollection(entrantId).document(item.getId());

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot waitSnapshot = transaction.get(waitRef);
            if (!waitSnapshot.exists()) {
                throw new IllegalStateException("Invitation no longer exists");
            }

            String status = waitSnapshot.getString("status");
            if (!"selected".equals(status)) {
                throw new IllegalStateException("Invitation is no longer available");
            }

            transaction.update(waitRef,
                    "status", "enrolled",
                    "respondedAt", FieldValue.serverTimestamp());
            transaction.update(eventRef,
                    "selectedCount", FieldValue.increment(-1),
                    "enrolledCount", FieldValue.increment(1));
            transaction.update(notificationRef,
                    "unread", false,
                    "actionRequired", false,
                    "actionStatus", NotificationItem.ACTION_ACCEPTED,
                    "respondedAt", FieldValue.serverTimestamp());
            return null;
        });
    }

    public Task<Void> declineInvitation(String entrantId, NotificationItem item) {
        if (item == null || item.getEventId() == null || item.getId() == null) {
            return Tasks.forException(new IllegalArgumentException("Missing notification data"));
        }

        DocumentReference eventRef = db.collection("events").document(item.getEventId());
        DocumentReference waitRef = eventRef.collection("waitingList").document(entrantId);
        DocumentReference notificationRef = getNotificationCollection(entrantId).document(item.getId());

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot waitSnapshot = transaction.get(waitRef);
            if (!waitSnapshot.exists()) {
                throw new IllegalStateException("Invitation no longer exists");
            }

            String status = waitSnapshot.getString("status");
            if (!"selected".equals(status)) {
                throw new IllegalStateException("Invitation is no longer available");
            }

            transaction.update(waitRef,
                    "status", "declined",
                    "respondedAt", FieldValue.serverTimestamp());
            transaction.update(eventRef,
                    "selectedCount", FieldValue.increment(-1));
            transaction.update(notificationRef,
                    "unread", false,
                    "actionRequired", false,
                    "actionStatus", NotificationItem.ACTION_DECLINED,
                    "respondedAt", FieldValue.serverTimestamp());
            return null;
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Exception exception = task.getException();
                if (exception != null) {
                    throw exception;
                }
                throw new IllegalStateException("Failed to decline invitation");
            }
            return new WaitlistController().drawReplacement(item.getEventId()).continueWith(replacementTask -> null);
        });
    }

    public CollectionReference getNotificationCollection(String entrantId) {
        return db.collection("entrants")
                .document(entrantId)
                .collection("notifications");
    }

    public NotificationItem fromDocument(DocumentSnapshot doc) {
        NotificationItem item = doc.toObject(NotificationItem.class);
        if (item == null) {
            item = new NotificationItem();
        }

        item.setId(doc.getId());
        if (item.getEntrantId() == null || item.getEntrantId().trim().isEmpty()) {
            item.setEntrantId(doc.getString("entrantId"));
        }
        if (item.getEventId() == null || item.getEventId().trim().isEmpty()) {
            item.setEventId(doc.getString("eventId"));
        }
        if (item.getEventName() == null || item.getEventName().trim().isEmpty()) {
            item.setEventName(doc.getString("eventName"));
        }
        if (item.getType() == null || item.getType().trim().isEmpty()) {
            item.setType(doc.getString("type"));
        }
        if (item.getMessage() == null || item.getMessage().trim().isEmpty()) {
            item.setMessage(doc.getString("message"));
        }
        if (item.getActionStatus() == null || item.getActionStatus().trim().isEmpty()) {
            item.setActionStatus(NotificationItem.ACTION_NONE);
        }
        return item;
    }

    public static Map<String, Object> buildSelectedNotificationData(String entrantId,
                                                                    String eventId,
                                                                    String eventName) {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", entrantId);
        data.put("eventId", eventId);
        data.put("eventName", eventName != null ? eventName : "");
        data.put("type", NotificationItem.TYPE_SELECTED);
        data.put("message", NotificationActionHelper.buildSelectedMessage(eventName));
        data.put("unread", true);
        data.put("actionRequired", true);
        data.put("actionStatus", NotificationItem.ACTION_PENDING);
        data.put("createdAt", FieldValue.serverTimestamp());
        return data;
    }

    public static Map<String, Object> buildNotSelectedNotificationData(String entrantId,
                                                                       String eventId,
                                                                       String eventName) {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", entrantId);
        data.put("eventId", eventId);
        data.put("eventName", eventName != null ? eventName : "");
        data.put("type", NotificationItem.TYPE_NOT_SELECTED);
        data.put("message", NotificationActionHelper.buildNotSelectedMessage(eventName));
        data.put("unread", true);
        data.put("actionRequired", false);
        data.put("actionStatus", NotificationItem.ACTION_NONE);
        data.put("createdAt", FieldValue.serverTimestamp());
        return data;
    }

    public static void addSelectedNotificationToBatch(WriteBatch batch,
                                                      FirebaseFirestore db,
                                                      String entrantId,
                                                      String eventId,
                                                      String eventName) {
        if (entrantId == null || entrantId.trim().isEmpty()) {
            return;
        }
        DocumentReference ref = db.collection("entrants")
                .document(entrantId)
                .collection("notifications")
                .document();
        batch.set(ref, buildSelectedNotificationData(entrantId, eventId, eventName));
    }

    public static void addNotSelectedNotificationToBatch(WriteBatch batch,
                                                         FirebaseFirestore db,
                                                         String entrantId,
                                                         String eventId,
                                                         String eventName) {
        if (entrantId == null || entrantId.trim().isEmpty()) {
            return;
        }
        DocumentReference ref = db.collection("entrants")
                .document(entrantId)
                .collection("notifications")
                .document();
        batch.set(ref, buildNotSelectedNotificationData(entrantId, eventId, eventName));
    }
}
