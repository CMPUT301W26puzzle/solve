package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Control tests for WaitlistMapActivity.
 */
@RunWith(AndroidJUnit4.class)
public class WaitlistMapActivityIntentTest {

    @Test
    public void missingEventId_destroysActivity() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WaitlistMapActivity.class
        );
        intent.putExtra("ORGANIZER_ID", "organizer_demo_001");

        ActivityScenario<WaitlistMapActivity> scenario = ActivityScenario.launch(intent);

        assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
    }

    @Test
    public void missingOrganizerId_destroysActivity() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WaitlistMapActivity.class
        );
        intent.putExtra("EVENT_ID", "event_001");

        ActivityScenario<WaitlistMapActivity> scenario = ActivityScenario.launch(intent);

        assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
    }
}