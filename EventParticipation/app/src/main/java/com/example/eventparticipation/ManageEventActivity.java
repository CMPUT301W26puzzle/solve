package com.example.eventparticipation;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Organizer / co-organizer screen for managing a specific event.
 *
 * <p>Access rules:
 * <ul>
 *     <li>Organizer can access the event.</li>
 *     <li>Co-organizer can access the event.</li>
 *     <li>Co-organizer cannot assign another co-organizer.</li>
 * </ul>
 *
 * <p>This screen also displays derived waitlist counts and keeps the top-level
 * event document counts synchronized with the authoritative waitlist subcollection.</p>
 */
public class ManageEventActivity extends AppCompatActivity {

    private TextView tvEventName;
    private TextView tvEventDate;
    private TextView tvEventCapacity;

    private TextView tvWaitingCount;
    private TextView tvSelectedCount;
    private TextView tvEnrolledCount;

    private ImageView imgEventPoster;
    private LinearLayout layoutPosterPlaceholder;
    private FloatingActionButton fabRemovePoster;

    private MaterialButton btnUploadPoster;
    private MaterialButton btnUpdatePoster;

    private MaterialButton btnViewEntrants;
    private MaterialButton btnViewMap;

    private MaterialButton btnRunLottery;
    private MaterialButton btnDrawReplacement;
    private MaterialButton btnShowQRCode;
    private MaterialButton btnEditEvent;
    private MaterialButton btnExportCsv;
    private MaterialButton btnAssignCoOrganizer;

    private String eventId;
    private String organizerId;
    private String currentUserId;

    private boolean hasPoster = false;
    private String currentPosterUrl = "";

    private boolean isOwner = false;
    private boolean isCoOrganizer = false;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String accessMode;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadPosterToFirebase(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_event);

        applyWindowInsets();

        eventId = getIntent().getStringExtra("EVENT_ID");
        organizerId = getIntent().getStringExtra("ORGANIZER_ID");
        currentUserId = DeviceIdProvider.getId(this);
        accessMode = getIntent().getStringExtra("ACCESS_MODE");
        if (accessMode == null || accessMode.trim().isEmpty()) {
            accessMode = "organizer";
        }

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
        storage = FirebaseStorage.getInstance();

