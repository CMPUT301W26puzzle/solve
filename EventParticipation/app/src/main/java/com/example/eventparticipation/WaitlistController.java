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
 * <p>Manages the state transitions of entrants within an event's waitlist
 * subcollection. Contains the logic for randomly sampling entrants (The Lottery)
 * and drawing replacement applicants.</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 * <li>US 02.05.02 Sample a specified number of attendees (Lottery)</li>
 * <li>US 02.05.03 Draw a replacement applicant</li>
 * </ul>
 */
public class WaitlistController {
    private final FirebaseFirestore db;

    public WaitlistController() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Runs the lottery to randomly select a specified number of entrants from the waiting pool.
     *
     * @param eventId The ID of the event.
     * @param sampleSize The number of entrants to select.
     * @return A Task that resolves when the batch update completes.
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
        Task<QuerySnapshot> waitingTask = eventRef.collection("waitingList")
                .whereEqualTo("status", "waiting")
                .get();

        return Tasks.whenAllSuccess(eventTask, waitingTask).continueWithTask(done -> {
            if (!done.isSuccessful()) {
                Exception exception = done.getException();
                if (exception != null) {
                    throw exception;
                }
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
// shuffle the list for randomness
            Collections.shuffle(waitingEntrants);
            // pick the winners up to the sample size (or max available)
            int winnersCount = Math.min(sampleSize, waitingEntrants.size());
            String eventName = eventSnapshot != null ? eventSnapshot.getString("name") : "";
// batch update their status to "selected"
            WriteBatch batch = db.batch();
            for (int i = 0; i < waitingEntrants.size(); i++) {
                DocumentSnapshot entrantSnapshot = waitingEntrants.get(i);
                String entrantId = resolveEntrantId(entrantSnapshot);
                if (entrantId == null || entrantId.trim().isEmpty()) {
                    continue;
                }

                if (i < winnersCount) {
                    batch.update(entrantSnapshot.getReference(),
                            "status", "selected",
                            "selectedAt", FieldValue.serverTimestamp());
                    NotificationRepository.addSelectedNotificationToBatch(batch, db, entrantId, eventId, eventName);
                } else {
                    NotificationRepository.addNotSelectedNotificationToBatch(batch, db, entrantId, eventId, eventName);
                }
            }

            batch.update(eventRef,
                    "selectedCount", FieldValue.increment(winnersCount),
                    "waitingCount", FieldValue.increment(-winnersCount));
// optionally update event document counts here
            return batch.commit();
        });
    }

    /**
     * Draws a single replacement applicant from the waiting pool if a spot opens up.
     *
     * @param eventId The ID of the event.
     * @return A Task that resolves to the ID of the newly selected entrant, or null if empty.
     */
    public Task<String> drawReplacement(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Missing event id"));
        }

        DocumentReference eventRef = db.collection("events").document(eventId);
        Task<DocumentSnapshot> eventTask = eventRef.get();
        Task<QuerySnapshot> waitingTask = eventRef.collection("waitingList")
                .whereEqualTo("status", "waiting")
                .get();

        return Tasks.whenAllSuccess(eventTask, waitingTask).continueWithTask(done -> {
            if (!done.isSuccessful()) {
                Exception exception = done.getException();
                if (exception != null) {
                    throw exception;
                }
                throw new IllegalStateException("Failed to draw replacement");
            }

            QuerySnapshot waitingSnapshot = waitingTask.getResult();
            if (waitingSnapshot == null || waitingSnapshot.isEmpty()) {
                return Tasks.forResult(null);
            }

            List<DocumentSnapshot> waiting = new ArrayList<>(waitingSnapshot.getDocuments());
            // shuffle and pick 1
            Collections.shuffle(waiting);
            DocumentSnapshot replacement = waiting.get(0);
            String entrantId = resolveEntrantId(replacement);
            String eventName = eventTask.getResult() != null ? eventTask.getResult().getString("name") : "";

            WriteBatch batch = db.batch();
            batch.update(replacement.getReference(),
                    "status", "selected",
                    "selectedAt", FieldValue.serverTimestamp());
            batch.update(eventRef,
                    "selectedCount", FieldValue.increment(1),
                    "waitingCount", FieldValue.increment(-1));
            NotificationRepository.addSelectedNotificationToBatch(batch, db, entrantId, eventId, eventName);
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
