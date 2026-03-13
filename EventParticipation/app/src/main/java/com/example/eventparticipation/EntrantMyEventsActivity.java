package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * My Events screen showing the entrant's registered events grouped by status.
 *
 * <p>Tabs: Waiting | Selected | Enrolled | Past</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 *     <li>US 01.05.01 - Another chance if selected user declines (shows re-selected status)</li>
 * </ul>
 */
public class EntrantMyEventsActivity extends BaseEntrantActivity {

    /** Hardcoded device ID until auth is implemented. */
    private static final String DEVICE_ID = "device_demo_001";

    private TextView tabWaiting, tabSelected, tabEnrolled, tabPast;
    private TextView tvTotalRegistrations, tvEnrolledCount;
    private TextView tvEmptyMessage;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBar;
    private RecyclerView rvMyEvents;

    private FirebaseFirestore db;
    private String currentTab = "waiting";

    /** All events the user has joined, mapped with their status. */
    private List<MyEventItem> allItems = new ArrayList<>();
    private MyEventAdapter adapter;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

    /**
     * Simple data class pairing an Event with the entrant's status for it.
     */
    static class MyEventItem {
        Event event;
        String status; // "waiting", "selected", "enrolled", "past"

        MyEventItem(Event event, String status) {
            this.event = event;
            this.status = status;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_my_events);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupTabs();
        setupRecyclerView();
        setupBottomNav(R.id.nav_my_events);
        loadMyEvents();
    }

    private void initViews() {
        tabWaiting           = findViewById(R.id.tabWaiting);
        tabSelected          = findViewById(R.id.tabSelected);
        tabEnrolled          = findViewById(R.id.tabEnrolled);
        tabPast              = findViewById(R.id.tabPast);
        tvTotalRegistrations = findViewById(R.id.tvTotalRegistrations);
        tvEnrolledCount      = findViewById(R.id.tvEnrolledCount);
        tvEmptyMessage       = findViewById(R.id.tvEmptyMessage);
        layoutEmptyState     = findViewById(R.id.layoutEmptyState);
        progressBar          = findViewById(R.id.progressBar);
        rvMyEvents           = findViewById(R.id.rvMyEvents);
    }

    private void setupTabs() {
        tabWaiting.setOnClickListener(v -> switchTab("waiting"));
        tabSelected.setOnClickListener(v -> switchTab("selected"));
        tabEnrolled.setOnClickListener(v -> switchTab("enrolled"));
        tabPast.setOnClickListener(v -> switchTab("past"));
    }

    private void setupRecyclerView() {
        adapter = new MyEventAdapter(new ArrayList<>());
        rvMyEvents.setLayoutManager(new LinearLayoutManager(this));
        rvMyEvents.setAdapter(adapter);
    }

    /**
     * Switches the active tab and refreshes the displayed list.
     *
     * @param tab one of "waiting", "selected", "enrolled", "past"
     */
    private void switchTab(String tab) {
        currentTab = tab;

        // Reset all tabs
        int unselected = 0x88888888;
        tabWaiting.setTextColor(unselected);
        tabSelected.setTextColor(unselected);
        tabEnrolled.setTextColor(unselected);
        tabPast.setTextColor(unselected);
        tabWaiting.setBackground(null);
        tabSelected.setBackground(null);
        tabEnrolled.setBackground(null);
        tabPast.setBackground(null);

        // Highlight selected tab
        TextView activeTab = tabWaiting;
        switch (tab) {
            case "selected": activeTab = tabSelected; break;
            case "enrolled": activeTab = tabEnrolled; break;
            case "past":     activeTab = tabPast;     break;
        }
        activeTab.setTextColor(0xFF000000);
        activeTab.setBackgroundResource(R.drawable.tab_selected_bg);

        filterAndDisplay();
    }

    /**
     * Loads all events the device has joined by querying each event's waitingList subcollection.
     * For each event in the top-level "events" collection, checks if DEVICE_ID is present.
     */
    private void loadMyEvents() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        rvMyEvents.setVisibility(View.GONE);
        allItems.clear();

        db.collection("events").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> docs = querySnapshot.getDocuments();
                    if (docs.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        showEmpty("You haven't joined any events yet");
                        return;
                    }

                    // Counter to track async completions
                    final int[] remaining = {docs.size()};

