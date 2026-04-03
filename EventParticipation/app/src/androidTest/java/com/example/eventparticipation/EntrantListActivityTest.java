package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for EntrantListActivity (US 02.02.01).
 */
@RunWith(AndroidJUnit4.class)
public class EntrantListActivityTest {

    private static final String TEST_ORG_ID = "organizer_demo_001";
    private static final String TEST_EVENT_ID = "event_001";

    private static final String WAITLIST_COLLECTION = "waitlist";
    private static final String TOM_DOC_ID = "tom_test_id";
    private static final String AMY_DOC_ID = "amy_test_id";

    /**
     * Seeds Firestore with test entrants before each test.
     *
     * <p>Two entrants are added so the search test can verify that the matching
     * entrant is shown while the non-matching entrant is filtered out.</p>
     */
    @Before
    public void setUp() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> tom = new HashMap<>();
        tom.put("entrantId", TOM_DOC_ID);
        tom.put("entrantName", "Tom Lee");
        tom.put("entrantEmail", "tom@test.com");
        tom.put("selectionStatus", "waiting");
        tom.put("responseStatus", "");
        tom.put("finalStatus", "");
        tom.put("joinedAt", new Timestamp(new Date(1704067200000L)));

        Map<String, Object> amy = new HashMap<>();
        amy.put("entrantId", AMY_DOC_ID);
        amy.put("entrantName", "Amy Wong");
        amy.put("entrantEmail", "amy@test.com");
        amy.put("selectionStatus", "waiting");
        amy.put("responseStatus", "");
        amy.put("finalStatus", "");
        amy.put("joinedAt", new Timestamp(new Date(1704153600000L)));

        Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .collection(WAITLIST_COLLECTION)
                        .document(TOM_DOC_ID)
                        .set(tom),
                5,
                TimeUnit.SECONDS
        );

        Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .collection(WAITLIST_COLLECTION)
                        .document(AMY_DOC_ID)
                        .set(amy),
                5,
                TimeUnit.SECONDS
        );
    }

    /**
     * Removes seeded Firestore test data after each test run.
     */
    @After
    public void tearDown() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .collection(WAITLIST_COLLECTION)
                        .document(TOM_DOC_ID)
                        .delete(),
                5,
                TimeUnit.SECONDS
        );

        Tasks.await(
                db.collection("events")
                        .document(TEST_EVENT_ID)
                        .collection(WAITLIST_COLLECTION)
                        .document(AMY_DOC_ID)
                        .delete(),
                5,
                TimeUnit.SECONDS
        );
    }

    /**
     * US 02.02.01: View entrants on the waiting list.
     *
     * <p>Verifies that searching by email filters the list so the matching
     * entrant remains visible while a non-matching entrant is removed.</p>
     */
    @Test
    public void searchEntrantsByEmail_showsOnlyMatchingEntrant() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantListActivity.class
        );
        intent.putExtra("ORGANIZER_ID", TEST_ORG_ID);
        intent.putExtra("EVENT_ID", TEST_EVENT_ID);

        try (ActivityScenario<EntrantListActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.etSearch))
                    .perform(replaceText("tom@test.com"), closeSoftKeyboard());

            onView(withText("Tom Lee")).check(matches(isDisplayed()));
            onView(withText("Amy Wong")).check(doesNotExist());
        }
    }
}