package com.example.eventparticipation.organizer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventparticipation.R;
import com.example.eventparticipation.universal.SessionManager;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

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

    private FirebaseFirestore db;
    private final String TEST_EVENT_ID = "test_event_123";

    @Before
    public void setUp() throws Exception {
        // 1. Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // 2. Create mock data for the event so the Activity doesn't fail to load
        Map<String, Object> mockEvent = new HashMap<>();
        mockEvent.put("eventName", "Test Draw Event");
        mockEvent.put("organizerId", "org_123");

        // IMPORTANT: Add any other fields here that ManageEventActivity REQUIRES to render.
        // For example, if it needs a capacity or a date to avoid crashing, add them:
        // mockEvent.put("capacity", 50);

        // 3. Force the test thread to wait until the document is successfully written to Firestore
        // Note: Change "events" to whatever your actual Firestore collection name is.
        Tasks.await(db.collection("events").document(TEST_EVENT_ID).set(mockEvent));

        // 4. Set up session
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession("org_123", "organizer");
    }

    @After
    public void tearDown() throws Exception {
        // Clean up the database after the test so it doesn't clutter your Firestore
        if (db != null) {
            Tasks.await(db.collection("events").document(TEST_EVENT_ID).delete());
        }
    }

    @Test
    public void drawReplacement_executesSuccessfully() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, ManageEventActivity.class);

        // Pass the ID of the mock document we just created
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            // Wait for activity to fetch the data we just created
            Thread.sleep(3000);

            // Click the button. This directly triggers drawReplacementApplicant()
            onView(ViewMatchers.withId(R.id.btnDrawReplacement)).perform(scrollTo(), click());

            // Wait for the background Firestore task in WaitlistController to complete
            Thread.sleep(2000);
        }
    }
}