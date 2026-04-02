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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FieldValue;
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
 * Access rules:
 * - organizer can access
 * - co-organizer can access
 * - co-organizer cannot assign another co-organizer
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

                    isOwner = currentUserId.equals(ownerId) || currentUserId.equals(organizerId);
                    isCoOrganizer = coOrganizerIds != null && coOrganizerIds.contains(currentUserId);

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

    private void applyRoleRestrictions() {
        if (isCoOrganizer && !isOwner) {
            btnAssignCoOrganizer.setVisibility(View.GONE);
        } else {
            btnAssignCoOrganizer.setVisibility(View.VISIBLE);
        }
    }

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

    private void showAssignCoOrganizerDialog() {
        if (!isOwner) {
            Toast.makeText(this, "Only the organizer can assign co-organizers", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Entrant> eligibleEntrants = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        entrant.setId(doc.getId());

                        String selectionStatus = safe(entrant.getSelectionStatus()).toLowerCase();
                        String responseStatus = safe(entrant.getResponseStatus()).toLowerCase();
                        String finalStatus = safe(entrant.getFinalStatus()).toLowerCase();

                        boolean canAssign =
                                "waiting".equals(selectionStatus)
                                        || ("selected".equals(selectionStatus)
                                        && "pending".equals(responseStatus)
                                        && !"enrolled".equals(finalStatus));

                        if (canAssign) {
                            eligibleEntrants.add(entrant);
                        }
                    }

                    if (eligibleEntrants.isEmpty()) {
                        Toast.makeText(this,
                                "No eligible entrants available for co-organizer assignment",
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
                        } else if ("selected".equals(selectionStatus) && "pending".equals(responseStatus)) {
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
                            .setTitle("Assign Co-organizer")
                            .setSingleChoiceItems(labels, -1, (dialog, which) -> selectedIndex[0] = which)
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Assign", (dialog, which) -> {
                                if (selectedIndex[0] < 0) {
                                    Toast.makeText(this, "Please select an entrant", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                assignCoOrganizer(eligibleEntrants.get(selectedIndex[0]));
                            })
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load entrants", Toast.LENGTH_LONG).show());
    }

    private void assignCoOrganizer(Entrant entrant) {
        if (entrant == null || safe(entrant.getEntrantId()).isEmpty()) {
            Toast.makeText(this, "Invalid entrant", Toast.LENGTH_SHORT).show();
            return;
        }

        String entrantId = entrant.getEntrantId();

        com.google.firebase.firestore.DocumentReference eventRef =
                db.collection("events").document(eventId);

        com.google.firebase.firestore.DocumentReference waitRef =
                eventRef.collection("waitlist").document(entrantId);

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        batch.update(eventRef, "coOrganizerIds", FieldValue.arrayUnion(entrantId));
        batch.delete(waitRef);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Co-organizer assigned successfully", Toast.LENGTH_SHORT).show();
                    loadWaitlistCounts();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to assign co-organizer", Toast.LENGTH_LONG).show());
    }

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

    private void loadWaitlistCounts() {
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

                        if ("waiting".equals(selectionStatus)) {
                            waiting++;
                        } else if ("selected".equals(selectionStatus) && !"enrolled".equals(finalStatus)) {
                            selected++;
                        } else if ("enrolled".equals(finalStatus)) {
                            enrolled++;
                        }
                    }

                    tvWaitingCount.setText(String.valueOf(waiting));
                    tvSelectedCount.setText(String.valueOf(selected));
                    tvEnrolledCount.setText(String.valueOf(enrolled));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load waitlist counts", Toast.LENGTH_SHORT).show());
    }

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

    private String formatCsvDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date);
    }

    private String csv(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

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

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

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

    @NonNull
    private String safe(String value) {
        return value == null ? "" : value;
    }
}