package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 *     <li>US 01.01.04 As an entrant, I want to filter events based on my availability and event capacity.</li>
 *     <li>US 01.01.05 As an entrant, I want to search for events by keyword to find events based on my interests.</li>
 *     <li>US 01.01.06 As an entrant, I want to use keyword search with filtering to narrow my event search.</li>
 *     <li>US 01.05.04 - Waiting list count shown on each card</li>
 * </ul>
 */
public class EntrantDashboardActivity extends BaseEntrantActivity {

    private RecyclerView rvEntrantEvents;
    private EntrantEventAdapter eventAdapter;
    private List<Event> allEvents;
    private List<Event> filteredEvents;
    private final Map<String, String> participationStatusByEventId = new HashMap<>();

    private EditText etSearch;
    private CardView btnFilter;

    private ImageButton infoBtn;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String entrantId;
    private String registrationFilter = EntrantDashboardFilterLogic.REGISTRATION_FILTER_ALL;
    private String participationFilter = EntrantDashboardFilterLogic.PARTICIPATION_FILTER_ALL;
    private boolean onlyAvailableSpots = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_dashboard);

        db = FirebaseFirestore.getInstance();
        entrantId = DeviceIdProvider.getId(this);

        if (!DeviceIdProvider.isValidId(entrantId)) {
            Toast.makeText(this, "Failed to get device ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupSearch();
        setupBottomNav(R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    /**
     * Binds layout views.
     */
    private void initViews() {
        rvEntrantEvents  = findViewById(R.id.rvEntrantEvents);
        etSearch         = findViewById(R.id.etSearch);
        btnFilter        = findViewById(R.id.btnFilter);
        infoBtn          = findViewById(R.id.infoBtn);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        progressBar      = findViewById(R.id.progressBar);

        btnFilter.setOnClickListener(v -> showFilterDialog());

        infoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), InfoPopup.class);
                startActivity(intent);
            }
        });
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
     * Loads all events from the top-level Firestore "events" collection.
     * Also loads this entrant's waitlist status for each event so participation
     * filters can be applied locally.
     */
    private void loadEvents() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        rvEntrantEvents.setVisibility(View.GONE);

        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allEvents.clear();
                    filteredEvents.clear();
                    participationStatusByEventId.clear();

                    if (querySnapshot.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        eventAdapter.notifyDataSetChanged();
                        layoutEmptyState.setVisibility(View.VISIBLE);
                        rvEntrantEvents.setVisibility(View.GONE);
                        return;
                    }

                    final int[] remaining = {querySnapshot.size()};

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        final String eventId = doc.getId();
                        Event event = doc.toObject(Event.class);
                        if (event == null) {
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                refreshVisibleEvents();
                            }
                            continue;
                        }

                        event.setId(eventId);
                        allEvents.add(event);

                        db.collection("events")
                                .document(eventId)
                                .collection("waitlist")
                                .document(entrantId)
                                .get()
                                .addOnSuccessListener(waitDoc -> {
                                    participationStatusByEventId.put(
                                            eventId,
                                            resolveParticipationStatus(waitDoc)
                                    );

                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        refreshVisibleEvents();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        refreshVisibleEvents();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Filters the displayed events by name or venue address matching the search query.
     *
     * @param query search text
     */
    private void filterEvents(String query) {
        filteredEvents.clear();

        String lower = query == null ? "" : query.trim().toLowerCase();

        for (Event event : allEvents) {
            if (!matchesActiveFilters(event)) {
                continue;
            }

            if (EntrantDashboardFilterLogic.matchesKeyword(event, lower)) {
                filteredEvents.add(event);
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

    private void refreshVisibleEvents() {
        progressBar.setVisibility(View.GONE);
        String query = etSearch.getText() == null ? "" : etSearch.getText().toString();
        filterEvents(query);
    }

    /**
     * Applies the active registration, availability, and participation filters.
     *
     * @param event event to evaluate
     * @return true if the event passes all active filters
     */
    private boolean matchesActiveFilters(Event event) {
        return EntrantDashboardFilterLogic.matchesRegistrationFilter(
                event,
                registrationFilter,
                new Date()
        ) && EntrantDashboardFilterLogic.matchesAvailabilityFilter(
                event,
                registrationFilter,
                onlyAvailableSpots
        ) && EntrantDashboardFilterLogic.matchesParticipationFilter(
                participationStatusByEventId.get(event.getId()),
                participationFilter
        );
    }

    /**
     * Resolves this entrant's dashboard status for one waitlist document.
     *
     * @param waitDoc waitlist document for the current entrant
     * @return normalized dashboard participation status
     */
    private String resolveParticipationStatus(DocumentSnapshot waitDoc) {
        if (!waitDoc.exists()) {
            return EntrantDashboardFilterLogic.PARTICIPATION_FILTER_NOT_JOINED;
        }

        return EntrantDashboardFilterLogic.resolveParticipationStatus(
                waitDoc.getString("selectionStatus"),
                waitDoc.getString("responseStatus"),
                waitDoc.getString("finalStatus")
        );
    }

    /**
     * Shows the filter dialog for discover events.
     */
    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_entrant_event_filters, null, false);

        ChipGroup chipGroupRegistration = dialogView.findViewById(R.id.chipGroupRegistration);
        ChipGroup chipGroupParticipation = dialogView.findViewById(R.id.chipGroupParticipation);
        MaterialCheckBox cbOnlyAvailableSpots = dialogView.findViewById(R.id.cbOnlyAvailableSpots);
        MaterialButton btnResetFilters = dialogView.findViewById(R.id.btnResetFilters);
        MaterialButton btnApplyFilters = dialogView.findViewById(R.id.btnApplyFilters);

        syncDialogState(chipGroupRegistration, chipGroupParticipation, cbOnlyAvailableSpots);

        chipGroupRegistration.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isOpenSelected = checkedId == R.id.chipRegistrationOpen;
            cbOnlyAvailableSpots.setEnabled(isOpenSelected);
            if (!isOpenSelected) {
                cbOnlyAvailableSpots.setChecked(false);
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        btnResetFilters.setOnClickListener(v -> {
            chipGroupRegistration.check(R.id.chipRegistrationAll);
            chipGroupParticipation.check(R.id.chipParticipationAll);
            cbOnlyAvailableSpots.setChecked(false);
            cbOnlyAvailableSpots.setEnabled(false);
        });

        btnApplyFilters.setOnClickListener(v -> {
            registrationFilter = resolveRegistrationFilter(chipGroupRegistration.getCheckedChipId());
            participationFilter = resolveParticipationFilter(chipGroupParticipation.getCheckedChipId());
            onlyAvailableSpots = cbOnlyAvailableSpots.isChecked();
            refreshVisibleEvents();
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Binds the current filter state into the dialog controls.
     *
     * @param chipGroupRegistration registration filter chips
     * @param chipGroupParticipation participation filter chips
     * @param cbOnlyAvailableSpots availability checkbox
     */
    private void syncDialogState(ChipGroup chipGroupRegistration,
                                 ChipGroup chipGroupParticipation,
                                 MaterialCheckBox cbOnlyAvailableSpots) {
        chipGroupRegistration.check(getRegistrationChipId(registrationFilter));
        chipGroupParticipation.check(getParticipationChipId(participationFilter));
        cbOnlyAvailableSpots.setChecked(
                onlyAvailableSpots
                        && EntrantDashboardFilterLogic.REGISTRATION_FILTER_OPEN.equals(registrationFilter)
        );
        cbOnlyAvailableSpots.setEnabled(
                EntrantDashboardFilterLogic.REGISTRATION_FILTER_OPEN.equals(registrationFilter)
        );
    }

    /**
     * Maps the selected registration chip to a filter key.
     *
     * @param checkedChipId selected registration chip id
     * @return registration filter key
     */
    private String resolveRegistrationFilter(int checkedChipId) {
        if (checkedChipId == R.id.chipRegistrationUpcoming) {
            return EntrantDashboardFilterLogic.REGISTRATION_FILTER_UPCOMING;
        } else if (checkedChipId == R.id.chipRegistrationOpen) {
            return EntrantDashboardFilterLogic.REGISTRATION_FILTER_OPEN;
        } else if (checkedChipId == R.id.chipRegistrationClosed) {
            return EntrantDashboardFilterLogic.REGISTRATION_FILTER_CLOSED;
        }
        return EntrantDashboardFilterLogic.REGISTRATION_FILTER_ALL;
    }

    /**
     * Maps the selected participation chip to a filter key.
     *
     * @param checkedChipId selected participation chip id
     * @return participation filter key
     */
    private String resolveParticipationFilter(int checkedChipId) {
        if (checkedChipId == R.id.chipParticipationNotJoined) {
            return EntrantDashboardFilterLogic.PARTICIPATION_FILTER_NOT_JOINED;
        } else if (checkedChipId == R.id.chipParticipationWaiting) {
            return EntrantDashboardFilterLogic.PARTICIPATION_FILTER_WAITING;
        } else if (checkedChipId == R.id.chipParticipationSelected) {
            return EntrantDashboardFilterLogic.PARTICIPATION_FILTER_SELECTED;
        } else if (checkedChipId == R.id.chipParticipationEnrolled) {
            return EntrantDashboardFilterLogic.PARTICIPATION_FILTER_ENROLLED;
        }
        return EntrantDashboardFilterLogic.PARTICIPATION_FILTER_ALL;
    }

    /**
     * Returns the chip id for the current registration filter.
     *
     * @param filter current registration filter key
     * @return chip id matching the filter
     */
    private int getRegistrationChipId(String filter) {
        switch (filter) {
            case EntrantDashboardFilterLogic.REGISTRATION_FILTER_UPCOMING:
                return R.id.chipRegistrationUpcoming;
            case EntrantDashboardFilterLogic.REGISTRATION_FILTER_OPEN:
                return R.id.chipRegistrationOpen;
            case EntrantDashboardFilterLogic.REGISTRATION_FILTER_CLOSED:
                return R.id.chipRegistrationClosed;
            case EntrantDashboardFilterLogic.REGISTRATION_FILTER_ALL:
            default:
                return R.id.chipRegistrationAll;
        }
    }

    /**
     * Returns the chip id for the current participation filter.
     *
     * @param filter current participation filter key
     * @return chip id matching the filter
     */
    private int getParticipationChipId(String filter) {
        switch (filter) {
            case EntrantDashboardFilterLogic.PARTICIPATION_FILTER_NOT_JOINED:
                return R.id.chipParticipationNotJoined;
            case EntrantDashboardFilterLogic.PARTICIPATION_FILTER_WAITING:
                return R.id.chipParticipationWaiting;
            case EntrantDashboardFilterLogic.PARTICIPATION_FILTER_SELECTED:
                return R.id.chipParticipationSelected;
            case EntrantDashboardFilterLogic.PARTICIPATION_FILTER_ENROLLED:
                return R.id.chipParticipationEnrolled;
            case EntrantDashboardFilterLogic.PARTICIPATION_FILTER_ALL:
            default:
                return R.id.chipParticipationAll;
        }
    }

}
