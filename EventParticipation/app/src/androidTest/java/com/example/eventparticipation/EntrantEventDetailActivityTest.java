package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * UI tests for EntrantEventDetailActivity using ActivityScenario only.
 * Covers US 01.01.01, US 01.01.02, US 01.05.04, US 01.08.01, and US 01.08.02.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventDetailActivityTest {

    private FirebaseFirestore db;
    private final String TEST_ENTRANT_ID = "test_entrant_id";
    private final String TEST_EVENT_ID = "test_001";

    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession(TEST_ENTRANT_ID, "entrant");

        // Create a dummy user profile so the comment name resolves correctly
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", "Test Entrant User");
        Tasks.await(db.collection("entrants").document(TEST_ENTRANT_ID).set(userMap), 5, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup comments and test user to avoid polluting the database
        if (db != null) {
            db.collection("events").document(TEST_EVENT_ID).collection("comments").get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                            doc.getReference().delete();
                        }
                    });
            db.collection("entrants").document(TEST_ENTRANT_ID).delete();
        }

        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).clearSession();
    }

    private Intent makeTestIntent() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailActivity.class
        );
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);
        intent.putExtra("ORGANIZER_ID", "organizer_demo_001");
        intent.putExtra("EVENT_NAME", "Spring Music Festival");
        intent.putExtra("VENUE_ADDRESS", "Central Park Music Plaza");
        intent.putExtra("CAPACITY", 500);
        intent.putExtra("ENROLLED_COUNT", 450);
        intent.putExtra("WAITING_COUNT", 234);
        return intent;
    }

    /** US 01.01.01 - Activity launches with valid event extras. */
    @Test
    public void activityLaunchesWithValidExtras_reachesResumed() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent())) {
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Activity instance is not null. */
    @Test
    public void activityInstance_isNotNull() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent())) {
            scenario.onActivity(activity -> assertNotNull(activity));
        }
    }

    /** Activity survives recreation (rotation). */
    @Test
    public void activityRecreated_doesNotCrash() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(makeTestIntent())) {
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
        Intent intent = makeTestIntent();
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
                     ActivityScenario.launch(makeTestIntent())) {
            scenario.moveToState(Lifecycle.State.STARTED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** * US 01.08.01 & US 01.08.02: Tests an Entrant posting a comment,
     * verifying it appears, and then deleting their own comment.
     */
    @Test
    public void testEntrantPostAndDeleteComment() throws Exception {
        try (ActivityScenario<EntrantEventDetailActivity> scenario = ActivityScenario.launch(makeTestIntent())) {

            // Setup a unique comment string to test
            String uniqueComment = "Can't wait for the festival!";

            // Scroll to the comment input field, type text, and close keyboard
            onView(withId(R.id.etComment))
                    .perform(typeText(uniqueComment), closeSoftKeyboard());

            // Click the Post button
            onView(withId(R.id.btnPostComment)).perform(click());

            // Wait briefly for Firestore to process and RecyclerView to update
            Thread.sleep(2000);

            // US 01.08.02: Verify the newly posted comment is visible on screen
            onView(withText(uniqueComment)).check(matches(isDisplayed()));

            // Verify Entrant self-deletion rule: click delete on their own comment
            onView(withId(R.id.btnDeleteComment)).perform(click());

            // Confirm the deletion in the Material Dialog
            onView(withText("Delete")).perform(click());

            // Wait briefly for Firestore to delete the item
            Thread.sleep(2000);

            // Verify the comment has been removed from the UI
            onView(withText(uniqueComment)).check(doesNotExist());
        }
    }
}