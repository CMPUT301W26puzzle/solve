package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
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
public class EventCommentsActivityTest {

    private FirebaseFirestore db;
    private final String TEST_EVENT_ID = "comment_test_event_123";
    private final String TEST_USER_ID = "org_admin_test_user";

    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession(TEST_USER_ID, "organizer");

        // FIX: Create the dummy user profile in the "entrants" collection to match app logic
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", "Test User");
        Tasks.await(db.collection("entrants").document(TEST_USER_ID).set(userMap), 5, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup comments and test user
        if (db != null) {
            db.collection("events").document(TEST_EVENT_ID).collection("comments").get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                            doc.getReference().delete();
                        }
                    });
            db.collection("entrants").document(TEST_USER_ID).delete();
        }
        SessionManager.getInstance(ApplicationProvider.getApplicationContext()).clearSession();
    }

    /**
     * US 02.08.02 & US 02.08.01: Tests Organizer posting a comment and then deleting it.
     */
    @Test
    public void testOrganizerPostAndDeleteComment() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventCommentsActivity.class);
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);
        intent.putExtra("IS_ORGANIZER", true);
        intent.putExtra("IS_ADMIN", false);

        try (ActivityScenario<EventCommentsActivity> scenario = ActivityScenario.launch(intent)) {

            // Test Posting (US 02.08.02)
            String uniqueComment = "Organizer official announcement!";
            onView(withId(R.id.etCommentInput)).perform(typeText(uniqueComment), closeSoftKeyboard());
            onView(withId(R.id.btnSendComment)).perform(click());

            // Wait for Firestore to sync
            Thread.sleep(2000);

            // Verify the comment and the Organizer tag appear
            onView(withText(uniqueComment)).check(matches(isDisplayed()));
            onView(withText("Test User (Organizer)")).check(matches(isDisplayed()));

            // Test Deleting (US 02.08.01)
            onView(withId(R.id.btnDeleteComment)).perform(click());
            onView(withText("Delete")).perform(click());

            Thread.sleep(2000);

            // Verify the comment is removed
            onView(withText(uniqueComment)).check(doesNotExist());
        }
    }

    /**
     * US 03.10.01: Tests Admin view formatting and moderation capabilities.
     */
    @Test
    public void testAdminViewAndModeration() throws Exception {
        // First, inject a dummy comment into the database to moderate
        Map<String, Object> dummyComment = new HashMap<>();
        dummyComment.put("text", "Inappropriate spam comment");
        dummyComment.put("userName", "Spammer");
        dummyComment.put("userId", "spam_123");
        Tasks.await(db.collection("events").document(TEST_EVENT_ID)
                .collection("comments").add(dummyComment), 5, TimeUnit.SECONDS);

        // Launch as Admin
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventCommentsActivity.class);
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);
        intent.putExtra("IS_ORGANIZER", false);
        intent.putExtra("IS_ADMIN", true);

        try (ActivityScenario<EventCommentsActivity> scenario = ActivityScenario.launch(intent)) {
            Thread.sleep(2000);

            // Verify Input Box is hidden (Admins do not post)
            onView(withId(R.id.layoutCommentInput)).check(matches(not(isDisplayed())));

            // Verify the dummy comment is visible
            onView(withText("Inappropriate spam comment")).check(matches(isDisplayed()));

            // Test Admin Deleting (US 03.10.01)
            // The delete button should be visible due to the isAdmin flag in CommentAdapter
            onView(withId(R.id.btnDeleteComment)).perform(click());
            onView(withText("Delete")).perform(click());

            Thread.sleep(2000);

            // Verify the comment was successfully moderated/removed
            onView(withText("Inappropriate spam comment")).check(doesNotExist());
        }
    }
}