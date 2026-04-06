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
    private final String TEST_ORG_ID = "org_comment_test";

    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession(TEST_ORG_ID, "organizer");

        // Create a dummy user profile so the name resolves correctly
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", "Organizer Bob");
        Tasks.await(db.collection("users").document(TEST_ORG_ID).set(userMap), 5, TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup comments
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

    /**
     * US 02.08.02 & US 02.08.01: Tests posting a comment and then deleting it.
     */
    @Test
    public void testPostAndDeleteComment() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventCommentsActivity.class);
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);
        intent.putExtra("IS_ORGANIZER", true); // Ensure they get the delete button

        try (ActivityScenario<EventCommentsActivity> scenario = ActivityScenario.launch(intent)) {

            // Test Posting (US 02.08.02)
            String uniqueComment = "Don't forget to bring water!";

            onView(withId(R.id.etCommentInput))
                    .perform(typeText(uniqueComment), closeSoftKeyboard());

            onView(withId(R.id.btnSendComment)).perform(click());

            // Wait for Firestore to sync
            Thread.sleep(2000);

            // Verify the comment appears in the RecyclerView
            onView(withText(uniqueComment)).check(matches(isDisplayed()));

            // Test Deleting (US 02.08.01)
            // Click the delete button on the comment
            onView(withId(R.id.btnDeleteComment)).perform(click());

            // Click "Delete" on the confirmation dialog
            onView(withText("Delete")).perform(click());

            // Wait for Firestore to sync deletion
            Thread.sleep(2000);

            // Verify the comment is removed from the UI
            onView(withText(uniqueComment)).check(doesNotExist());
        }
    }
}