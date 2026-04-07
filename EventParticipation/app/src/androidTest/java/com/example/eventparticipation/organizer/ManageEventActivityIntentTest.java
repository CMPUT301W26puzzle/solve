package com.example.eventparticipation.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventparticipation.R;
import com.example.eventparticipation.universal.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent tests for ManageEventActivity.
 */
@RunWith(AndroidJUnit4.class)
public class ManageEventActivityIntentTest {

    private static final String EVENT_ID = "event_001";
    private static final String ORGANIZER_ID = "organizer_demo_001";

    @Before
    public void setUp() {
        Intents.init();

        Context context = ApplicationProvider.getApplicationContext();
        // Mock the session before launching any activity
        SessionManager.getInstance(context).saveSession(ORGANIZER_ID, "organizer");
    }

    @After
    public void tearDown() {
        Intents.release();

        Context context = ApplicationProvider.getApplicationContext();
        // Clear the session after the test completes
        SessionManager.getInstance(context).clearSession();
    }

    @Test
    public void clickingViewEntrants_launchesEntrantListActivity() {
        // Use try-with-resources to automatically close the scenario after the test
        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(validIntent())) {

            onView(ViewMatchers.withId(R.id.btnViewEntrants)).perform(scrollTo(), click());

            intended(allOf(
                    hasComponent(EntrantListActivity.class.getName()),
                    hasExtra("EVENT_ID", EVENT_ID),
                    hasExtra("ORGANIZER_ID", ORGANIZER_ID)
            ));
        }
    }

    @Test
    public void clickingViewMap_launchesWaitlistMapActivity() {
        // Use try-with-resources to automatically close the scenario after the test
        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(validIntent())) {

            onView(withId(R.id.btnViewMap)).perform(scrollTo(), click());

            intended(allOf(
                    hasComponent(WaitlistMapActivity.class.getName()),
                    hasExtra("EVENT_ID", EVENT_ID),
                    hasExtra("ORGANIZER_ID", ORGANIZER_ID)
            ));
        }
    }

    private Intent validIntent() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                ManageEventActivity.class
        );
        intent.putExtra("EVENT_ID", EVENT_ID);
        intent.putExtra("ORGANIZER_ID", ORGANIZER_ID);
        return intent;
    }
}