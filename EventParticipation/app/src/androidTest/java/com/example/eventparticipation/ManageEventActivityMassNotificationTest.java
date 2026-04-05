package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for sending mass notifications.
 *
 * <p>User stories covered:</p>
 * <ul>
 * <li>US 02.07.01 - As an organizer I want to send notifications to all entrants on the waiting list</li>
 * <li>US 02.07.02 - As an organizer I want to send notifications to all selected entrants</li>
 * <li>US 02.07.03 - As an organizer I want to send a notification to all cancelled entrants</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class ManageEventActivityMassNotificationTest {

    private static final String EVENT_ID = "event_mass_notif_123";
    private static final String ORG_ID = "org_mass_notif_123";
    private FirebaseFirestore db;

    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();
        Context context = ApplicationProvider.getApplicationContext();

        // 1. Mock Organizer Session
        SessionManager.getInstance(context).saveSession(ORG_ID, "organizer");

        // 2. Seed Event
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("id", EVENT_ID);
        eventData.put("name", "Mass Notif Event");
        eventData.put("organizerId", ORG_ID);
        Tasks.await(db.collection("events").document(EVENT_ID).set(eventData, SetOptions.merge()), 15, TimeUnit.SECONDS);

        // 3. Seed 3 Users in different statuses
        seedWaitlistUser("user_waiting", "waiting");
        seedWaitlistUser("user_selected", "selected");
        seedWaitlistUser("user_cancelled", "cancelled");
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup Firestore
        db.collection("events").document(EVENT_ID).collection("waitlist").document("user_waiting").delete();
        db.collection("events").document(EVENT_ID).collection("waitlist").document("user_selected").delete();
        db.collection("events").document(EVENT_ID).collection("waitlist").document("user_cancelled").delete();
        db.collection("events").document(EVENT_ID).delete();

        // Clear Session
        SessionManager.getInstance(ApplicationProvider.getApplicationContext()).clearSession();
    }

    private void seedWaitlistUser(String userId, String status) throws Exception {
        Map<String, Object> entrant = new HashMap<>();
        entrant.put("entrantId", userId);
        entrant.put("selectionStatus", status);
        Tasks.await(db.collection("events").document(EVENT_ID).collection("waitlist").document(userId).set(entrant), 15, TimeUnit.SECONDS);

        // Clear any old notifications for clean test state
        QuerySnapshot oldNotifs = Tasks.await(db.collection("entrants").document(userId).collection("notifications").get(), 15, TimeUnit.SECONDS);
        for (DocumentSnapshot doc : oldNotifs) doc.getReference().delete();
    }

    @Test
    public void massNotification_opensSelectionDialog() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", EVENT_ID);
        intent.putExtra("ORGANIZER_ID", ORG_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000); // Wait for Firestore permissions to fetch
            onView(withId(R.id.btnMassNotification)).perform(scrollTo(), click());
            Thread.sleep(1000);

            // Verify options for all 3 user stories exist
            onView(withText("Send Mass Notification")).inRoot(isDialog()).check(matches(isDisplayed()));
            onView(withText("Waiting List")).inRoot(isDialog()).check(matches(isDisplayed())); // US 02.07.01
            onView(withText("Selected Entrants")).inRoot(isDialog()).check(matches(isDisplayed())); // US 02.07.02
            onView(withText("Cancelled Entrants")).inRoot(isDialog()).check(matches(isDisplayed())); // US 02.07.03
        }
    }

    @Test
    public void massNotification_sendsToSpecificGroup() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", EVENT_ID);
        intent.putExtra("ORGANIZER_ID", ORG_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(3000);
            onView(withId(R.id.btnMassNotification)).perform(scrollTo(), click());
            Thread.sleep(1000);

            // Select ONLY "Waiting List"
            onView(withText("Waiting List")).inRoot(isDialog()).perform(click());
            onView(withText("Next")).inRoot(isDialog()).perform(click());
            Thread.sleep(1000);

            // Type Message and Send
            onView(isAssignableFrom(EditText.class))
                    .inRoot(isDialog())
                    .perform(replaceText("Hello Waitlist!"), closeSoftKeyboard());
            onView(withText("Send")).inRoot(isDialog()).perform(click());

            Thread.sleep(3000); // Wait for Firestore Write
        }

        // VERIFY: user_waiting should have the notification
        QuerySnapshot waitingNotifs = Tasks.await(db.collection("entrants").document("user_waiting").collection("notifications").get(), 15, TimeUnit.SECONDS);
        assertEquals("Waiting user should get 1 notification", 1, waitingNotifs.size());
        assertEquals("Hello Waitlist!", waitingNotifs.getDocuments().get(0).getString("message"));

        // VERIFY: user_selected should NOT have the notification
        QuerySnapshot selectedNotifs = Tasks.await(db.collection("entrants").document("user_selected").collection("notifications").get(), 15, TimeUnit.SECONDS);
        assertTrue("Selected user should get 0 notifications", selectedNotifs.isEmpty());
    }
}