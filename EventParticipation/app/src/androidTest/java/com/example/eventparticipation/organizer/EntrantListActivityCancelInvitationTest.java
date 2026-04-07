package com.example.eventparticipation.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for cancelling a selected entrant invitation.
 *
 * <p>User story covered:</p>
 * <ul>
 *     <li>US 02.06.04 - As an organizer, I want to cancel entrants that did
 *     not sign up for the event.</li>
 * </ul>
 *
 * <p>Test purpose:</p>
 * <ul>
 *     <li>Verify that an eligible selected entrant can be cancelled.</li>
 *     <li>Verify that the waitlist record is updated to the cancelled state.</li>
 * </ul>
 *
 * <p>Preconditions:</p>
 * <ul>
 *     <li>A dedicated test event is created before the test.</li>
 *     <li>A dedicated test entrant exists in the waitlist with:
 *         <ul>
 *             <li>selectionStatus = selected</li>
 *             <li>responseStatus = pending</li>
 *             <li>finalStatus not enrolled</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <p>Expected result:</p>
 * <ul>
 *     <li>After the organizer confirms cancellation, selectionStatus becomes cancelled.</li>
 *     <li>responseStatus remains pending.</li>
 *     <li>finalStatus is cleared.</li>
 * </ul>
 *
 * <p>All test-created Firestore data is removed after execution.</p>
 */
@RunWith(AndroidJUnit4.class)
public class EntrantListActivityCancelInvitationTest {

    private static final String TEST_EVENT_ID = "test_event_cancel_001";
    private static final String TEST_ORGANIZER_ID = "organizer_demo_001";
    private static final String TEST_ENTRANT_ID = "entrant_test_cancel";
    private static final String TEST_ENTRANT_NAME = "Entrant Cancel Test";
    private static final String TEST_ENTRANT_EMAIL = "entrant_cancel_test@test.com";

    private FirebaseFirestore db;

    /**
     * Creates a dedicated test event and seeds a selected pending entrant
     * eligible for cancellation.
     *
     * @throws Exception when Firestore setup fails
     */
    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();
        Map<String, Object> event = new HashMap<>();
        event.put("id", TEST_EVENT_ID);
        event.put("organizerId", TEST_ORGANIZER_ID); // Matches the ID in the intent
        Tasks.await(db.collection("events").document(TEST_EVENT_ID).set(event), 15, TimeUnit.SECONDS);
        event.put("name", "Cancel Invitation Test Event");
        event.put("capacity", 100);
        event.put("coOrganizerIds", new java.util.ArrayList<String>());
        event.put("posterUrl", "");
        event.put("qrCodeUrl", "");
        event.put("geolocationRequired", false);
        event.put("registrationStart", new Timestamp(new Date(1703980800000L)));
        event.put("registrationEnd", new Timestamp(new Date(1893456000000L))); // future date

        Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .set(event),
                5,
                TimeUnit.SECONDS
        );

        Map<String, Object> entrant = new HashMap<>();
        entrant.put("entrantId", TEST_ENTRANT_ID);
        entrant.put("entrantName", TEST_ENTRANT_NAME);
        entrant.put("entrantEmail", TEST_ENTRANT_EMAIL);
        entrant.put("selectionStatus", "selected");
        entrant.put("responseStatus", "pending");
        entrant.put("finalStatus", "");
        entrant.put("joinedAt", new Timestamp(new Date(1704067200000L)));

        Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .collection("waitlist")
                        .document(TEST_ENTRANT_ID)
                        .set(entrant),
                5,
                TimeUnit.SECONDS
        );
    }

    /**
     * Removes all Firestore data created or modified by the test.
     *
     * <p>Each cleanup step runs independently so later cleanup still happens
     * even if an earlier step fails.</p>
     *
     * @throws Exception when one or more cleanup steps fail
     */
    @After
    public void tearDown() throws Exception {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }

        StringBuilder errors = new StringBuilder();

        runCleanupStep("Delete cancelled waitlist entrant", () ->
                Tasks.await(
                        db.collection("events")
                                .document(TEST_EVENT_ID)
                                .collection("waitlist")
                                .document(TEST_ENTRANT_ID)
                                .delete(),
                        5,
                        TimeUnit.SECONDS
                ), errors);

        runCleanupStep("Delete test event document", () ->
                Tasks.await(
                        db.collection("events")
                                .document(TEST_EVENT_ID)
                                .delete(),
                        5,
                        TimeUnit.SECONDS
                ), errors);

        if (errors.length() > 0) {
            throw new AssertionError("Cleanup failed:\n" + errors);
        }
    }

    /**
     * US 02.06.04: Cancel an entrant who did not sign up for the event.
     *
     * <p>Verifies that after the organizer confirms cancellation:
     * <ul>
     *     <li>selectionStatus becomes cancelled</li>
     *     <li>responseStatus remains pending</li>
     *     <li>finalStatus is cleared</li>
     * </ul>
     *
     * @throws Exception when Firestore verification fails
     */
    @Test
    public void cancelInvitation_updatesEntrantStatusToCancelled() throws Exception {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantListActivity.class
        );
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);
        intent.putExtra("ORGANIZER_ID", TEST_ORGANIZER_ID);

        try (ActivityScenario<EntrantListActivity> scenario = ActivityScenario.launch(intent)) {
            waitForFirestoreUi();

            onView(withText(org.hamcrest.Matchers.startsWith("Selected")))
                    .perform(click());

            waitForFirestoreUi();

            onView(withText(TEST_ENTRANT_NAME)).perform(click());

            onView(withText("Cancel Invitation")).check(matches(isDisplayed()));
            onView(withText("Yes")).perform(click());

            waitForFirestoreUi();
        }

        DocumentSnapshot snapshot = Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .collection("waitlist")
                        .document(TEST_ENTRANT_ID)
                        .get(),
                5,
                TimeUnit.SECONDS
        );

        assertEquals("cancelled", snapshot.getString("selectionStatus"));
        assertEquals("pending", snapshot.getString("responseStatus"));
        assertNull(snapshot.get("finalStatus"));
    }

    /**
     * Provides a short delay so asynchronous Firestore reads/writes and UI
     * refreshes can settle before assertions.
     *
     * @throws InterruptedException when sleep is interrupted
     */
    private void waitForFirestoreUi() throws InterruptedException {
        Thread.sleep(2500);
    }

    /**
     * Runs one cleanup step and records any failure without stopping later
     * cleanup steps from executing.
     *
     * @param label step description
     * @param action cleanup action to run
     * @param errors collector for cleanup failures
     */
    private void runCleanupStep(String label, CleanupAction action, StringBuilder errors) {
        try {
            action.run();
        } catch (Exception e) {
            errors.append(label)
                    .append(" failed: ")
                    .append(e.getMessage())
                    .append("\n");
        }
    }

    /**
     * Functional interface for cleanup steps that may throw exceptions.
     */
    private interface CleanupAction {
        void run() throws Exception;
    }
}