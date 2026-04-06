package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
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
 * UI tests for EntrantEventDetailActivity using ActivityScenario only.
 * Covers US 01.01.01, US 01.01.02, US 01.05.04, US 01.08.01, US 01.08.02.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventDetailActivityTest {

    private static final String TEST_EVENT_ID = "ui-comment-event-001";
    private static final String TEST_EMPTY_EVENT_ID = "ui-comment-event-empty-001";
    private static final String TEST_OWN_COMMENT_ID = "ui-own-comment-001";
    private static final String TEST_OTHER_COMMENT_ID = "ui-other-comment-001";
    private static final String TEST_OTHER_ENTRANT_ID = "ui-other-entrant-001";
    private static final String TEST_OWN_COMMENT_TEXT = "This is my own seeded comment";
    private static final String TEST_OTHER_COMMENT_TEXT = "This is another entrant comment";

    private FirebaseFirestore db;
    private String entrantId;

    /**
     * Creates Firestore fixtures for event detail tests.
     *
     * @throws Exception if setup fails
     */
    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();
        entrantId = DeviceIdProvider.getId(ApplicationProvider.getApplicationContext());

        deleteTestData();
        seedEntrants();
        seedEvents();
        seedComments();
    }

    /**
     * Removes Firestore fixtures after each test.
     *
     * @throws Exception if cleanup fails
     */
    @After
    public void tearDown() throws Exception {
        deleteTestData();
    }

    /**
     * Builds a launch intent for the test event.
     *
     * @param eventId event id to open
     * @return detail activity intent
     */
    private Intent makeTestIntent(String eventId) {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailActivity.class
        );
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("ORGANIZER_ID", "organizer_ui_test");
        intent.putExtra("EVENT_NAME", "Spring Music Festival");
        intent.putExtra("VENUE_ADDRESS", "Central Park Music Plaza");
        intent.putExtra("CAPACITY", 500);
        intent.putExtra("ENROLLED_COUNT", 12);
        intent.putExtra("WAITING_COUNT", 7);
        return intent;
    }

    /**
     * Seeds entrant profiles for comment authorship.
     *
     * @throws Exception if writes fail
     */
    private void seedEntrants() throws Exception {
        Tasks.await(db.collection("entrants").document(entrantId).set(buildEntrantData(
                entrantId,
                "Current Entrant"
        )), 10, TimeUnit.SECONDS);

        Tasks.await(db.collection("entrants").document(TEST_OTHER_ENTRANT_ID).set(buildEntrantData(
                TEST_OTHER_ENTRANT_ID,
                "Other Entrant"
        )), 10, TimeUnit.SECONDS);
    }

    /**
     * Seeds event documents for detail tests.
     *
     * @throws Exception if writes fail
     */
    private void seedEvents() throws Exception {
        Tasks.await(db.collection("events").document(TEST_EVENT_ID).set(buildEventData("Commented Event")), 10, TimeUnit.SECONDS);
        Tasks.await(db.collection("events").document(TEST_EMPTY_EVENT_ID).set(buildEventData("Empty Comment Event")), 10, TimeUnit.SECONDS);
    }

    /**
     * Seeds comments for the main test event.
     *
     * @throws Exception if writes fail
     */
    private void seedComments() throws Exception {
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("comments").document(TEST_OWN_COMMENT_ID)
                .set(buildCommentData(TEST_OWN_COMMENT_ID, entrantId, "Current Entrant", TEST_OWN_COMMENT_TEXT, minutesAgo(2))),
                10, TimeUnit.SECONDS);

        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("comments").document(TEST_OTHER_COMMENT_ID)
                .set(buildCommentData(TEST_OTHER_COMMENT_ID, TEST_OTHER_ENTRANT_ID, "Other Entrant", TEST_OTHER_COMMENT_TEXT, minutesAgo(1))),
                10, TimeUnit.SECONDS);
    }

    /**
     * Deletes all Firestore test documents used by this suite.
     *
     * @throws Exception if cleanup fails
     */
    private void deleteTestData() throws Exception {
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("comments").document(TEST_OWN_COMMENT_ID).delete(), 10, TimeUnit.SECONDS);
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("comments").document(TEST_OTHER_COMMENT_ID).delete(), 10, TimeUnit.SECONDS);

        // Remove any comments posted dynamically during tests.
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("comments")
                .whereEqualTo("entrantId", entrantId)
                .get(), 10, TimeUnit.SECONDS)
                .getDocuments()
                .forEach(doc -> {
                    try {
                        Tasks.await(doc.getReference().delete(), 10, TimeUnit.SECONDS);
                    } catch (Exception ignored) {
                    }
                });

        Tasks.await(db.collection("events").document(TEST_EVENT_ID).delete(), 10, TimeUnit.SECONDS);
        Tasks.await(db.collection("events").document(TEST_EMPTY_EVENT_ID).delete(), 10, TimeUnit.SECONDS);
        Tasks.await(db.collection("entrants").document(entrantId).delete(), 10, TimeUnit.SECONDS);
        Tasks.await(db.collection("entrants").document(TEST_OTHER_ENTRANT_ID).delete(), 10, TimeUnit.SECONDS);
    }

    /**
     * Builds a profile document payload.
     *
     * @param id entrant id
     * @param name entrant name
     * @return entrant document data
     */
    private Map<String, Object> buildEntrantData(String id, String name) {
        Map<String, Object> data = new HashMap<>();
        data.put("entrantId", id);
        data.put("role", "entrant");
        data.put("name", name);
        data.put("email", id + "@example.com");
        data.put("phone", "");
        return data;
    }

    /**
     * Builds an event document payload.
     *
     * @param name event name
     * @return event document data
     */
    private Map<String, Object> buildEventData(String name) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", TEST_EVENT_ID);
        data.put("name", name);
        data.put("organizerId", "organizer_ui_test");
        data.put("facilityId", "");
        data.put("posterUrl", "");
        data.put("qrCodeUrl", "");
        data.put("venueAddress", "Central Park Music Plaza");
        data.put("geolocationRequired", false);
        data.put("enrolledCount", 12);
        data.put("waitingCount", 7);
        data.put("selectedCount", 1);
        data.put("venueLat", 53.5232);
        data.put("venueLng", -113.5263);
        data.put("registrationStart", minutesAgo(60));
        data.put("registrationEnd", minutesFromNow(60));
        data.put("waitlistLimit", 25);
        return data;
    }

    /**
     * Builds a comment document payload.
     *
     * @param commentId comment id
     * @param entrantId author entrant id
     * @param entrantName author display name
     * @param text comment text
     * @param createdAt creation date
     * @return comment document data
     */
    private Map<String, Object> buildCommentData(String commentId,
                                                 String entrantId,
                                                 String entrantName,
                                                 String text,
                                                 Date createdAt) {
        Map<String, Object> data = new HashMap<>();
        data.put("commentId", commentId);
        data.put("entrantId", entrantId);
        data.put("entrantName", entrantName);
        data.put("text", text);
        data.put("createdAt", createdAt);
        return data;
    }

    /**
     * Returns a date in the past by the given number of minutes.
     *
     * @param minutes minutes before now
     * @return past date
     */
    private Date minutesAgo(long minutes) {
        return new Date(System.currentTimeMillis() - minutes * 60_000L);
    }

    /**
     * Returns a date in the future by the given number of minutes.
     *
     * @param minutes minutes after now
     * @return future date
     */
    private Date minutesFromNow(long minutes) {
        return new Date(System.currentTimeMillis() + minutes * 60_000L);
    }

    /**
     * Waits until a comment text appears in the recycler.
     *
     * @param scenario activity scenario
     * @param text expected comment text
     */
    private void waitForComment(ActivityScenario<EntrantEventDetailActivity> scenario, String text) {
        long deadline = System.currentTimeMillis() + 15000;

        while (System.currentTimeMillis() < deadline) {
            if (commentsContainText(scenario, text)) {
                return;
            }
            SystemClock.sleep(300);
        }

        throw new AssertionError("Timed out waiting for comment: " + text);
    }

    /**
     * Waits for the comments list to populate or empty state to show.
     *
     * @param scenario activity scenario
     */
    private void waitForCommentsUi(ActivityScenario<EntrantEventDetailActivity> scenario) {
        long deadline = System.currentTimeMillis() + 15000;

        while (System.currentTimeMillis() < deadline) {
            final boolean[] ready = {false};

            scenario.onActivity(activity -> {
                RecyclerView rvComments = activity.findViewById(R.id.rvComments);
                TextView tvCommentsEmpty = activity.findViewById(R.id.tvCommentsEmpty);
                RecyclerView.Adapter adapter = rvComments.getAdapter();

                boolean hasComments = adapter != null && adapter.getItemCount() > 0;
                boolean showsEmpty = tvCommentsEmpty.getVisibility() == View.VISIBLE;
                ready[0] = hasComments || showsEmpty;
            });

            if (ready[0]) {
                return;
            }

            SystemClock.sleep(300);
        }

        throw new AssertionError("Timed out waiting for comments UI");
    }

    /**
     * Checks whether the comments recycler contains the given text.
     *
     * @param scenario activity scenario
     * @param text comment text to search for
     * @return true if the text is present in the comment list
     */
    private boolean commentsContainText(ActivityScenario<EntrantEventDetailActivity> scenario, String text) {
        final boolean[] found = {false};

        scenario.onActivity(activity -> {
            RecyclerView rvComments = activity.findViewById(R.id.rvComments);
            RecyclerView.Adapter adapter = rvComments.getAdapter();

            if (adapter == null) {
                return;
            }

            for (int i = 0; i < adapter.getItemCount(); i++) {
                RecyclerView.ViewHolder holder = adapter.createViewHolder(
                        rvComments,
                        adapter.getItemViewType(i)
                );
                adapter.bindViewHolder(holder, i);

                TextView tvCommentText = holder.itemView.findViewById(R.id.tvCommentText);
                if (tvCommentText != null && text.equals(tvCommentText.getText().toString())) {
                    found[0] = true;
                    break;
                }
            }
        });

        return found[0];
    }

    /**
     * Returns the Firestore comment count for the main test event.
     *
     * @return comment count
     * @throws Exception if the query fails
     */
    private int getCommentCount() throws Exception {
        return Tasks.await(
                db.collection("events").document(TEST_EVENT_ID)
                        .collection("comments")
                        .get(),
                10,
                TimeUnit.SECONDS
        ).size();
    }

    /** US 01.01.01 - Activity launches with valid event extras. */
    @Test
    public void activityLaunchesWithValidExtras_reachesResumed() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent(TEST_EVENT_ID))) {
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Activity instance is not null. */
    @Test
    public void activityInstance_isNotNull() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent(TEST_EVENT_ID))) {
            scenario.onActivity(activity -> assertNotNull(activity));
        }
    }

    /** Activity survives recreation (rotation). */
    @Test
    public void activityRecreated_doesNotCrash() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent(TEST_EVENT_ID))) {
            scenario.recreate();
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** US 01.01.02 - Activity launched without EVENT_ID does not crash. */
    @Test
    public void missingEventId_doesNotCrash() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailActivity.class
        );
        intent.putExtra("ORGANIZER_ID", "organizer_demo_001");
        intent.putExtra("EVENT_NAME", "Spring Music Festival");

        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(intent)) {
            assertNotNull(scenario.getState());
        }
    }

    /** US 01.05.04 - Activity launched with waiting/enrolled counts does not crash. */
    @Test
    public void launchWithWaitingAndEnrolledCounts_doesNotCrash() {
        Intent intent = makeTestIntent(TEST_EVENT_ID);
        intent.putExtra("WAITING_COUNT", 999);
        intent.putExtra("ENROLLED_COUNT", 1);

        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Activity can go to background and return. */
    @Test
    public void activityPausedThenResumed_isResumed() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent(TEST_EVENT_ID))) {
            scenario.moveToState(Lifecycle.State.STARTED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Empty state is shown when an event has no comments. */
    @Test
    public void commentsEmptyState_shownWhenNoCommentsExist() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent(TEST_EMPTY_EVENT_ID))) {
            waitForCommentsUi(scenario);
            onView(withId(R.id.tvCommentsEmpty)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }

    /** Existing comments are displayed under the event. */
    @Test
    public void existingComments_areDisplayed() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent(TEST_EVENT_ID))) {
            waitForComment(scenario, TEST_OWN_COMMENT_TEXT);

            assertTrue(commentsContainText(scenario, TEST_OWN_COMMENT_TEXT));
            assertTrue(commentsContainText(scenario, TEST_OTHER_COMMENT_TEXT));
        }
    }

    /** Another entrant's comment is visible on the event detail screen. */
    @Test
    public void otherUsersComment_isVisibleOnEventDetail() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent(TEST_EVENT_ID))) {
            waitForComment(scenario, TEST_OTHER_COMMENT_TEXT);

            assertTrue(commentsContainText(scenario, TEST_OTHER_COMMENT_TEXT));
        }
    }

    /** Posting a comment adds it to the list and clears the input. */
    @Test
    public void postComment_addsCommentToList() {
        String newComment = "UI posted comment from current entrant";

        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent(TEST_EVENT_ID))) {
            waitForComment(scenario, TEST_OWN_COMMENT_TEXT);

            onView(withId(R.id.etComment))
                    .perform(scrollTo(), replaceText(newComment), closeSoftKeyboard());
            onView(withId(R.id.btnPostComment)).perform(scrollTo(), click());

            waitForComment(scenario, newComment);

            assertTrue(commentsContainText(scenario, newComment));
            scenario.onActivity(activity ->
                    assertEquals("", ((TextView) activity.findViewById(R.id.etComment)).getText().toString())
            );
        }
    }

    /** Empty comment input does not create a new comment. */
    @Test
    public void postComment_emptyInput_doesNotCreateComment() throws Exception {
        int before = getCommentCount();

        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent(TEST_EVENT_ID))) {
            waitForComment(scenario, TEST_OWN_COMMENT_TEXT);

            onView(withId(R.id.etComment))
                    .perform(scrollTo(), replaceText("   "), closeSoftKeyboard());
            onView(withId(R.id.btnPostComment)).perform(scrollTo(), click());

            SystemClock.sleep(600);
        }

        assertEquals(before, getCommentCount());
    }

    /** Long press allows the author to delete their own comment. */
    @Test
    public void deleteOwnComment_removesCommentAfterConfirmation() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent(TEST_EVENT_ID))) {
            waitForComment(scenario, TEST_OWN_COMMENT_TEXT);

            onView(withText(TEST_OWN_COMMENT_TEXT)).perform(scrollTo(), longClick());
            onView(withText("Delete comment?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));
            onView(withText("Delete"))
                    .inRoot(isDialog())
                    .perform(click());

            long deadline = System.currentTimeMillis() + 10000;
            while (System.currentTimeMillis() < deadline) {
                if (!commentsContainText(scenario, TEST_OWN_COMMENT_TEXT)) {
                    break;
                }
                SystemClock.sleep(300);
            }

            assertFalse(commentsContainText(scenario, TEST_OWN_COMMENT_TEXT));
        }
    }

    /** Long press on another entrant's comment does not show the delete dialog. */
    @Test
    public void deleteOtherUsersComment_doesNotShowDeleteDialog() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent(TEST_EVENT_ID))) {
            waitForComment(scenario, TEST_OTHER_COMMENT_TEXT);

            onView(withText(TEST_OTHER_COMMENT_TEXT)).perform(scrollTo(), longClick());
            SystemClock.sleep(500);

            onView(withText("Delete comment?")).check(doesNotExist());
        }
    }
}
