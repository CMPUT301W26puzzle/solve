package com.example.eventparticipation.user;

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

import com.example.eventparticipation.universal.Event;
import com.example.eventparticipation.universal.InfoPopup;
import com.example.eventparticipation.R;
import com.example.eventparticipation.universal.SelectRoleActivity;
import com.example.eventparticipation.universal.SessionManager;
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
 * <li>US 01.01.01 - Join waiting list (navigates to detail)</li>
 * <li>US 01.01.02 - Leave waiting list (navigates to detail)</li>
 * <li>US 01.01.04 As an entrant, I want to filter events based on my availability and event capacity.</li>
 * <li>US 01.01.05 As an entrant, I want to search for events by keyword to find events based on my interests.</li>
 * <li>US 01.01.06 As an entrant, I want to use keyword search with filtering to narrow my event search.</li>
 * <li>US 01.05.04 - Waiting list count shown on each card</li>
 * </ul>
 */
public class EntrantDashboardActivity extends BaseEntrantActivity {

    private static final String REGISTRATION_FILTER_ALL = "all";
    private static final String REGISTRATION_FILTER_UPCOMING = "upcoming";
    private static final String REGISTRATION_FILTER_OPEN = "open";
    private static final String REGISTRATION_FILTER_CLOSED = "closed";

    private static final String PARTICIPATION_FILTER_ALL = "all";
    private static final String PARTICIPATION_FILTER_NOT_JOINED = "not_joined";
    private static final String PARTICIPATION_FILTER_WAITING = "waiting";
    private static final String PARTICIPATION_FILTER_SELECTED = "selected";
    private static final String PARTICIPATION_FILTER_ENROLLED = "enrolled";

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
    private String registrationFilter = REGISTRATION_FILTER_ALL;
    private String participationFilter = PARTICIPATION_FILTER_ALL;
    private boolean onlyAvailableSpots = false;

