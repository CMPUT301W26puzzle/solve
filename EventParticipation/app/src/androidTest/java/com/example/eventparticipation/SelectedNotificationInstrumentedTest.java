package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.button.MaterialButton;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * US 01.04.01
 * As an entrant, I want to receive a notification when I am chosen to participate
 * from the waiting list.
 *
 * This test does not use Espresso.
 * It replaces the RecyclerView adapter directly with a known test adapter.
 */
@RunWith(AndroidJUnit4.class)
public class SelectedNotificationInstrumentedTest {

    @Test
    public void selectedNotification_isVisible_andAcceptInvitationLaunchesDetail() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        IntentCaptureMonitor captureMonitor =
                new IntentCaptureMonitor(EntrantEventDetailActivity.class.getName());
        instrumentation.addMonitor(captureMonitor.monitor);

        ActivityScenario<NotificationsActivity> scenario =
                ActivityScenario.launch(NotificationsActivity.class);

        EntrantNotification notification = new EntrantNotification();
        notification.setId("notif_selected_001");
        notification.setEventId("event_001");
        notification.setOrganizerId("organizer_demo_001");
        notification.setEventName("Spring Music Festival");
        notification.setType("selected");
        notification.setMessage("Congratulations! You have been selected as a replacement for the event. Click to accept your invitation!");
        notification.setStatus("pending");
        notification.setRead(false);
        notification.setCreatedAt(new Date());

        AtomicReference<EntrantNotificationAdapter> adapterRef = new AtomicReference<>();

        scenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvNotifications);
            assertNotNull("RecyclerView is null", recyclerView);

            List<EntrantNotification> testNotifications = new ArrayList<>();
            testNotifications.add(notification);

            EntrantNotificationAdapter testAdapter =
                    new EntrantNotificationAdapter(testNotifications, clickedNotification -> {
                        Intent intent = new Intent(activity, EntrantEventDetailActivity.class);
                        intent.putExtra("EVENT_ID", clickedNotification.getEventId());
                        intent.putExtra("ORGANIZER_ID", clickedNotification.getOrganizerId());
                        intent.putExtra("EVENT_NAME", clickedNotification.getEventName());
                        intent.putExtra("NOTIFICATION_ID", clickedNotification.getId());
                        activity.startActivity(intent);
                    });

            adapterRef.set(testAdapter);

            recyclerView.setLayoutManager(new LinearLayoutManager(activity));
            recyclerView.setAdapter(testAdapter);
            testAdapter.notifyDataSetChanged();

            assertNotNull("Adapter was not set", recyclerView.getAdapter());
            assertEquals("Adapter item count should be 1", 1, testAdapter.getItemCount());

            recyclerView.measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(2400, View.MeasureSpec.AT_MOST)
            );
            recyclerView.layout(0, 0, 1080, 2400);
            recyclerView.scrollToPosition(0);
        });

        instrumentation.waitForIdleSync();
        SystemClock.sleep(800);

        scenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvNotifications);
            assertNotNull("RecyclerView is null during verification", recyclerView);

            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(0);

            if (holder == null) {
                RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = recyclerView.getAdapter();
                assertNotNull("RecyclerView adapter is null during verification", adapter);

                int viewType = adapter.getItemViewType(0);
                holder = adapter.createViewHolder(recyclerView, viewType);
                adapter.bindViewHolder(holder, 0);
            }

            assertNotNull("Could not create or find ViewHolder at position 0", holder);

            TextView tvMessage = holder.itemView.findViewById(R.id.tvMessage);
            assertNotNull("tvMessage is null", tvMessage);
            assertEquals(
                    "Congratulations! You have been selected as a replacement for the event. Click to accept your invitation!",
                    tvMessage.getText().toString()
            );

            MaterialButton btnAction = holder.itemView.findViewById(R.id.btnAction);
            assertNotNull("btnAction is null", btnAction);
            assertEquals(View.VISIBLE, btnAction.getVisibility());
            assertEquals("Accept Invitation", btnAction.getText().toString());

            btnAction.performClick();
        });

        Activity launched = instrumentation.waitForMonitorWithTimeout(captureMonitor.monitor, 5000);
        assertNotNull("EntrantEventDetailActivity was not launched", launched);

        Intent launchedIntent = launched.getIntent();
        assertNotNull("Launched activity intent is null", launchedIntent);

        assertEquals("event_001", launchedIntent.getStringExtra("EVENT_ID"));
        assertEquals("organizer_demo_001", launchedIntent.getStringExtra("ORGANIZER_ID"));
        assertEquals("Spring Music Festival", launchedIntent.getStringExtra("EVENT_NAME"));
        assertEquals("notif_selected_001", launchedIntent.getStringExtra("NOTIFICATION_ID"));

        launched.finish();
        instrumentation.removeMonitor(captureMonitor.monitor);
    }

    private static class IntentCaptureMonitor {
        final Instrumentation.ActivityMonitor monitor;
        volatile Intent lastStartedIntent;

        IntentCaptureMonitor(String activityClassName) {
            monitor = new Instrumentation.ActivityMonitor(activityClassName, null, false) {
                @Override
                public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                    lastStartedIntent = intent;
                    return super.onStartActivity(intent);
                }
            };
        }
    }
}