                    for (DocumentSnapshot doc : docs) {
                        Event event = doc.toObject(Event.class);
                        if (event == null) {
                            remaining[0]--;
                            if (remaining[0] == 0) onAllLoaded();
                            continue;
                        }
                        event.setId(doc.getId());

                        // Check if this device is in the waitingList subcollection
                        db.collection("events").document(doc.getId())
                                .collection("waitingList").document(DEVICE_ID)
                                .get()
                                .addOnSuccessListener(waitDoc -> {
                                    if (waitDoc.exists()) {
                                        String status = waitDoc.getString("status");
                                        if (status == null) status = "waiting";
                                        allItems.add(new MyEventItem(event, status));
                                    }
                                    remaining[0]--;
                                    if (remaining[0] == 0) onAllLoaded();
                                })
                                .addOnFailureListener(e -> {
                                    remaining[0]--;
                                    if (remaining[0] == 0) onAllLoaded();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Called when all async Firestore checks are complete.
     * Updates stats and displays the current tab's events.
     */
    private void onAllLoaded() {
        progressBar.setVisibility(View.GONE);
        updateStats();
        switchTab(currentTab);
    }

    /**
     * Updates the stats card with total registrations and enrolled count.
     */
    private void updateStats() {
        int total = allItems.size();
        int enrolled = 0;
        for (MyEventItem item : allItems) {
            if ("enrolled".equals(item.status)) enrolled++;
        }
        tvTotalRegistrations.setText(String.valueOf(total));
        tvEnrolledCount.setText(String.valueOf(enrolled));
    }

    /**
     * Filters allItems by current tab status and updates the RecyclerView.
     */
    private void filterAndDisplay() {
        List<MyEventItem> filtered = new ArrayList<>();
        for (MyEventItem item : allItems) {
            if (item.status.equals(currentTab)) {
                filtered.add(item);
            }
        }

        adapter.updateItems(filtered);

        if (filtered.isEmpty()) {
            rvMyEvents.setVisibility(View.GONE);
            switch (currentTab) {
                case "waiting":  showEmpty("You're not on any waiting lists"); break;
                case "selected": showEmpty("You haven't been selected yet"); break;
                case "enrolled": showEmpty("You're not enrolled in any events"); break;
                case "past":     showEmpty("No past events"); break;
            }
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvMyEvents.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty(String message) {
        tvEmptyMessage.setText(message);
        layoutEmptyState.setVisibility(View.VISIBLE);
        rvMyEvents.setVisibility(View.GONE);
    }

//    private void setupBottomNav() {
//        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
//        bottomNav.setSelectedItemId(R.id.nav_my_events);
//
//        bottomNav.setOnItemSelectedListener(item -> {
//            int id = item.getItemId();
//            if (id == R.id.nav_my_events) {
//                return true;
//            } else if (id == R.id.nav_home) {
//                startActivity(new Intent(this, EntrantDashboardActivity.class));
//                finish();
//                return true;
//            } else if (id == R.id.nav_scan) {
//                Toast.makeText(this, "Scan coming soon", Toast.LENGTH_SHORT).show();
//                return true;
//            } else if (id == R.id.nav_notifications) {
//                Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show();
//                return true;
//            } else if (id == R.id.nav_profile) {
//                Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show();
//                return true;
//            }
//            return false;
//        });
//    }

    /**
     * Inline adapter for the My Events RecyclerView.
     */
    class MyEventAdapter extends RecyclerView.Adapter<MyEventAdapter.MyEventViewHolder> {

        private List<MyEventItem> items;

        MyEventAdapter(List<MyEventItem> items) {
            this.items = items;
        }

        void updateItems(List<MyEventItem> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MyEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_event, parent, false);
            return new MyEventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyEventViewHolder holder, int position) {
            MyEventItem item = items.get(position);
            Event event = item.event;

            holder.tvEventName.setText(event.getName() != null ? event.getName() : "");
            holder.tvEventLocation.setText(event.getVenueAddress() != null ? event.getVenueAddress() : "Location TBD");
            holder.tvEventDate.setText(event.getStartTime() != null ? dateFormat.format(event.getStartTime()) : "Date TBD");

            // Status badge
            holder.tvStatus.setText(capitalize(item.status));
            switch (item.status) {
                case "waiting":
                    holder.tvStatus.setTextColor(0xFFCC0000);
                    holder.tvStatus.setBackgroundResource(R.drawable.badge_status);
                    break;
                case "selected":
                    holder.tvStatus.setTextColor(0xFF1565C0);
                    holder.tvStatus.setBackgroundResource(R.drawable.badge_status);
                    break;
                case "enrolled":
                    holder.tvStatus.setTextColor(0xFF2E7D32);
                    holder.tvStatus.setBackgroundResource(R.drawable.badge_status);
                    break;
                default:
                    holder.tvStatus.setTextColor(0xFF888888);
                    holder.tvStatus.setBackgroundResource(R.drawable.badge_status);
                    break;
            }

            // Poster
            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(holder.ivEventPoster.getContext())
                        .load(android.net.Uri.parse(event.getPosterUrl()))
                        .centerCrop()
                        .into(holder.ivEventPoster);
            }

            // Tap to view detail
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(EntrantMyEventsActivity.this, EntrantEventDetailActivity.class);
                intent.putExtra("EVENT_ID", event.getId());
                intent.putExtra("ORGANIZER_ID", event.getOrganizerId());
                intent.putExtra("EVENT_NAME", event.getName());
                intent.putExtra("VENUE_ADDRESS", event.getVenueAddress());
                intent.putExtra("CAPACITY", event.getCapacity());
                intent.putExtra("ENROLLED_COUNT", event.getEnrolledCount());
                intent.putExtra("WAITING_COUNT", event.getWaitingCount());
                intent.putExtra("POSTER_URL", event.getPosterUrl());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String capitalize(String s) {
            if (s == null || s.isEmpty()) return s;
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }

        class MyEventViewHolder extends RecyclerView.ViewHolder {
            ImageView ivEventPoster;
            TextView tvEventName, tvStatus, tvEventDate, tvEventLocation, tvTag1;

            MyEventViewHolder(@NonNull View itemView) {
                super(itemView);
                ivEventPoster  = itemView.findViewById(R.id.ivEventPoster);
                tvEventName    = itemView.findViewById(R.id.tvEventName);
                tvStatus       = itemView.findViewById(R.id.tvStatus);
                tvEventDate    = itemView.findViewById(R.id.tvEventDate);
                tvEventLocation = itemView.findViewById(R.id.tvEventLocation);
                tvTag1         = itemView.findViewById(R.id.tvTag1);
            }
        }
    }
}
