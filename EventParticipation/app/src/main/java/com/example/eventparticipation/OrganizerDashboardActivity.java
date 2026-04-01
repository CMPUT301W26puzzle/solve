package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Organizer dashboard screen that displays summary statistics and event cards.
 *
 * <p>This screen serves as the main entry point to organizer actions such as managing
 * an event, viewing entrants, and opening related event tools.</p>
 */
public class OrganizerDashboardActivity extends AppCompatActivity {

    /** Demo organizer id currently used to query event data. */
    private static final String ORGANIZER_ID = "organizer_demo_001";

    /** RecyclerView showing organizer events. */
    private RecyclerView rvEvents;

    /** Adapter for the event list. */
    private EventAdapter eventAdapter;

    /** Backing event data set. */
    private List<Event> eventList;

    /** Statistic showing total number of events. */
    private TextView tvTotalEvents;

    /** Statistic showing currently active events. */
    private TextView tvActiveEvents;

    /** Statistic showing total event capacity. */
    private TextView tvTotalCapacity;

    /** Loading state container. */
    private View layoutLoading;

    /** Empty/error state container. */
    private View layoutEmptyState;

    /** Firestore database reference. */
    private FirebaseFirestore db;

    /** Prevents duplicate concurrent loads. */
    private boolean isLoading = false;

    /**
     * Initializes the dashboard.
     *
     * @param savedInstanceState previously saved state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
    }

    /**
     * Reload events whenever returning to this screen.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    /**
     * Binds layout views.
     */
    private void initViews() {
        rvEvents = findViewById(R.id.rvEvents);
        tvTotalEvents = findViewById(R.id.tvTotalEvents);
        tvActiveEvents = findViewById(R.id.tvActiveEvents);
        tvTotalCapacity = findViewById(R.id.tvTotalCapacity);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        findViewById(R.id.btnBackToEntrant).setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectRoleActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Configures the dashboard RecyclerView and item click callbacks.
     */
    private void setupRecyclerView() {
        eventList = new ArrayList<>();

        eventAdapter = new EventAdapter(eventList, new OnEventClickListener() {
            @Override
            public void onManageClick(Event event) {
                Intent intent = new Intent(
                        OrganizerDashboardActivity.this,
                        ManageEventActivity.class
                );
                intent.putExtra("EVENT_ID", event.getId());
                intent.putExtra("ORGANIZER_ID", ORGANIZER_ID);
                startActivity(intent);
            }

            @Override
            public void onEntrantsClick(Event event) {
                Intent intent = new Intent(
                        OrganizerDashboardActivity.this,
                        EntrantListActivity.class
                );
                intent.putExtra("EVENT_ID", event.getId());
                intent.putExtra("ORGANIZER_ID", ORGANIZER_ID);
                startActivity(intent);
            }

            @Override
            public void onLotteryClick(Event event) {
                Toast.makeText(
                        OrganizerDashboardActivity.this,
                        "Lottery feature coming soon",
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onQRCodeClick(Event event) {
                Toast.makeText(
                        OrganizerDashboardActivity.this,
                        "QR Code feature coming soon",
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onViewClick(Event event) {
                Toast.makeText(
                        OrganizerDashboardActivity.this,
                        "View event feature coming soon",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(eventAdapter);
    }

    /**
     * Loads organizer events from Firestore.
     *
     * <p>Reads organizer event references from:
     * organizers/{organizerId}/events
     * then fetches the full event documents from:
     * events/{eventId}</p>
     */
    private void loadEvents() {
        if (isLoading) {
            return;
        }
        isLoading = true;

        layoutLoading.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        rvEvents.setVisibility(View.GONE);

        db.collection("organizers")
                .document(ORGANIZER_ID)
                .collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    eventList.clear();
                    eventAdapter.notifyDataSetChanged();

                    if (querySnapshot.isEmpty()) {
                        isLoading = false;
                        layoutLoading.setVisibility(View.GONE);
                        layoutEmptyState.setVisibility(View.VISIBLE);
                        rvEvents.setVisibility(View.GONE);
                        return;
                    }

                    Set<String> loadedEventIds = new HashSet<>();
                    final int[] remaining = {querySnapshot.size()};

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String eventId = doc.getString("eventId");

                        // Skip invalid references
                        if (eventId == null || eventId.trim().isEmpty()) {
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                isLoading = false;
                                finishLoading();
                            }
                            continue;
                        }

                        // Skip duplicate references to the same event
                        if (loadedEventIds.contains(eventId)) {
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                isLoading = false;
                                finishLoading();
                            }
                            continue;
                        }

                        loadedEventIds.add(eventId);

                        db.collection("events")
                                .document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {
                                    if (eventDoc.exists()) {
                                        Event event = eventDoc.toObject(Event.class);

                                        if (event != null) {
                                            event.setId(eventDoc.getId());
                                            eventList.add(event);
                                        }
                                    }

                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        isLoading = false;
                                        finishLoading();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        isLoading = false;
                                        finishLoading();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    layoutLoading.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    rvEvents.setVisibility(View.GONE);

                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Finishes loading events and updates UI.
     */
    private void finishLoading() {
        updateStatistics();
        eventAdapter.notifyDataSetChanged();

        layoutLoading.setVisibility(View.GONE);

        if (eventList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Recomputes dashboard statistics from the currently loaded events.
     */
    private void updateStatistics() {
        int totalEvents = eventList.size();
        int activeEvents = 0;
        int totalCapacity = 0;

        Date now = new Date();

        for (Event event : eventList) {
            if (event.getRegistrationEnd() != null && event.getRegistrationEnd().after(now)) {
                activeEvents++;
            }

            // If your Event model uses capacity, replace this with event.getCapacity()
            Integer capacity = event.getCapacity();
            if (capacity != null) {
                totalCapacity += capacity;
            }
        }

        tvTotalEvents.setText(String.valueOf(totalEvents));
        tvActiveEvents.setText(String.valueOf(activeEvents));
        tvTotalCapacity.setText(String.valueOf(totalCapacity));
    }

    /**
     * Sets click listeners for buttons.
     */
    private void setupListeners() {
        MaterialButton btnCreateEvent = findViewById(R.id.btnCreateEvent);
        btnCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(
                    OrganizerDashboardActivity.this,
                    CreateEventActivity.class
            );
            startActivity(intent);
        });

        MaterialButton btnCreateEventEmpty = findViewById(R.id.btnCreateEventEmpty);
        if (btnCreateEventEmpty != null) {
            btnCreateEventEmpty.setOnClickListener(v -> {
                Intent intent = new Intent(
                        OrganizerDashboardActivity.this,
                        CreateEventActivity.class
                );
                startActivity(intent);
            });
        }
    }
}