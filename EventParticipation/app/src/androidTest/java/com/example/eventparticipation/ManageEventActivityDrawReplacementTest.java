package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for drawing replacement applicants.
 *
 * <p>User stories covered:</p>
 * <ul>
 * <li>US 02.05.03 - As an organizer I want to be able to draw a replacement applicant from the pooling system when a previously selected applicant cancels or rejects the invitation.</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class ManageEventActivityDrawReplacementTest {

    @Test
    public void drawReplacement_opensDialogAndExecutes() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession("org_123", "organizer");

        Intent intent = new Intent(context, ManageEventActivity.class);
        intent.putExtra("EVENT_ID", "event_123");

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000); // Wait for Firestore load

            onView(withId(R.id.btnDrawReplacement)).perform(scrollTo(), click());
            Thread.sleep(1000);

            // Verify dialog appears
            onView(withText("Draw Replacement"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));

            // Click confirm
            onView(withId(android.R.id.button1))
                    .inRoot(isDialog())
                    .perform(click());
        }
    }
}