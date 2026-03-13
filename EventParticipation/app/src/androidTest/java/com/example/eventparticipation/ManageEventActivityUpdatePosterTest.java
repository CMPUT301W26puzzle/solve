package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

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
 * Espresso test for update poster button state.
 *
 * <p>User story covered:</p>
 * <ul>
 * <li>US 02.04.02 - As an organizer, I want to update the event poster</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class ManageEventActivityUpdatePosterTest {

    private static final String EVENT_ID = "event_001";
    private static final String ORG_ID = "organizer_demo_001";

    /**
     * Seeds Firestore with an event that HAS a poster.
     */
    @Before
    public void setUp() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Poster Event");
        data.put("posterUrl", "http://example.com/image.png"); // Crucial for Update button

        Tasks.await(db.collection("organizers").document(ORG_ID)
                .collection("events").document(EVENT_ID)
                .set(data), 5, TimeUnit.SECONDS);
    }

    @Test
    public void updatePosterButton_isEnabled_whenPosterAlreadyExists() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", EVENT_ID);
        intent.putExtra("ORGANIZER_ID", ORG_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            // Scroll to find the button and verify it is enabled
            onView(withId(R.id.btnUpdatePoster))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                    .check(matches(isEnabled()));
        }
    }
}