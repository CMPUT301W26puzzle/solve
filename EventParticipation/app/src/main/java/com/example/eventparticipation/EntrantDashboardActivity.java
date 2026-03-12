package com.example.eventparticipation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entrant dashboard showing all available events ("Discover Events").
 *
 * <p>Loads events from the top-level Firestore "events" collection and displays
 * them as scrollable cards. Tapping a card opens the event detail screen.</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 *     <li>US 01.01.01 - Join waiting list (navigates to detail)</li>
 *     <li>US 01.01.02 - Leave waiting list (navigates to detail)</li>
 *     <li>US 01.05.04 - Waiting list count shown on each card</li>
 * </ul>
 */
public class EntrantDashboardActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2001;

    private RecyclerView rvEntrantEvents;
    private EntrantEventAdapter eventAdapter;
    private List<Event> allEvents;
    private List<Event> filteredEvents;

    private EditText etSearch;
    private CardView btnFilter;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBar;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_dashboard);

        db = FirebaseFirestore.getInstance();

        NotificationHelper.createNotificationChannel(this);
        requestNotificationPermissionIfNeeded();

        initViews();
        setupRecyclerView();
        setupSearch();
        setupBottomNav();
        loadEvents();
    }

    /**
     * Binds layout views.
     */
    private void initViews() {
        rvEntrantEvents  = findViewById(R.id.rvEntrantEvents);
        etSearch         = findViewById(R.id.etSearch);
        btnFilter        = findViewById(R.id.btnFilter);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        progressBar      = findViewById(R.id.progressBar);

        btnFilter.setOnClickListener(v ->
            Toast.makeText(this, "Filter coming soon", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Sets up the RecyclerView and adapter.
     */
    private void setupRecyclerView() {
        allEvents      = new ArrayList<>();
        filteredEvents = new ArrayList<>();

        eventAdapter = new EntrantEventAdapter(filteredEvents, event -> {
            Intent intent = new Intent(this, EntrantEventDetailActivity.class);
            intent.putExtra("EVENT_ID", event.getId());
            intent.putExtra("ORGANIZER_ID", event.getOrganizerId());
            intent.putExtra("EVENT_NAME", event.getName());
            intent.putExtra("VENUE_ADDRESS", event.getVenueAddress());
            intent.putExtra("CAPACITY", event.getCapacity());
            intent.putExtra("ENROLLED_COUNT", event.getEnrolledCount());
            intent.putExtra("WAITING_COUNT", event.getWaitingCount());
            startActivity(intent);
        });

        rvEntrantEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEntrantEvents.setAdapter(eventAdapter);
    }

    /**
     * Filters the event list as the user types in the search bar.
     */
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Wires the bottom navigation bar.
     */
    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_my_events) {
                return true;
            } else if (id == R.id.nav_scan) {
                Toast.makeText(this, "Scan coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    /**
     * Loads all events from the top-level Firestore "events" collection.
     */
    private void loadEvents() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        rvEntrantEvents.setVisibility(View.GONE);

        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        allEvents.add(event);
                    }
                    filteredEvents.clear();
                    filteredEvents.addAll(allEvents);
                    eventAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    if (filteredEvents.isEmpty()) {
                        layoutEmptyState.setVisibility(View.VISIBLE);
                        rvEntrantEvents.setVisibility(View.GONE);
                    } else {
                        layoutEmptyState.setVisibility(View.GONE);
                        rvEntrantEvents.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_LONG).show();
                });
    }

    private void filterEvents(String query) {
        filteredEvents.clear();

        if (query.isEmpty()) {
            filteredEvents.addAll(allEvents);
        } else {
            String lower = query.toLowerCase();
            for (Event event : allEvents) {
                if (event.getName() != null && event.getName().toLowerCase().contains(lower)) {
                    filteredEvents.add(event);
                }
            }
        }

        eventAdapter.notifyDataSetChanged();

        if (filteredEvents.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvEntrantEvents.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvEntrantEvents.setVisibility(View.VISIBLE);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }
    }
}
