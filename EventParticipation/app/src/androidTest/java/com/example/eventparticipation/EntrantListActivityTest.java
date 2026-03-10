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

import com.google.android.material.tabs.TabLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso tests for viewing and filtering entrants in the waiting list.
 *
 * <p>User story covered:</p>
 * <ul>
 *     <li>US 02.02.01 - As an organizer, I want to view the list of entrants on my event waiting list</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class EntrantListActivityTest {

    /**
     * Verifies that real entrants are displayed when the screen opens.
     */
    @Test
    public void launchEntrantList_showsRealEntrants() throws InterruptedException {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantListActivity.class
        );
        intent.putExtra("EVENT_ID", "event_001");
        intent.putExtra("ORGANIZER_ID", "organizer_demo_001");

        ActivityScenario.launch(intent);

        Thread.sleep(3000);

        onView(withText("Tom Lee")).check(matches(isDisplayed()));
        onView(withText("Sarah Kim")).check(matches(isDisplayed()));
    }

    /**
     * Verifies that searching by email filters the visible list.
     */
    @Test
    public void searchEntrantsByEmail_showsOnlyMatchingEntrant() throws InterruptedException {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantListActivity.class
        );
        intent.putExtra("EVENT_ID", "event_001");
        intent.putExtra("ORGANIZER_ID", "organizer_demo_001");

        ActivityScenario.launch(intent);

        Thread.sleep(3000);

        onView(withId(R.id.etSearch))
                .perform(replaceText("tom@test.com"), closeSoftKeyboard());

        Thread.sleep(1000);

        onView(withText("Tom Lee")).check(matches(isDisplayed()));
        onView(withText("Sarah Kim")).check(doesNotExist());
    }

    /**
     * Verifies that searching by name filters the visible list.
     */
    @Test
    public void searchEntrantsByName_showsOnlyMatchingEntrant() throws InterruptedException {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantListActivity.class
        );
        intent.putExtra("EVENT_ID", "event_001");
        intent.putExtra("ORGANIZER_ID", "organizer_demo_001");

        ActivityScenario.launch(intent);

        Thread.sleep(3000);

        onView(withId(R.id.etSearch))
                .perform(replaceText("Sarah"), closeSoftKeyboard());

        Thread.sleep(1000);

        onView(withText("Sarah Kim")).check(matches(isDisplayed()));
        onView(withText("Tom Lee")).check(doesNotExist());
    }

    /**
     * Verifies that selecting the Waiting tab shows the current waiting entrants.
     */
    @Test
    public void selectingWaitingTab_showsWaitingEntrants() throws InterruptedException {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                EntrantListActivity.class
        );
        intent.putExtra("EVENT_ID", "event_001");
        intent.putExtra("ORGANIZER_ID", "organizer_demo_001");

        ActivityScenario<EntrantListActivity> scenario = ActivityScenario.launch(intent);

        Thread.sleep(3000);

        scenario.onActivity(activity -> {
            TabLayout tabLayout = activity.findViewById(R.id.tabLayout);
            TabLayout.Tab waitingTab = tabLayout.getTabAt(1);
            if (waitingTab != null) {
                waitingTab.select();
            }
        });

        Thread.sleep(1000);

        onView(withText("Tom Lee")).check(matches(isDisplayed()));
        onView(withText("Sarah Kim")).check(matches(isDisplayed()));
    }
}