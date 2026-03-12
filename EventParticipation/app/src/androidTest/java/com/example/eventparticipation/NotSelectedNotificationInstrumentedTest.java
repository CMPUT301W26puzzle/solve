package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

/**
 * US 01.04.02
 * As an entrant, I want to receive a notification when I am not chosen to participate.
 */
@RunWith(AndroidJUnit4.class)
public class NotSelectedNotificationInstrumentedTest {

    @Test
    public void notSelectedNotification_isVisible_andAcceptButtonIsHidden() {
        ActivityScenario<NotificationsActivity> scenario =
                ActivityScenario.launch(NotificationsActivity.class);

        EntrantNotification notification = new EntrantNotification();
        notification.setId("notif_not_selected_001");
        notification.setEventId("event_001");
        notification.setOrganizerId("organizer_demo_001");
        notification.setEventName("Spring Music Festival");
        notification.setType("not_selected");
        notification.setMessage("Thank you for your interest. You were not selected for this event.");
        notification.setStatus("pending");
        notification.setRead(false);
        notification.setCreatedAt(new Date());

        injectSingleNotification(scenario, notification);

        onView(withText("Thank you for your interest. You were not selected for this event."))
                .check(matches(isDisplayed()));

        onView(withId(R.id.btnAction))
                .check(matches(withEffectiveVisibility(GONE)));
    }

    @SuppressWarnings("unchecked")
    private void injectSingleNotification(ActivityScenario<NotificationsActivity> scenario,
                                          EntrantNotification notification) {
        scenario.onActivity(activity -> {
            try {
                Field notificationsField = NotificationsActivity.class.getDeclaredField("notifications");
                notificationsField.setAccessible(true);
                List<EntrantNotification> notifications =
                        (List<EntrantNotification>) notificationsField.get(activity);

                notifications.clear();
                notifications.add(notification);

                Field adapterField = NotificationsActivity.class.getDeclaredField("adapter");
                adapterField.setAccessible(true);
                RecyclerView.Adapter<?> adapter =
                        (RecyclerView.Adapter<?>) adapterField.get(activity);

                activity.runOnUiThread(adapter::notifyDataSetChanged);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}