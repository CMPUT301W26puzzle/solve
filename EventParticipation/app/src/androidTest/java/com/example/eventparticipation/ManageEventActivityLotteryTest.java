package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for the Lottery and Waitlist management features
 * inside ManageEventActivity.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ManageEventActivityLotteryTest {

    /**
     * Verifies that the "Run Lottery" button is visible and opens the correct
     * dialog when clicked, prompting the user for a sample size.
     */
    @Test
    public void testRunLotteryDialogAppears() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", "test_event_123");
        intent.putExtra("ORGANIZER_ID", "test_organizer_123");

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {

            onView(withId(R.id.btnRunLottery)).check(matches(isDisplayed()));
            onView(withId(R.id.btnRunLottery)).perform(click());

            // should have the title "Run Lottery"
            onView(withText("Run Lottery"))
                    .check(matches(isDisplayed()));

            // should have the instructional message
            onView(withText("Select how many entrants should receive invitations."))
                    .check(matches(isDisplayed()));

            // should have the "Cancel" and "Run" buttons
            onView(withText("Cancel")).check(matches(isDisplayed()));
            onView(withText("Run")).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that entering an invalid number (or empty string) in the
     * lottery dialog prevents the lottery from crashing the app.
     */
    @Test
    public void testRunLotteryDialog_EmptyInputValidation() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", "test_event_123");
        intent.putExtra("ORGANIZER_ID", "test_organizer_123");

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnRunLottery)).perform(click());
            onView(withText("Run")).perform(click());
            onView(withId(R.id.btnRunLottery)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that the "Draw Replacement Applicant" button is correctly rendered
     * on the screen so the organizer can manually pull a new user from the waitlist.
     */
    @Test
    public void testDrawReplacementButtonExists() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", "test_event_123");
        intent.putExtra("ORGANIZER_ID", "test_organizer_123");

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {

            // verify the new Draw Replacement button is displayed
            onView(withId(R.id.btnDrawReplacement))
                    .check(matches(isDisplayed()));

            // verif the button text is correct
            onView(withId(R.id.btnDrawReplacement))
                    .check(matches(withText("Draw Replacement Applicant")));
        }
    }
}