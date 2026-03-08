package com.example.eventparticipation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Organizer screen for viewing entrants who joined a specific event waitlist.
 *
 * <p>This activity supports tab-based filtering, search by name or email,
 * and empty-state handling.</p>
 *
 * <p>Relevant user story:</p>
 * <ul>
 *     <li>US 02.02.01 - View the list of entrants who joined the waiting list</li>
 * </ul>
 */
public class EntrantListActivity extends AppCompatActivity {

    /** RecyclerView displaying entrant rows. */
    private RecyclerView rvEntrants;

    /** Adapter used by the entrant list. */
    private EntrantAdapter entrantAdapter;

    /** Full list loaded from Firestore. */
    private final List<Entrant> entrantList = new ArrayList<>();

    /** Filtered list currently shown in the UI. */
    private final List<Entrant> filteredList = new ArrayList<>();

    /** Tab layout for status filtering. */
    private TabLayout tabLayout;

    /** Search field used for name/email filtering. */
    private EditText etSearch;

    /** Empty state layout shown when no results exist. */
    private LinearLayout layoutEmptyState;

    /** Placeholder export button. */
    private FloatingActionButton fabExport;

    /** Event id passed into this activity. */
    private String eventId;

    /** Organizer id passed into this activity. */
    private String organizerId;

    /** Current status filter. */
    private String currentFilter = "all";

    /** Firestore database instance. */
    private FirebaseFirestore db;

    /**
     * Initializes the activity, validates intent extras, sets up the toolbar,
     * and loads waitlist entrants.
     *
     * @param savedInstanceState previously saved state bundle, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_list);

        applyWindowInsets();

        eventId = getIntent().getStringExtra("EVENT_ID");
        organizerId = getIntent().getStringExtra("ORGANIZER_ID");

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing EVENT_ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (organizerId == null || organizerId.trim().isEmpty()) {
            Toast.makeText(this, "Missing ORGANIZER_ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        setupToolbar();
        initViews();
        setupRecyclerView();
        setupListeners();
        loadEntrants();
    }

    /**
     * Applies status bar insets to the toolbar so that the toolbar content
     * stays below the system status bar on edge-to-edge devices.
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

    /**
     * Returns the default toolbar height from the current theme.
     *
     * @return toolbar height in pixels
     */
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

    /**
     * Configures the toolbar and enables back navigation.
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Handles the toolbar up button.
     *
     * @return always returns {@code true}
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Binds layout views.
     */
    private void initViews() {
        rvEntrants = findViewById(R.id.rvEntrants);
        tabLayout = findViewById(R.id.tabLayout);
        etSearch = findViewById(R.id.etSearch);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        fabExport = findViewById(R.id.fabExport);
    }

    /**
     * Configures the RecyclerView and its adapter.
     */
    private void setupRecyclerView() {
        entrantAdapter = new EntrantAdapter(filteredList);
        rvEntrants.setLayoutManager(new LinearLayoutManager(this));
        rvEntrants.setAdapter(entrantAdapter);
    }

    /**
     * Registers listeners for tabs, search, and placeholder actions.
     */
    private void setupListeners() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "waiting";
                        break;
                    case 2:
                        currentFilter = "selected";
                        break;
                    case 3:
                        currentFilter = "enrolled";
                        break;
                    case 4:
                        currentFilter = "cancelled";
                        break;
                    default:
                        currentFilter = "all";
                        break;
                }
                applyFilterAndSearch();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        fabExport.setOnClickListener(v ->
                Toast.makeText(this, "Export feature coming soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnFilter).setOnClickListener(v ->
                Toast.makeText(this, "Filter options coming soon", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads waitlist entrants from Firestore.
     */
    private void loadEntrants() {
        db.collection("organizers")
                .document(organizerId)
                .collection("events")
                .document(eventId)
                .collection("waitlist")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    entrantList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        entrant.setId(doc.getId());
                        entrantList.add(entrant);
                    }

                    updateTabCounts();
                    applyFilterAndSearch();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load entrants", Toast.LENGTH_LONG).show());
    }

    /**
     * Applies both status filtering and text search to the entrant list.
     */
    private void applyFilterAndSearch() {
        String query = etSearch.getText() == null
                ? ""
                : etSearch.getText().toString().trim().toLowerCase();

        filteredList.clear();

        for (Entrant entrant : entrantList) {
            if (!matchesStatusFilter(entrant, currentFilter)) {
                continue;
            }

            String name = safe(entrant.getEntrantName()).toLowerCase();
            String email = safe(entrant.getEntrantEmail()).toLowerCase();

            if (query.isEmpty() || name.contains(query) || email.contains(query)) {
                filteredList.add(entrant);
            }
        }

        updateUI();
    }

    /**
     * Checks whether an entrant matches the current status filter.
     *
     * @param entrant entrant to inspect
     * @param filter current filter string
     * @return {@code true} if the entrant should be shown
     */
    private boolean matchesStatusFilter(Entrant entrant, String filter) {
        String status = entrant.getStatus();

        if ("all".equals(filter)) {
            return true;
        }

        return filter.equals(status);
    }

    /**
     * Updates visible UI state after filtering.
     */
    private void updateUI() {
        if (filteredList.isEmpty()) {
            rvEntrants.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvEntrants.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }

        entrantAdapter.notifyDataSetChanged();
    }

    /**
     * Recomputes tab titles with entrant counts for each status.
     */
    private void updateTabCounts() {
        int allCount = entrantList.size();
        int waitingCount = 0;
        int selectedCount = 0;
        int enrolledCount = 0;
        int cancelledCount = 0;

        for (Entrant entrant : entrantList) {
            String status = entrant.getStatus();

            if ("waiting".equals(status)) {
                waitingCount++;
            }
            if ("selected".equals(status)) {
                selectedCount++;
            }
            if ("enrolled".equals(status)) {
                enrolledCount++;
            }
            if ("cancelled".equals(status)) {
                cancelledCount++;
            }
        }

        setTabText(0, "All (" + allCount + ")");
        setTabText(1, "Waiting (" + waitingCount + ")");
        setTabText(2, "Selected (" + selectedCount + ")");
        setTabText(3, "Enrolled (" + enrolledCount + ")");
        setTabText(4, "Cancelled (" + cancelledCount + ")");
    }

    /**
     * Safely updates tab text if the tab exists.
     *
     * @param index tab index
     * @param text tab title text
     */
    private void setTabText(int index, String text) {
        TabLayout.Tab tab = tabLayout.getTabAt(index);
        if (tab != null) {
            tab.setText(text);
        }
    }

    /**
     * Converts a nullable string into a non-null value for filtering.
     *
     * @param value raw value
     * @return empty string when null, otherwise the original value
     */
    @NonNull
    private String safe(String value) {
        return value == null ? "" : value;
    }
}