        setupToolbar();
        initViews();
        setInitialPlaceholderValues();
        setupClickListeners();
        updatePosterUI();
        checkManageAccessAndLoad();
    }

    /**
     * Applies system bar insets to the toolbar so that content stays below the status bar.
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
     * Returns the toolbar height defined by the current theme.
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
     * Configures the toolbar and up navigation behavior.
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Binds all views from the layout.
     */
    private void initViews() {
        tvEventName = findViewById(R.id.tvEventName);
        tvEventDate = findViewById(R.id.tvEventDate);
        tvEventCapacity = findViewById(R.id.tvEventCapacity);

        tvWaitingCount = findViewById(R.id.tvWaitingCount);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        tvEnrolledCount = findViewById(R.id.tvEnrolledCount);

        imgEventPoster = findViewById(R.id.imgEventPoster);
        layoutPosterPlaceholder = findViewById(R.id.layoutPosterPlaceholder);
        fabRemovePoster = findViewById(R.id.fabRemovePoster);

        btnUploadPoster = findViewById(R.id.btnUploadPoster);
        btnUpdatePoster = findViewById(R.id.btnUpdatePoster);

        btnViewEntrants = findViewById(R.id.btnViewEntrants);
        btnViewMap = findViewById(R.id.btnViewMap);

        btnRunLottery = findViewById(R.id.btnRunLottery);
        btnDrawReplacement = findViewById(R.id.btnDrawReplacement);
        btnShowQRCode = findViewById(R.id.btnShowQRCode);
        btnEditEvent = findViewById(R.id.btnEditEvent);
        btnExportCsv = findViewById(R.id.btnExportCsv);
        btnAssignCoOrganizer = findViewById(R.id.btnAssignCoOrganizer);
    }

    /**
     * Fills the screen with placeholder values before Firestore data is loaded.
     */
    private void setInitialPlaceholderValues() {
        tvEventName.setText("Event Name");
        tvEventDate.setText("Date not available");
        tvEventCapacity.setText("Waitlist Limit: Unlimited");

        tvWaitingCount.setText("0");
        tvSelectedCount.setText("0");
        tvEnrolledCount.setText("0");

        currentPosterUrl = "";
        hasPoster = false;
        imgEventPoster.setImageDrawable(null);
    }

    /**
     * Wires up all click listeners used on the screen.
     */
    private void setupClickListeners() {
        btnUploadPoster.setOnClickListener(v -> openImagePicker());
        btnUpdatePoster.setOnClickListener(v -> openImagePicker());
        fabRemovePoster.setOnClickListener(v -> removePoster());

        btnViewEntrants.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantListActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            intent.putExtra("ORGANIZER_ID", organizerId);
            startActivity(intent);
        });

        btnViewMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, WaitlistMapActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            intent.putExtra("ORGANIZER_ID", organizerId);
            startActivity(intent);
        });

        btnRunLottery.setOnClickListener(v -> showRunLotteryDialog());
        btnDrawReplacement.setOnClickListener(v -> drawReplacementApplicant());
        btnAssignCoOrganizer.setOnClickListener(v -> showAssignCoOrganizerDialog());

        btnShowQRCode.setOnClickListener(v ->
                Toast.makeText(this, "QR code feature coming soon", Toast.LENGTH_SHORT).show());

        btnEditEvent.setOnClickListener(v ->
                Toast.makeText(this, "Edit event feature coming soon", Toast.LENGTH_SHORT).show());

        btnExportCsv.setOnClickListener(v -> exportEnrolledEntrantsToCsv());
    }

    /**
     * Verifies whether the current user has permission to manage the event and, if so,
     * loads all event details and current counts.
     */
    private void checkManageAccessAndLoad() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String ownerId = safe(doc.getString("organizerId"));
                    List<String> coOrganizerIds = (List<String>) doc.get("coOrganizerIds");

                    if ("organizer".equals(accessMode)) {
                        isOwner = organizerId.equals(ownerId);
                        isCoOrganizer = false;
                    } else if ("coorganizer".equals(accessMode)) {
                        isOwner = false;
                        isCoOrganizer = coOrganizerIds != null && coOrganizerIds.contains(currentUserId);
                    } else {
                        isOwner = false;
                        isCoOrganizer = false;
                    }

                    if (!isOwner && !isCoOrganizer) {
                        Toast.makeText(this, "You do not have access to manage this event", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    applyRoleRestrictions();
                    loadEventData();
                    loadWaitlistCounts();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to verify access", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    /**
     * Applies UI restrictions based on the current role.
     */
    private void applyRoleRestrictions() {
        if (isCoOrganizer && !isOwner) {
            btnAssignCoOrganizer.setVisibility(View.GONE);
        } else {
            btnAssignCoOrganizer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows the dialog used to run a lottery and select a number of entrants.
     */
    private void showRunLotteryDialog() {
        EditText input = new EditText(this);
        input.setHint("Number of entrants to select");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Run Lottery")
                .setMessage("Select how many entrants should receive invitations.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Run", (dialog, which) -> {
                    String value = input.getText() == null ? "" : input.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(this, "Enter a lottery size", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int sampleSize;
                    try {
                        sampleSize = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new WaitlistController().runLottery(eventId, sampleSize)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Lottery complete. Notifications sent.", Toast.LENGTH_SHORT).show();
                                loadWaitlistCounts();
                            })
                            .addOnFailureListener(e -> Toast.makeText(
                                    this,
                                    e.getMessage() != null ? e.getMessage() : "Failed to run lottery",
                                    Toast.LENGTH_LONG
                            ).show());
                })
                .show();
    }

    /**
     * Draws a single replacement applicant and refreshes counts afterwards.
     */
    private void drawReplacementApplicant() {
        new WaitlistController().drawReplacement(eventId)
                .addOnSuccessListener(entrantId -> {
                    if (entrantId != null) {
                        Toast.makeText(this, "Replacement drawn successfully!", Toast.LENGTH_SHORT).show();
                        loadWaitlistCounts();
                    } else {
                        Toast.makeText(this, "Waitlist is empty. No replacement drawn.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to draw replacement.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads all eligible entrants and allows the organizer to send one a co-organizer invitation.
     *
     * <p>Entrants who already have a pending co-organizer invitation for this event
     * are filtered out and not shown in the selection dialog.</p>
     */
    private void showAssignCoOrganizerDialog() {
        if (!isOwner) {
            Toast.makeText(this, "Only the organizer can assign co-organizers", Toast.LENGTH_SHORT).show();
            return;
        }

        NotificationRepository repository = new NotificationRepository(db);

        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<com.google.android.gms.tasks.Task<EntrantCandidate>> candidateTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        entrant.setId(doc.getId());

                        String selectionStatus = safe(entrant.getSelectionStatus()).toLowerCase();
                        String responseStatus = safe(entrant.getResponseStatus()).toLowerCase();
                        String finalStatus = safe(entrant.getFinalStatus()).toLowerCase();

                        boolean baseEligible =
                                "waiting".equals(selectionStatus)
                                        || ("selected".equals(selectionStatus)
                                        && "pending".equals(responseStatus)
                                        && !"enrolled".equals(finalStatus));

                        if (!baseEligible) {
                            continue;
                        }

                        String entrantId = safe(entrant.getEntrantId());
                        if (entrantId.isEmpty()) {
                            continue;
                        }

                        com.google.android.gms.tasks.Task<EntrantCandidate> candidateTask =
                                repository.hasPendingCoOrganizerInvitation(entrantId, eventId)
                                        .continueWith(task -> {
                                            boolean hasPending = false;
                                            if (task.isSuccessful() && task.getResult() != null) {
                                                hasPending = task.getResult();
                                            }
                                            return new EntrantCandidate(entrant, hasPending);
                                        });

                        candidateTasks.add(candidateTask);
                    }

                    if (candidateTasks.isEmpty()) {
                        Toast.makeText(this,
                                "No eligible entrants available for co-organizer invitation",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Tasks.whenAllSuccess(candidateTasks)
                            .addOnSuccessListener(results -> {
                                List<Entrant> eligibleEntrants = new ArrayList<>();

                                for (Object result : results) {
                                    EntrantCandidate candidate = (EntrantCandidate) result;
                                    if (!candidate.hasPendingInvitation && candidate.entrant != null) {
                                        eligibleEntrants.add(candidate.entrant);
                                    }
                                }

                                if (eligibleEntrants.isEmpty()) {
                                    Toast.makeText(this,
                                            "All eligible entrants already have pending co-organizer invitations",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                String[] labels = new String[eligibleEntrants.size()];
                                for (int i = 0; i < eligibleEntrants.size(); i++) {
                                    Entrant entrant = eligibleEntrants.get(i);

                                    String selectionStatus = safe(entrant.getSelectionStatus()).toLowerCase();
                                    String responseStatus = safe(entrant.getResponseStatus()).toLowerCase();

                                    String displayStatus;
                                    if ("waiting".equals(selectionStatus)) {
                                        displayStatus = "Waiting";
                                    } else if ("selected".equals(selectionStatus)
                                            && "pending".equals(responseStatus)) {
                                        displayStatus = "Selected / Pending";
                                    } else {
                                        displayStatus = "Eligible";
                                    }

                                    labels[i] = safe(entrant.getEntrantName())
                                            + " (" + safe(entrant.getEntrantEmail()) + ")"
                                            + " - " + displayStatus;
                                }

                                final int[] selectedIndex = {-1};

                                new MaterialAlertDialogBuilder(this)
                                        .setTitle("Invite Co-organizer")
                                        .setSingleChoiceItems(
                                                labels,
                                                -1,
                                                (dialog, which) -> selectedIndex[0] = which
                                        )
                                        .setNegativeButton("Cancel", null)
                                        .setPositiveButton("Send Invitation", (dialog, which) -> {
                                            if (selectedIndex[0] < 0) {
                                                Toast.makeText(this,
                                                        "Please select an entrant",
                                                        Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            assignCoOrganizer(eligibleEntrants.get(selectedIndex[0]));
                                        })
                                        .show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "Failed to check existing co-organizer invitations",
                                            Toast.LENGTH_LONG
                                    ).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load entrants", Toast.LENGTH_LONG).show());
    }

    /**
     * Sends a co-organizer invitation to the selected entrant.
     *
     * <p>This does NOT immediately promote the entrant to co-organizer and does NOT
     * remove them from the waitlist. Promotion only happens after the entrant accepts
     * the invitation from their notifications screen.</p>
     *
     * <p>If a pending co-organizer invitation already exists for the same entrant
     * and event, this method does not send a duplicate invitation.</p>
     *
     * @param entrant entrant to invite
     */
    private void assignCoOrganizer(Entrant entrant) {
        if (entrant == null || safe(entrant.getEntrantId()).isEmpty()) {
            Toast.makeText(this, "Invalid entrant", Toast.LENGTH_SHORT).show();
            return;
        }

        String entrantId = entrant.getEntrantId();

        String eventName = tvEventName.getText() == null
                ? ""
                : tvEventName.getText().toString().trim();

        NotificationRepository repository = new NotificationRepository(db);

        repository.sendCoOrganizerInvitation(entrantId, eventId, eventName)
                .addOnSuccessListener(result -> {
                    if (result == null) {
                        Toast.makeText(
                                this,
                                "Failed to send co-organizer invitation",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (result.isAlreadyPending()) {
                        Toast.makeText(
                                this,
                                "Co-organizer invitation already pending",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    Toast.makeText(
                            this,
                            "Co-organizer invitation sent",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadWaitlistCounts();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                e.getMessage() != null
                                        ? e.getMessage()
                                        : "Failed to send co-organizer invitation",
                                Toast.LENGTH_LONG
                        ).show());
    }

    /**
     * Loads immutable event metadata shown on the screen.
     */
    private void loadEventData() {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String name = safe(documentSnapshot.getString("name"));
                    String posterUrl = safe(documentSnapshot.getString("posterUrl"));

                    Long limitLong = documentSnapshot.getLong("waitlistLimit");
                    String limitText = limitLong == null ? "Unlimited" : String.valueOf(limitLong);

                    Object eventDateObject = documentSnapshot.get("registrationStart");
                    String formattedDate = formatEventDate(eventDateObject);

                    tvEventName.setText(name.isEmpty() ? "Event Name" : name);
                    tvEventDate.setText(formattedDate);
                    tvEventCapacity.setText("Waitlist Limit: " + limitText);

                    currentPosterUrl = posterUrl;
                    hasPoster = !currentPosterUrl.isEmpty();

                    if (hasPoster) {
                        Glide.with(this)
                                .load(currentPosterUrl)
                                .into(imgEventPoster);
                    } else {
                        imgEventPoster.setImageDrawable(null);
                    }

                    updatePosterUI();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Loads the authoritative waitlist counts from the waitlist subcollection,
     * updates the UI, and writes those counts back to the top-level event document.
     */
    private void loadWaitlistCounts() {
        computeAndPersistWaitlistCounts()
                .addOnSuccessListener(counts -> {
                    tvWaitingCount.setText(String.valueOf(counts.waiting));
                    tvSelectedCount.setText(String.valueOf(counts.selected));
                    tvEnrolledCount.setText(String.valueOf(counts.enrolled));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load waitlist counts", Toast.LENGTH_SHORT).show());
    }

    /**
     * Computes waitlist counts from the waitlist subcollection and persists them to
     * the top-level event document.
     *
     * @return a task containing the computed counts
     */
    private com.google.android.gms.tasks.Task<WaitlistCounts> computeAndPersistWaitlistCounts() {
        TaskCompletionSource<WaitlistCounts> taskSource = new TaskCompletionSource<>();

        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int waiting = 0;
                    int selected = 0;
                    int enrolled = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String selectionStatus = safe(doc.getString("selectionStatus")).toLowerCase();
                        String finalStatus = safe(doc.getString("finalStatus")).toLowerCase();

                        if ("enrolled".equals(finalStatus)) {
                            enrolled++;
                        } else if ("selected".equals(selectionStatus)) {
                            selected++;
                        } else if ("waiting".equals(selectionStatus)) {
                            waiting++;
                        }
                    }

                    WaitlistCounts counts = new WaitlistCounts(waiting, selected, enrolled);

                    db.collection("events")
                            .document(eventId)
                            .update(
                                    "waitingCount", waiting,
                                    "selectedCount", selected,
                                    "enrolledCount", enrolled
                            )
                            .addOnSuccessListener(unused -> taskSource.setResult(counts))
                            .addOnFailureListener(taskSource::setException);
                })
                .addOnFailureListener(taskSource::setException);

        return taskSource.getTask();
    }

    /**
     * Exports all currently enrolled entrants to a CSV file.
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

    /**
     * Writes the enrolled entrants to a CSV file stored in app external storage.
     *
     * @param enrolledEntrants entrants to export
     */
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
                writer.append(csv(formatCsvDate(entrant.getJoinedAt()))).append(",");
                writer.append(csv(safe(entrant.getSelectionStatus()))).append(",");
                writer.append(csv(safe(entrant.getResponseStatus()))).append(",");
                writer.append(csv(safe(entrant.getFinalStatus()))).append("\n");
            }

            writer.flush();
            Toast.makeText(this, "CSV exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

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

    /**
     * Formats a date for CSV export.
     *
     * @param date date to format
     * @return formatted timestamp string, or empty string if null
     */
    private String formatCsvDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date);
    }

    /**
     * Escapes a value for safe CSV output.
     *
     * @param value raw value
     * @return quoted and escaped CSV-safe string
     */
    private String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    /**
     * Updates poster-related UI visibility and button state.
     */
    private void updatePosterUI() {
        if (hasPoster) {
            imgEventPoster.setVisibility(View.VISIBLE);
            layoutPosterPlaceholder.setVisibility(View.GONE);
            fabRemovePoster.setVisibility(View.VISIBLE);

            btnUploadPoster.setEnabled(false);
            btnUpdatePoster.setEnabled(true);
        } else {
            imgEventPoster.setVisibility(View.GONE);
            layoutPosterPlaceholder.setVisibility(View.VISIBLE);
            fabRemovePoster.setVisibility(View.GONE);

            btnUploadPoster.setEnabled(true);
            btnUpdatePoster.setEnabled(false);
        }
    }

    /**
     * Opens the image picker for poster upload.
     */
    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    /**
     * Uploads the selected poster image to Firebase Storage and stores its download URL.
     *
     * @param imageUri chosen image URI
     */
    private void uploadPosterToFirebase(Uri imageUri) {
        StorageReference posterRef = storage.getReference()
                .child("posters/" + organizerId + "/" + eventId + "/poster.jpg");

        posterRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        if (exception != null) {
                            throw exception;
                        }
                    }
                    return posterRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    currentPosterUrl = downloadUri.toString();
                    hasPoster = true;

                    db.collection("events")
                            .document(eventId)
                            .update("posterUrl", currentPosterUrl)
                            .addOnSuccessListener(unused -> {
                                Glide.with(this)
                                        .load(currentPosterUrl)
                                        .into(imgEventPoster);

                                updatePosterUI();
                                Toast.makeText(this, "Poster uploaded successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to save poster URL", Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Removes the poster URL from the event document and resets poster UI.
     */
    private void removePoster() {
        db.collection("events")
                .document(eventId)
                .update("posterUrl", "")
                .addOnSuccessListener(unused -> {
                    currentPosterUrl = "";
                    hasPoster = false;
                    imgEventPoster.setImageDrawable(null);
                    updatePosterUI();
                    Toast.makeText(this, "Poster removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove poster", Toast.LENGTH_LONG).show());
    }

    /**
     * Formats the event registration start value for display.
     *
     * @param eventDateObject Firestore field value
     * @return formatted date string
     */
    @NonNull
    private String formatEventDate(Object eventDateObject) {
        if (eventDateObject instanceof com.google.firebase.Timestamp) {
            Date date = ((com.google.firebase.Timestamp) eventDateObject).toDate();
            return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date);
        }

        if (eventDateObject instanceof Date) {
            return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format((Date) eventDateObject);
        }

        if (eventDateObject instanceof String) {
            String value = ((String) eventDateObject).trim();
            return value.isEmpty() ? "Date not available" : value;
        }

        return "Date not available";
    }

    /**
     * Returns a non-null string for null-safe comparisons.
     *
     * @param value input string
     * @return original string or empty string if null
     */
    @NonNull
    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * Small helper object used while building the co-organizer candidate list.
     */
    private static class EntrantCandidate {
        final Entrant entrant;
        final boolean hasPendingInvitation;

        EntrantCandidate(Entrant entrant, boolean hasPendingInvitation) {
            this.entrant = entrant;
            this.hasPendingInvitation = hasPendingInvitation;
        }
    }

    /**
     * Immutable value object holding waitlist-derived counts.
     */
    private static class WaitlistCounts {
        final int waiting;
        final int selected;
        final int enrolled;

        WaitlistCounts(int waiting, int selected, int enrolled) {
            this.waiting = waiting;
            this.selected = selected;
            this.enrolled = enrolled;
        }
    }
}