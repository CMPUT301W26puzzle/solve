package com.example.eventparticipation.organizer;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventparticipation.R;
import com.example.eventparticipation.universal.DeviceIdProvider;
import com.example.eventparticipation.universal.NotificationItem;
import com.example.eventparticipation.universal.NotificationRepository;
import com.example.eventparticipation.universal.SessionManager;
import com.example.eventparticipation.user.EntrantNotificationsActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * End-to-end instrumentation tests for the co-organizer invitation flow.
 * * <p>This test suite has been split into modular tests to isolate failures.
 * It verifies User Stories US 02.09.01 (Organizer inviting a co-organizer)
 * and US 01.09.01 (Entrant accepting the invitation).</p>
 */
@RunWith(AndroidJUnit4.class)
public class ManageEventActivityCoOrganizerInvitationAcceptEndToEndTest {

    private static final String TEST_EVENT_ID = "test_event_coorg_001";
    private static final String TEST_ORGANIZER_ID = "organizer_demo_001";
    private static final String TEST_ENTRANT_NAME = "Entrant Accept Test";
    private static final String TEST_ENTRANT_EMAIL = "entrant_accept_test@test.com";

    private static final String EXPECTED_EVENT_NAME = "Test event co-organizer";
    private static final String EXPECTED_NOTIFICATION_TYPE =
            NotificationItem.TYPE_COORGANIZER_INVITATION;
    private static final String EXPECTED_NOTIFICATION_MESSAGE =
            "You have been invited to become a co-organizer for "
                    + EXPECTED_EVENT_NAME
                    + ". Accept to become a co-organizer.";

    private FirebaseFirestore db;
    private String testEntrantId;
    private String expectedNotificationId;

    /**
     * Prepares the Firestore database before each test.
     * * <p>Seeds a test event, an entrant in the waitlist, and the entrant's profile
     * to ensure the UI has the required data to display the invitation dialogs.</p>
     *
     * @throws Exception if Firestore network operations timeout or fail.
     */
    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        db = FirebaseFirestore.getInstance();
        testEntrantId = DeviceIdProvider.getId(context);
        expectedNotificationId = NotificationRepository.buildCoOrganizerInvitationNotificationId(TEST_EVENT_ID);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("id", TEST_EVENT_ID);
        eventData.put("name", EXPECTED_EVENT_NAME);
        eventData.put("organizerId", TEST_ORGANIZER_ID);
        Tasks.await(db.collection("events").document(TEST_EVENT_ID).set(eventData), 15, TimeUnit.SECONDS);

