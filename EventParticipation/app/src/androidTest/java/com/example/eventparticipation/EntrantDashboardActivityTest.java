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
 * UI tests for EntrantDashboardActivity using ActivityScenario only.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantDashboardActivityTest {

    /** Activity launches and reaches RESUMED state. */
    @Test
    public void activityLaunches_reachesResumedState() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantDashboardActivity.class
        );
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Activity instance is not null on launch. */
    @Test
    public void activityInstance_isNotNull() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantDashboardActivity.class
        );
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> assertNotNull(activity));
        }
    }

    /** Activity survives recreation (rotation). */
    @Test
    public void activityRecreated_doesNotCrash() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantDashboardActivity.class
        );
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.recreate();
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Activity can go to background and return. */
    @Test
    public void activityPausedThenResumed_isResumed() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantDashboardActivity.class
        );
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.moveToState(Lifecycle.State.STARTED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Activity can be moved to CREATED state without crashing. */
    @Test
    public void activityMovedToCreated_doesNotCrash() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantDashboardActivity.class
        );
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.moveToState(Lifecycle.State.CREATED);
            assertEquals(Lifecycle.State.CREATED, scenario.getState());
        }
    }
}
