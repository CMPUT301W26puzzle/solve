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
 * <p>Status model used by regular event invitation actions:
 * <ul>
 *     <li>selectionStatus: waiting / selected / cancelled</li>
 *     <li>responseStatus: pending / accepted / declined</li>
 *     <li>finalStatus: enrolled</li>
 * </ul>
 *
 * <p>Co-organizer invitations are handled separately from regular event invitations.
 * A co-organizer invitation must be accepted before the entrant is actually promoted
 * to co-organizer and removed from the waitlist.</p>
 */
public class NotificationRepository {

    /**
     * Result wrapper for sending a co-organizer invitation.
     */
    public static class CoOrganizerInvitationResult {
        public static final String STATUS_SENT = "sent";
        public static final String STATUS_ALREADY_PENDING = "already_pending";

        private final String status;

        /**
         * Creates a new send result.
         *
         * @param status result status
         */
        public CoOrganizerInvitationResult(String status) {
            this.status = status;
        }

        /**
         * Returns the result status.
         *
         * @return result status string
         */
        public String getStatus() {
            return status;
        }

        /**
         * Returns whether a new invitation was sent.
         *
         * @return true if invitation was sent
         */
        public boolean isSent() {
            return STATUS_SENT.equals(status);
        }

        /**
         * Returns whether a pending invitation already existed.
         *
         * @return true if a pending invitation already exists
         */
        public boolean isAlreadyPending() {
            return STATUS_ALREADY_PENDING.equals(status);
        }
    }

    /**
     * Listener used by the notifications screen to receive live updates.
     */
    public interface NotificationListener {

        /**
         * Called when the latest notification list is available.
         *
         * @param items current notifications
         */
        void onNotificationsChanged(List<NotificationItem> items);

        /**
         * Called when notification loading fails.
         *
         * @param e failure
         */
        void onError(Exception e);
    }

    private final FirebaseFirestore db;

