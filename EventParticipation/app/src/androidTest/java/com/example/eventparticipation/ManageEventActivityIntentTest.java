package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent tests for ManageEventActivity.
 */
@RunWith(AndroidJUnit4.class)
public class ManageEventActivityIntentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void clickingViewEntrants_launchesEntrantListActivity() {
        ActivityScenario.launch(validIntent());

        onView(withId(R.id.btnViewEntrants)).perform(scrollTo(), click());

        intended(allOf(
                hasComponent(EntrantListActivity.class.getName()),
                hasExtra("EVENT_ID", "event_001"),
                hasExtra("ORGANIZER_ID", "organizer_demo_001")
        ));
    }

    @Test
    public void clickingViewMap_launchesWaitlistMapActivity() {
        ActivityScenario.launch(validIntent());

        onView(withId(R.id.btnViewMap)).perform(scrollTo(), click());

        intended(allOf(
                hasComponent(WaitlistMapActivity.class.getName()),
                hasExtra("EVENT_ID", "event_001"),
                hasExtra("ORGANIZER_ID", "organizer_demo_001")
        ));
    }

    private Intent validIntent() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                ManageEventActivity.class
        );
        intent.putExtra("EVENT_ID", "event_001");
        intent.putExtra("ORGANIZER_ID", "organizer_demo_001");
        return intent;
    }
}