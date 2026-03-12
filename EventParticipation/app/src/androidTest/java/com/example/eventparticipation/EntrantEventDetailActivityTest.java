package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for EntrantEventDetailActivity using ActivityScenario only.
 * Covers US 01.01.01, US 01.01.02, US 01.05.04.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantEventDetailActivityTest {

    private Intent makeTestIntent() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailActivity.class
        );
        intent.putExtra("EVENT_ID", "test_001");
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
}