    /**
     * Creates a repository backed by the default Firestore instance.
     */
    public NotificationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Creates a repository backed by the provided Firestore instance.
     *
     * @param db firestore instance
     */
    public NotificationRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Starts listening for entrant notifications ordered by newest first.
     *
     * @param entrantId entrant id
     * @param listener callback listener
     * @return Firestore listener registration
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
     * Marks a notification as read.
     *
     * @param entrantId entrant id
     * @param notificationId notification id
     * @return task completing when the update finishes
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
     * Accepts a regular event invitation.
     *
     * <p>State changes:
     * <ul>
     *     <li>selectionStatus remains selected</li>
     *     <li>responseStatus becomes accepted</li>
     *     <li>finalStatus becomes enrolled</li>
     * </ul>
     *
     * @param entrantId entrant id
     * @param item notification being accepted
     * @return task completing when the transaction finishes
     */
    public Task<Void> acceptInvitation(String entrantId, NotificationItem item) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || item == null
                || item.getEventId() == null || item.getEventId().trim().isEmpty()
                || item.getId() == null || item.getId().trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Missing notification data"));
        }

        DocumentReference eventRef = db.collection("events").document(item.getEventId());
        DocumentReference waitRef = eventRef.collection("waitlist").document(entrantId);
        DocumentReference notificationRef = getNotificationCollection(entrantId).document(item.getId());

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot waitSnapshot = transaction.get(waitRef);
            if (!waitSnapshot.exists()) {
                throw new IllegalStateException("Invitation no longer exists");
            }

            String selectionStatus = safe(waitSnapshot.getString("selectionStatus"));
            String responseStatus = safe(waitSnapshot.getString("responseStatus"));
            String finalStatus = safe(waitSnapshot.getString("finalStatus"));

            if (!"selected".equals(selectionStatus)) {
                throw new IllegalStateException("Invitation is no longer available");
            }

            if ("enrolled".equals(finalStatus)) {
                throw new IllegalStateException("Already enrolled");
            }

            if ("declined".equals(responseStatus)) {
                throw new IllegalStateException("Invitation has already been declined");
            }

            transaction.update(waitRef,
                    "selectionStatus", "selected",
                    "responseStatus", "accepted",
                    "finalStatus", "enrolled",
                    "respondedAt", FieldValue.serverTimestamp(),
                    "enrolledAt", FieldValue.serverTimestamp());

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
     * Declines a regular event invitation.
     *
     * <p>State changes:
     * <ul>
     *     <li>selectionStatus remains selected</li>
     *     <li>responseStatus becomes declined</li>
     *     <li>finalStatus remains null / empty</li>
     * </ul>
     *
     * @param entrantId entrant id
     * @param item notification being declined
     * @return task completing when the transaction finishes
     */
    public Task<Void> declineInvitation(String entrantId, NotificationItem item) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || item == null
                || item.getEventId() == null || item.getEventId().trim().isEmpty()
                || item.getId() == null || item.getId().trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Missing notification data"));
        }

        DocumentReference eventRef = db.collection("events").document(item.getEventId());
        DocumentReference waitRef = eventRef.collection("waitlist").document(entrantId);
        DocumentReference notificationRef = getNotificationCollection(entrantId).document(item.getId());

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot waitSnapshot = transaction.get(waitRef);
            if (!waitSnapshot.exists()) {
                throw new IllegalStateException("Invitation no longer exists");
            }

            String selectionStatus = safe(waitSnapshot.getString("selectionStatus"));
            String responseStatus = safe(waitSnapshot.getString("responseStatus"));
            String finalStatus = safe(waitSnapshot.getString("finalStatus"));

            if (!"selected".equals(selectionStatus)) {
                throw new IllegalStateException("Invitation is no longer available");
            }

            if ("enrolled".equals(finalStatus)) {
                throw new IllegalStateException("Already enrolled");
            }

            if ("declined".equals(responseStatus)) {
                throw new IllegalStateException("Invitation has already been declined");
            }

            transaction.update(waitRef,
                    "selectionStatus", "selected",
                    "responseStatus", "declined",
                    "finalStatus", null,
                    "respondedAt", FieldValue.serverTimestamp());

            transaction.update(eventRef,
                    "selectedCount", FieldValue.increment(-1));

            transaction.update(notificationRef,
                    "unread", false,
                    "actionRequired", false,
                    "actionStatus", NotificationItem.ACTION_DECLINED,
                    "respondedAt", FieldValue.serverTimestamp());

            return null;
        });
    }

    /**
     * Returns whether the entrant currently has a pending co-organizer invitation
     * for the given event.
     *
     * @param entrantId entrant id
     * @param eventId event id
     * @return task resolving to true when a pending invitation already exists
     */
    public Task<Boolean> hasPendingCoOrganizerInvitation(String entrantId, String eventId) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || eventId == null || eventId.trim().isEmpty()) {
            return Tasks.forResult(false);
        }

        DocumentReference notificationRef = getCoOrganizerInvitationNotificationRef(entrantId, eventId);

        return notificationRef.get().continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception exception = task.getException();
                if (exception != null) {
                    throw exception;
                }
                return false;
            }

            DocumentSnapshot snapshot = task.getResult();
            if (snapshot == null || !snapshot.exists()) {
                return false;
            }

            String type = safe(snapshot.getString("type"));
            String actionStatus = safe(snapshot.getString("actionStatus"));

            return NotificationItem.TYPE_COORGANIZER_INVITATION.equals(type)
                    && NotificationItem.ACTION_PENDING.equals(actionStatus);
        });
    }

    /**
     * Sends a co-organizer invitation if there is no pending invitation yet
     * for the same entrant and event.
     *
     * <p>Behavior:
     * <ul>
     *     <li>If no invitation document exists, create a pending invitation.</li>
     *     <li>If a pending invitation already exists, do not overwrite it.</li>
     *     <li>If an old invitation exists but is accepted or declined, overwrite it
     *     with a fresh pending invitation.</li>
     * </ul>
     *
     * <p>This method is idempotent for the pending state because it uses a fixed
     * notification document id per entrant and event.</p>
     *
     * @param entrantId entrant receiving the invitation
     * @param eventId target event id
     * @param eventName target event name
     * @return task containing the send result
     */
    public Task<CoOrganizerInvitationResult> sendCoOrganizerInvitation(String entrantId,
                                                                       String eventId,
                                                                       String eventName) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || eventId == null || eventId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Missing invitation data"));
        }

        DocumentReference notificationRef = getCoOrganizerInvitationNotificationRef(entrantId, eventId);

        return db.runTransaction((Transaction.Function<CoOrganizerInvitationResult>) transaction -> {
            DocumentSnapshot existingSnapshot = transaction.get(notificationRef);

            if (existingSnapshot.exists()) {
                String existingType = safe(existingSnapshot.getString("type"));
                String existingActionStatus = safe(existingSnapshot.getString("actionStatus"));

                if (NotificationItem.TYPE_COORGANIZER_INVITATION.equals(existingType)
                        && NotificationItem.ACTION_PENDING.equals(existingActionStatus)) {
                    return new CoOrganizerInvitationResult(
                            CoOrganizerInvitationResult.STATUS_ALREADY_PENDING
                    );
                }
            }

            transaction.set(
                    notificationRef,
                    buildCoOrganizerInvitationNotificationData(entrantId, eventId, eventName)
            );

            return new CoOrganizerInvitationResult(
                    CoOrganizerInvitationResult.STATUS_SENT
            );
        });
    }

    /**
     * Accepts a co-organizer invitation.
     *
     * <p>On acceptance:
     * <ul>
     *     <li>entrant id is added to events/{eventId}.coOrganizerIds</li>
     *     <li>entrant is removed from events/{eventId}/waitlist/{entrantId}</li>
     *     <li>notification action status becomes accepted</li>
     * </ul>
     *
     * <p>This is separate from {@link #acceptInvitation(String, NotificationItem)}
     * because accepting a co-organizer invitation does not enroll the entrant into
     * the event.</p>
     *
     * @param entrantId entrant id
     * @param item notification being accepted
     * @return task completing when the transaction finishes
     */
    public Task<Void> acceptCoOrganizerInvitation(String entrantId, NotificationItem item) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || item == null
                || item.getEventId() == null || item.getEventId().trim().isEmpty()
                || item.getId() == null || item.getId().trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Missing notification data"));
        }

        DocumentReference eventRef = db.collection("events").document(item.getEventId());
        DocumentReference waitRef = eventRef.collection("waitlist").document(entrantId);
        DocumentReference notificationRef = getNotificationCollection(entrantId).document(item.getId());

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot eventSnapshot = transaction.get(eventRef);
            if (!eventSnapshot.exists()) {
                throw new IllegalStateException("Event no longer exists");
            }

            DocumentSnapshot waitSnapshot = transaction.get(waitRef);
            if (!waitSnapshot.exists()) {
                throw new IllegalStateException("Co-organizer invitation is no longer available");
            }

            DocumentSnapshot notificationSnapshot = transaction.get(notificationRef);
            if (!notificationSnapshot.exists()) {
                throw new IllegalStateException("Notification no longer exists");
            }

            String type = safe(notificationSnapshot.getString("type"));
            String actionStatus = safe(notificationSnapshot.getString("actionStatus"));

            if (!NotificationItem.TYPE_COORGANIZER_INVITATION.equals(type)) {
                throw new IllegalStateException("Notification is not a co-organizer invitation");
            }

            if (NotificationItem.ACTION_ACCEPTED.equals(actionStatus)) {
                throw new IllegalStateException("Co-organizer invitation already accepted");
            }

            if (NotificationItem.ACTION_DECLINED.equals(actionStatus)) {
                throw new IllegalStateException("Co-organizer invitation already declined");
            }

            String selectionStatus = safe(waitSnapshot.getString("selectionStatus"));
            String finalStatus = safe(waitSnapshot.getString("finalStatus"));

            Map<String, Object> eventUpdates = new HashMap<>();
            eventUpdates.put("coOrganizerIds", FieldValue.arrayUnion(entrantId));

            if ("enrolled".equals(finalStatus)) {
                eventUpdates.put("enrolledCount", FieldValue.increment(-1));
            } else if ("selected".equals(selectionStatus)) {
                eventUpdates.put("selectedCount", FieldValue.increment(-1));
            } else if ("waiting".equals(selectionStatus)) {
                eventUpdates.put("waitingCount", FieldValue.increment(-1));
            }

            transaction.update(eventRef, eventUpdates);
            transaction.delete(waitRef);

            transaction.update(notificationRef,
                    "unread", false,
                    "actionRequired", false,
                    "actionStatus", NotificationItem.ACTION_ACCEPTED,
                    "respondedAt", FieldValue.serverTimestamp());

            return null;
        });
    }

    /**
     * Declines a co-organizer invitation.
     *
     * <p>On decline:
     * <ul>
     *     <li>waitlist data remains unchanged</li>
     *     <li>notification action status becomes declined</li>
     * </ul>
     *
     * @param entrantId entrant id
     * @param item notification being declined
     * @return task completing when the transaction finishes
     */
    public Task<Void> declineCoOrganizerInvitation(String entrantId, NotificationItem item) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || item == null
                || item.getId() == null || item.getId().trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Missing notification data"));
        }

        DocumentReference notificationRef = getNotificationCollection(entrantId).document(item.getId());

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot notificationSnapshot = transaction.get(notificationRef);
            if (!notificationSnapshot.exists()) {
                throw new IllegalStateException("Notification no longer exists");
            }

            String type = safe(notificationSnapshot.getString("type"));
            String actionStatus = safe(notificationSnapshot.getString("actionStatus"));

            if (!NotificationItem.TYPE_COORGANIZER_INVITATION.equals(type)) {
                throw new IllegalStateException("Notification is not a co-organizer invitation");
            }

            if (NotificationItem.ACTION_ACCEPTED.equals(actionStatus)) {
                throw new IllegalStateException("Co-organizer invitation already accepted");
            }

            if (NotificationItem.ACTION_DECLINED.equals(actionStatus)) {
                throw new IllegalStateException("Co-organizer invitation already declined");
            }

            transaction.update(notificationRef,
                    "unread", false,
                    "actionRequired", false,
                    "actionStatus", NotificationItem.ACTION_DECLINED,
                    "respondedAt", FieldValue.serverTimestamp());

            return null;
        });
    }

    /**
     * Returns the notifications subcollection for an entrant.
     *
     * @param entrantId entrant id
     * @return notification collection reference
     */
    public CollectionReference getNotificationCollection(String entrantId) {
        return db.collection("entrants")
                .document(entrantId)
                .collection("notifications");
    }

    /**
     * Returns the fixed notification document reference used for one
     * co-organizer invitation per event per entrant.
     *
     * @param entrantId entrant id
     * @param eventId event id
     * @return fixed document reference
     */
    public DocumentReference getCoOrganizerInvitationNotificationRef(String entrantId, String eventId) {
        return getNotificationCollection(entrantId)
                .document(buildCoOrganizerInvitationNotificationId(eventId));
    }

    /**
     * Builds the fixed document id used for co-organizer invitation notifications.
     *
     * @param eventId event id
     * @return fixed notification document id
     */
    public static String buildCoOrganizerInvitationNotificationId(String eventId) {
        return "coorganizer_invitation_" + safe(eventId);
    }

    /**
     * Converts a Firestore document into a {@link NotificationItem}.
     *
     * @param doc firestore document
     * @return populated notification item
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
     * Builds notification data for a regular selected invitation.
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
     * Builds notification data for a not-selected informational notification.
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
     * Builds notification data for a co-organizer assigned informational notification.
     *
     * <p>This is kept for compatibility, though the current flow uses
     * co-organizer invitations first.</p>
     */
    public static Map<String, Object> buildCoOrganizerAssignedNotificationData(String entrantId,
                                                                               String eventId,
                                                                               String eventName) {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", entrantId);
        data.put("eventId", eventId);
        data.put("eventName", eventName != null ? eventName : "");
        data.put("type", NotificationItem.TYPE_COORGANIZER_ASSIGNED);
        data.put("message", NotificationActionHelper.buildCoOrganizerAssignedMessage(eventName));
        data.put("unread", true);
        data.put("actionRequired", false);
        data.put("actionStatus", NotificationItem.ACTION_NONE);
        data.put("createdAt", FieldValue.serverTimestamp());
        return data;
    }

    /**
     * Builds notification data for a co-organizer invitation.
     */
    public static Map<String, Object> buildCoOrganizerInvitationNotificationData(String entrantId,
                                                                                 String eventId,
                                                                                 String eventName) {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", entrantId);
        data.put("eventId", eventId);
        data.put("eventName", eventName != null ? eventName : "");
        data.put("type", NotificationItem.TYPE_COORGANIZER_INVITATION);
        data.put("message", NotificationActionHelper.buildCoOrganizerInvitationMessage(eventName));
        data.put("unread", true);
        data.put("actionRequired", true);
        data.put("actionStatus", NotificationItem.ACTION_PENDING);
        data.put("createdAt", FieldValue.serverTimestamp());
        return data;
    }

    /**
     * Adds a selected notification to a batch.
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
     * Adds a not-selected notification to a batch.
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

    /**
     * Adds a co-organizer assigned notification to a batch.
     */
    public static void addCoOrganizerAssignedNotificationToBatch(WriteBatch batch,
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
        batch.set(ref, buildCoOrganizerAssignedNotificationData(entrantId, eventId, eventName));
    }

    /**
     * Adds a co-organizer invitation notification to a batch using a fixed document id.
     *
     * <p>This helper is deterministic but does not itself block duplicate pending invitations.
     * For duplicate-safe invitation sending, prefer
     * {@link #sendCoOrganizerInvitation(String, String, String)}.</p>
     */
    public static void addCoOrganizerInvitationNotificationToBatch(WriteBatch batch,
                                                                   FirebaseFirestore db,
                                                                   String entrantId,
                                                                   String eventId,
                                                                   String eventName) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || eventId == null || eventId.trim().isEmpty()) {
            return;
        }

        DocumentReference ref = db.collection("entrants")
                .document(entrantId)
                .collection("notifications")
                .document(buildCoOrganizerInvitationNotificationId(eventId));

        batch.set(ref, buildCoOrganizerInvitationNotificationData(entrantId, eventId, eventName));
    }

    // -------------------------------------------------------------------------
    // Private event invitations
    // -------------------------------------------------------------------------

    /**
     * Builds the fixed notification document id used for one private invite
     * per entrant per event, preventing duplicate pending notifications.
     *
     * @param eventId event id
     * @return fixed notification document id
     */
    public static String buildPrivateInviteNotificationId(String eventId) {
        return "private_invite_" + safe(eventId);
    }

    /**
     * Returns the fixed notification document reference for a private invite.
     *
     * @param entrantId entrant id
     * @param eventId   event id
     * @return fixed document reference
     */
    public DocumentReference getPrivateInviteNotificationRef(String entrantId, String eventId) {
        return getNotificationCollection(entrantId)
                .document(buildPrivateInviteNotificationId(eventId));
    }

    /**
     * Builds the Firestore map for a private event invitation notification.
     * The notification requires an action (accept / decline) by the entrant.
     *
     * @param entrantId entrant id
     * @param eventId   event id
     * @param eventName event display name
     * @return notification data map
     */
    public static Map<String, Object> buildPrivateInviteNotificationData(String entrantId,
                                                                         String eventId,
                                                                         String eventName) {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", entrantId);
        data.put("eventId", eventId);
        data.put("eventName", eventName != null ? eventName : "");
        data.put("type", NotificationItem.TYPE_PRIVATE_INVITE);
        data.put("message", "You have been personally invited to \""
                + (eventName != null ? eventName : "an event")
                + "\". Please accept or decline.");
        data.put("unread", true);
        data.put("actionRequired", true);
        data.put("actionStatus", NotificationItem.ACTION_PENDING);
        data.put("createdAt", FieldValue.serverTimestamp());
        return data;
    }

    /**
     * Sends a private event invitation notification if no pending invitation
     * already exists for this entrant and event.
     *
     * <p>Behaviour mirrors {@link #sendCoOrganizerInvitation}: if a pending
     * invite already exists it is left untouched; if an old accepted / declined
     * invite exists it is overwritten with a fresh pending one.</p>
     *
     * @param entrantId entrant receiving the invite
     * @param eventId   target event id
     * @param eventName target event name
     * @return task containing the send result
     */
    public Task<CoOrganizerInvitationResult> sendPrivateInvitation(String entrantId,
                                                                   String eventId,
                                                                   String eventName) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || eventId == null || eventId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Missing invitation data"));
        }

        DocumentReference notificationRef = getPrivateInviteNotificationRef(entrantId, eventId);

        return db.runTransaction((Transaction.Function<CoOrganizerInvitationResult>) transaction -> {
            DocumentSnapshot existing = transaction.get(notificationRef);

            if (existing.exists()) {
                String existingType   = safe(existing.getString("type"));
                String existingStatus = safe(existing.getString("actionStatus"));

                if (NotificationItem.TYPE_PRIVATE_INVITE.equals(existingType)
                        && NotificationItem.ACTION_PENDING.equals(existingStatus)) {
                    return new CoOrganizerInvitationResult(
                            CoOrganizerInvitationResult.STATUS_ALREADY_PENDING);
                }
            }

            transaction.set(notificationRef,
                    buildPrivateInviteNotificationData(entrantId, eventId, eventName));

            return new CoOrganizerInvitationResult(CoOrganizerInvitationResult.STATUS_SENT);
        });
    }

    /**
     * Adds a private invite notification to an existing write batch using a
     * fixed document id (idempotent within the batch).
     *
     * <p>For full duplicate-safe sending prefer
     * {@link #sendPrivateInvitation(String, String, String)}.</p>
     *
     * @param batch     write batch to add the operation to
     * @param db        firestore instance
     * @param entrantId entrant receiving the invite
     * @param eventId   target event id
     * @param eventName target event name
     */
    public static void addPrivateInviteNotificationToBatch(WriteBatch batch,
                                                           FirebaseFirestore db,
                                                           String entrantId,
                                                           String eventId,
                                                           String eventName) {
        if (entrantId == null || entrantId.trim().isEmpty()
                || eventId == null || eventId.trim().isEmpty()) {
            return;
        }

        DocumentReference ref = db.collection("entrants")
                .document(entrantId)
                .collection("notifications")
                .document(buildPrivateInviteNotificationId(eventId));

        batch.set(ref, buildPrivateInviteNotificationData(entrantId, eventId, eventName));
    }

    // -------------------------------------------------------------------------

    /**
     * Returns a non-null string for null-safe comparisons.
     *
     * @param value input string
     * @return original string or empty string if null
     */
    private static String safe(String value) {
        return value == null ? "" : value;
    }
}