package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
 * End-to-end instrumentation test for the co-organizer invitation flow.
 *
 * <p>User stories covered:
 * <ul>
 * <li>US 02.09.01 - As an organizer, I want to invite an entrant to become a
 * co-organizer for my event.</li>
 * <li>US 01.09.01 - As an entrant, I want to receive and accept a co-organizer
 * invitation.</li>
 * </ul>
 *
 * <p>This test verifies the full updated flow:
 * <ul>
 * <li>The organizer sends a co-organizer invitation from ManageEventActivity.</li>
 * <li>The entrant is not immediately promoted and remains in the waitlist.</li>
 * <li>A pending co-organizer invitation notification is created.</li>
 * <li>The entrant notifications UI displays the invitation and action buttons.</li>
 * <li>The entrant accepts the invitation.</li>
 * <li>After acceptance, the entrant is added to coOrganizerIds.</li>
 * <li>After acceptance, the entrant is removed from the waitlist.</li>
 * <li>The invitation notification is marked as accepted.</li>
 * <li>The entrant can then access ManageEventActivity in coorganizer mode.</li>
 * <li>The co-organizer dashboard applies the expected role restriction:
 * the assign-co-organizer button is hidden with GONE visibility.</li>
 * </ul>
 * </p>
 */
@RunWith(AndroidJUnit4.class)
public class ManageEventActivityCoOrganizerInvitationAcceptEndToEndTest {

    private static final String TEST_EVENT_ID = "event_001";
    private static final String TEST_ORGANIZER_ID = "organizer_demo_001";
    private static final String TEST_ENTRANT_NAME = "Entrant Accept Test";
    private static final String TEST_ENTRANT_EMAIL = "entrant_accept_test@test.com";

