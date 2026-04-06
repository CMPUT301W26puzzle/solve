package com.example.eventparticipation;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
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
 * Controller handling waitlist business logic for an event.
 *
 * <p>This class is responsible for:
 * <ul>
 * <li>Running the lottery to move entrants from waiting to selected</li>
 * <li>Drawing a replacement entrant when a spot becomes available</li>
 * <li>Synchronizing derived event counts back to the top-level event document</li>
 * </ul>
 *
 * <p>Status model used in waitlist documents:
 * <ul>
 * <li>selectionStatus: waiting / selected / cancelled</li>
 * <li>responseStatus: pending / accepted / declined</li>
 * <li>finalStatus: enrolled</li>
 * </ul>
 *
 * <p>Count model used in the top-level event document:
 * <ul>
 * <li>waitingCount: number of waitlist docs with selectionStatus == waiting</li>
 * <li>selectedCount: number of waitlist docs with selectionStatus == selected and finalStatus != enrolled</li>
 * <li>enrolledCount: number of waitlist docs with finalStatus == enrolled</li>
 * </ul>
 */
public class WaitlistController {

    /** Firestore instance used for all event and waitlist operations. */
    private final FirebaseFirestore db;

    /**
     * Creates a controller using the default Firestore instance.
     */
    public WaitlistController() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Creates a controller with an injected Firestore instance.
     *
     * @param injectedDb Firestore instance to use
     */
    public WaitlistController(FirebaseFirestore injectedDb) {
        this.db = injectedDb;
    }

    /**
     * Runs a lottery for the specified event.
     *
     * <p>The method:
     * <ol>
     * <li>Loads all entrants currently marked as {@code waiting}</li>
     * <li>Randomly selects up to {@code sampleSize} winners</li>
     * <li>Marks winners as {@code selected} with {@code responseStatus = pending}</li>
     * <li>Sends notifications where applicable</li>
     * <li>Recomputes and persists accurate event counts</li>
     * </ol>
     *
     * @param eventId the Firestore ID of the event
     * @param sampleSize the maximum number of waiting entrants to select
     * @return a task that completes when the lottery and count synchronization finish
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
                return syncEventCounts(eventId);
            }

            Collections.shuffle(waitingEntrants);

            int winnersCount = Math.min(sampleSize, waitingEntrants.size());
            String eventName = eventSnapshot != null ? safe(eventSnapshot.getString("name")) : "";

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
                    batch.update(
                            entrantSnapshot.getReference(),
                            "selectionStatus", "selected",
                            "responseStatus", "pending",
                            "finalStatus", null,
                            "selectedAt", FieldValue.serverTimestamp()
                    );

                    if (!isOptedOut) {
                        NotificationRepository.addSelectedNotificationToBatch(
                                batch, db, entrantId, eventId, eventName
                        );
                    }
                }
            }

            return batch.commit().continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Exception exception = task.getException();
                    if (exception != null) {
                        throw exception;
                    }
                    throw new IllegalStateException("Failed to commit lottery updates");
                }
                return syncEventCounts(eventId);
            });
        });
    }

    /**
     * Draws exactly one replacement entrant from the waiting pool.
     *
     * <p>The replacement entrant is moved to:
     * <ul>
     * <li>{@code selectionStatus = selected}</li>
     * <li>{@code responseStatus = pending}</li>
     * <li>{@code finalStatus = null}</li>
     * </ul>
     *
     * <p>After the update, this method recomputes and persists the top-level event counts.
     *
     * @param eventId the Firestore ID of the event
     * @return a task whose result is the selected entrant document ID, or {@code null} if none were available
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
                if (exception != null) {
                    throw exception;
                }
                throw new IllegalStateException("Failed to draw replacement");
            }

            QuerySnapshot waitingSnapshot = waitingTask.getResult();
            if (waitingSnapshot == null || waitingSnapshot.isEmpty()) {
                return Tasks.forResult(null);
            }

            List<DocumentSnapshot> waitingEntrants = new ArrayList<>(waitingSnapshot.getDocuments());
            Collections.shuffle(waitingEntrants);

            DocumentSnapshot replacement = waitingEntrants.get(0);
            String entrantId = resolveEntrantId(replacement);
            String eventName = eventTask.getResult() != null
                    ? safe(eventTask.getResult().getString("name"))
                    : "";

            Boolean optOut = replacement.getBoolean("optOutNotifications");
            boolean isOptedOut = optOut != null && optOut;

            WriteBatch batch = db.batch();
            batch.update(
                    replacement.getReference(),
                    "selectionStatus", "selected",
                    "responseStatus", "pending",
                    "finalStatus", null,
                    "selectedAt", FieldValue.serverTimestamp()
            );

            if (!isOptedOut) {
                NotificationRepository.addSelectedNotificationToBatch(
                        batch, db, entrantId, eventId, eventName
                );
            }

            return batch.commit().continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Exception exception = task.getException();
                    if (exception != null) {
                        throw exception;
                    }
                    throw new IllegalStateException("Failed to commit replacement update");
                }

                return syncEventCounts(eventId).continueWith(syncTask -> {
                    if (!syncTask.isSuccessful()) {
                        Exception exception = syncTask.getException();
                        if (exception != null) {
                            throw new RuntimeException(exception);
                        }
                        throw new RuntimeException("Failed to sync event counts");
                    }
                    return replacement.getId();
                });
            });
        });
    }

    /**
     * Recomputes the derived counts for an event from the authoritative waitlist subcollection.
     *
     * <p>This method scans all documents under {@code events/{eventId}/waitlist} and writes
     * the following values back to the top-level event document:
     * <ul>
     * <li>{@code waitingCount}</li>
     * <li>{@code selectedCount}</li>
     * <li>{@code enrolledCount}</li>
     * </ul>
     *
     * <p>This should be called after any operation that changes waitlist status, including:
     * <ul>
     * <li>lottery runs</li>
     * <li>replacement draws</li>
     * <li>accepting an invitation</li>
     * <li>declining an invitation</li>
     * <li>leaving the waitlist</li>
     * <li>promoting an entrant to co-organizer and removing them from waitlist</li>
     * </ul>
     *
     * @param eventId the Firestore ID of the event
     * @return a task that completes when the event document has been updated
     */
    public Task<Void> syncEventCounts(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Missing event id"));
        }

