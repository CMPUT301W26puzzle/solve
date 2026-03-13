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
 * UI tests for SelectRoleActivity using ActivityScenario only (no Espresso view interactions).
 */
@RunWith(AndroidJUnit4.class)
public class SelectRoleActivityTest {

    /** Activity launches and reaches RESUMED state. */
    @Test
    public void activityLaunches_reachesResumedState() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                SelectRoleActivity.class
        );
        try (ActivityScenario<SelectRoleActivity> scenario = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Activity is not destroyed on normal launch. */
    @Test
    public void activityLaunches_isNotDestroyed() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                SelectRoleActivity.class
        );
        try (ActivityScenario<SelectRoleActivity> scenario = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Activity can be launched multiple times without crashing. */
    @Test
    public void activityLaunchedTwice_doesNotCrash() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                SelectRoleActivity.class
        );
        try (ActivityScenario<SelectRoleActivity> s1 = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.RESUMED, s1.getState());
        }
        try (ActivityScenario<SelectRoleActivity> s2 = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.RESUMED, s2.getState());
        }
    }

    /** Activity survives recreation (e.g. rotation). */
    @Test
    public void activityRecreated_doesNotCrash() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                SelectRoleActivity.class
        );
        try (ActivityScenario<SelectRoleActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.recreate();
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Activity can be moved to STARTED state (simulates going to background). */
    @Test
    public void activityMovedToBackground_reachesStartedState() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                SelectRoleActivity.class
        );
        try (ActivityScenario<SelectRoleActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.moveToState(Lifecycle.State.STARTED);
            assertEquals(Lifecycle.State.STARTED, scenario.getState());
        }
    }

    /** Activity can be moved to CREATED state. */
    @Test
    public void activityMovedToCreated_reachesCreatedState() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                SelectRoleActivity.class
        );
        try (ActivityScenario<SelectRoleActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.moveToState(Lifecycle.State.CREATED);
            assertEquals(Lifecycle.State.CREATED, scenario.getState());
        }
    }

    /** Activity instance is accessible via onActivity. */
    @Test
    public void activityInstance_isAccessible() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                SelectRoleActivity.class
        );
        try (ActivityScenario<SelectRoleActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> assertNotNull(activity));
        }
    }

    /** Activity resumes correctly after being paused and resumed. */
    @Test
    public void activityPausedThenResumed_isResumed() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                SelectRoleActivity.class
        );
        try (ActivityScenario<SelectRoleActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.moveToState(Lifecycle.State.STARTED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }
}
