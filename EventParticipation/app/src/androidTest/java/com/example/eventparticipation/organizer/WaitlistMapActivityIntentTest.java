package com.example.eventparticipation.organizer;

import static org.junit.Assert.assertEquals;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent tests for WaitlistMapActivity.
 *
 * <p>These tests verify basic launch behavior with valid and missing intent extras.</p>
 */
@RunWith(AndroidJUnit4.class)
public class WaitlistMapActivityIntentTest {

    /**
     * Verifies that the activity launches successfully when both EVENT_ID
     * and ORGANIZER_ID are provided.
     */
    @Test
    public void validOrganizerAndEvent_activityLaunches() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WaitlistMapActivity.class
        );
        intent.putExtra("EVENT_ID", "event_001");
        intent.putExtra("ORGANIZER_ID", "organizer_demo_001");

        try (ActivityScenario<WaitlistMapActivity> scenario = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /**
     * Verifies that the activity still launches when ORGANIZER_ID is missing
     * but EVENT_ID is present.
     */
    @Test
    public void missingOrganizerId_activityStillLaunches() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WaitlistMapActivity.class
        );
        intent.putExtra("EVENT_ID", "event_001");

        try (ActivityScenario<WaitlistMapActivity> scenario = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /**
     * Verifies that the activity is destroyed when EVENT_ID is missing.
     */
    @Test
    public void missingEventId_destroysActivity() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WaitlistMapActivity.class
        );

        try (ActivityScenario<WaitlistMapActivity> scenario = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }
}