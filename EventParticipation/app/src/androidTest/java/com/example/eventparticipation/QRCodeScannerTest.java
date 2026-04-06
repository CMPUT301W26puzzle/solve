package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for verifying the launch of EntrantEventDetailActivity.
 */
@RunWith(AndroidJUnit4.class)
public class QRCodeScannerTest {

    @Before
    public void setUp() {
        // MOCK SESSION to prevent the Activity from calling finish() immediately
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession("test_user_id", "entrant");
    }

    @After
    public void tearDown() {
        // Clear session to ensure test isolation
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).clearSession();
    }

    @Test
    public void launchEventDetailActivity_withEventId() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, EntrantEventDetailActivity.class);
        intent.putExtra("EVENT_ID", "testEvent123");

        // Use try-with-resources to manage the scenario lifecycle
        try (ActivityScenario<EntrantEventDetailActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify that the activity UI is actually displayed
            onView(withId(R.id.tvEventName)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void activityReceivesEventId() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, EntrantEventDetailActivity.class);
        intent.putExtra("EVENT_ID", "event123");

        try (ActivityScenario<EntrantEventDetailActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                String eventIdFromIntent = activity.getIntent().getStringExtra("EVENT_ID");

                // Use JUnit assertEquals for reliable assertions
                assertEquals("The Activity did not receive the correct EVENT_ID", "event123", eventIdFromIntent);

                // Confirm the activity did not call finish() during onCreate
                assertFalse("Activity should not be finishing", activity.isFinishing());
            });

            // Confirm the UI rendered the initial data
            onView(withId(R.id.tvEventName)).check(matches(isDisplayed()));
        }
    }
}