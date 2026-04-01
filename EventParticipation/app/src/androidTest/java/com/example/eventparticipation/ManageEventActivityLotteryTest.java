package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for the Lottery and Waitlist management features
 * inside ManageEventActivity.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ManageEventActivityLotteryTest {

    private static final String EVENT_ID = "test_event_123";
    private static final String ORG_ID = "test_organizer_123";

    @Before
    public void setUp() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> dummyEvent = new HashMap<>();
        dummyEvent.put("id", EVENT_ID);
        dummyEvent.put("name", "Lottery Test Event");
        dummyEvent.put("organizerId", ORG_ID);
        dummyEvent.put("waitlistLimit", 100);

        Tasks.await(
                db.collection("events")
                        .document(EVENT_ID)
                        .set(dummyEvent, SetOptions.merge()),
                5,
                TimeUnit.SECONDS
        );
    }

    @Test
    public void testRunLotteryDialogAppears() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                ManageEventActivity.class
        );
        intent.putExtra("EVENT_ID", EVENT_ID);
        intent.putExtra("ORGANIZER_ID", ORG_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnRunLottery)).perform(scrollTo(), click());
            onView(withText("Run Lottery")).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1))
                    .check(matches(withText("Run")))
                    .check(matches(isDisplayed()));
            onView(withId(android.R.id.button2))
                    .check(matches(withText("Cancel")))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void testRunLotteryDialog_WithInput() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                ManageEventActivity.class
        );
        intent.putExtra("EVENT_ID", EVENT_ID);
        intent.putExtra("ORGANIZER_ID", ORG_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnRunLottery)).perform(scrollTo(), click());

            onView(isAssignableFrom(EditText.class))
                    .perform(replaceText("5"), closeSoftKeyboard());

            onView(withId(android.R.id.button1)).perform(click());

            onView(withId(R.id.btnRunLottery)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testDrawReplacementButtonExists() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                ManageEventActivity.class
        );
        intent.putExtra("EVENT_ID", EVENT_ID);
        intent.putExtra("ORGANIZER_ID", ORG_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnDrawReplacement))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnDrawReplacement))
                    .check(matches(withText("Draw Replacement Applicant")));
        }
    }
}