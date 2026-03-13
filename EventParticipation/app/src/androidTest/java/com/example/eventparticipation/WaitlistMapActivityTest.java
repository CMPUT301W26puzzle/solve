package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso tests for the waitlist map screen.
 *
 * <p>User story covered:</p>
 * <ul>
 *     <li>US 02.02.02 - As an organizer, I want to see on a map where entrants joined the waiting list from</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class WaitlistMapActivityTest {

    /**
     * Verifies that the map screen launches successfully with a valid event id.
     */
    @Test
    public void launchWithValidEventId_showsToolbar() throws InterruptedException {
        ActivityScenario.launch(validIntent());

        Thread.sleep(3000);

        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
    }

    private Intent validIntent() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WaitlistMapActivity.class
        );
        intent.putExtra("EVENT_ID", "event_001");
        return intent;
    }
}