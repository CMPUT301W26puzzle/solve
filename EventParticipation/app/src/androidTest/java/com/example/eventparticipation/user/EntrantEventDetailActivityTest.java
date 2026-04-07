package com.example.eventparticipation.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventparticipation.R;
import com.example.eventparticipation.universal.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for EntrantEventDetailActivity.
 * Updated to verify navigation to the new unified EventCommentsActivity.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventDetailActivityTest {

    @Before
    public void setUp() {
        Intents.init(); // Initialize for intent verification
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession("test_entrant_id", "entrant");
    }

    @After
    public void tearDown() {
        Intents.release();
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).clearSession();
    }

    private Intent makeTestIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantEventDetailActivity.class);
        intent.putExtra("EVENT_ID", "test_001");
        intent.putExtra("EVENT_NAME", "Spring Music Festival");
        return intent;
    }

    /** Verifies that clicking 'View Discussion' launches EventCommentsActivity with User permissions. */
    @Test
    public void testViewCommentsButton_launchesCommentsActivity() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario = ActivityScenario.launch(makeTestIntent())) {
            // Scroll to and click the new discussion button
            onView(ViewMatchers.withId(R.id.btnViewComments)).perform(scrollTo(), click());

            // Verify the intent was sent to the right place with correct defaults
            intended(allOf(
                    hasComponent(EventCommentsActivity.class.getName()),
                    hasExtra("EVENT_ID", "test_001"),
                    hasExtra("IS_ORGANIZER", false),
                    hasExtra("IS_ADMIN", false)
            ));
        }
    }

    @Test
    public void activityLaunchesWithValidExtras_reachesResumed() {
        try (ActivityScenario<EntrantEventDetailActivity> scenario = ActivityScenario.launch(makeTestIntent())) {
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }
}