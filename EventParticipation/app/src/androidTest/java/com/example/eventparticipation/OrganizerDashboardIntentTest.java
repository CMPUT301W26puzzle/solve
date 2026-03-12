package com.example.eventparticipation;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;

import android.content.Intent;

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
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerDashboardIntentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void clickingManage_launchesManageEventActivity_withCorrectExtras() {
        ActivityScenario<OrganizerDashboardActivity> scenario = ActivityScenario.launch(OrganizerDashboardActivity.class);

        scenario.onActivity(activity -> {
            try {
                List<Event> eventList = getEventList(activity);
                eventList.clear();

                Event event = new Event();
                event.setId("event_001");
                event.setName("Sample Event");
                event.setStartTime(new java.util.Date());
                event.setRegistrationStart(new java.util.Date());
                event.setRegistrationEnd(new java.util.Date());
                event.setCapacity(100);

                eventList.add(event);

                invokeSetupRecyclerView(activity);

                OnEventClickListener listener = getAdapterListener(activity);
                listener.onManageClick(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        intended(hasComponent(ManageEventActivity.class.getName()));
        intended(hasExtra("EVENT_ID", "event_001"));
        intended(hasExtra("ORGANIZER_ID", "organizer_demo_001"));
    }

    @Test
    public void clickingEntrants_launchesEntrantListActivity_withCorrectExtras() {
        try (ActivityScenario<OrganizerDashboardActivity> scenario = ActivityScenario.launch(OrganizerDashboardActivity.class)) {

            scenario.onActivity(activity -> {
                try {
                    Event event = new Event();
                    event.setId("event_001");
                    event.setName("Sample Event");

                    invokeSetupRecyclerView(activity);

                    OnEventClickListener listener = getAdapterListener(activity);
                    listener.onEntrantsClick(event);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            intended(hasComponent(EntrantListActivity.class.getName()));
            intended(hasExtra("EVENT_ID", "event_001"));
            intended(hasExtra("ORGANIZER_ID", "organizer_demo_001"));
        }
    }

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

    private void invokeSetupRecyclerView(OrganizerDashboardActivity activity) throws Exception {
        Method method = OrganizerDashboardActivity.class.getDeclaredMethod("setupRecyclerView");
        method.setAccessible(true);
        method.invoke(activity);
    }

    private OnEventClickListener getAdapterListener(OrganizerDashboardActivity activity) throws Exception {
        Field adapterField = OrganizerDashboardActivity.class.getDeclaredField("eventAdapter");
        adapterField.setAccessible(true);
        EventAdapter adapter = (EventAdapter) adapterField.get(activity);

        Field listenerField = EventAdapter.class.getDeclaredField("listener");
        listenerField.setAccessible(true);
        return (OnEventClickListener) listenerField.get(adapter);
    }
}