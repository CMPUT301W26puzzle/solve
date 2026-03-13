package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo; // Add this import
import static androidx.test.espresso.action.ViewActions.typeText;
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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for the Lottery and Waitlist management features
 * inside ManageEventActivity.
 *
 * <p>Relevant user stories:</p>
 * <ul>
 * <li>US 02.05.02 - Sample a specified number of attendees</li>
 * <li>US 02.05.03 - Draw a replacement applicant</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ManageEventActivityLotteryTest {

    @Before
    public void setUp() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> dummyEvent = new HashMap<>();
        dummyEvent.put("name", "Lottery Test Event");
        dummyEvent.put("capacity", 100);

        Task<Void> task = db.collection("organizers")
                .document("test_organizer_123")
                .collection("events")
                .document("test_event_123")
                .set(dummyEvent);

        Tasks.await(task, 5, TimeUnit.SECONDS);
    }

    /**
     * US 02.05.02: Sample a specified number of attendees.
     * Verifies that the "Run Lottery" button is visible and opens the correct
     * dialog when clicked. Use scrollTo() to handle views off-screen.
     */
    @Test
    public void testRunLotteryDialogAppears() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", "test_event_123");
        intent.putExtra("ORGANIZER_ID", "test_organizer_123");

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
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", "test_event_123");
        intent.putExtra("ORGANIZER_ID", "test_organizer_123");

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnRunLottery)).perform(scrollTo(), click());

            // Type "5" into the EditText inside the dialog
            onView(isAssignableFrom(EditText.class)).perform(typeText("5"));

            // Click the Positive Button
            onView(withId(android.R.id.button1)).perform(click());

            // Verify we are back on the main screen
            onView(withId(R.id.btnRunLottery)).check(matches(isDisplayed()));
        }
    }

    /**
     * US 02.05.03: Draw a replacement applicant.
     * Verifies the button exists and is displayed after scrolling.
     */
    @Test
    public void testDrawReplacementButtonExists() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", "test_event_123");
        intent.putExtra("ORGANIZER_ID", "test_organizer_123");

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {

            // Use scrollTo to bring the button into view
            onView(withId(R.id.btnDrawReplacement))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            onView(withId(R.id.btnDrawReplacement))
                    .check(matches(withText("Draw Replacement Applicant")));
        }
    }
}