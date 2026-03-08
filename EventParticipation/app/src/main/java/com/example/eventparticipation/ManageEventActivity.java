package com.example.eventparticipation;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Organizer screen for managing a specific event.
 *
 * <p>This activity loads event details, displays waitlist statistics, and supports
 * event poster upload, update, and removal. It is the main implementation for:</p>
 * <ul>
 *     <li>US 02.04.01 - Upload an event poster</li>
 *     <li>US 02.04.02 - Update an event poster</li>
 * </ul>
 */
public class ManageEventActivity extends AppCompatActivity {

    /** Event name label. */
    private TextView tvEventName;

    /** Event date label. */
    private TextView tvEventDate;

    /** Event capacity label. */
    private TextView tvEventCapacity;

    /** Waiting count label. */
    private TextView tvWaitingCount;

    /** Selected count label. */
    private TextView tvSelectedCount;

    /** Enrolled count label. */
    private TextView tvEnrolledCount;

    /** Poster preview image. */
    private ImageView imgEventPoster;

    /** Placeholder shown when no poster exists. */
    private LinearLayout layoutPosterPlaceholder;

    /** Floating action button that removes the current poster. */
    private FloatingActionButton fabRemovePoster;

    /** Button used when there is no poster yet. */
    private MaterialButton btnUploadPoster;

    /** Button used when a poster already exists and needs replacement. */
    private MaterialButton btnUpdatePoster;

    /** Button opening the entrant list screen. */
    private MaterialButton btnViewEntrants;

    /** Button opening the waitlist map screen. */
    private MaterialButton btnViewMap;

    /** Placeholder lottery action. */
    private MaterialButton btnRunLottery;

    /** Placeholder QR code action. */
    private MaterialButton btnShowQRCode;

    /** Placeholder event edit action. */
    private MaterialButton btnEditEvent;

    /** Current event id. */
    private String eventId;

    /** Current organizer id. */
    private String organizerId;

    /** Indicates whether the event currently has a poster. */
    private boolean hasPoster = false;

    /** Cached poster URL for the current event. */
    private String currentPosterUrl = "";

    /** Firestore database reference. */
    private FirebaseFirestore db;

    /** Firebase Storage reference. */
    private FirebaseStorage storage;

