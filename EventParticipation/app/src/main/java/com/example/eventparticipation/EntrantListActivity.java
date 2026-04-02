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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Organizer screen for viewing entrants who joined a specific event waitlist.
 *
 * Supports:
 * - tab filtering by organizer-facing status buckets
 * - search by entrant name/email
 * - export of final enrolled entrants to CSV
 */
public class EntrantListActivity extends AppCompatActivity {

    private RecyclerView rvEntrants;
    private EntrantAdapter entrantAdapter;

    private final List<Entrant> entrantList = new ArrayList<>();
    private final List<Entrant> displayedList = new ArrayList<>();

    private TabLayout tabLayout;
    private EditText etSearch;
    private LinearLayout layoutEmptyState;
    private FloatingActionButton fabExport;

    private String eventId;
    private String organizerId;
    private String currentFilter = "all";

    private FirebaseFirestore db;

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
        rvEntrants = findViewById(R.id.rvEntrants);
        tabLayout = findViewById(R.id.tabLayout);
        etSearch = findViewById(R.id.etSearch);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        fabExport = findViewById(R.id.fabExport);
    }

    private void setupRecyclerView() {
        entrantAdapter = new EntrantAdapter(displayedList);
        rvEntrants.setLayoutManager(new LinearLayoutManager(this));
        rvEntrants.setAdapter(entrantAdapter);
    }

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

        fabExport.setOnClickListener(v -> exportEnrolledEntrantsToCsv());
    }

    private void loadEntrants() {
        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .orderBy("joinedAt", Query.Direction.ASCENDING)
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

    private void applyFilterAndSearch() {
        String query = etSearch.getText() == null
                ? ""
                : etSearch.getText().toString().trim().toLowerCase();

        displayedList.clear();

        for (Entrant entrant : entrantList) {
            if (!matchesStatusFilter(entrant, currentFilter)) {
                continue;
            }

            String name = safe(entrant.getEntrantName()).toLowerCase();
            String email = safe(entrant.getEntrantEmail()).toLowerCase();

            if (query.isEmpty() || name.contains(query) || email.contains(query)) {
                displayedList.add(entrant);
            }
        }

        updateUI();
    }

    private boolean matchesStatusFilter(Entrant entrant, String filter) {
        if ("all".equals(filter)) {
            return true;
        }

        String selectionStatus = safe(entrant.getSelectionStatus()).toLowerCase();
        String finalStatus = safe(entrant.getFinalStatus()).toLowerCase();

        switch (filter) {
            case "waiting":
                return "waiting".equals(selectionStatus);

            case "selected":
                return "selected".equals(selectionStatus)
                        && !"enrolled".equals(finalStatus);

            case "enrolled":
                return "enrolled".equals(finalStatus);

            case "cancelled":
                return "cancelled".equals(selectionStatus);

            default:
                return true;
        }
    }

    private void updateUI() {
        if (displayedList.isEmpty()) {
            rvEntrants.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvEntrants.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }

        entrantAdapter.notifyDataSetChanged();
    }

    private void updateTabCounts() {
        int allCount = entrantList.size();
        int waitingCount = 0;
        int selectedCount = 0;
        int enrolledCount = 0;
        int cancelledCount = 0;

        for (Entrant entrant : entrantList) {
            String selectionStatus = safe(entrant.getSelectionStatus()).toLowerCase();
            String finalStatus = safe(entrant.getFinalStatus()).toLowerCase();

            if ("waiting".equals(selectionStatus)) {
                waitingCount++;
            } else if ("selected".equals(selectionStatus) && !"enrolled".equals(finalStatus)) {
                selectedCount++;
            } else if ("enrolled".equals(finalStatus)) {
                enrolledCount++;
            } else if ("cancelled".equals(selectionStatus)) {
                cancelledCount++;
            }
        }

        setTabText(0, "All (" + allCount + ")");
        setTabText(1, "Waiting (" + waitingCount + ")");
        setTabText(2, "Selected (" + selectedCount + ")");
        setTabText(3, "Enrolled (" + enrolledCount + ")");
        setTabText(4, "Cancelled (" + cancelledCount + ")");
    }

    private void setTabText(int index, String text) {
        TabLayout.Tab tab = tabLayout.getTabAt(index);
        if (tab != null) {
            tab.setText(text);
        }
    }

    /**
     * Export only final enrolled entrants for this event to CSV.
     */
    private void exportEnrolledEntrantsToCsv() {
        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Entrant> enrolledEntrants = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        entrant.setId(doc.getId());

                        if ("enrolled".equalsIgnoreCase(safe(entrant.getFinalStatus()))) {
                            enrolledEntrants.add(entrant);
                        }
                    }

                    if (enrolledEntrants.isEmpty()) {
                        Toast.makeText(this, "No enrolled entrants to export", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    writeCsvFile(enrolledEntrants);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to export CSV", Toast.LENGTH_LONG).show());
    }

    private void writeCsvFile(List<Entrant> enrolledEntrants) {
        OutputStreamWriter writer = null;
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String fileName = "enrolled_entrants_" + eventId + "_" + timestamp + ".csv";

            File directory = getExternalFilesDir(null);
            if (directory == null) {
                Toast.makeText(this, "Storage unavailable", Toast.LENGTH_LONG).show();
                return;
            }

            File file = new File(directory, fileName);
            writer = new OutputStreamWriter(new FileOutputStream(file));

            writer.append("Entrant ID,Name,Email,Joined At,Selection Status,Response Status,Final Status\n");

            for (Entrant entrant : enrolledEntrants) {
                writer.append(csv(safe(entrant.getEntrantId()))).append(",");
                writer.append(csv(safe(entrant.getEntrantName()))).append(",");
                writer.append(csv(safe(entrant.getEntrantEmail()))).append(",");
                writer.append(csv(formatDate(entrant.getJoinedAt()))).append(",");
                writer.append(csv(safe(entrant.getSelectionStatus()))).append(",");
                writer.append(csv(safe(entrant.getResponseStatus()))).append(",");
                writer.append(csv(safe(entrant.getFinalStatus()))).append("\n");
            }

            writer.flush();

            Toast.makeText(
                    this,
                    "CSV exported to: " + file.getAbsolutePath(),
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to write CSV", Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date);
    }

    private String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    @NonNull
    private String safe(String value) {
        return value == null ? "" : value;
    }
}