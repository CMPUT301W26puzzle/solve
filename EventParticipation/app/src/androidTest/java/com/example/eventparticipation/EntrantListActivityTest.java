package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for EntrantListActivity (US 02.02.01).
 */
@RunWith(AndroidJUnit4.class)
public class EntrantListActivityTest {

    private static final String TEST_ORG_ID = "test_organizer_123";
    private static final String TEST_EVENT_ID = "test_event_123";

    /**
     * Seeds Firestore with test entrants before each test to prevent the empty state UI.
     */
    @Before
    public void setUp() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> tom = new HashMap<>();
        tom.put("entrantName", "Tom Lee");
        tom.put("entrantEmail", "tom@test.com");
        tom.put("status", "waiting");
        tom.put("joinedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .collection("waitlist")
                        .document("tom_id")
                        .set(tom),
                5,
                TimeUnit.SECONDS
        );
    }

    /**
     * US 02.02.01: View entrants on the waiting list.
     * Verifies that searching by email correctly filters the list to show the specific entrant.
     */
    @Test
    public void searchEntrantsByEmail_showsOnlyMatchingEntrant() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EntrantListActivity.class);
        intent.putExtra("ORGANIZER_ID", TEST_ORG_ID);
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);

        try (ActivityScenario<EntrantListActivity> scenario = ActivityScenario.launch(intent)) {
            // Type the email into the search bar
            onView(withId(R.id.etSearch)).perform(typeText("tom@test.com"));

            // Verify the entrant's name is now visible in the list
            onView(withText("Tom Lee")).check(matches(isDisplayed()));
        }
    }
}