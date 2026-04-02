package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard showing only events that the current user can access as a co-organizer.
 */
public class CoOrganizerDashboardActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private LinearLayout layoutEmptyState;

    private FirebaseFirestore db;
    private String currentUserId;

    private final List<Event> eventList = new ArrayList<>();
    private CoOrganizerEventAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_co_organizer_dashboard);

        db = FirebaseFirestore.getInstance();
        currentUserId = DeviceIdProvider.getId(this);

        applyWindowInsets();
        setupToolbar();
        initViews();
        setupRecyclerView();
        loadCoOrganizerEvents();
    }

    /**
     * Applies status bar insets to keep toolbar content below the system status bar.
     */
    private void applyWindowInsets() {
        Toolbar toolbar = findViewById(R.id.toolbar);

        final int originalPaddingLeft = toolbar.getPaddingLeft();
        final int originalPaddingTop = toolbar.getPaddingTop();
        final int originalPaddingRight = toolbar.getPaddingRight();
        final int originalPaddingBottom = toolbar.getPaddingBottom();
        final int originalToolbarHeight = getToolbarHeight();

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());

            view.setPadding(
                    originalPaddingLeft,
                    originalPaddingTop + insets.top,
                    originalPaddingRight,
                    originalPaddingBottom
            );

            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = originalToolbarHeight + insets.top;
            view.setLayoutParams(layoutParams);

            return windowInsets;
        });
    }

    private int getToolbarHeight() {
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimensionPixelSize(
                    typedValue.data,
                    getResources().getDisplayMetrics()
            );
        }
        return (int) (56 * getResources().getDisplayMetrics().density);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void initViews() {
        rvEvents = findViewById(R.id.rvCoOrganizerEvents);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
    }

    private void setupRecyclerView() {
        adapter = new CoOrganizerEventAdapter(eventList, event -> {
            Intent intent = new Intent(this, ManageEventActivity.class);
            intent.putExtra("EVENT_ID", event.getId());
            intent.putExtra("ORGANIZER_ID", event.getOrganizerId());
            startActivity(intent);
        });

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);
    }

    private void loadCoOrganizerEvents() {
        db.collection("events")
                .whereArrayContains("coOrganizerIds", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventList.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        eventList.add(event);
                    }

                    updateUI();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load co-organizer events", Toast.LENGTH_LONG).show());
    }

    private void updateUI() {
        if (eventList.isEmpty()) {
            rvEvents.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvEvents.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
    }
}