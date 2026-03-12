package com.example.eventparticipation;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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
        return db.collection("events").document(eventId).collection("waitingList")
                .whereEqualTo("status", "waiting")
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        throw new Exception("Failed to fetch waiting list");
                    }

                    List<DocumentSnapshot> waitingEntrants = new ArrayList<>(task.getResult().getDocuments());

                    // shuffle the list for randomness
                    Collections.shuffle(waitingEntrants);

                    // pick the winners up to the sample size (or max available)
                    int winnersCount = Math.min(sampleSize, waitingEntrants.size());
                    List<DocumentSnapshot> winners = waitingEntrants.subList(0, winnersCount);

                    // batch update their status to "selected"
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot winner : winners) {
                        batch.update(winner.getReference(), "status", "selected");
                    }

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
        return db.collection("events").document(eventId).collection("waitingList")
                .whereEqualTo("status", "waiting")
                .get()
                .continueWithTask(task -> {
                    List<DocumentSnapshot> waiting = task.getResult().getDocuments();
                    if (waiting.isEmpty()) return Tasks.forResult(null);

                    // shuffle and pick 1
                    Collections.shuffle(waiting);
                    DocumentSnapshot replacement = waiting.get(0);

                    return replacement.getReference().update("status", "selected")
                            .continueWith(updateTask -> replacement.getId());
                });
    }
}