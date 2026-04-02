
package com.example.eventparticipation;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controller handling Waitlist business logic, including the Lottery system.
 *
 * Status model:
 * - selectionStatus: waiting / selected / cancelled
 * - responseStatus: pending / accepted / declined
 * - finalStatus: enrolled
 */
public class WaitlistController {

    private final FirebaseFirestore db;

    public WaitlistController() {
        this.db = FirebaseFirestore.getInstance();
    }

    public WaitlistController(FirebaseFirestore injectedDb) {
        this.db = injectedDb;
    }

    /**
     * Runs the lottery to randomly select a specified number of entrants from the waiting pool.
     *
     * Selected entrants become:
     * - selectionStatus = selected
     * - responseStatus = pending
     * - finalStatus = null
     */
    public Task<Void> runLottery(String eventId, int sampleSize) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Missing event id"));
        }
        if (sampleSize <= 0) {
            return Tasks.forException(new IllegalArgumentException("Lottery size must be at least 1"));
        }

        DocumentReference eventRef = db.collection("events").document(eventId);
        Task<DocumentSnapshot> eventTask = eventRef.get();

        Task<QuerySnapshot> waitingTask = eventRef.collection("waitlist")
                .whereEqualTo("selectionStatus", "waiting")
                .get();

        return Tasks.whenAllSuccess(eventTask, waitingTask).continueWithTask(done -> {
            if (!done.isSuccessful()) {
                Exception exception = done.getException();
                if (exception != null) throw exception;
                throw new IllegalStateException("Failed to run lottery");
            }

            DocumentSnapshot eventSnapshot = eventTask.getResult();
            QuerySnapshot waitingSnapshot = waitingTask.getResult();

            if (waitingSnapshot == null) {
                throw new IllegalStateException("Failed to load waiting list");
            }

            List<DocumentSnapshot> waitingEntrants = new ArrayList<>(waitingSnapshot.getDocuments());
            if (waitingEntrants.isEmpty()) {
                return Tasks.forResult(null);
            }

            Collections.shuffle(waitingEntrants);

            int winnersCount = Math.min(sampleSize, waitingEntrants.size());
            String eventName = eventSnapshot != null ? eventSnapshot.getString("name") : "";

            WriteBatch batch = db.batch();

            for (int i = 0; i < waitingEntrants.size(); i++) {
                DocumentSnapshot entrantSnapshot = waitingEntrants.get(i);
                String entrantId = resolveEntrantId(entrantSnapshot);
                if (entrantId == null || entrantId.trim().isEmpty()) {
                    continue;
                }

                Boolean optOut = entrantSnapshot.getBoolean("optOutNotifications");
                boolean isOptedOut = optOut != null && optOut;

                if (i < winnersCount) {
                    batch.update(entrantSnapshot.getReference(),
                            "selectionStatus", "selected",
                            "responseStatus", "pending",
                            "finalStatus", null,
                            "selectedAt", FieldValue.serverTimestamp());

                    if (!isOptedOut) {
                        NotificationRepository.addSelectedNotificationToBatch(
                                batch, db, entrantId, eventId, eventName
                        );
                    }
                } else {
                    // Keep the rest in waiting.
                    // Optional: notify them that they were not selected in this round.
                    if (!isOptedOut) {
                        NotificationRepository.addNotSelectedNotificationToBatch(
                                batch, db, entrantId, eventId, eventName
                        );
                    }
                }
            }

            batch.update(eventRef,
                    "selectedCount", FieldValue.increment(winnersCount),
                    "waitingCount", FieldValue.increment(-winnersCount));

            return batch.commit();
        });
    }

    /**
     * Draws a single replacement applicant from the waiting pool if a spot opens up.
     *
     * Replacement entrant becomes:
     * - selectionStatus = selected
     * - responseStatus = pending
     * - finalStatus = null
     */
    public Task<String> drawReplacement(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Missing event id"));
        }

        DocumentReference eventRef = db.collection("events").document(eventId);
        Task<DocumentSnapshot> eventTask = eventRef.get();

        Task<QuerySnapshot> waitingTask = eventRef.collection("waitlist")
                .whereEqualTo("selectionStatus", "waiting")
                .get();

        return Tasks.whenAllSuccess(eventTask, waitingTask).continueWithTask(done -> {
            if (!done.isSuccessful()) {
                Exception exception = done.getException();
                if (exception != null) throw exception;
                throw new IllegalStateException("Failed to draw replacement");
            }

            QuerySnapshot waitingSnapshot = waitingTask.getResult();
            if (waitingSnapshot == null || waitingSnapshot.isEmpty()) {
                return Tasks.forResult(null);
            }

            List<DocumentSnapshot> waiting = new ArrayList<>(waitingSnapshot.getDocuments());
            Collections.shuffle(waiting);

            DocumentSnapshot replacement = waiting.get(0);
            String entrantId = resolveEntrantId(replacement);
            String eventName = eventTask.getResult() != null
                    ? eventTask.getResult().getString("name")
                    : "";

            Boolean optOut = replacement.getBoolean("optOutNotifications");
            boolean isOptedOut = optOut != null && optOut;

            WriteBatch batch = db.batch();
            batch.update(replacement.getReference(),
                    "selectionStatus", "selected",
                    "responseStatus", "pending",
                    "finalStatus", null,
                    "selectedAt", FieldValue.serverTimestamp());

            batch.update(eventRef,
                    "selectedCount", FieldValue.increment(1),
                    "waitingCount", FieldValue.increment(-1));

            if (!isOptedOut) {
                NotificationRepository.addSelectedNotificationToBatch(
                        batch, db, entrantId, eventId, eventName
                );
            }

            return batch.commit().continueWith(task -> replacement.getId());
        });
    }

    private String resolveEntrantId(DocumentSnapshot entrantSnapshot) {
        String entrantId = entrantSnapshot.getString("entrantId");
        if (entrantId == null || entrantId.trim().isEmpty()) {
            entrantId = entrantSnapshot.getString("deviceId");
        }
        if (entrantId == null || entrantId.trim().isEmpty()) {
            entrantId = entrantSnapshot.getId();
        }
        return entrantId;
    }
}