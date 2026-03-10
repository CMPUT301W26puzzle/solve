package com.example.eventparticipation;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
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
 * event poster upload, update, and removal.</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 *     <li>US 02.02.01 - View the list of entrants in the waiting list</li>
 *     <li>US 02.02.02 - View entrant locations on a map</li>
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

    /** Placeholder layout shown when no poster exists. */
    private LinearLayout layoutPosterPlaceholder;

    /** Floating button used to remove the current poster. */
    private FloatingActionButton fabRemovePoster;

    /** Button used to upload a poster when none exists. */
    private MaterialButton btnUploadPoster;

    /** Button used to replace an existing poster. */
    private MaterialButton btnUpdatePoster;

    /** Button opening the entrant list screen. */
    private MaterialButton btnViewEntrants;

    /** Button opening the waitlist map screen. */
    private MaterialButton btnViewMap;

    /** Placeholder lottery button. */
    private MaterialButton btnRunLottery;

    /** Placeholder QR code button. */
    private MaterialButton btnShowQRCode;

    /** Placeholder edit event button. */
    private MaterialButton btnEditEvent;

    /** Event id passed into this screen. */
    private String eventId;

    /** Organizer id passed into this screen. */
    private String organizerId;

    /** Indicates whether the event currently has a poster. */
    private boolean hasPoster = false;

    /** Cached poster URL of the current event. */
    private String currentPosterUrl = "";

    /** Firestore database instance. */
    private FirebaseFirestore db;

    /** Firebase Storage instance. */
    private FirebaseStorage storage;

    /**
     * Activity result launcher used to pick an image from the device.
     */
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadPosterToFirebase(uri);
                }
            });

    /**
     * Initializes the activity, validates intent extras, sets up the toolbar,
     * and loads event data.
     *
     * @param savedInstanceState previously saved state bundle, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_event);

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
        storage = FirebaseStorage.getInstance();

        setupToolbar();
        initViews();
        setInitialPlaceholderValues();
        setupClickListeners();
        updatePosterUI();

        loadEventData();
        loadWaitlistCounts();
    }

    /**
     * Applies status bar insets to the toolbar so that the toolbar content
     * stays below the system status bar on edge-to-edge devices such as Pixel 9.
     *
     * <p>This method increases both the top padding and the toolbar height,
     * which keeps the back arrow and title visible instead of being clipped.</p>
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Binds layout views from the XML layout.
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
     * Sets safe placeholder values before Firebase data finishes loading.
     * This prevents old sample values from briefly flashing on screen.
     */
    private void setInitialPlaceholderValues() {
        tvEventName.setText("Event Name");
        tvEventDate.setText("Date not available");
        tvEventCapacity.setText("Capacity: 0");

        tvWaitingCount.setText("0");
        tvSelectedCount.setText("0");
        tvEnrolledCount.setText("0");

        currentPosterUrl = "";
        hasPoster = false;
        imgEventPoster.setImageDrawable(null);
    }

    /**
     * Registers click listeners for screen actions.
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

        btnRunLottery.setOnClickListener(v ->
                Toast.makeText(this, "Lottery feature coming soon", Toast.LENGTH_SHORT).show());

        btnShowQRCode.setOnClickListener(v ->
                Toast.makeText(this, "QR code feature coming soon", Toast.LENGTH_SHORT).show());

        btnEditEvent.setOnClickListener(v ->
                Toast.makeText(this, "Edit event feature coming soon", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads event details from Firestore and updates the event info UI.
     */
    private void loadEventData() {
        db.collection("organizers")
                .document(organizerId)
                .collection("events")
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

                    Long capacityLong = documentSnapshot.getLong("capacity");
                    int capacity = capacityLong == null ? 0 : capacityLong.intValue();

                    Object eventDateObject = documentSnapshot.get("eventDate");
                    String formattedDate = formatEventDate(eventDateObject);

                    tvEventName.setText(name.isEmpty() ? "Event Name" : name);
                    tvEventDate.setText(formattedDate);
                    tvEventCapacity.setText("Capacity: " + capacity);

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
     * Loads waitlist entries and counts waiting, selected, and enrolled entrants.
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
     * Updates poster-related controls depending on whether a poster exists.
     */
    private void updatePosterUI() {
        if (hasPoster) {
            imgEventPoster.setVisibility(ViewGroup.VISIBLE);
            layoutPosterPlaceholder.setVisibility(ViewGroup.GONE);
            fabRemovePoster.setVisibility(ViewGroup.VISIBLE);

            btnUploadPoster.setEnabled(false);
            btnUpdatePoster.setEnabled(true);
        } else {
            imgEventPoster.setVisibility(ViewGroup.GONE);
            layoutPosterPlaceholder.setVisibility(ViewGroup.VISIBLE);
            fabRemovePoster.setVisibility(ViewGroup.GONE);

            btnUploadPoster.setEnabled(true);
            btnUpdatePoster.setEnabled(false);
        }
    }

    /**
     * Opens the system image picker for poster selection.
     */
    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    /**
     * Uploads a selected poster image to Firebase Storage and saves its URL in Firestore.
     *
     * @param imageUri selected local image URI
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
     * Removes the current poster URL from Firestore and resets poster UI state.
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
     * Formats an event date object into display text.
     *
     * @param eventDateObject raw Firestore event date value
     * @return formatted date string, or a safe fallback
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
     * Converts a nullable string into a non-null value.
     *
     * @param value raw value
     * @return empty string when null, otherwise the original value
     */
    @NonNull
    private String safe(String value) {
        return value == null ? "" : value;
    }
}