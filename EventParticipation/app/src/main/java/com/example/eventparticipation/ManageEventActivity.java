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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main control panel for organizers and co-organizers to manage an event.
 *
 * <p>This activity provides comprehensive event management tools including lottery execution,
 * waitlist management, participant communications, poster modifications, and metrics tracking.
 * Access is restricted strictly to the event's designated owner and assigned co-organizers.</p>
 */
public class ManageEventActivity extends AppCompatActivity {

    /** Text view for displaying the event's name. */
    private TextView tvEventName;

    /** Text view for displaying the event's registration date range. */
    private TextView tvEventDate;

    /** Text view for displaying the event's maximum waitlist capacity. */
    private TextView tvEventCapacity;

    /** Text view displaying the current number of entrants in the "waiting" state. */
    private TextView tvWaitingCount;

    /** Text view displaying the current number of entrants in the "selected" state. */
    private TextView tvSelectedCount;

    /** Text view displaying the current number of entrants in the "enrolled" state. */
    private TextView tvEnrolledCount;

    /** Image view for rendering the event's promotional poster. */
    private ImageView imgEventPoster;

    /** Layout container displayed when no poster is currently uploaded. */
    private LinearLayout layoutPosterPlaceholder;

    /** Floating action button for removing the currently uploaded poster. */
    private FloatingActionButton fabRemovePoster;

    /** Button to trigger the initial upload of a poster. */
    private MaterialButton btnUploadPoster;

    /** Button to trigger updating or replacing an existing poster. */
    private MaterialButton btnUpdatePoster;

    /** Button to navigate to the entrant list management screen. */
    private MaterialButton btnViewEntrants;

    /** Button to navigate to the waitlist geolocation map. */
    private MaterialButton btnViewMap;

    /** Button to trigger the lottery selection process. */
    private MaterialButton btnRunLottery;

    /** Button to draw a single replacement entrant from the waitlist. */
    private MaterialButton btnDrawReplacement;

    /** Button to display the event's promotional QR code. */
    private MaterialButton btnShowQRCode;

    /** Button to edit the event's core details. */
    private MaterialButton btnEditEvent;

    /** Button to export the final enrolled entrant list to a CSV file. */
    private MaterialButton btnExportCsv;

    /** Button to assign a waiting entrant as a co-organizer. */
    private MaterialButton btnAssignCoOrganizer;

    /** Button to manually invite a specific user to the event via email. */
    private MaterialButton btnInviteUser;

    /** Button to send a custom notification to a specific demographic group. */
    private MaterialButton btnMassNotification;

    /** Button to manually cancel an unresponsive entrant's spot. */
    private MaterialButton btnCancelEntrant;

    /** The unique document ID of the event being managed. */
    private String eventId;

    /** The device ID of the user who originally created the event (the owner). */
    private String organizerId;

    /** The device ID of the current user viewing this screen. */
    private String currentUserId;

    /** The string determining the user's entry access mode ("organizer" or "coorganizer"). */
    private String accessMode;

    /** Flag indicating whether the event currently has a poster uploaded. */
    private boolean hasPoster = false;

    /** The current download URL for the event's poster in Firebase Storage. */
    private String currentPosterUrl = "";

    /** Flag indicating if the current user is the primary creator and owner of the event. */
    private boolean isOwner = false;

    /** Flag indicating if the current user is an assigned co-organizer for the event. */
    private boolean isCoOrganizer = false;

    /** Entry point for Firestore database operations. */
    private FirebaseFirestore db;

    /** Entry point for Firebase Storage operations. */
    private FirebaseStorage storage;

