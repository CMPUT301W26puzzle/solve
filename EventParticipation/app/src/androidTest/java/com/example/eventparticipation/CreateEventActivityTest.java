package com.example.eventparticipation;

import static android.app.Activity.RESULT_OK;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.containsString;

import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * UI Tests for Event Creation User Stories.
 */
@RunWith(AndroidJUnit4.class)
public class CreateEventActivityTest {

    @Rule
    public ActivityScenarioRule<CreateEventActivity> activityRule = new ActivityScenarioRule<>(CreateEventActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Comprehensive Test: Fills out EVERY variable in the UI before saving.
     */
    @Test
    public void testCreateEventWithAllVariables() {
        // MOCK THE IMAGE PICKER (US 02.04.01)
        // create a dummy result intent with a fake image URI
        Intent resultData = new Intent();
        resultData.setData(Uri.parse("content://dummy/image/path.jpg"));
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(RESULT_OK, resultData);

        intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(result);

        // click the poster image to trigger the mocked intent
        onView(withId(R.id.imgEventPoster)).perform(click());

        // INPUT EVENT NAME (US 02.01.01)
        // use replaceText instead of typeText to avoid issues with pre-existing hints
        onView(withId(R.id.etEventName))
                .perform(replaceText("Ultimate Mega Hackathon"), closeSoftKeyboard());

        // SET WAITLIST LIMIT (US 02.03.01)
        onView(withId(R.id.etWaitlistLimit))
                .perform(replaceText("150"), closeSoftKeyboard());

        // TOGGLE GEOLOCATION (US 02.02.03)
        onView(withId(R.id.switchGeolocation))
                .perform(click())
                .check(matches(isChecked()));

        // SELECT REGISTRATION DATES (US 02.01.04)
        onView(withId(R.id.btnDateRange)).perform(click());

        // Select today
        onView(withContentDescription(containsString("Today")))
                .perform(click());

        // Select tomorrow
        clickTomorrow();

        // Confirm selected date range
        onView(withId(com.google.android.material.R.id.confirm_button))
                .check(matches(isEnabled()))
                .perform(click());

        // verify the button text changed to reflect the selected dates
        onView(withId(R.id.btnDateRange))
                .check(matches(withText(R.string.period_set)));

        // SAVE THE EVENT
        onView(withId(R.id.btnSaveEvent)).perform(click());
    }

    /**
     * US 02.01.01: Create a new event and generate a QR code.
     * Verifies that the organizer can enter a name and click the save button to trigger creation.
     */
    @Test
    public void testCreateEventWithRequiredFields() {
        onView(withId(R.id.etEventName))
                .perform(typeText("Annual Tech Symposium"), closeSoftKeyboard());

        onView(withId(R.id.btnSaveEvent))
                .perform(click());

        // the actual generation of the QR code (US 02.01.01) is handled
        // by QRCodeGenerator.generateQRCode() upon successful save.
    }

    /**
     * US 02.01.04: Set a registration period.
     * Verifies that the date range picker button is visible and clickable.
     */
    @Test
    public void testSetRegistrationPeriod() {
        onView(withId(R.id.btnDateRange))
                .check(matches(isDisplayed()))
                .perform(click());

        // Verifies the MaterialDatePicker dialog pops up
        onView(withText(R.string.select_registration_period))
                .check(matches(isDisplayed()));
    }

    /**
     * US 02.02.03: Enable or disable the geolocation requirement.
     * Verifies that the organizer can toggle the geolocation switch.
     */
    @Test
    public void testToggleGeolocationRequirement() {
        // Toggle the switch on
        onView(withId(R.id.switchGeolocation))
                .perform(click())
                .check(matches(isChecked()));
    }

    /**
     * US 02.03.01: OPTIONALLY limit the number of entrants.
     * Verifies that the organizer can input a numerical capacity limit.
     */
    @Test
    public void testOptionalWaitlistLimit() {
        onView(withId(R.id.etWaitlistLimit))
                .perform(typeText("500"), closeSoftKeyboard())
                .check(matches(withText("500")));
    }
    /**
     * Selects tomorrow in the currently visible MaterialDatePicker.
     */
    private void clickTomorrow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat formatter = new SimpleDateFormat("MMMM d", Locale.ENGLISH);
        String tomorrow = formatter.format(calendar.getTime());

        onView(withContentDescription(containsString(tomorrow)))
                .perform(click());
    }
}