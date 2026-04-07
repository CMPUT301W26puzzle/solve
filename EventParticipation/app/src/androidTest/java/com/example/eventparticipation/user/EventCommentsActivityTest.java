package com.example.eventparticipation.user;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

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
import java.util.concurrent.TimeUnit;

/**
 * Unified test suite for EventCommentsActivity.
 * Tests Entrant, Organizer, and Admin role behaviors.
 */
@RunWith(AndroidJUnit4.class)
public class EventCommentsActivityTest {

    private FirebaseFirestore db;
    private final String TEST_EVENT_ID = "unified_test_event";
    private final String TEST_USER_ID = "unified_test_user";

    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession(TEST_USER_ID, "entrant");

        // FIX: The activity now looks in "entrants", not "users"
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", "Unified Tester");
        Tasks.await(db.collection("entrants").document(TEST_USER_ID).set(userMap), 5, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
        if (db != null) {
            db.collection("events").document(TEST_EVENT_ID).collection("comments").get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                            doc.getReference().delete();
                        }
                    });
        }
        SessionManager.getInstance(ApplicationProvider.getApplicationContext()).clearSession();
    }

    /** US 01.08.01: Verifies Entrant can post a comment. */
    @Test
    public void testEntrantCanPostComment() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventCommentsActivity.class);
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);
        intent.putExtra("IS_ORGANIZER", false);
        intent.putExtra("IS_ADMIN", false);

        try (ActivityScenario<EventCommentsActivity> scenario = ActivityScenario.launch(intent)) {
            String comment = "Entrant comment test";
            onView(ViewMatchers.withId(R.id.etCommentInput)).perform(replaceText(comment), closeSoftKeyboard());
            onView(withId(R.id.btnSendComment)).perform(click());

            Thread.sleep(2000);
            onView(withText(comment)).check(matches(isDisplayed()));
        }
    }

    /** US 02.08.02: Verifies Organizer posts with an identifying tag. */
    @Test
    public void testOrganizerPost_showsOrganizerTag() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventCommentsActivity.class);
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);
        intent.putExtra("IS_ORGANIZER", true);

        try (ActivityScenario<EventCommentsActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.etCommentInput)).perform(replaceText("Organizer post"), closeSoftKeyboard());
            onView(withId(R.id.btnSendComment)).perform(click());

            Thread.sleep(2000);
            // Verify name is appended with (Organizer)
            onView(withText("Unified Tester (Organizer)")).check(matches(isDisplayed()));
        }
    }

    /** US 03.10.01: Verifies Admin cannot see the input box but can see comments. */
    @Test
    public void testAdminUI_hidesInputBox() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventCommentsActivity.class);
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);
        intent.putExtra("IS_ADMIN", true);

        try (ActivityScenario<EventCommentsActivity> scenario = ActivityScenario.launch(intent)) {
            // Admin should not be able to see the input layout
            onView(withId(R.id.layoutCommentInput)).check(matches(not(isDisplayed())));
        }
    }
}