package com.example.eventparticipation;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.text.InputType;
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
 * <li>US 02.02.01 - View the list of entrants in the waiting list</li>
 * <li>US 02.02.02 - View entrant locations on a map</li>
 * <li>US 02.04.01 - Upload an event poster</li>
 * <li>US 02.04.02 - Update an event poster</li>
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

    /** Button to draw a replacement applicant. */
    private MaterialButton btnDrawReplacement;

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

        // Using direct paths instead of fallbacks for consistency
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
        // The toolbar view to apply edge-to-edge padding to
        Toolbar toolbar = findViewById(R.id.toolbar);

        // Captured original left padding of the toolbar
        final int originalPaddingLeft = toolbar.getPaddingLeft();
        // Captured original top padding of the toolbar
        final int originalPaddingTop = toolbar.getPaddingTop();
        // Captured original right padding of the toolbar
        final int originalPaddingRight = toolbar.getPaddingRight();
        // Captured original bottom padding of the toolbar
        final int originalPaddingBottom = toolbar.getPaddingBottom();
        // Captured default defined height of the toolbar from the theme
        final int originalToolbarHeight = getToolbarHeight();

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, windowInsets) -> {
            // Extracted system insets specifically for the status bar
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());

            view.setPadding(
                    originalPaddingLeft,
                    originalPaddingTop + insets.top,
                    originalPaddingRight,
                    originalPaddingBottom
            );

            // Layout parameter references used to dynamically adjust the view's height
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
        // Holder object to resolve attribute data dynamically from the context's theme
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
        // The toolbar view fetched from the layout file
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
        btnDrawReplacement = findViewById(R.id.btnDrawReplacement);
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
        tvEventCapacity.setText("Waitlist Limit: Unlimited");

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
            // Intent to navigate to the list of waiting entrants
            Intent intent = new Intent(this, EntrantListActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            intent.putExtra("ORGANIZER_ID", organizerId);
            startActivity(intent);
        });

        btnViewMap.setOnClickListener(v -> {
            // Intent to navigate to the geographical map of waitlisted users
            Intent intent = new Intent(this, WaitlistMapActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            intent.putExtra("ORGANIZER_ID", organizerId);
            startActivity(intent);
        });

        btnRunLottery.setOnClickListener(v -> showRunLotteryDialog());

        if (btnDrawReplacement != null) {
            btnDrawReplacement.setOnClickListener(v -> drawReplacementApplicant());
        }

        btnShowQRCode.setOnClickListener(v ->
                Toast.makeText(this, "QR code feature coming soon", Toast.LENGTH_SHORT).show());

        btnEditEvent.setOnClickListener(v ->
                Toast.makeText(this, "Edit event feature coming soon", Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows a dialog to collect the sample size and triggers the lottery algorithm.
     */
    private void showRunLotteryDialog() {
        // Text input field provided to the MaterialAlertDialog to gather sample size
        EditText input = new EditText(this);
        input.setHint("Number of entrants to select");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Run Lottery")
                .setMessage("Select how many entrants should receive invitations.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Run", (dialog, which) -> {
                    // Raw string value extracted from the edit text view
                    String value = input.getText() == null ? "" : input.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(this, "Enter a lottery size", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Parsed integer designating the quantity of users to sample
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
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    e.getMessage() != null ? e.getMessage() : "Failed to run lottery",
                                    Toast.LENGTH_LONG).show());
                })
                .show();
    }

    /**
     * Draws a single replacement using WaitlistController.
     */
    private void drawReplacementApplicant() {
        new WaitlistController().drawReplacement(eventId)
                .addOnSuccessListener(entrantId -> {
                    if (entrantId != null) {
                        Toast.makeText(this, "Replacement drawn successfully!", Toast.LENGTH_SHORT).show();
                        loadWaitlistCounts(); // Refresh UI counts
                    } else {
                        Toast.makeText(this, "Waitlist is empty. No replacement drawn.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to draw replacement.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads event details from Firestore and updates the event info UI.
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
     * Loads waitlist entries and counts waiting, selected, and enrolled entrants.
     */
    private void loadWaitlistCounts() {
        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Running tally for the users whose status is currently "waiting"
                    int waiting = 0;
                    // Running tally for the users whose status is currently "selected"
                    int selected = 0;
                    // Running tally for the users whose status is currently "enrolled"
                    int enrolled = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // The string denoting the waitlist progression status of an individual document
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
        // Concrete storage path pointing to the event poster jpg location in Google Cloud Storage
        StorageReference posterRef = storage.getReference()
                .child("posters/" + organizerId + "/" + eventId + "/poster.jpg");

        posterRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        // The potential error emitted during the firebase storage upload action
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
     * Removes the current poster URL from Firestore and resets poster UI state.
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
     * Formats an event date object into display text.
     *
     * @param eventDateObject raw Firestore event date value
     * @return formatted date string, or a safe fallback
     */
    @NonNull
    private String formatEventDate(Object eventDateObject) {
        if (eventDateObject instanceof com.google.firebase.Timestamp) {
            // Standard Java date instance mapped directly from the incoming Firebase Timestamp
            Date date = ((com.google.firebase.Timestamp) eventDateObject).toDate();
            return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date);
        }

        if (eventDateObject instanceof Date) {
            return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format((Date) eventDateObject);
        }

        if (eventDateObject instanceof String) {
            // Trimmed plain text string fallback for unparseable dates
            String value = ((String) eventDateObject).trim();
            return value.isEmpty() ? "Date not available" : value;
        }

        return "Date not available";
    }

    /**
     * Converts a nullable string into a non-null value
     * @param value raw value
     * @return empty string when null, otherwise the original value
     */
    @NonNull
    private String safe(String value) {
        return value == null ? "" : value;
    }
}