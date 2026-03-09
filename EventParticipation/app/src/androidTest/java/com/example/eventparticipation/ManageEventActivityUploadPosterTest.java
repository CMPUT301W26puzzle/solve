package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso test for upload poster button state.
 *
 * <p>User story covered:</p>
 * <ul>
 *     <li>US 02.04.01 - As an organizer, I want to upload an event poster</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class ManageEventActivityUploadPosterTest {

    /**
     * Verifies that the upload poster button becomes disabled
     * when the event already has a poster in Firestore.
     */
    @Test
    public void uploadPosterButton_isDisabled_whenPosterAlreadyExists() {
        ActivityScenario.launch(validIntent());

        waitForButtonState(R.id.btnUploadPoster, false, 8000);

        onView(withId(R.id.btnUploadPoster)).check(matches(isDisplayed()));
        onView(withId(R.id.btnUploadPoster)).check(matches(isNotEnabled()));
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

    private void waitForButtonState(int viewId, boolean expectedEnabled, long timeoutMs) {
        long start = SystemClock.elapsedRealtime();
        AssertionError lastError = null;

        while (SystemClock.elapsedRealtime() - start < timeoutMs) {
            try {
                if (expectedEnabled) {
                    onView(withId(viewId)).check(matches(isDisplayed()));
                    onView(withId(viewId)).check(matches(androidx.test.espresso.matcher.ViewMatchers.isEnabled()));
                } else {
                    onView(withId(viewId)).check(matches(isDisplayed()));
                    onView(withId(viewId)).check(matches(isNotEnabled()));
                }
                return;
            } catch (AssertionError e) {
                lastError = e;
                SystemClock.sleep(300);
            }
        }

        if (lastError != null) {
            throw lastError;
        }
    }
}