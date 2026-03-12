package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Confirms the Notifications bottom-nav tab opens NotificationsActivity.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantDashboardNotificationsNavTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void clickingNotificationsTab_launchesNotificationsActivity() {
        ActivityScenario.launch(EntrantDashboardActivity.class);

        onView(withId(R.id.nav_notifications)).perform(click());

        intended(hasComponent(NotificationsActivity.class.getName()));
    }
}