        TaskCompletionSource<Void> taskSource = new TaskCompletionSource<>();
        DocumentReference eventRef = db.collection("events").document(eventId);

        eventRef.collection("waitlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int waiting = 0;
                    int selected = 0;
                    int enrolled = 0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String selectionStatus = safe(doc.getString("selectionStatus")).toLowerCase();
                        String finalStatus = safe(doc.getString("finalStatus")).toLowerCase();

                        if ("enrolled".equals(finalStatus)) {
                            enrolled++;
                        } else if ("selected".equals(selectionStatus)) {
                            selected++;
                        } else if ("waiting".equals(selectionStatus)) {
                            waiting++;
                        }
                    }

                    eventRef.update(
                                    "waitingCount", waiting,
                                    "selectedCount", selected,
                                    "enrolledCount", enrolled
                            )
                            .addOnSuccessListener(unused -> taskSource.setResult(null))
                            .addOnFailureListener(taskSource::setException);
                })
                .addOnFailureListener(taskSource::setException);

        return taskSource.getTask();
    }

    /**
     * Resolves the logical entrant ID from a waitlist document.
     *
     * <p>The method tries multiple fields in order:
     * <ol>
     * <li>{@code entrantId}</li>
     * <li>{@code deviceId}</li>
     * <li>document ID</li>
     * </ol>
     *
     * @param entrantSnapshot the waitlist document snapshot
     * @return the best available entrant identifier
     */
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

    /**
     * Returns a non-null string for null-safe comparisons.
     *
     * @param value the input string
     * @return the original string, or an empty string if null
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }
}