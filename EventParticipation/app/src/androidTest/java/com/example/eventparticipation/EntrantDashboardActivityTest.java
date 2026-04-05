package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.SystemClock;
import android.widget.TextView;

import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
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
 * UI tests for EntrantDashboardActivity.
 */
@RunWith(AndroidJUnit4.class)
public class EntrantDashboardActivityTest {

    private static final String EVENT_OPEN_AVAILABLE = "ui-dashboard-open-available";
    private static final String EVENT_OPEN_FULL = "ui-dashboard-open-full";
    private static final String EVENT_UPCOMING = "ui-dashboard-upcoming";
    private static final String EVENT_CLOSED = "ui-dashboard-closed";
    private static final String EVENT_WAITING = "ui-dashboard-waiting";
    private static final String EVENT_SELECTED = "ui-dashboard-selected";
    private static final String EVENT_ENROLLED = "ui-dashboard-enrolled";

    private static final String NAME_OPEN_AVAILABLE = "UI Search Open Available Alpha";
    private static final String NAME_OPEN_FULL = "UI Search Open Full Beta";
    private static final String NAME_UPCOMING = "UI Search Upcoming Gamma";
    private static final String NAME_CLOSED = "UI Search Closed Delta";
    private static final String NAME_WAITING = "UI Search Waiting Epsilon";
    private static final String NAME_SELECTED = "UI Search Selected Zeta";
    private static final String NAME_ENROLLED = "UI Search Enrolled Eta";

    private FirebaseFirestore db;
    private String entrantId;

    /**
     * Creates dashboard test data in Firestore.
     *
     * @throws Exception if setup fails
     */
    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();
        entrantId = DeviceIdProvider.getId(ApplicationProvider.getApplicationContext());

