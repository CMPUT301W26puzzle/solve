package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ManageEventActivityActionsTest {

    private FirebaseFirestore db;
    private final String TEST_EVENT_ID = "action_test_event_123";
    private final String TEST_ORG_ID = "org_123";

    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();
        Context context = ApplicationProvider.getApplicationContext();

        // 1. Mock Organizer Session
        SessionManager.getInstance(context).saveSession(TEST_ORG_ID, "organizer");

        // 2. Seed Mock Event
        Map<String, Object> mockEvent = new HashMap<>();
        mockEvent.put("eventName", "Test Actions Event");
        mockEvent.put("organizerId", TEST_ORG_ID);
        Tasks.await(db.collection("events").document(TEST_EVENT_ID).set(mockEvent), 5, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
        if (db != null) {
            Tasks.await(db.collection("events").document(TEST_EVENT_ID).delete());
            // Cleanup any mock entrants created during tests
            // Tasks.await(db.collection("entrants").document("mock_entrant_id").delete());
        }
        SessionManager.getInstance(ApplicationProvider.getApplicationContext()).clearSession();
    }

    /**
     * US 02.01.03: Invite specific entrants to a private event.
     */
    @Test
    public void testInviteUser_OpensDialogAndSubmits() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);
        intent.putExtra("ORGANIZER_ID", TEST_ORG_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(2000); // Wait for load

            // Click Invite User
            onView(withId(R.id.btnInviteUser)).perform(scrollTo(), click());

            // Verify dialog appears
            onView(withText("Private Invite")).inRoot(isDialog()).check(matches(isDisplayed()));

            // Type a search term
            onView(isAssignableFrom(EditText.class))
                    .inRoot(isDialog())
                    .perform(replaceText("testuser@example.com"), closeSoftKeyboard());

            // Click Invite
            onView(withText("Invite")).inRoot(isDialog()).perform(click());

            // Note: If the user doesn't exist in the DB, it will toast "User not found".
            // To test a successful invite, you would need to seed a fake user in the "entrants"
            // collection during the @Before setup.
        }
    }

    /**
     * US 02.06.04: Cancel entrants that did not sign up.
     */
    @Test
    public void testCancelEntrant_OpensDialogAndSubmits() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);
        intent.putExtra("ORGANIZER_ID", TEST_ORG_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(2000);

            // Click Cancel Entrant
            onView(withId(R.id.btnCancelEntrant)).perform(scrollTo(), click());

            // Verify dialog appears
            onView(withText("Cancel Entrant")).inRoot(isDialog()).check(matches(isDisplayed()));

            // Type email to cancel
            onView(isAssignableFrom(EditText.class))
                    .inRoot(isDialog())
                    .perform(replaceText("cancelme@example.com"), closeSoftKeyboard());

            // Submit
            onView(withText("Cancel User")).inRoot(isDialog()).perform(click());
        }
    }

    /**
     * US 02.06.05: Export final list of enrolled entrants in CSV format.
     */
    @Test
    public void testExportCsv_FiresShareIntent() throws Exception {
        // Seed an enrolled user so the CSV function doesn't abort early
        Map<String, Object> enrolledUser = new HashMap<>();
        enrolledUser.put("finalStatus", "enrolled");
        enrolledUser.put("entrantName", "John Doe");
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("waitlist").document("mock_enrolled_user").set(enrolledUser));

        Intents.init();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);
        intent.putExtra("ORGANIZER_ID", TEST_ORG_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(2000);

            // Stub the intent chooser so it doesn't actually open the Android share sheet
            intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

            // Click Export
            onView(withId(R.id.btnExportCsv)).perform(scrollTo(), click());

            Thread.sleep(1000); // Give the DB query time to process and fire the intent

            // Verify a Chooser intent was launched
            intended(hasAction(Intent.ACTION_CHOOSER));

        } finally {
            Intents.release();
        }
    }
}