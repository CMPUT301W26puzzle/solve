package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * US 01.05.02
 * As an entrant, I want to be able to accept the invitation to register when chosen.
 */
@RunWith(AndroidJUnit4.class)
public class AcceptInvitationInstrumentedTest {

    @Test
    public void selectedStatus_showsAcceptInvitationUi() {
        ActivityScenario<EntrantEventDetailActivity> scenario =
                ActivityScenario.launch(detailIntentWithoutFirestoreIds());

        setStatusAndRefreshUi(scenario, "selected");

        onView(withId(R.id.btnJoinLeave))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
                .check(matches(withText("Accept Invitation")))
                .check(matches(isEnabled()));

        onView(withId(R.id.btnDeclineInvitation))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
                .check(matches(withText("Decline Invitation")));
    }

    @Test
    public void enrolledStatus_showsAlreadyEnrolledUi() {
        ActivityScenario<EntrantEventDetailActivity> scenario =
                ActivityScenario.launch(detailIntentWithoutFirestoreIds());

        setStatusAndRefreshUi(scenario, "enrolled");

        onView(withId(R.id.btnJoinLeave))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
                .check(matches(withText("Already Enrolled")))
                .check(matches(isNotEnabled()));

        onView(withId(R.id.btnDeclineInvitation))
                .check(matches(withEffectiveVisibility(GONE)));
    }

    private Intent detailIntentWithoutFirestoreIds() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantEventDetailActivity.class
        );
        intent.putExtra("EVENT_NAME", "Spring Music Festival");
        intent.putExtra("NOTIFICATION_ID", "notif_selected_001");
        return intent;
    }

    private void setStatusAndRefreshUi(ActivityScenario<EntrantEventDetailActivity> scenario,
                                       String status) {
        scenario.onActivity(activity -> {
            try {
                Field currentStatusField =
                        EntrantEventDetailActivity.class.getDeclaredField("currentStatus");
                currentStatusField.setAccessible(true);
                currentStatusField.set(activity, status);

                Method updateButtonsMethod =
                        EntrantEventDetailActivity.class.getDeclaredMethod("updateButtons");
                updateButtonsMethod.setAccessible(true);
                updateButtonsMethod.invoke(activity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
