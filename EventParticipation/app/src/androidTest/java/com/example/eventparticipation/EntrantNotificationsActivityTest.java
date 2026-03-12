package com.example.eventparticipation;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;

@RunWith(AndroidJUnit4.class)
public class EntrantNotificationsActivityTest {

    @Test
    public void selectedNotification_showsInvitationActions() {
        try (ActivityScenario<EntrantNotificationsActivity> scenario =
                     ActivityScenario.launch(makeIntent(makeSelectedNotification()))) {
            onView(withText("Accept Invitation")).check(matches(isDisplayed()));
            onView(withText("Decline")).check(matches(isDisplayed()));
            onView(withText("5 minutes ago")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void acceptInvitation_updatesCardState() {
        try (ActivityScenario<EntrantNotificationsActivity> scenario =
                     ActivityScenario.launch(makeIntent(makeSelectedNotification()))) {
            onView(withText("Accept Invitation")).perform(click());
            onView(withText("Invitation accepted")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void notSelectedNotification_hasNoInvitationButton() {
        try (ActivityScenario<EntrantNotificationsActivity> scenario =
                     ActivityScenario.launch(makeIntent(makeNotSelectedNotification()))) {
            onView(withText("Accept Invitation")).check(doesNotExist());
            onView(withText("You were not selected in this draw for Tech Night. You remain on the waiting list."))
                    .check(matches(isDisplayed()));
        }
    }

    private Intent makeIntent(NotificationItem item) {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantNotificationsActivity.class
        );
        ArrayList<NotificationItem> items = new ArrayList<>();
        items.add(item);
        intent.putExtra(EntrantNotificationsActivity.EXTRA_TEST_MODE, true);
        intent.putExtra(EntrantNotificationsActivity.EXTRA_TEST_NOTIFICATIONS, items);
        return intent;
    }

    private NotificationItem makeSelectedNotification() {
        NotificationItem item = new NotificationItem();
        item.setId("notification_1");
        item.setEventId("event_1");
        item.setEventName("Tech Night");
        item.setType(NotificationItem.TYPE_SELECTED);
        item.setMessage(NotificationActionHelper.buildSelectedMessage("Tech Night"));
        item.setUnread(true);
        item.setActionRequired(true);
        item.setActionStatus(NotificationItem.ACTION_PENDING);
        item.setCreatedAt(new Date(System.currentTimeMillis() - 5 * 60 * 1000L));
        return item;
    }

    private NotificationItem makeNotSelectedNotification() {
        NotificationItem item = new NotificationItem();
        item.setId("notification_2");
        item.setEventId("event_1");
        item.setEventName("Tech Night");
        item.setType(NotificationItem.TYPE_NOT_SELECTED);
        item.setMessage(NotificationActionHelper.buildNotSelectedMessage("Tech Night"));
        item.setUnread(false);
        item.setActionRequired(false);
        item.setActionStatus(NotificationItem.ACTION_NONE);
        item.setCreatedAt(new Date(System.currentTimeMillis() - 5 * 60 * 1000L));
        return item;
    }
}
