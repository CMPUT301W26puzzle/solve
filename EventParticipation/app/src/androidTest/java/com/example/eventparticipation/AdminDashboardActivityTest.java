package com.example.eventparticipation;

import static androidx.test.core.app.ActivityScenario.launch;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AdminDashboardActivityTest {

    /**
     * US 03.01.01
     * As an administrator, I want to be able to remove events.
     *
     * This test verifies that the delete action exists and opens the correct confirmation dialog.
     * That is the stable UI test for the remove-event flow.
     */
    @Test
    public void adminCanStartRemovingEvent() {
        try (ActivityScenario<AdminDashboardActivity> scenario = launch(AdminDashboardActivity.class)) {
            onView(withId(R.id.btnTabEvents)).perform(click());

            scenario.onActivity(activity -> {
                List<Object> fakeItems = new ArrayList<>();

                Event event = new Event();
                event.setId("event_delete_1");
                event.setName("Delete Me Event");
                event.setOrganizerId("org_delete");
                event.setCapacity(10);
                event.setRegistrationStart(new Date());

                fakeItems.add(new AdminEventItem(event));
                injectItems(activity, fakeItems);
            });

            onView(withId(R.id.rvAdminItems))
                    .check(matches(hasDescendant(withText("Delete Me Event"))));

            onView(withId(R.id.rvAdminItems))
                    .perform(clickChildViewWithIdAtPosition(0, R.id.btnDelete));

            onView(withText("Delete event?")).check(matches(isDisplayed()));
            onView(withText("This will permanently delete \"Delete Me Event\"."))
                    .check(matches(isDisplayed()));
            onView(withText("Delete")).check(matches(isDisplayed()));
            onView(withText("Cancel")).check(matches(isDisplayed()));
        }
    }

    /**
     * US 03.04.01
     * As an administrator, I want to browse events.
     */
    @Test
    public void adminCanBrowseEvents() {
        try (ActivityScenario<AdminDashboardActivity> scenario = launch(AdminDashboardActivity.class)) {
            onView(withId(R.id.btnTabEvents)).perform(click());

            scenario.onActivity(activity -> {
                List<Object> fakeItems = new ArrayList<>();

                Event event1 = new Event();
                event1.setId("event_1");
                event1.setName("Admin Test Event One");
                event1.setOrganizerId("org_1");
                event1.setCapacity(50);
                event1.setRegistrationStart(new Date());

                Event event2 = new Event();
                event2.setId("event_2");
                event2.setName("Admin Test Event Two");
                event2.setOrganizerId("org_2");
                event2.setCapacity(25);
                event2.setRegistrationStart(new Date());

                fakeItems.add(new AdminEventItem(event1));
                fakeItems.add(new AdminEventItem(event2));

                injectItems(activity, fakeItems);
            });

            onView(withId(R.id.tvSectionTitle)).check(matches(withText("All Events")));
            onView(withId(R.id.rvAdminItems)).check(matches(isDisplayed()));
            onView(withId(R.id.rvAdminItems))
                    .check(new RecyclerViewItemCountAssertion(greaterThanOrEqualTo(2)));
            onView(withId(R.id.rvAdminItems))
                    .check(matches(hasDescendant(withText("Admin Test Event One"))));
            onView(withId(R.id.rvAdminItems))
                    .check(matches(hasDescendant(withText("Admin Test Event Two"))));
        }
    }

    /**
     * US 03.05.01
     * As an administrator, I want to browse profiles.
     */
    @Test
    public void adminCanBrowseProfiles() {
        try (ActivityScenario<AdminDashboardActivity> scenario = launch(AdminDashboardActivity.class)) {
            onView(withId(R.id.btnTabProfiles)).perform(click());

            scenario.onActivity(activity -> {
                List<Object> fakeItems = new ArrayList<>();
                fakeItems.add(new AdminProfileItem("entrant_1", "entrant", "Entrant Test User", "entrant@test.com"));
                fakeItems.add(new AdminProfileItem("organizer_1", "organizer", "Organizer Test User", "organizer@test.com"));
                fakeItems.add(new AdminProfileItem("admin_1", "admin", "Admin Test User", "admin@test.com"));
                injectItems(activity, fakeItems);
            });

            onView(withId(R.id.tvSectionTitle)).check(matches(withText("All User Profiles")));
            onView(withId(R.id.rvAdminItems)).check(matches(isDisplayed()));
            onView(withId(R.id.rvAdminItems))
                    .check(new RecyclerViewItemCountAssertion(greaterThanOrEqualTo(3)));
            onView(withId(R.id.rvAdminItems))
                    .check(matches(hasDescendant(withText("Entrant Test User"))));
            onView(withId(R.id.rvAdminItems))
                    .check(matches(hasDescendant(withText("Organizer Test User"))));
            onView(withId(R.id.rvAdminItems))
                    .check(matches(hasDescendant(withText("Admin Test User"))));
        }
    }

    /**
     * US 03.06.01
     * As an administrator, I want to browse uploaded images for moderation purposes.
     */
    @Test
    public void adminCanBrowseUploadedImages() {
        try (ActivityScenario<AdminDashboardActivity> scenario = launch(AdminDashboardActivity.class)) {
            onView(withId(R.id.btnTabImages)).perform(click());

            onView(withId(R.id.tvSectionTitle))
                    .check(matches(withText("Uploaded Images")));

            onView(withId(R.id.tvSectionSubtitle))
                    .check(matches(withText("Browse uploaded event images for moderation")));
        }
    }

    /**
     * US 03.08.01
     * As an administrator, I want to review logs of all notifications sent to entrants.
     */
    @Test
    public void adminCanReviewNotificationLogs() {
        try (ActivityScenario<AdminDashboardActivity> scenario = launch(AdminDashboardActivity.class)) {
            onView(withId(R.id.btnTabLogs)).perform(click());

            scenario.onActivity(activity -> {
                List<Object> fakeItems = new ArrayList<>();
                fakeItems.add(new AdminNotificationLogItem(
                        "log_1",
                        "entrant_1",
                        "event_1",
                        "Admin Test Event One",
                        "WAITLIST_SELECTED",
                        "You were selected from the waitlist",
                        true,
                        true,
                        "pending",
                        new Date()
                ));
                injectItems(activity, fakeItems);
            });

            onView(withId(R.id.tvSectionTitle)).check(matches(withText("Notification Logs")));
            onView(withId(R.id.rvAdminItems)).check(matches(isDisplayed()));
            onView(withId(R.id.rvAdminItems))
                    .check(new RecyclerViewItemCountAssertion(greaterThanOrEqualTo(1)));
            onView(withId(R.id.rvAdminItems))
                    .check(matches(hasDescendant(withText("Admin Test Event One"))));
            onView(withId(R.id.rvAdminItems))
                    .check(matches(hasDescendant(withText("You were selected from the waitlist"))));
        }
    }



    private static void injectItems(AdminDashboardActivity activity, List<Object> fakeItems) {
        try {
            Field itemsField = AdminDashboardActivity.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> realItems = (List<Object>) itemsField.get(activity);
            realItems.clear();
            realItems.addAll(fakeItems);

            Field adapterField = AdminDashboardActivity.class.getDeclaredField("adapter");
            adapterField.setAccessible(true);
            RecyclerView.Adapter<?> adapter = (RecyclerView.Adapter<?>) adapterField.get(activity);

            Field progressBarField = AdminDashboardActivity.class.getDeclaredField("progressBar");
            progressBarField.setAccessible(true);
            View progressBar = (View) progressBarField.get(activity);

            Field recyclerField = AdminDashboardActivity.class.getDeclaredField("recyclerView");
            recyclerField.setAccessible(true);
            RecyclerView recyclerView = (RecyclerView) recyclerField.get(activity);

            Field emptyField = AdminDashboardActivity.class.getDeclaredField("tvEmpty");
            emptyField.setAccessible(true);
            View emptyView = (View) emptyField.get(activity);

            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject admin test items", e);
        }
    }

    public static ViewAction clickChildViewWithIdAtPosition(int position, int viewId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return withId(R.id.rvAdminItems);
            }

            @Override
            public String getDescription() {
                return "Click child view with id " + viewId + " at recycler position " + position;
            }

            @Override
            public void perform(UiController uiController, View view) {
                RecyclerView recyclerView = (RecyclerView) view;
                recyclerView.scrollToPosition(position);
                uiController.loopMainThreadUntilIdle();

                RecyclerView.ViewHolder viewHolder =
                        recyclerView.findViewHolderForAdapterPosition(position);

                if (viewHolder == null) {
                    throw new PerformException.Builder()
                            .withCause(new Throwable("No ViewHolder at position: " + position))
                            .build();
                }

                View child = viewHolder.itemView.findViewById(viewId);
                if (child == null) {
                    throw new PerformException.Builder()
                            .withCause(new Throwable("No child view with id: " + viewId))
                            .build();
                }

                child.performClick();
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static class RecyclerViewItemCountAssertion implements ViewAssertion {
        private final org.hamcrest.Matcher<Integer> matcher;

        public RecyclerViewItemCountAssertion(org.hamcrest.Matcher<Integer> matcher) {
            this.matcher = matcher;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();

            if (adapter == null) {
                throw new AssertionError("RecyclerView has no adapter");
            }

            if (!matcher.matches(adapter.getItemCount())) {
                throw new AssertionError(
                        "RecyclerView item count was " + adapter.getItemCount()
                                + " but expected " + matcher.toString()
                );
            }
        }
    }
}