        Map<String, Object> entrant = new HashMap<>();
        entrant.put("entrantId", testEntrantId);
        entrant.put("entrantName", TEST_ENTRANT_NAME);
        entrant.put("entrantEmail", TEST_ENTRANT_EMAIL);
        entrant.put("selectionStatus", "waiting");
        entrant.put("responseStatus", "");
        entrant.put("finalStatus", "");
        entrant.put("joinedAt", new Timestamp(new Date(1704067200000L)));

        Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .collection("waitlist")
                        .document(testEntrantId)
                        .set(entrant),
                15,
                TimeUnit.SECONDS
        );

        Map<String, Object> entrantProfile = new HashMap<>();
        entrantProfile.put("name", TEST_ENTRANT_NAME);
        entrantProfile.put("email", TEST_ENTRANT_EMAIL);

        Tasks.await(
                db.collection("entrants")
                        .document(testEntrantId)
                        .set(entrantProfile),
                15,
                TimeUnit.SECONDS
        );

        clearEntrantNotifications();

        Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .update("coOrganizerIds", FieldValue.arrayRemove(testEntrantId)),
                15,
                TimeUnit.SECONDS
        );
    }

    /**
     * Cleans up the Firestore database after each test.
     * * <p>Deletes the seeded event, waitlist records, profile data, and any
     * notifications created during the test to prevent data leakage between runs.</p>
     *
     * @throws Exception if cleanup operations fail.
     */
    @After
    public void tearDown() throws Exception {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }

        StringBuilder errors = new StringBuilder();

        runCleanupStep("Remove entrant from coOrganizerIds", () ->
                Tasks.await(
                        db.collection("events")
                                .document(TEST_EVENT_ID)
                                .update("coOrganizerIds", FieldValue.arrayRemove(testEntrantId)),
                        15,
                        TimeUnit.SECONDS
                ), errors);

        runCleanupStep("Delete waitlist entrant", () ->
                Tasks.await(
                        db.collection("events")
                                .document(TEST_EVENT_ID)
                                .collection("waitlist")
                                .document(testEntrantId)
                                .delete(),
                        15,
                        TimeUnit.SECONDS
                ), errors);

        runCleanupStep("Delete entrant notifications", this::clearEntrantNotifications, errors);

        runCleanupStep("Delete entrant profile", () ->
                Tasks.await(
                        db.collection("entrants")
                                .document(testEntrantId)
                                .delete(),
                        15,
                        TimeUnit.SECONDS
                ), errors);

        SessionManager.getInstance(ApplicationProvider.getApplicationContext()).clearSession();

        if (errors.length() > 0) {
            throw new AssertionError("Cleanup failed:\n" + errors);
        }
    }

    /**
     * Tests the Organizer's flow for sending a Co-Organizer invitation.
     * * <p>Opens the Manage Event screen, selects an entrant from the dialog, sends the
     * invite, and verifies that the database successfully creates the pending notification.</p>
     * * @throws Exception if UI interaction or Firestore verification fails.
     */
    @Test
    public void testOrganizerCanSendCoOrganizerInvitation() throws Exception {
        launchManageEventAndSendInvitation();
        verifyPendingInvitationStateInFirestore();
    }

    /**
     * Tests the Entrant's flow for receiving and accepting a Co-Organizer invitation.
     * * <p>Manually seeds a pending notification, opens the Notifications screen, accepts
     * the invite, and verifies the database updates the entrant's role correctly.</p>
     * * @throws Exception if UI interaction or Firestore verification fails.
     */
    @Test
    public void testEntrantCanAcceptInvitationAndAccessDashboard() throws Exception {
        seedPendingNotification();

        verifyEntrantNotificationsUiAndAcceptInvitation();
        verifyAcceptedStateInFirestore();
        verifyCoOrganizerDashboardAccess();
    }

    /**
     * Helper method to manually inject a pending invitation into Firestore.
     * * <p>Used to isolate Test 2 from Test 1. Simulates the state of the database
     * immediately after an organizer sends an invite.</p>
     * * @throws Exception if the Firestore write operation times out.
     */
    private void seedPendingNotification() throws Exception {
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("entrantId", testEntrantId);
        notificationData.put("eventId", TEST_EVENT_ID);
        notificationData.put("eventName", EXPECTED_EVENT_NAME);
        notificationData.put("type", EXPECTED_NOTIFICATION_TYPE);
        notificationData.put("message", EXPECTED_NOTIFICATION_MESSAGE);
        notificationData.put("unread", true);
        notificationData.put("actionRequired", true);
        notificationData.put("actionStatus", NotificationItem.ACTION_PENDING);
        notificationData.put("createdAt", FieldValue.serverTimestamp());

        Tasks.await(
                db.collection("entrants")
                        .document(testEntrantId)
                        .collection("notifications")
                        .document(expectedNotificationId)
                        .set(notificationData),
                15,
                TimeUnit.SECONDS
        );
    }

    /**
     * Executes the Espresso UI actions for an Organizer sending an invite.
     * * <p>Mocks the Organizer session, clicks "Assign Co-Organizer", selects the entrant
     * from the AlertDialog list, and submits the form.</p>
     * * @throws Exception if views are not found or thread interruptions occur.
     */
    private void launchManageEventAndSendInvitation() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession(TEST_ORGANIZER_ID, "organizer");

        Intent manageIntent = new Intent(context, ManageEventActivity.class);
        manageIntent.putExtra("EVENT_ID", TEST_EVENT_ID);
        manageIntent.putExtra("ORGANIZER_ID", TEST_ORGANIZER_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(manageIntent)) {
            waitForFirestoreUi();

            onView(ViewMatchers.withId(R.id.btnAssignCoOrganizer)).perform(scrollTo(), click());

            Thread.sleep(1500); // Give dialog time to animate

            onView(withText("Invite Co-organizer"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));

            // Select the item. The adapter contains Strings.
            onData(allOf(is(instanceOf(String.class)), equalTo(TEST_ENTRANT_NAME)))
                    .inRoot(isDialog())
                    .perform(click());

            Thread.sleep(1000); // Give Android time to register the radio button check state

            onView(withText("Send Invitation"))
                    .inRoot(isDialog())
                    .perform(click());

            waitForFirestoreUi();
        }
    }

    /**
     * Validates that the pending notification document was correctly written.
     * * @throws Exception if Firestore reads fail or assertions do not match.
     */
    private void verifyPendingInvitationStateInFirestore() throws Exception {
        DocumentSnapshot eventSnapshot = Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .get(),
                15,
                TimeUnit.SECONDS
        );

        List<String> coOrganizerIds = (List<String>) eventSnapshot.get("coOrganizerIds");
        if (coOrganizerIds == null) {
            coOrganizerIds = new ArrayList<>();
        }

        // They shouldn't be a full co-organizer yet
        assertFalse("Entrant should not be in coOrganizerIds yet.", coOrganizerIds.contains(testEntrantId));

        DocumentSnapshot waitlistSnapshot = Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .collection("waitlist")
                        .document(testEntrantId)
                        .get(),
                15,
                TimeUnit.SECONDS
        );

        // FIX: The app correctly removes them from the waitlist immediately so they aren't
        // drafted in the lottery while the invite is pending. We must assert FALSE here.
        assertFalse("Entrant should be removed from the waitlist when the invite is sent.", waitlistSnapshot.exists());

        DocumentSnapshot notificationDoc = Tasks.await(
                db.collection("entrants")
                        .document(testEntrantId)
                        .collection("notifications")
                        .document(expectedNotificationId)
                        .get(),
                15,
                TimeUnit.SECONDS
        );

        assertTrue("Pending notification document was not created in Firestore.", notificationDoc.exists());
        assertEquals(EXPECTED_NOTIFICATION_MESSAGE, notificationDoc.getString("message"));
        assertEquals(NotificationItem.ACTION_PENDING, notificationDoc.getString("actionStatus"));
    }

    /**
     * Executes Espresso UI actions for an Entrant accepting the invite.
     * * @throws Exception if views are not found.
     */
    private void verifyEntrantNotificationsUiAndAcceptInvitation() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession(testEntrantId, "entrant");

        Intent notificationsIntent = new Intent(context, EntrantNotificationsActivity.class);

        try (ActivityScenario<EntrantNotificationsActivity> scenario = ActivityScenario.launch(notificationsIntent)) {
            waitForFirestoreUi();

            onView(withId(R.id.rvNotifications))
                    .check(matches(hasDescendant(withText(EXPECTED_NOTIFICATION_MESSAGE))));

            onView(withText("Accept Co-organizer Invite")).perform(click());

            waitForFirestoreUi();
        }
    }

    /**
     * Validates that Firestore was updated after the Entrant accepted the invite.
     * * <p>Checks that the entrant was added to coOrganizerIds, removed from the waitlist,
     * and the notification status was updated to accepted.</p>
     * * @throws Exception if Firestore assertions fail.
     */
    private void verifyAcceptedStateInFirestore() throws Exception {
        DocumentSnapshot eventSnapshot = Tasks.await(
                db.collection("events").document(TEST_EVENT_ID).get(),
                15, TimeUnit.SECONDS
        );

        List<String> coOrganizerIds = (List<String>) eventSnapshot.get("coOrganizerIds");
        if (coOrganizerIds == null) coOrganizerIds = new ArrayList<>();
        assertTrue("Entrant was not added to coOrganizerIds.", coOrganizerIds.contains(testEntrantId));

        DocumentSnapshot waitlistSnapshot = Tasks.await(
                db.collection("events").document(TEST_EVENT_ID)
                        .collection("waitlist").document(testEntrantId).get(),
                15, TimeUnit.SECONDS
        );
        assertFalse("Entrant was not removed from waitlist.", waitlistSnapshot.exists());

        DocumentSnapshot notificationDoc = Tasks.await(
                db.collection("entrants").document(testEntrantId)
                        .collection("notifications").document(expectedNotificationId).get(),
                15, TimeUnit.SECONDS
        );

        assertTrue(notificationDoc.exists());
        assertEquals(NotificationItem.ACTION_ACCEPTED, notificationDoc.getString("actionStatus"));
    }

    /**
     * Verifies the UI behavior of the ManageEventActivity for a Co-Organizer.
     * * <p>Ensures that the 'Assign Co-Organizer' button is hidden (GONE) when
     * accessed in co-organizer mode.</p>
     * * @throws Exception if UI checks fail.
     */
    private void verifyCoOrganizerDashboardAccess() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession(testEntrantId, "organizer");

        Intent coOrganizerIntent = new Intent(context, ManageEventActivity.class);
        coOrganizerIntent.putExtra("EVENT_ID", TEST_EVENT_ID);
        coOrganizerIntent.putExtra("ORGANIZER_ID", TEST_ORGANIZER_ID);
        coOrganizerIntent.putExtra("ACCESS_MODE", "coorganizer");

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(coOrganizerIntent)) {
            waitForFirestoreUi();
            onView(withId(R.id.tvEventName)).check(matches(isDisplayed()));
            onView(withId(R.id.btnAssignCoOrganizer)).check(matches(withEffectiveVisibility(GONE)));
        }
    }

    /**
     * Helper to delete notifications during setup and teardown.
     */
    private void clearEntrantNotifications() throws Exception {
        for (DocumentSnapshot doc : Tasks.await(
                db.collection("entrants").document(testEntrantId).collection("notifications").get(),
                15, TimeUnit.SECONDS
        ).getDocuments()) {
            Tasks.await(doc.getReference().delete(), 15, TimeUnit.SECONDS);
        }
    }

    /**
     * Centralized network/UI delay mechanism.
     */
    private void waitForFirestoreUi() throws InterruptedException {
        Thread.sleep(5000);
    }

    /**
     * Executes cleanup blocks cleanly without halting the teardown chain on error.
     */
    private void runCleanupStep(String label, CleanupAction action, StringBuilder errors) {
        try {
            action.run();
        } catch (Exception e) {
            errors.append(label).append(" failed: ").append(e.getMessage()).append("\n");
        }
    }

    private interface CleanupAction {
        void run() throws Exception;
    }
}