    /**
     * Initializes activity variables, performs a session login check, and triggers
     * view bindings, RecyclerView configuration, and event loading pipelines.
     *
     * @param savedInstanceState Persisted instance data for recreation.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_dashboard);

        db = FirebaseFirestore.getInstance();
        SessionManager session = SessionManager.getInstance(this);
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SelectRoleActivity.class));
            finish();
            return;
        }

        entrantId = session.getUserId();

        initViews();
        setupRecyclerView();
        setupSearch();
        setupBottomNav(R.id.nav_home);
    }

    /**
     * Refreshes the events loaded from Firestore anytime the activity regains focus
     * to keep waitlist metrics and event availability completely up to date.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    /**
     * Captures and binds structural view references matching the layout XML document.
     * Sets base interaction behavior for top-level interactive icons.
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
     * Prepares and attaches the event Adapter and LayoutManager to the RecyclerView,
     * and maps user click-through intent behavior (pushing data to event detail views).
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
     * Subscribes a TextWatcher listener to the search input allowing live
     * text-matching adjustments to the event list whenever the search input changes.
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
     * Loads all public events from the top-level Firestore "events" collection.
     * <p>This method iterates through all global events and explicitly skips any events
     * marked as private (where {@code isPrivate == true}) to ensure they do not leak onto
     * the public event listing dashboard. It also concurrently loads this entrant's waitlist
     * status for each displayed event so participation filters can be applied locally.</p>
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

                        // filter out private events
                        Boolean isPrivate = doc.getBoolean("isPrivate");
                        if (isPrivate != null && isPrivate) {
                            remaining[0]--;
                            if (remaining[0] == 0) refreshVisibleEvents();
                            continue;
                        }

                        Event event = doc.toObject(Event.class);
                        if (event == null) {
                            remaining[0]--;
                            if (remaining[0] == 0) refreshVisibleEvents();
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
     * Core filtration logic executed against the user's specific typed query.
     * Evaluates active menu constraints alongside the text search.
     *
     * @param query The specific search terminology typed by the user.
     */
    private void filterEvents(String query) {
        filteredEvents.clear();

        String lower = query == null ? "" : query.trim().toLowerCase();

        for (Event event : allEvents) {
            if (!matchesActiveFilters(event)) {
                continue;
            }

            if (lower.isEmpty()) {
                filteredEvents.add(event);
                continue;
            }

            String name = event.getName() != null ? event.getName().toLowerCase() : "";
            String venueAddress = event.getVenueAddress() != null
                    ? event.getVenueAddress().toLowerCase()
                    : "";

            if (name.contains(lower) || venueAddress.contains(lower)) {
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

    /**
     * Immediately applies and renders any adjustments forced onto the event array.
     */
    private void refreshVisibleEvents() {
        progressBar.setVisibility(View.GONE);
        String query = etSearch.getText() == null ? "" : etSearch.getText().toString();
        filterEvents(query);
    }

    /**
     * Determines if a single event correctly aligns with every currently active user constraint.
     *
     * @param event The Event block undergoing verification.
     * @return boolean True if the event matches all three distinct filter systems.
     */
    private boolean matchesActiveFilters(Event event) {
        return matchesRegistrationFilter(event)
                && matchesAvailabilityFilter(event)
                && matchesParticipationFilter(event);
    }

    /**
     * Deciphers if an Event passes time-based constraint checks based on its opening/closing boundaries.
     *
     * @param event The target Event checking for date validity.
     * @return boolean Validation metric matching registration parameters.
     */
    private boolean matchesRegistrationFilter(Event event) {
        Date now = new Date();
        Date registrationStart = event.getRegistrationStart();
        Date registrationEnd = event.getRegistrationEnd();

        switch (registrationFilter) {
            case REGISTRATION_FILTER_UPCOMING:
                return registrationStart != null && now.before(registrationStart);
            case REGISTRATION_FILTER_OPEN:
                return (registrationStart == null || !now.before(registrationStart))
                        && (registrationEnd == null || !now.after(registrationEnd));
            case REGISTRATION_FILTER_CLOSED:
                return registrationEnd != null && now.after(registrationEnd);
            case REGISTRATION_FILTER_ALL:
            default:
                return true;
        }
    }

    /**
     * Checks if the event conforms to user-specified waitlist capacity limitations.
     *
     * @param event The Event entity being evaluated.
     * @return boolean Result validating that spaces remain (if forced open filter is active).
     */
    private boolean matchesAvailabilityFilter(Event event) {
        if (!onlyAvailableSpots || !REGISTRATION_FILTER_OPEN.equals(registrationFilter)) {
            return true;
        }

        Integer waitlistLimit = event.getWaitlistLimit();
        return waitlistLimit == null || event.getWaitingCount() < waitlistLimit;
    }

    /**
     * Verifies that the event matches the expected user participation/waitlist criteria.
     *
     * @param event Evaluating logic specific to this event.
     * @return boolean Validation state for participation limits.
     */
    private boolean matchesParticipationFilter(Event event) {
        String status = participationStatusByEventId.get(event.getId());

        switch (participationFilter) {
            case PARTICIPATION_FILTER_NOT_JOINED:
                return PARTICIPATION_FILTER_NOT_JOINED.equals(status);
            case PARTICIPATION_FILTER_WAITING:
                return PARTICIPATION_FILTER_WAITING.equals(status);
            case PARTICIPATION_FILTER_SELECTED:
                return PARTICIPATION_FILTER_SELECTED.equals(status);
            case PARTICIPATION_FILTER_ENROLLED:
                return PARTICIPATION_FILTER_ENROLLED.equals(status);
            case PARTICIPATION_FILTER_ALL:
            default:
                return true;
        }
    }

    /**
     * Interprets and converts complex three-tiered Firestore state mappings
     * into a simplified single-layer demographic filter representation.
     *
     * @param waitDoc The specific snapshot data mapping to this Entrant and Event.
     * @return String Translated constant resolving user position.
     */
    private String resolveParticipationStatus(DocumentSnapshot waitDoc) {
        if (!waitDoc.exists()) {
            return PARTICIPATION_FILTER_NOT_JOINED;
        }

        String selectionStatus = waitDoc.getString("selectionStatus");
        String responseStatus = waitDoc.getString("responseStatus");
        String finalStatus = waitDoc.getString("finalStatus");

        if ("enrolled".equals(finalStatus)) {
            return PARTICIPATION_FILTER_ENROLLED;
        }

        if ("waiting".equals(selectionStatus)) {
            return PARTICIPATION_FILTER_WAITING;
        }

        if ("selected".equals(selectionStatus) && "pending".equals(responseStatus)) {
            return PARTICIPATION_FILTER_SELECTED;
        }

        if ("cancelled".equals(selectionStatus) || "declined".equals(responseStatus)) {
            return PARTICIPATION_FILTER_NOT_JOINED;
        }

        return PARTICIPATION_FILTER_NOT_JOINED;
    }

    /**
     * Presents an expansive Material UI dialog panel enabling deep refinement of the Event list.
     * Employs synchronous interaction rules to avoid logically incompatible filter combinations.
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
     * Mounts pre-existing active logic arrays into the visual constraints of the filter dialog.
     *
     * @param chipGroupRegistration  The layout mapping representing Date constraints.
     * @param chipGroupParticipation The layout grouping addressing Entrant history constraints.
     * @param cbOnlyAvailableSpots   The checkbox strictly tracking event waitlist limit rules.
     */
    private void syncDialogState(ChipGroup chipGroupRegistration,
                                 ChipGroup chipGroupParticipation,
                                 MaterialCheckBox cbOnlyAvailableSpots) {
        chipGroupRegistration.check(getRegistrationChipId(registrationFilter));
        chipGroupParticipation.check(getParticipationChipId(participationFilter));
        cbOnlyAvailableSpots.setChecked(
                onlyAvailableSpots && REGISTRATION_FILTER_OPEN.equals(registrationFilter)
        );
        cbOnlyAvailableSpots.setEnabled(REGISTRATION_FILTER_OPEN.equals(registrationFilter));
    }

    /**
     * Reads numerical View ID integers generated by the filter dialogue and transforms them
     * into readable system constants addressing Registration criteria.
     *
     * @param checkedChipId Int ID retrieved natively from the View block.
     * @return String Constant equivalent mapped globally for parsing limits.
     */
    private String resolveRegistrationFilter(int checkedChipId) {
        if (checkedChipId == R.id.chipRegistrationUpcoming) {
            return REGISTRATION_FILTER_UPCOMING;
        } else if (checkedChipId == R.id.chipRegistrationOpen) {
            return REGISTRATION_FILTER_OPEN;
        } else if (checkedChipId == R.id.chipRegistrationClosed) {
            return REGISTRATION_FILTER_CLOSED;
        }
        return REGISTRATION_FILTER_ALL;
    }

    /**
     * Translates interface View IDs into core Participant constraint mapping commands.
     *
     * @param checkedChipId Formative int assigned by the system build index.
     * @return String Identifiable instruction limit for the search block.
     */
    private String resolveParticipationFilter(int checkedChipId) {
        if (checkedChipId == R.id.chipParticipationNotJoined) {
            return PARTICIPATION_FILTER_NOT_JOINED;
        } else if (checkedChipId == R.id.chipParticipationWaiting) {
            return PARTICIPATION_FILTER_WAITING;
        } else if (checkedChipId == R.id.chipParticipationSelected) {
            return PARTICIPATION_FILTER_SELECTED;
        } else if (checkedChipId == R.id.chipParticipationEnrolled) {
            return PARTICIPATION_FILTER_ENROLLED;
        }
        return PARTICIPATION_FILTER_ALL;
    }

    /**
     * Locates the precise UI layer ID mapped to the currently defined registration constraint string.
     *
     * @param filter Internal system string flag declaring boundaries.
     * @return int Mapped resource ID targeting the required view block.
     */
    private int getRegistrationChipId(String filter) {
        switch (filter) {
            case REGISTRATION_FILTER_UPCOMING:
                return R.id.chipRegistrationUpcoming;
            case REGISTRATION_FILTER_OPEN:
                return R.id.chipRegistrationOpen;
            case REGISTRATION_FILTER_CLOSED:
                return R.id.chipRegistrationClosed;
            case REGISTRATION_FILTER_ALL:
            default:
                return R.id.chipRegistrationAll;
        }
    }

    /**
     * Parses the current backend Participation constraint string and outputs the linked visual interface ID.
     *
     * @param filter Target behavior filter requirement string.
     * @return int Equivalent resource ID associated with the front-end layout group.
     */
    private int getParticipationChipId(String filter) {
        switch (filter) {
            case PARTICIPATION_FILTER_NOT_JOINED:
                return R.id.chipParticipationNotJoined;
            case PARTICIPATION_FILTER_WAITING:
                return R.id.chipParticipationWaiting;
            case PARTICIPATION_FILTER_SELECTED:
                return R.id.chipParticipationSelected;
            case PARTICIPATION_FILTER_ENROLLED:
                return R.id.chipParticipationEnrolled;
            case PARTICIPATION_FILTER_ALL:
            default:
                return R.id.chipParticipationAll;
        }
    }

}