    private static final String EXPECTED_EVENT_NAME = "Tech Conference 2026";
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
     * Seeds Firestore with an eligible entrant record for the current device user.
     *
     * <p>The entrant id intentionally uses the current device id because
     * EntrantNotificationsActivity and ManageEventActivity both resolve the active
     * user through DeviceIdProvider.getId(...).</p>
     *
     * @throws Exception when Firestore setup fails
     */
    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        db = FirebaseFirestore.getInstance();
        testEntrantId = DeviceIdProvider.getId(context);
        expectedNotificationId =
                NotificationRepository.buildCoOrganizerInvitationNotificationId(TEST_EVENT_ID);

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
                5,
                TimeUnit.SECONDS
        );

        Map<String, Object> entrantProfile = new HashMap<>();
        entrantProfile.put("name", TEST_ENTRANT_NAME);
        entrantProfile.put("email", TEST_ENTRANT_EMAIL);

        Tasks.await(
                db.collection("entrants")
                        .document(testEntrantId)
                        .set(entrantProfile),
                5,
                TimeUnit.SECONDS
        );

        clearEntrantNotifications();

        Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .update("coOrganizerIds", FieldValue.arrayRemove(testEntrantId)),
                5,
                TimeUnit.SECONDS
        );
    }

    /**
     * Cleans up all Firestore data created or modified by the test.
     *
     * @throws Exception when one or more cleanup steps fail
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
                        5,
                        TimeUnit.SECONDS
                ), errors);

        runCleanupStep("Delete waitlist entrant", () ->
                Tasks.await(
                        db.collection("events")
                                .document(TEST_EVENT_ID)
                                .collection("waitlist")
                                .document(testEntrantId)
                                .delete(),
                        5,
                        TimeUnit.SECONDS
                ), errors);

        runCleanupStep("Delete entrant notifications", this::clearEntrantNotifications, errors);

        runCleanupStep("Delete entrant profile", () ->
                Tasks.await(
                        db.collection("entrants")
                                .document(testEntrantId)
                                .delete(),
                        5,
                        TimeUnit.SECONDS
                ), errors);

        // CLEAR SESSION AT THE END TO PREVENT LEAKS
        SessionManager.getInstance(ApplicationProvider.getApplicationContext()).clearSession();

        if (errors.length() > 0) {
            throw new AssertionError("Cleanup failed:\n" + errors);
        }
    }

    /**
     * Verifies the full co-organizer invitation acceptance flow and post-acceptance dashboard access.
     *
     * @throws Exception when Firestore verification fails
     */
    @Test
    public void coOrganizerInvitation_acceptance_allowsCoOrganizerDashboardAccess() throws Exception {
        launchManageEventAndSendInvitation();
        verifyPendingInvitationStateInFirestore();
        verifyEntrantNotificationsUiAndAcceptInvitation();
        verifyAcceptedStateInFirestore();
        verifyCoOrganizerDashboardAccess();
    }

    /**
     * Launches ManageEventActivity as organizer and sends a co-organizer invitation.
     *
     * @throws Exception when UI synchronization fails
     */
    private void launchManageEventAndSendInvitation() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        // 1. MOCK THE ORGANIZER SESSION
        SessionManager.getInstance(context).saveSession(TEST_ORGANIZER_ID, "organizer");

        Intent manageIntent = new Intent(context, ManageEventActivity.class);
        manageIntent.putExtra("EVENT_ID", TEST_EVENT_ID);
        manageIntent.putExtra("ORGANIZER_ID", TEST_ORGANIZER_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(manageIntent)) {
            waitForFirestoreUi();

            onView(withId(R.id.btnAssignCoOrganizer))
                    .perform(scrollTo(), click());
            Thread.sleep(1000);
            onView(withText("Invite Co-organizer"))
                    .check(matches(isDisplayed()));

            onData(equalTo(TEST_ENTRANT_NAME + " (" + TEST_ENTRANT_EMAIL + ") - Waiting"))
                    .inAdapterView(withId(androidx.appcompat.R.id.select_dialog_listview))
                    .perform(click());

            onView(withText("Send Invitation")).perform(click());

            waitForFirestoreUi();
        }
    }

    /**
     * Verifies that sending the invitation does not immediately promote the entrant
     * and that the pending invitation document is written correctly.
     *
     * @throws Exception when Firestore reads fail
     */
    private void verifyPendingInvitationStateInFirestore() throws Exception {
        DocumentSnapshot eventSnapshot = Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .get(),
                5,
                TimeUnit.SECONDS
        );

        List<String> coOrganizerIds = (List<String>) eventSnapshot.get("coOrganizerIds");
        if (coOrganizerIds == null) {
            coOrganizerIds = new ArrayList<>();
        }

        assertFalse(coOrganizerIds.contains(testEntrantId));

        DocumentSnapshot waitlistSnapshot = Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .collection("waitlist")
                        .document(testEntrantId)
                        .get(),
                5,
                TimeUnit.SECONDS
        );

        assertTrue(waitlistSnapshot.exists());

        DocumentSnapshot notificationDoc = Tasks.await(
                db.collection("entrants")
                        .document(testEntrantId)
                        .collection("notifications")
                        .document(expectedNotificationId)
                        .get(),
                5,
                TimeUnit.SECONDS
        );

        assertTrue(notificationDoc.exists());
        assertEquals(testEntrantId, notificationDoc.getString("entrantId"));
        assertEquals(TEST_EVENT_ID, notificationDoc.getString("eventId"));
        assertEquals(EXPECTED_EVENT_NAME, notificationDoc.getString("eventName"));
        assertEquals(EXPECTED_NOTIFICATION_TYPE, notificationDoc.getString("type"));
        assertEquals(EXPECTED_NOTIFICATION_MESSAGE, notificationDoc.getString("message"));
        assertEquals(Boolean.TRUE, notificationDoc.getBoolean("unread"));
        assertEquals(Boolean.TRUE, notificationDoc.getBoolean("actionRequired"));
        assertEquals(NotificationItem.ACTION_PENDING, notificationDoc.getString("actionStatus"));
        assertTrue(notificationDoc.get("createdAt") != null);
    }

    /**
     * Launches EntrantNotificationsActivity in live mode for the current device user,
     * verifies the pending invitation UI, and accepts the co-organizer invitation.
     *
     * @throws Exception when UI synchronization fails
     */
    private void verifyEntrantNotificationsUiAndAcceptInvitation() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        // 2. SWITCH TO THE ENTRANT SESSION
        SessionManager.getInstance(context).saveSession(testEntrantId, "entrant");

        Intent notificationsIntent = new Intent(context, EntrantNotificationsActivity.class);

        try (ActivityScenario<EntrantNotificationsActivity> scenario =
                     ActivityScenario.launch(notificationsIntent)) {

            waitForFirestoreUi();

            onView(withId(R.id.rvNotifications)).check(matches(isDisplayed()));
            onView(withId(R.id.rvNotifications))
                    .check(matches(hasDescendant(withText(EXPECTED_NOTIFICATION_MESSAGE))));
            onView(withText("Accept Co-organizer Invite"))
                    .check(matches(isDisplayed()));
            onView(withText("Decline"))
                    .check(matches(isDisplayed()));

            onView(withText("Accept Co-organizer Invite")).perform(click());

            waitForFirestoreUi();
        }
    }

    /**
     * Verifies the post-acceptance Firestore state:
     * the entrant is promoted to co-organizer, removed from the waitlist,
     * and the notification is marked as accepted.
     *
     * @throws Exception when Firestore reads fail
     */
    private void verifyAcceptedStateInFirestore() throws Exception {
        DocumentSnapshot eventSnapshot = Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .get(),
                5,
                TimeUnit.SECONDS
        );

        List<String> coOrganizerIds = (List<String>) eventSnapshot.get("coOrganizerIds");
        if (coOrganizerIds == null) {
            coOrganizerIds = new ArrayList<>();
        }

        assertTrue(coOrganizerIds.contains(testEntrantId));

        DocumentSnapshot waitlistSnapshot = Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .collection("waitlist")
                        .document(testEntrantId)
                        .get(),
                5,
                TimeUnit.SECONDS
        );

        assertFalse(waitlistSnapshot.exists());

        DocumentSnapshot notificationDoc = Tasks.await(
                db.collection("entrants")
                        .document(testEntrantId)
                        .collection("notifications")
                        .document(expectedNotificationId)
                        .get(),
                5,
                TimeUnit.SECONDS
        );

        assertTrue(notificationDoc.exists());
        assertEquals(Boolean.FALSE, notificationDoc.getBoolean("unread"));
        assertEquals(Boolean.FALSE, notificationDoc.getBoolean("actionRequired"));
        assertEquals(NotificationItem.ACTION_ACCEPTED, notificationDoc.getString("actionStatus"));
        assertTrue(notificationDoc.get("respondedAt") != null);
    }

    /**
     * Launches ManageEventActivity as the accepted co-organizer and verifies that
     * access is granted in coorganizer mode.
     *
     * <p>This assertion uses the role restriction already present in the screen:
     * co-organizers can open the dashboard, but the assign-co-organizer button
     * must be hidden with {@code GONE} visibility.</p>
     *
     * @throws Exception when UI synchronization fails
     */
    private void verifyCoOrganizerDashboardAccess() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        // 3. SWITCH TO CO-ORGANIZER SESSION
        SessionManager.getInstance(context).saveSession(testEntrantId, "organizer");

        Intent coOrganizerIntent = new Intent(context, ManageEventActivity.class);
        coOrganizerIntent.putExtra("EVENT_ID", TEST_EVENT_ID);
        coOrganizerIntent.putExtra("ORGANIZER_ID", TEST_ORGANIZER_ID);
        coOrganizerIntent.putExtra("ACCESS_MODE", "coorganizer");

        try (ActivityScenario<ManageEventActivity> scenario =
                     ActivityScenario.launch(coOrganizerIntent)) {

            waitForFirestoreUi();

            onView(withId(R.id.tvEventName)).check(matches(isDisplayed()));
            onView(withId(R.id.btnAssignCoOrganizer))
                    .check(matches(withEffectiveVisibility(GONE)));
        }
    }

    /**
     * Deletes all notification documents for the seeded test entrant.
     *
     * @throws Exception when Firestore cleanup fails
     */
    private void clearEntrantNotifications() throws Exception {
        for (DocumentSnapshot doc : Tasks.await(
                db.collection("entrants")
                        .document(testEntrantId)
                        .collection("notifications")
                        .get(),
                5,
                TimeUnit.SECONDS
        ).getDocuments()) {
            Tasks.await(doc.getReference().delete(), 5, TimeUnit.SECONDS);
        }
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