    /**
     * Launcher used to open the system image picker for selecting a poster.
     */
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadPosterToFirebase(uri);
                }
            });

    /**
     * Initializes the screen, validates intent extras, and loads event data.
     *
     * @param savedInstanceState previously saved state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_event);

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
        storage = FirebaseStorage.getInstance();

        setupToolbar();
        initViews();
        setupClickListeners();
        updatePosterUI();

        loadEventData();
        loadWaitlistCounts();
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
     * Handles toolbar up navigation.
     *
     * @return always returns {@code true}
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Binds view references from the layout.
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
        btnShowQRCode = findViewById(R.id.btnShowQRCode);
        btnEditEvent = findViewById(R.id.btnEditEvent);
    }

    /**
     * Registers click actions for poster operations and navigation buttons.
     */
    private void setupClickListeners() {
        btnUploadPoster.setOnClickListener(v -> openImagePicker());
        btnUpdatePoster.setOnClickListener(v -> openImagePicker());

        fabRemovePoster.setOnClickListener(v -> removePoster());

        btnViewEntrants.setOnClickListener(v -> {
            Intent intent = new Intent(ManageEventActivity.this, EntrantListActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            intent.putExtra("ORGANIZER_ID", organizerId);
            startActivity(intent);
        });

        btnViewMap.setOnClickListener(v -> {
            Intent intent = new Intent(ManageEventActivity.this, WaitlistMapActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            intent.putExtra("ORGANIZER_ID", organizerId);
            startActivity(intent);
        });

        btnRunLottery.setOnClickListener(v ->
                Toast.makeText(this, "Lottery feature coming soon", Toast.LENGTH_SHORT).show());

        btnShowQRCode.setOnClickListener(v ->
                Toast.makeText(this, "QR Code feature coming soon", Toast.LENGTH_SHORT).show());

        btnEditEvent.setOnClickListener(v ->
                Toast.makeText(this, "Edit event feature coming soon", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads the main event document and updates the UI with event and poster data.
     */
    private void loadEventData() {
        db.collection("organizers")
                .document(organizerId)
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Event document not found", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String rawName = doc.getString("name");
                    Long rawCapacity = doc.getLong("capacity");

                    tvEventName.setText(rawName != null && !rawName.trim().isEmpty() ? rawName : "-");
                    tvEventCapacity.setText("Capacity: " + (rawCapacity != null ? rawCapacity : 0));

                    Date startTime = doc.getDate("startTime");
                    if (startTime != null) {
                        tvEventDate.setText(formatDate(startTime));
                    } else {
                        tvEventDate.setText("-");
                    }

                    currentPosterUrl = doc.getString("posterUrl");
                    if (currentPosterUrl == null) {
                        currentPosterUrl = "";
                    }

                    hasPoster = !currentPosterUrl.isEmpty();

                    if (hasPoster) {
                        Glide.with(this)
                                .load(currentPosterUrl)
                                .into(imgEventPoster);
                    } else {
                        imgEventPoster.setImageDrawable(null);
                    }

                    updatePosterUI();

                    android.util.Log.d(
                            "MANAGE_EVENT",
                            "eventId=" + eventId
                                    + ", organizerId=" + organizerId
                                    + ", name=" + rawName
                                    + ", capacity=" + rawCapacity
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Loads waitlist entries and calculates status counts for summary cards.
     */
    private void loadWaitlistCounts() {
        db.collection("organizers")
                .document(organizerId)
                .collection("events")
                .document(eventId)
                .collection("waitlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int waiting = 0;
                    int selected = 0;
                    int enrolled = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String status = doc.getString("status");

                        if ("waiting".equals(status)) {
                            waiting++;
                        } else if ("selected".equals(status)) {
                            selected++;
                        } else if ("enrolled".equals(status)) {
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

    /**
     * Opens the system image picker for selecting a poster image.
     */
    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    /**
     * Uploads the selected poster to Firebase Storage and stores its URL in Firestore.
     *
     * <p>If the event already has a poster, this method replaces it with the new image,
     * which satisfies the poster update user story.</p>
     *
     * @param imageUri URI of the selected local image
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

                    db.collection("organizers")
                            .document(organizerId)
                            .collection("events")
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
     * Removes the poster URL from Firestore and updates the poster UI to empty state.
     */
    private void removePoster() {
        db.collection("organizers")
                .document(organizerId)
                .collection("events")
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
     * Refreshes poster-related controls depending on whether a poster currently exists.
     */
    private void updatePosterUI() {
        if (hasPoster) {
            imgEventPoster.setVisibility(android.view.View.VISIBLE);
            layoutPosterPlaceholder.setVisibility(android.view.View.GONE);
            fabRemovePoster.setVisibility(android.view.View.VISIBLE);
            btnUploadPoster.setEnabled(false);
            btnUpdatePoster.setEnabled(true);
        } else {
            imgEventPoster.setVisibility(android.view.View.GONE);
            layoutPosterPlaceholder.setVisibility(android.view.View.VISIBLE);
            fabRemovePoster.setVisibility(android.view.View.GONE);
            btnUploadPoster.setEnabled(true);
            btnUpdatePoster.setEnabled(false);
        }
    }

    /**
     * Formats a date value for UI display.
     *
     * @param date raw date value
     * @return formatted date string, or {@code "-"} if null
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "-";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Converts a nullable string into a display-safe fallback value.
     *
     * @param value raw text value
     * @return original value, or {@code "-"} if null
     */
    private String safe(String value) {
        return value == null ? "-" : value;
    }

    /**
     * Reloads event data when the activity returns to the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadEventData();
        loadWaitlistCounts();
    }
}