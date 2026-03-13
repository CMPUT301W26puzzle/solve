package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Espresso test for upload poster button state.
 *
 * <p>User story covered:</p>
 * <ul>
 * <li>US 02.04.01 - As an organizer, I want to upload an event poster</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class ManageEventActivityUploadPosterTest {

    private static final String EVENT_ID = "event_001";
    private static final String ORG_ID = "organizer_demo_001";

    /**
     * Seeds Firestore with an event that HAS a poster.
     */
    @Before
    public void setUp() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Tech Conference 2026");
        data.put("posterUrl", "https://firebasestorage.googleapis.com/v0/b/eventparticipation-7522d.firebasestorage.app/o/posters%2Forganizer_demo_001%2Fevent_001%2Fposter.jpg?alt=media&token=fe4c54df-1a44-4e09-b7e7-c2e55f2a8c1b");

        Tasks.await(
                db.collection("events")
                        .document(EVENT_ID)
                        .set(data, SetOptions.merge()),
                5,
                TimeUnit.SECONDS
        );
    }
    @Test
    public void uploadPosterButton_isDisabled_whenPosterAlreadyExists() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ManageEventActivity.class);
        intent.putExtra("EVENT_ID", EVENT_ID);
        intent.putExtra("ORGANIZER_ID", ORG_ID);

        try (ActivityScenario<ManageEventActivity> scenario = ActivityScenario.launch(intent)) {
            // Scroll to find the button and verify it is disabled
            onView(withId(R.id.btnUploadPoster))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                    .check(matches(isNotEnabled()));
        }
    }
}