        deleteTestData();
        seedEventDocuments();
        seedWaitlistDocuments();
    }

    /**
     * Removes dashboard test data from Firestore.
     *
     * @throws Exception if cleanup fails
     */
    @After
    public void tearDown() throws Exception {
        deleteTestData();
    }

    /**
     * Creates a launch intent for the dashboard activity.
     *
     * @return dashboard intent
     */
    private Intent makeIntent() {
        return new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantDashboardActivity.class
        );
    }

    /**
     * Writes the test event fixtures.
     *
     * @throws Exception if event writes fail
     */
    private void seedEventDocuments() throws Exception {
        Tasks.await(db.collection("events").document(EVENT_OPEN_AVAILABLE).set(buildEventData(
                NAME_OPEN_AVAILABLE,
                "North Search Hall",
                minutesFromNow(-120),
                minutesFromNow(120),
                2,
                5,
                0,
                0
        )), 10, TimeUnit.SECONDS);

        Tasks.await(db.collection("events").document(EVENT_OPEN_FULL).set(buildEventData(
                NAME_OPEN_FULL,
                "Full Search Hall",
                minutesFromNow(-120),
                minutesFromNow(120),
                5,
                5,
                0,
                0
        )), 10, TimeUnit.SECONDS);

        Tasks.await(db.collection("events").document(EVENT_UPCOMING).set(buildEventData(
                NAME_UPCOMING,
                "Future Search Centre",
                minutesFromNow(120),
                minutesFromNow(240),
                0,
                10,
                0,
                0
        )), 10, TimeUnit.SECONDS);

        Tasks.await(db.collection("events").document(EVENT_CLOSED).set(buildEventData(
                NAME_CLOSED,
                "Past Search Venue",
                minutesFromNow(-240),
                minutesFromNow(-120),
                1,
                10,
                0,
                0
        )), 10, TimeUnit.SECONDS);

        Tasks.await(db.collection("events").document(EVENT_WAITING).set(buildEventData(
                NAME_WAITING,
                "Waiting Search Room",
                minutesFromNow(-120),
                minutesFromNow(120),
                3,
                8,
                0,
                0
        )), 10, TimeUnit.SECONDS);

        Tasks.await(db.collection("events").document(EVENT_SELECTED).set(buildEventData(
                NAME_SELECTED,
                "Selected Search Hub",
                minutesFromNow(-120),
                minutesFromNow(120),
                2,
                8,
                0,
                1
        )), 10, TimeUnit.SECONDS);

        Tasks.await(db.collection("events").document(EVENT_ENROLLED).set(buildEventData(
                NAME_ENROLLED,
                "Enrolled Search Lab",
                minutesFromNow(-120),
                minutesFromNow(120),
                1,
                8,
                1,
                0
        )), 10, TimeUnit.SECONDS);
    }

    /**
     * Writes waitlist documents for the current entrant.
     *
     * @throws Exception if waitlist writes fail
     */
    private void seedWaitlistDocuments() throws Exception {
        Tasks.await(db.collection("events").document(EVENT_WAITING)
                .collection("waitlist").document(entrantId)
                .set(buildWaitlistData("waiting", null, null)), 10, TimeUnit.SECONDS);

        Tasks.await(db.collection("events").document(EVENT_SELECTED)
                .collection("waitlist").document(entrantId)
                .set(buildWaitlistData("selected", "pending", null)), 10, TimeUnit.SECONDS);

        Tasks.await(db.collection("events").document(EVENT_ENROLLED)
                .collection("waitlist").document(entrantId)
                .set(buildWaitlistData("selected", "accepted", "enrolled")), 10, TimeUnit.SECONDS);
    }

    /**
     * Deletes test events and waitlist entries.
     *
     * @throws Exception if cleanup fails
     */
    private void deleteTestData() throws Exception {
        String[] eventIds = {
                EVENT_OPEN_AVAILABLE,
                EVENT_OPEN_FULL,
                EVENT_UPCOMING,
                EVENT_CLOSED,
                EVENT_WAITING,
                EVENT_SELECTED,
                EVENT_ENROLLED
        };

        for (String eventId : eventIds) {
            Tasks.await(db.collection("events").document(eventId)
                    .collection("waitlist").document(entrantId).delete(), 10, TimeUnit.SECONDS);
            Tasks.await(db.collection("events").document(eventId).delete(), 10, TimeUnit.SECONDS);
        }
    }

    /**
     * Builds an event document payload.
     *
     * @param name event name
     * @param venueAddress venue address
     * @param registrationStart registration start time
     * @param registrationEnd registration end time
     * @param waitingCount waiting count
     * @param waitlistLimit waitlist limit
     * @param enrolledCount enrolled count
     * @param selectedCount selected count
     * @return event data map
     */
    private Map<String, Object> buildEventData(String name,
                                               String venueAddress,
                                               Date registrationStart,
                                               Date registrationEnd,
                                               int waitingCount,
                                               Integer waitlistLimit,
                                               int enrolledCount,
                                               int selectedCount) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("venueAddress", venueAddress);
        data.put("registrationStart", registrationStart);
        data.put("registrationEnd", registrationEnd);
        data.put("waitingCount", waitingCount);
        data.put("waitlistLimit", waitlistLimit);
        data.put("enrolledCount", enrolledCount);
        data.put("selectedCount", selectedCount);
        data.put("capacity", 50);
        data.put("organizerId", "ui_test_organizer");
        data.put("posterUrl", "");
        data.put("facilityId", "");
        data.put("geolocationRequired", false);
        return data;
    }

    /**
     * Builds a waitlist document payload.
     *
     * @param selectionStatus selection status
     * @param responseStatus response status
     * @param finalStatus final status
     * @return waitlist data map
     */
    private Map<String, Object> buildWaitlistData(String selectionStatus,
                                                  String responseStatus,
                                                  String finalStatus) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", entrantId);
        data.put("entrantId", entrantId);
        data.put("selectionStatus", selectionStatus);
        data.put("responseStatus", responseStatus);
        data.put("finalStatus", finalStatus);
        data.put("joinedAt", new Date());
        return data;
    }

    /**
     * Returns a timestamp offset from now in minutes.
     *
     * @param minutes minute offset
     * @return offset date
     */
    private Date minutesFromNow(long minutes) {
        return new Date(System.currentTimeMillis() + minutes * 60_000L);
    }

    /**
     * Waits until the recycler contains a specific event title.
     *
     * @param scenario dashboard scenario
     * @param eventName event title to wait for
     */
    private void waitForEvent(ActivityScenario<EntrantDashboardActivity> scenario, String eventName) {
        long deadline = System.currentTimeMillis() + 15000;

        while (System.currentTimeMillis() < deadline) {
            if (recyclerContainsEvent(scenario, eventName)) {
                return;
            }
            SystemClock.sleep(300);
        }

        throw new AssertionError("Timed out waiting for event: " + eventName);
    }

    /**
     * Waits briefly for a local filter update to settle.
     */
    private void waitForFilterUpdate() {
        SystemClock.sleep(500);
    }

    /**
     * Checks whether the recycler currently contains an event title.
     *
     * @param scenario dashboard scenario
     * @param eventName event title to search for
     * @return true if the recycler contains the event
     */
    private boolean recyclerContainsEvent(ActivityScenario<EntrantDashboardActivity> scenario,
                                          String eventName) {
        final boolean[] found = {false};

        scenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvEntrantEvents);
            RecyclerView.Adapter adapter = recyclerView.getAdapter();

            if (adapter == null) {
                return;
            }

            for (int i = 0; i < adapter.getItemCount(); i++) {
                RecyclerView.ViewHolder holder = adapter.createViewHolder(
                        recyclerView,
                        adapter.getItemViewType(i)
                );
                adapter.bindViewHolder(holder, i);

                TextView titleView = holder.itemView.findViewById(R.id.tvEventName);
                if (titleView != null && eventName.equals(titleView.getText().toString())) {
                    found[0] = true;
                    break;
                }
            }
        });

        return found[0];
    }

    /**
     * Asserts that the recycler contains an event title.
     *
     * @param scenario dashboard scenario
     * @param eventName event title expected in the recycler
     */
    private void assertEventPresent(ActivityScenario<EntrantDashboardActivity> scenario,
                                    String eventName) {
        assertTrue("Expected recycler to contain " + eventName, recyclerContainsEvent(scenario, eventName));
    }

    /**
     * Asserts that the recycler does not contain an event title.
     *
     * @param scenario dashboard scenario
     * @param eventName event title expected to be absent
     */
    private void assertEventAbsent(ActivityScenario<EntrantDashboardActivity> scenario,
                                   String eventName) {
        assertFalse("Expected recycler not to contain " + eventName, recyclerContainsEvent(scenario, eventName));
    }

    /**
     * Opens the filter dialog.
     */
    private void openFilterDialog() {
        onView(withId(R.id.btnFilter)).perform(click());
        onView(withText("Filter events")).check(matches(isDisplayed()));
    }

    /**
     * Applies the currently selected filter options.
     */
    private void applyFilters() {
        onView(withId(R.id.btnApplyFilters)).perform(click());
        waitForFilterUpdate();
    }

    /** Activity launches and reaches RESUMED state. */
    @Test
    public void activityLaunches_reachesResumedState() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Activity instance is not null on launch. */
    @Test
    public void activityInstance_isNotNull() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            scenario.onActivity(activity -> assertNotNull(activity));
        }
    }

    /** Activity survives recreation (rotation). */
    @Test
    public void activityRecreated_doesNotCrash() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            scenario.recreate();
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Activity can go to background and return. */
    @Test
    public void activityPausedThenResumed_isResumed() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            scenario.moveToState(Lifecycle.State.STARTED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    /** Activity can be moved to CREATED state without crashing. */
    @Test
    public void activityMovedToCreated_doesNotCrash() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            scenario.moveToState(Lifecycle.State.CREATED);
            assertEquals(Lifecycle.State.CREATED, scenario.getState());
        }
    }

    /** Search filters events by name. */
    @Test
    public void searchByName_filtersVisibleEvents() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            waitForEvent(scenario, NAME_OPEN_AVAILABLE);

            onView(withId(R.id.etSearch))
                    .perform(replaceText("Available Alpha"), closeSoftKeyboard());

            waitForFilterUpdate();

            assertEventPresent(scenario, NAME_OPEN_AVAILABLE);
            assertEventAbsent(scenario, NAME_OPEN_FULL);
        }
    }

    /** Search filters events by venue address. */
    @Test
    public void searchByVenueAddress_filtersVisibleEvents() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            waitForEvent(scenario, NAME_OPEN_AVAILABLE);

            onView(withId(R.id.etSearch))
                    .perform(replaceText("North Search Hall"), closeSoftKeyboard());

            waitForFilterUpdate();

            assertEventPresent(scenario, NAME_OPEN_AVAILABLE);
            assertEventAbsent(scenario, NAME_OPEN_FULL);
        }
    }

    /** Registration open filter hides upcoming and closed events. */
    @Test
    public void registrationOpenFilter_hidesUpcomingAndClosedEvents() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            waitForEvent(scenario, NAME_OPEN_AVAILABLE);

            openFilterDialog();
            onView(withId(R.id.chipRegistrationOpen)).perform(click());
            applyFilters();

            assertEventPresent(scenario, NAME_OPEN_AVAILABLE);
            assertEventAbsent(scenario, NAME_UPCOMING);
            assertEventAbsent(scenario, NAME_CLOSED);
        }
    }

    /** Available spots filter hides full open events. */
    @Test
    public void availableSpotsFilter_hidesFullOpenEvents() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            waitForEvent(scenario, NAME_OPEN_AVAILABLE);

            openFilterDialog();
            onView(withId(R.id.chipRegistrationOpen)).perform(click());
            onView(withId(R.id.cbOnlyAvailableSpots)).perform(click());
            applyFilters();

            assertEventPresent(scenario, NAME_OPEN_AVAILABLE);
            assertEventAbsent(scenario, NAME_OPEN_FULL);
        }
    }

    /** Not joined filter hides events already joined by this entrant. */
    @Test
    public void participationNotJoined_hidesJoinedEvents() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            waitForEvent(scenario, NAME_OPEN_AVAILABLE);

            openFilterDialog();
            onView(withId(R.id.chipParticipationNotJoined)).perform(click());
            applyFilters();

            assertEventPresent(scenario, NAME_OPEN_AVAILABLE);
            assertEventAbsent(scenario, NAME_WAITING);
            assertEventAbsent(scenario, NAME_SELECTED);
            assertEventAbsent(scenario, NAME_ENROLLED);
        }
    }

    /** Waiting filter shows only waiting events for this entrant. */
    @Test
    public void participationWaiting_showsOnlyWaitingEvents() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            waitForEvent(scenario, NAME_OPEN_AVAILABLE);

            openFilterDialog();
            onView(withId(R.id.chipParticipationWaiting)).perform(click());
            applyFilters();

            assertEventPresent(scenario, NAME_WAITING);
            assertEventAbsent(scenario, NAME_SELECTED);
            assertEventAbsent(scenario, NAME_ENROLLED);
        }
    }

    /** Selected filter shows only selected events for this entrant. */
    @Test
    public void participationSelected_showsOnlySelectedEvents() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            waitForEvent(scenario, NAME_OPEN_AVAILABLE);

            openFilterDialog();
            onView(withId(R.id.chipParticipationSelected)).perform(click());
            applyFilters();

            assertEventPresent(scenario, NAME_SELECTED);
            assertEventAbsent(scenario, NAME_WAITING);
            assertEventAbsent(scenario, NAME_ENROLLED);
        }
    }

    /** Enrolled filter shows only enrolled events for this entrant. */
    @Test
    public void participationEnrolled_showsOnlyEnrolledEvents() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            waitForEvent(scenario, NAME_OPEN_AVAILABLE);

            openFilterDialog();
            onView(withId(R.id.chipParticipationEnrolled)).perform(click());
            applyFilters();

            assertEventPresent(scenario, NAME_ENROLLED);
            assertEventAbsent(scenario, NAME_WAITING);
            assertEventAbsent(scenario, NAME_SELECTED);
        }
    }

    /** Reset restores the default filter state. */
    @Test
    public void resetFilters_restoresDefaultResults() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            waitForEvent(scenario, NAME_OPEN_AVAILABLE);

            openFilterDialog();
            onView(withId(R.id.chipParticipationWaiting)).perform(click());
            onView(withId(R.id.btnResetFilters)).perform(click());
            applyFilters();

            assertEventPresent(scenario, NAME_OPEN_AVAILABLE);
            assertEventPresent(scenario, NAME_WAITING);
        }
    }

    /** Search and filters work together on the dashboard. */
    @Test
    public void searchAndFiltersTogether_narrowResults() {
        try (ActivityScenario<EntrantDashboardActivity> scenario = ActivityScenario.launch(makeIntent())) {
            waitForEvent(scenario, NAME_OPEN_AVAILABLE);

            onView(withId(R.id.etSearch))
                    .perform(replaceText("Search"), closeSoftKeyboard());

            openFilterDialog();
            onView(withId(R.id.chipRegistrationOpen)).perform(click());
            onView(withId(R.id.cbOnlyAvailableSpots)).perform(click());
            onView(withId(R.id.chipParticipationNotJoined)).perform(click());
            applyFilters();

            assertEventPresent(scenario, NAME_OPEN_AVAILABLE);
            assertEventAbsent(scenario, NAME_OPEN_FULL);
            assertEventAbsent(scenario, NAME_WAITING);
            assertEventAbsent(scenario, NAME_UPCOMING);
            assertEventAbsent(scenario, NAME_CLOSED);
        }
    }
}