    /** Launcher for the intent to pick images from the device gallery. */
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadPosterToFirebase(uri);
                }
            });

    /**
     * Initializes the activity, binds views, and triggers security access checks.
     *
     * @param savedInstanceState Contains data from the most recently supplied state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_event);

        applyWindowInsets();

        eventId = getIntent().getStringExtra("EVENT_ID");
        organizerId = getIntent().getStringExtra("ORGANIZER_ID");

        SessionManager session = SessionManager.getInstance(this);
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SelectRoleActivity.class));
            finish();
            return;
        }

        currentUserId = session.getUserId();
        accessMode = getIntent().getStringExtra("ACCESS_MODE");

        if (accessMode == null || accessMode.trim().isEmpty()) {
            accessMode = "organizer";
        }

        // Use current user ID as a fallback if organizerId was missing from Intent
        if (organizerId == null || organizerId.isEmpty()) {
            organizerId = currentUserId;
        }

        if (eventId == null) {
            Toast.makeText(this, "Missing Event ID", Toast.LENGTH_LONG).show();
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
     * Applies system bar insets to the toolbar to prevent overlap with the status bar.
     */
    private void applyWindowInsets() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        final int originalToolbarHeight = getToolbarHeight();

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), insets.top, view.getPaddingRight(), view.getPaddingBottom());
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = originalToolbarHeight + insets.top;
            view.setLayoutParams(layoutParams);
            return windowInsets;
        });
    }

    /**
     * Retrieves the default toolbar height defined by the current application theme.
     *
     * @return The toolbar height in pixels.
     */
    private int getToolbarHeight() {
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        return (int) (56 * getResources().getDisplayMetrics().density);
    }

    /**
     * Configures the action bar and enables up navigation.
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
     * Binds layout views to their corresponding class variables.
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

        btnInviteUser = findViewById(R.id.btnInviteUser);
        btnMassNotification = findViewById(R.id.btnMassNotification);
        btnCancelEntrant = findViewById(R.id.btnCancelEntrant);
    }

    /**
     * Populates the views with empty data before Firestore callbacks complete.
     */
    private void setInitialPlaceholderValues() {
        tvEventName.setText("Loading...");
        tvEventDate.setText("...");
        tvEventCapacity.setText("...");
        tvWaitingCount.setText("0");
        tvSelectedCount.setText("0");
        tvEnrolledCount.setText("0");
        imgEventPoster.setImageDrawable(null);
    }

    /**
     * Configures interaction callbacks for all action buttons.
     */
    private void setupClickListeners() {
        btnUploadPoster.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnUpdatePoster.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
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
        btnShowQRCode.setOnClickListener(v -> showQRCodeDialog());
        btnExportCsv.setOnClickListener(v -> exportEnrolledEntrantsToCsv());

        if (btnInviteUser != null) {
            btnInviteUser.setOnClickListener(v -> showInviteUserDialog());
        }
        if (btnMassNotification != null) {
            btnMassNotification.setOnClickListener(v -> showMassNotificationDialog());
        }
        if (btnCancelEntrant != null) {
            btnCancelEntrant.setOnClickListener(v -> showCancelEntrantDialog());
        }
    }

    /**
     * Verifies the user's role and halts execution if they lack administrative privileges.
     */
    private void checkManageAccessAndLoad() {
        db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                finish();
                return;
            }

            String actualOwnerId = safe(doc.getString("organizerId"));
            organizerId = actualOwnerId; // Ensure we use the server's owner ID

            List<String> coOrganizerIds = (List<String>) doc.get("coOrganizerIds");

            if ("organizer".equals(accessMode)) {
                isOwner = currentUserId.equals(actualOwnerId);
                isCoOrganizer = false;
            } else if ("coorganizer".equals(accessMode)) {
                isOwner = false;
                isCoOrganizer = coOrganizerIds != null && coOrganizerIds.contains(currentUserId);
            }

            if (!isOwner && !isCoOrganizer) {
                Toast.makeText(this, "Access Denied", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            btnAssignCoOrganizer.setVisibility((isCoOrganizer && !isOwner) ? View.GONE : View.VISIBLE);
            loadEventData(doc);
            loadWaitlistCounts();
        });
    }

    /**
     * Parses the Firestore document and populates the primary user interface elements.
     *
     * @param documentSnapshot The fetched event document.
     */
    private void loadEventData(DocumentSnapshot documentSnapshot) {
        String name = safe(documentSnapshot.getString("name"));
        String posterUrl = safe(documentSnapshot.getString("posterUrl"));
        Long limitLong = documentSnapshot.getLong("waitlistLimit");

        tvEventName.setText(name.isEmpty() ? "Unnamed Event" : name);
        tvEventCapacity.setText("Capacity: " + (limitLong == null ? "Unlimited" : limitLong));

        currentPosterUrl = posterUrl;
        hasPoster = !currentPosterUrl.isEmpty();

        if (hasPoster) {
            Glide.with(this).load(currentPosterUrl).into(imgEventPoster);
        }
        updatePosterUI();
    }

    /**
     * Fetches the waitlist, categorizes entrants by status, and synchronizes the totals.
     */
    private void loadWaitlistCounts() {
        db.collection("events").document(eventId).collection("waitlist").get()
                .addOnSuccessListener(querySnapshot -> {
                    int waiting = 0, selected = 0, enrolled = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String status = safe(doc.getString("selectionStatus")).toLowerCase();
                        String fStatus = safe(doc.getString("finalStatus")).toLowerCase();

                        if ("enrolled".equals(fStatus)) {
                            enrolled++;
                        } else if ("selected".equals(status)) {
                            selected++;
                        } else if ("waiting".equals(status)) {
                            waiting++;
                        }
                    }

                    tvWaitingCount.setText(String.valueOf(waiting));
                    tvSelectedCount.setText(String.valueOf(selected));
                    tvEnrolledCount.setText(String.valueOf(enrolled));

                    db.collection("events").document(eventId).update(
                            "waitingCount", waiting, "selectedCount", selected, "enrolledCount", enrolled);
                });
    }

    /**
     * Prompts the organizer for an identifying query (name, email, or phone) and forces a user onto the waitlist.
     */
    private void showInviteUserDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter user's name, email, or phone number");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Private Invite")
                .setMessage("Search for an entrant by name or contact info to add them directly to the waitlist.")
                .setView(input)
                .setPositiveButton("Invite", (dialog, which) -> {
                    String query = input.getText().toString().trim();
                    if (!query.isEmpty()) {
                        inviteUserBySearchTerm(query);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Locates a user document by matching their name, email, or phone number and generates a waitlist entity for them.
     *
     * @param searchTerm The target user's identifying information.
     */
    private void inviteUserBySearchTerm(String searchTerm) {
        db.collection("entrants")
                .where(com.google.firebase.firestore.Filter.or(
                        com.google.firebase.firestore.Filter.equalTo("email", searchTerm),
                        com.google.firebase.firestore.Filter.equalTo("name", searchTerm),
                        com.google.firebase.firestore.Filter.equalTo("phoneNumber", searchTerm)
                ))
                .limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String targetUserId = querySnapshot.getDocuments().get(0).getId();
                    Map<String, Object> waitlistEntry = new HashMap<>();
                    waitlistEntry.put("deviceId", targetUserId);
                    waitlistEntry.put("entrantId", targetUserId);
                    waitlistEntry.put("joinedAt", new Date());
                    waitlistEntry.put("selectionStatus", "waiting");

                    db.collection("events").document(eventId).collection("waitlist").document(targetUserId)
                            .set(waitlistEntry).addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "User successfully invited!", Toast.LENGTH_SHORT).show();
                                loadWaitlistCounts();
                            });
                });
    }

    /**
     * Displays a dialog allowing organizers to draft and route a custom message to an audience.
     */
    private void showMassNotificationDialog() {
        String[] audiences = {"All Waiting", "All Selected", "All Cancelled"};
        final int[] selectedAudience = {0};

        EditText inputMessage = new EditText(this);
        inputMessage.setHint("Type your message...");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Send Mass Notification")
                .setSingleChoiceItems(audiences, 0, (dialog, which) -> selectedAudience[0] = which)
                .setView(inputMessage)
                .setPositiveButton("Send", (dialog, which) -> {
                    String targetStatus = selectedAudience[0] == 1 ? "selected" : (selectedAudience[0] == 2 ? "cancelled" : "waiting");
                    sendMassNotification(targetStatus, inputMessage.getText().toString().trim());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Dispatches a custom notification payload to all entrants currently matching the target status.
     *
     * @param targetStatus The waitlist demographic to target.
     * @param message The content of the notification.
     */
    private void sendMassNotification(String targetStatus, String message) {
        if (message.isEmpty()) return;

        db.collection("events").document(eventId).collection("waitlist")
                .whereEqualTo("selectionStatus", targetStatus).get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        DocumentReference notifRef = db.collection("entrants").document(doc.getId())
                                .collection("notifications").document();

                        Map<String, Object> notification = new HashMap<>();
                        notification.put("eventId", eventId);
                        notification.put("type", "organizer_message");
                        notification.put("message", message);
                        notification.put("unread", true);
                        notification.put("createdAt", FieldValue.serverTimestamp());
                        batch.set(notifRef, notification);
                    }

                    batch.commit().addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Sent to " + querySnapshot.size() + " users.", Toast.LENGTH_SHORT).show());
                });
    }

    /**
     * Presents a dialog permitting the organizer to forcefully revoke an entrant's selection status.
     */
    private void showCancelEntrantDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter User Email to Cancel");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Entrant")
                .setView(input)
                .setPositiveButton("Cancel User", (dialog, which) -> {
                    db.collection("entrants").whereEqualTo("email", input.getText().toString().trim()).limit(1).get()
                            .addOnSuccessListener(q -> {
                                if (!q.isEmpty()) {
                                    cancelPendingEntrant(q.getDocuments().get(0).getId());
                                } else {
                                    Toast.makeText(this, "Entrant not found.", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Close", null)
                .show();
    }

    /**
     * Updates an entrant's state to cancelled, functionally ejecting them from the event.
     * Executes a query to locate the correct waitlist document safely.
     *
     * @param targetUserId The ID of the document to be terminated.
     */
    private void cancelPendingEntrant(String targetUserId) {
        db.collection("events").document(eventId).collection("waitlist")
                .whereEqualTo("entrantId", targetUserId).limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot waitlistDoc = querySnapshot.getDocuments().get(0);
                        waitlistDoc.getReference()
                                .update("selectionStatus", "cancelled", "responseStatus", "declined", "finalStatus", "cancelled")
                                .addOnSuccessListener(unused -> loadWaitlistCounts());
                    } else {
                        Toast.makeText(this, "Entrant not found on waitlist.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Deletes a specific event comment from the Firestore backend.
     *
     * @param commentId The unique ID of the comment to destroy.
     */
    public void deleteEventComment(String commentId) {
        if (!isOwner && !isCoOrganizer) return;

        db.collection("events").document(eventId)
                .collection("comments").document(commentId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show());
    }

    /**
     * Queries enrolled entrants, compiles their details into CSV format, and launches a system
     * share intent allowing the organizer to save or transmit the final file.
     */
    private void exportEnrolledEntrantsToCsv() {
        db.collection("events").document(eventId).collection("waitlist").whereEqualTo("finalStatus", "enrolled")
                .get().addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No enrolled entrants to export", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        File file = new File(getExternalFilesDir(null), "export_" + System.currentTimeMillis() + ".csv");
                        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file));
                        writer.append("Entrant ID,Name,Status\n");

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String name = doc.contains("entrantName") ? safe(doc.getString("entrantName")) : "Unknown";
                            writer.append(doc.getId()).append(",").append(name).append(",Enrolled\n");
                        }

                        writer.flush();
                        writer.close();

                        Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/csv");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(shareIntent, "Share CSV file"));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Generates a deep-link encoded QR code that routes scanning devices to the event listing.
     */
    private void showQRCodeDialog() {
        try {
            String deepLinkUrl = "https://eventparticipation.com/event?id=" + eventId;
            android.graphics.Bitmap qrBitmap = QRCodeGenerator.generateQRCode(deepLinkUrl, 512);
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(qrBitmap);
            imageView.setPadding(32, 32, 32, 32);

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Promotional QR Code")
                    .setView(imageView)
                    .setPositiveButton("Close", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Displays a numeric input prompt to execute the event lottery against the waiting queue.
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
                    if (value.isEmpty()) return;

                    int sampleSize;
                    try {
                        sampleSize = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        return;
                    }

                    new WaitlistController().runLottery(eventId, sampleSize)
                            .addOnSuccessListener(unused -> loadWaitlistCounts());
                })
                .show();
    }

    /**
     * Executes logic to randomly select a single replacement candidate from the pool.
     */
    private void drawReplacementApplicant() {
        new WaitlistController().drawReplacement(eventId)
                .addOnSuccessListener(entrantId -> {
                    if (entrantId != null) loadWaitlistCounts();
                });
    }

    /**
     * Fetches eligible users and displays a dialog allowing the organizer to dispatch
     * a co-organizer administrative role invitation.
     */
    private void showAssignCoOrganizerDialog() {
        if (!isOwner) return;

        NotificationRepository repository = new NotificationRepository(db);

        db.collection("events").document(eventId).collection("waitlist").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<com.google.android.gms.tasks.Task<EntrantCandidate>> candidateTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        entrant.setId(doc.getId());
                        String entId = safe(entrant.getEntrantId());
                        if (entId.isEmpty()) continue;

                        com.google.android.gms.tasks.Task<EntrantCandidate> candidateTask =
                                repository.hasPendingCoOrganizerInvitation(entId, eventId)
                                        .continueWith(task -> new EntrantCandidate(entrant, task.isSuccessful() && task.getResult() != null && task.getResult()));
                        candidateTasks.add(candidateTask);
                    }

                    if (candidateTasks.isEmpty()) return;

                    Tasks.whenAllSuccess(candidateTasks).addOnSuccessListener(results -> {
                        List<Entrant> eligibleEntrants = new ArrayList<>();
                        for (Object result : results) {
                            EntrantCandidate candidate = (EntrantCandidate) result;
                            if (!candidate.hasPendingInvitation && candidate.entrant != null) {
                                eligibleEntrants.add(candidate.entrant);
                            }
                        }

                        if (eligibleEntrants.isEmpty()) return;

                        String[] labels = new String[eligibleEntrants.size()];
                        for (int i = 0; i < eligibleEntrants.size(); i++) {
                            labels[i] = safe(eligibleEntrants.get(i).getEntrantName());
                        }

                        final int[] selectedIndex = {-1};
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Invite Co-organizer")
                                .setSingleChoiceItems(labels, -1, (dialog, which) -> selectedIndex[0] = which)
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Send Invitation", (dialog, which) -> {
                                    if (selectedIndex[0] >= 0) {
                                        assignCoOrganizer(eligibleEntrants.get(selectedIndex[0]));
                                    }
                                })
                                .show();
                    });
                });
    }

    /**
     * Commits a co-organizer invitation to the target entrant's notification array,
     * and strictly purges them from the event's waiting list to prevent lottery conflict.
     *
     * @param entrant The individual slated for promotion.
     */
    private void assignCoOrganizer(Entrant entrant) {
        if (entrant == null || safe(entrant.getEntrantId()).isEmpty()) return;

        NotificationRepository repository = new NotificationRepository(db);
        repository.sendCoOrganizerInvitation(entrant.getEntrantId(), eventId, safe(tvEventName.getText().toString()))
                .addOnSuccessListener(result -> {
                    // Forcefully remove the new co-organizer from the lottery pool
                    db.collection("events").document(eventId).collection("waitlist")
                            .whereEqualTo("entrantId", entrant.getEntrantId())
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                WriteBatch batch = db.batch();
                                for (QueryDocumentSnapshot doc : querySnapshot) {
                                    batch.delete(doc.getReference());
                                }
                                batch.commit().addOnSuccessListener(v -> loadWaitlistCounts());
                            });
                });
    }

    /**
     * Compresses the selected image URI into a smaller JPEG before committing it
     * to Firebase Storage to optimize data usage and prevent out-of-memory errors.
     * Uses modern ImageDecoder API for SDK 36 targeting.
     *
     * @param imageUri The local device location of the selected image.
     */
    private void uploadPosterToFirebase(Uri imageUri) {
        try {
            android.graphics.Bitmap bmp;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.graphics.ImageDecoder.Source source = android.graphics.ImageDecoder.createSource(getContentResolver(), imageUri);
                bmp = android.graphics.ImageDecoder.decodeBitmap(source);
            } else {
                bmp = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] data = baos.toByteArray();

            StorageReference posterRef = storage.getReference().child("posters/" + eventId + ".jpg");

            posterRef.putBytes(data).continueWithTask(task -> posterRef.getDownloadUrl()).addOnSuccessListener(downloadUri -> {
                db.collection("events").document(eventId).update("posterUrl", downloadUri.toString());
                Glide.with(this).load(downloadUri.toString()).into(imgEventPoster);
                hasPoster = true;
                currentPosterUrl = downloadUri.toString();
                updatePosterUI();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Swaps layout visibility states based on whether a poster is presently assigned.
     */
    private void updatePosterUI() {
        imgEventPoster.setVisibility(hasPoster ? View.VISIBLE : View.GONE);
        layoutPosterPlaceholder.setVisibility(hasPoster ? View.GONE : View.VISIBLE);
        fabRemovePoster.setVisibility(hasPoster ? View.VISIBLE : View.GONE);

        btnUploadPoster.setEnabled(!hasPoster);
        btnUpdatePoster.setEnabled(hasPoster);
    }

    /**
     * Detaches the poster URL from the event entity, deletes the physical file from
     * Firebase Storage to prevent memory leaks, and resets the placeholder interface.
     */
    private void removePoster() {
        if (hasPoster && !currentPosterUrl.isEmpty()) {
            try {
                // Delete the existing file from cloud storage
                StorageReference oldRef = storage.getReferenceFromUrl(currentPosterUrl);
                oldRef.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        db.collection("events").document(eventId).update("posterUrl", "");
        hasPoster = false;
        currentPosterUrl = "";
        updatePosterUI();
    }

    /**
     * Null-safe utility mechanism.
     *
     * @param value Potential null string.
     * @return Empty string or the original value.
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * Evaluative wrapper utilized while parsing the waitlist for potential co-organizers.
     */
    private static class EntrantCandidate {
        final Entrant entrant;
        final boolean hasPendingInvitation;

        EntrantCandidate(Entrant entrant, boolean hasPendingInvitation) {
            this.entrant = entrant;
            this.hasPendingInvitation = hasPendingInvitation;
        }
    }
}