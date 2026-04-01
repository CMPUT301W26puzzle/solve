package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WaitlistMapActivityIntentTest {

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