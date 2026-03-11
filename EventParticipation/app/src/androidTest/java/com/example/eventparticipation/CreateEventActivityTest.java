package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI Tests for Event Creation User Stories.
 */
@RunWith(AndroidJUnit4.class)
public class CreateEventActivityTest {

    @Rule
    public ActivityScenarioRule<CreateEventActivity> activityRule =
            new ActivityScenarioRule<>(CreateEventActivity.class);

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
}