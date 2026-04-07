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
 * Control tests for EntrantListActivity.
 *
 * <p>These tests verify basic launch behavior for valid and missing intent extras.</p>
 */
@RunWith(AndroidJUnit4.class)
public class EntrantListActivityIntentTest {

    /** Fixed test event id used across all intent tests. */
    private static final String EVENT_ID = "event_001";

    /** Fixed test organizer id used across all intent tests. */
    private static final String ORGANIZER_ID = "organizer_demo_001";

    /**
     * Verifies that the activity launches successfully when both
     * EVENT_ID and ORGANIZER_ID are provided.
     */
    @Test
    public void validEventAndOrganizer_launchesActivity() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantListActivity.class
        );
        intent.putExtra("EVENT_ID", EVENT_ID);
        intent.putExtra("ORGANIZER_ID", ORGANIZER_ID);

        try (ActivityScenario<EntrantListActivity> scenario = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /**
     * Verifies that the activity finishes immediately when EVENT_ID is missing,
     * even if ORGANIZER_ID is provided.
     */
    @Test
    public void missingEventId_finishesActivity() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantListActivity.class
        );
        intent.putExtra("ORGANIZER_ID", ORGANIZER_ID);

        try (ActivityScenario<EntrantListActivity> scenario = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    /**
     * Verifies that the activity finishes immediately when ORGANIZER_ID is missing,
     * even if EVENT_ID is provided.
     */
    @Test
    public void missingOrganizerId_finishesActivity() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantListActivity.class
        );
        intent.putExtra("EVENT_ID", EVENT_ID);

        try (ActivityScenario<EntrantListActivity> scenario = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }
}