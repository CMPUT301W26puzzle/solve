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
 *
 * <p>Handles data operations relating to sending and receiving notifications via Firestore.
 * Supports listening for new notifications, updating unread status, and securely processing
 * entrant invitation responses (accepting or declining) using database transactions.</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 * <li>US 01.04.01 Receive notification when chosen from the waiting list</li>
 * <li>US 01.04.02 Receive notification when not chosen from the waiting list</li>
 * <li>US 02.05.03 Draw a replacement applicant (Triggered upon decline)</li>
 * </ul>
 */
public class NotificationRepository {

    /**
     * Callback interface for listening to real-time notification changes.
     */
    public interface NotificationListener {
        /**
         * Called when the notification list is updated.
         * @param items The updated list of notification items.
         */
        void onNotificationsChanged(List<NotificationItem> items);

        /**
         * Called when an error occurs during the listener execution.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /** Firestore database instance. */
    private final FirebaseFirestore db;

    /**
     * Initializes the repository with the default Firestore instance.
     */
    public NotificationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Initializes the repository with a provided Firestore instance.
     * Useful for dependency injection during unit testing.
     *
     * @param db The Firestore instance to use.
     */
    public NotificationRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Sets up a real-time listener for an entrant's notifications.
     *
     * @param entrantId The unique identifier of the entrant.
     * @param listener The callback to trigger when notifications change.
     * @return A ListenerRegistration object to allow removing the listener later.
     */
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

    /**
     * Marks a specific notification as read.
     *
     * @param entrantId The unique identifier of the entrant.
     * @param notificationId The document ID of the notification.
     * @return A Task representing the asynchronous update operation.
     */
    public Task<Void> markAsRead(String entrantId, String notificationId) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || notificationId == null || notificationId.trim().isEmpty()) {
            return Tasks.forResult(null);
        }

        return getNotificationCollection(entrantId)
                .document(notificationId)
                .update("unread", false);
    }

    /**
     * Processes an entrant accepting an event invitation using a Firestore transaction.
     * Validates the current waitlist status, updates it to enrolled, adjusts event counts,
     * and updates the notification state.
     *
     * @param entrantId The unique identifier of the entrant accepting the invite.
     * @param item The notification item containing the event details.
     * @return A Task representing the asynchronous transaction operation.
     */
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

            // Ensure the entrant is currently in the selected state
            String status = waitSnapshot.getString("status");
            if (!"selected".equals(status)) {
                throw new IllegalStateException("Invitation is no longer available");
            }

            // Atomically update waitlist, event counts, and the notification item itself
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

    /**
     * Processes an entrant declining an event invitation using a Firestore transaction.
     * Validates the status, updates it to declined, adjusts event counts, and subsequently
     * triggers the WaitlistController to draw a replacement applicant.
     *
     * @param entrantId The unique identifier of the entrant declining the invite.
     * @param item The notification item containing the event details.
     * @return A Task representing the asynchronous transaction and subsequent redraw operation.
     */
    /**
     * Processes an entrant declining an event invitation using a Firestore transaction.
     * Validates the status, updates it to declined, adjusts event counts, and subsequently
     * triggers the WaitlistController to draw a replacement applicant.
     *
     * @param entrantId The unique identifier of the entrant declining the invite.
     * @param item The notification item containing the event details.
     * @return A Task representing the asynchronous transaction and subsequent redraw operation.
     */
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

            return new WaitlistController()
                    .drawReplacement(item.getEventId())
                    .continueWith(replacementTask -> null);
        });
    }

    /**
     * Helper method to get a reference to an entrant's notifications subcollection.
     *
     * @param entrantId The unique identifier of the entrant.
     * @return The Firestore CollectionReference to the notifications.
     */
    public CollectionReference getNotificationCollection(String entrantId) {
        return db.collection("entrants")
                .document(entrantId)
                .collection("notifications");
    }

    /**
     * Converts a Firestore DocumentSnapshot into a NotificationItem model.
     * Ensures all fields have safe fallback values if missing in the database.
     *
     * @param doc The DocumentSnapshot retrieved from Firestore.
     * @return A populated NotificationItem instance.
     */
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

    /**
     * Builds a standardized Map containing payload data for a "Selected" notification.
     *
     * @param entrantId The recipient entrant ID.
     * @param eventId The event ID.
     * @param eventName The descriptive name of the event.
     * @return A Map containing the database fields for the notification.
     */
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

    /**
     * Builds a standardized Map containing payload data for a "Not Selected" notification.
     *
     * @param entrantId The recipient entrant ID.
     * @param eventId The event ID.
     * @param eventName The descriptive name of the event.
     * @return A Map containing the database fields for the notification.
     */
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

    /**
     * Adds a "Selected" notification to an active Firestore WriteBatch.
     * Used heavily by the Lottery algorithm.
     *
     * @param batch The active WriteBatch.
     * @param db The Firestore instance.
     * @param entrantId The recipient entrant ID.
     * @param eventId The event ID.
     * @param eventName The descriptive name of the event.
     */
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

    /**
     * Adds a "Not Selected" notification to an active Firestore WriteBatch.
     * Used heavily by the Lottery algorithm.
     *
     * @param batch The active WriteBatch.
     * @param db The Firestore instance.
     * @param entrantId The recipient entrant ID.
     * @param eventId The event ID.
     * @param eventName The descriptive name of the event.
     */
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