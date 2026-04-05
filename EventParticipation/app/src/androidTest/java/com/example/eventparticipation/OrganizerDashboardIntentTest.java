package com.example.eventparticipation;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Intent tests for OrganizerDashboardActivity.
 *
 * <p>These tests verify that dashboard actions launch the correct destination
 * activities and pass the expected event and organizer extras.</p>
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerDashboardIntentTest {

    /** Fixed test event id used across all dashboard intent tests. */
    private static final String EVENT_ID = "event_001";

    /** Fixed test organizer id used across all dashboard intent tests. */
    private static final String ORGANIZER_ID = "organizer_demo_001";

    private ActivityScenario<CreateEventActivity> scenario;

    @Before
    public void setUp() {
        Intents.init();

        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession("test_organizer_id", "organizer");
        scenario = ActivityScenario.launch(CreateEventActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }

        Intents.release();

        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).clearSession();
    }

    /**
     * Verifies that clicking the Manage action launches ManageEventActivity
     * with the correct event and organizer extras.
     */
    @Test
    public void clickingManage_launchesManageEventActivity_withCorrectExtras() {
        try (ActivityScenario<OrganizerDashboardActivity> scenario =
                     ActivityScenario.launch(OrganizerDashboardActivity.class)) {

            scenario.onActivity(activity -> {
                try {
                    setOrganizerId(activity, ORGANIZER_ID);

                    List<Event> eventList = getEventList(activity);
                    eventList.clear();

                    Event event = new Event();
                    event.setId(EVENT_ID);
                    event.setName("Sample Event");
                    event.setRegistrationStart(new java.util.Date());
                    event.setRegistrationEnd(new java.util.Date());
                    event.setCapacity(100);

                    eventList.add(event);

                    invokeSetupRecyclerView(activity);

                    OnEventClickListener listener = getAdapterListener(activity);
                    listener.onManageClick(event);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            intended(hasComponent(ManageEventActivity.class.getName()));
            intended(hasExtra("EVENT_ID", EVENT_ID));
            intended(hasExtra("organizerId", ORGANIZER_ID));
        }
    }

    /**
     * Verifies that clicking the Entrants action launches EntrantListActivity
     * with the correct event and organizer extras.
     */
    @Test
    public void clickingEntrants_launchesEntrantListActivity_withCorrectExtras() {
        try (ActivityScenario<OrganizerDashboardActivity> scenario =
                     ActivityScenario.launch(OrganizerDashboardActivity.class)) {

            scenario.onActivity(activity -> {
                try {
                    setOrganizerId(activity, ORGANIZER_ID);

                    Event event = new Event();
                    event.setId(EVENT_ID);
                    event.setName("Sample Event");

                    invokeSetupRecyclerView(activity);

                    OnEventClickListener listener = getAdapterListener(activity);
                    listener.onEntrantsClick(event);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            intended(hasComponent(EntrantListActivity.class.getName()));
            intended(hasExtra("EVENT_ID", EVENT_ID));
            intended(hasExtra("organizerId", ORGANIZER_ID));
        }
    }

    /**
     * Returns the dashboard event list. Creates one if needed.
     */
    @SuppressWarnings("unchecked")
    private List<Event> getEventList(OrganizerDashboardActivity activity) throws Exception {
        Field field = OrganizerDashboardActivity.class.getDeclaredField("eventList");
        field.setAccessible(true);
        Object value = field.get(activity);
        if (value == null) {
            List<Event> list = new ArrayList<>();
            field.set(activity, list);
            return list;
        }
        return (List<Event>) value;
    }

    /**
     * Invokes the private RecyclerView setup method.
     */
    private void invokeSetupRecyclerView(OrganizerDashboardActivity activity) throws Exception {
        Method method = OrganizerDashboardActivity.class.getDeclaredMethod("setupRecyclerView");
        method.setAccessible(true);
        method.invoke(activity);
    }

    /**
     * Returns the adapter listener used for dashboard item actions.
     */
    private OnEventClickListener getAdapterListener(OrganizerDashboardActivity activity) throws Exception {
        Field adapterField = OrganizerDashboardActivity.class.getDeclaredField("eventAdapter");
        adapterField.setAccessible(true);
        EventAdapter adapter = (EventAdapter) adapterField.get(activity);

        Field listenerField = EventAdapter.class.getDeclaredField("listener");
        listenerField.setAccessible(true);
        return (OnEventClickListener) listenerField.get(adapter);
    }

    /**
     * Overrides the private organizerId field so the dashboard uses
     * a fixed organizer id during testing.
     */
    private void setOrganizerId(OrganizerDashboardActivity activity, String organizerId) throws Exception {
        Field field = OrganizerDashboardActivity.class.getDeclaredField("organizerId");
        field.setAccessible(true);
        field.set(activity, organizerId);
    }
}