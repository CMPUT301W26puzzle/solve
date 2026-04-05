package com.example.eventparticipation;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Event detail screen for entrants.
 *
 * Displays full event information and allows the entrant to join or leave
 * the waiting list. Also supports declining a pending invitation.
 *
 * Status model:
 * - selectionStatus: waiting / selected / cancelled
 * - responseStatus: pending / accepted / declined
 * - finalStatus: enrolled
 */
public class EntrantEventDetailActivity extends AppCompatActivity {

    private String eventId;
    private String entrantId;

    private ImageView ivEventPoster;
    private TextView tvEventName;
    private TextView tvEventPrice;
    private TextView tvTag1;
    private TextView tvTag2;
    private TextView tvTag3;
    private TextView tvEventDate;
    private TextView tvEventTime;
    private TextView tvVenueName;
    private TextView tvVenueAddress;
    private TextView tvCapacity;
    private TextView tvEnrolledWaiting;
    private TextView tvRegistrationDeadline;
    private TextView tvAbout;
    private TextView tvLotteryGuidelines;
    private MaterialButton btnJoinLeave;

    private FirebaseFirestore db;
    private boolean isOnWaitingList = false;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("h:mm a", Locale.getDefault());
    private final SimpleDateFormat deadlineFormat =
            new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_event_detail);

        db = FirebaseFirestore.getInstance();
        entrantId = DeviceIdProvider.getId(this);
        eventId = getIntent().getStringExtra("EVENT_ID");

        initViews();
        loadEventFromIntent();
        checkWaitingListStatus();
    }

    /**
     * Binds layout views and sets up back button and join/leave button.
     */
    private void initViews() {
        ivEventPoster = findViewById(R.id.ivEventPoster);
        tvEventName = findViewById(R.id.tvEventName);
        tvEventPrice = findViewById(R.id.tvEventPrice);
        tvTag1 = findViewById(R.id.tvTag1);
        tvTag2 = findViewById(R.id.tvTag2);
        tvTag3 = findViewById(R.id.tvTag3);
        tvEventDate = findViewById(R.id.tvEventDate);
        tvEventTime = findViewById(R.id.tvEventTime);
        tvVenueName = findViewById(R.id.tvVenueName);
        tvVenueAddress = findViewById(R.id.tvVenueAddress);
        tvCapacity = findViewById(R.id.tvCapacity);
        tvEnrolledWaiting = findViewById(R.id.tvEnrolledWaiting);
        tvRegistrationDeadline = findViewById(R.id.tvRegistrationDeadline);
        tvAbout = findViewById(R.id.tvAbout);
        tvLotteryGuidelines = findViewById(R.id.tvLotteryGuidelines);
        btnJoinLeave = findViewById(R.id.btnJoinLeave);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnJoinLeave.setOnClickListener(v -> {
            if ("Respond to Invitation".equals(btnJoinLeave.getText().toString())) {
                showResponseDialog();
            } else if (isOnWaitingList) {
                leaveWaitingList();
            } else {
                joinWaitingList();
            }
        });
    }

    /**
     * Displays an alert dialog to the entrant when they have been selected in the lottery.
     * * <p>This dialog prompts the user to either accept or decline the invitation to enroll
     * in the event. Accepting triggers {@link #acceptInvitation()}, while declining
     * triggers {@link #declineSelectedInvitation(DocumentReference, DocumentReference)}.</p>
     */
    private void showResponseDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("You Won the Lottery!")
                .setMessage("Do you want to accept this invitation and enroll in the event?")
                .setPositiveButton("Accept", (dialog, which) -> acceptInvitation())
                .setNegativeButton("Decline", (dialog, which) -> {
                    if (eventId != null) {
                        DocumentReference eventRef = db.collection("events").document(eventId);
                        DocumentReference waitRef = eventRef.collection("waitlist").document(entrantId);
                        declineSelectedInvitation(eventRef, waitRef);
                    }
                })
                .show();
    }

    /**
     * Processes the entrant's acceptance of an event invitation.
     * * <p>Updates the entrant's waitlist document in Firestore to mark their response status
     * as "accepted" and their final status as "enrolled". It also safely increments the event's
     * total enrolled count and decrements the selected count to ensure dashboard statistics
     * remain accurate.</p>
     */
    private void acceptInvitation() {
        if (eventId == null) return;
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitRef = eventRef.collection("waitlist").document(entrantId);

        waitRef.update(
                "responseStatus", "accepted",
                "finalStatus", "enrolled",
                "respondedAt", FieldValue.serverTimestamp()
        ).addOnSuccessListener(unused -> {
            // increment enrolled, decrement selected
            eventRef.update(
                    "enrolledCount", FieldValue.increment(1),
                    "selectedCount", FieldValue.increment(-1)
            );
            isOnWaitingList = true;
            updateButton();
            Toast.makeText(this, "Successfully enrolled!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to accept invitation", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Populates the UI using Event passed via Intent extras.
     * Falls back to Firestore fetch if no extras are present.
     */
    private void loadEventFromIntent() {
        tvEventName.setText(getIntent().getStringExtra("EVENT_NAME") != null
                ? getIntent().getStringExtra("EVENT_NAME")
                : "Event");

        tvEventPrice.setText("Free");
        tvEventDate.setText("See event details");
        tvEventTime.setText("");

        String venue = getIntent().getStringExtra("VENUE_ADDRESS");
        tvVenueName.setText(venue != null && !venue.isEmpty() ? venue : "Venue TBD");
        tvVenueAddress.setText("");

        int capacity = getIntent().getIntExtra("CAPACITY", 0);
        int enrolled = getIntent().getIntExtra("ENROLLED_COUNT", 0);
        int waiting = getIntent().getIntExtra("WAITING_COUNT", 0);

        tvCapacity.setText("Capacity: " + capacity);
        tvEnrolledWaiting.setText(enrolled + " enrolled • " + waiting + " waiting");

        tvRegistrationDeadline.setText("N/A");
        tvAbout.setText("N/A");

        if (eventId != null) {
            loadEventFromFirestore(eventId);
        }
    }

    /**
     * Loads full event data from Firestore.
     */
    private void loadEventFromFirestore(String eventId) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        return;
                    }

                    Event event = doc.toObject(Event.class);
                    if (event == null) {
                        return;
                    }

                    tvEventName.setText(event.getName());

                    if (event.getRegistrationStart() != null) {
                        tvEventDate.setText(dateFormat.format(event.getRegistrationStart()));
                        tvEventTime.setText(timeFormat.format(event.getRegistrationStart()));
                    }

                    if (event.getVenueAddress() != null) {
                        tvVenueName.setText(event.getVenueAddress());
                    }

                    Integer waitlistLimit = event.getWaitlistLimit();
                    tvCapacity.setText("Capacity: " +
                            (waitlistLimit == null ? "Unlimited" : waitlistLimit));

                    tvEnrolledWaiting.setText(event.getEnrolledCount() + " enrolled • "
                            + event.getWaitingCount() + " waiting");

                    if (event.getRegistrationEnd() != null) {
                        tvRegistrationDeadline.setText(
                                deadlineFormat.format(event.getRegistrationEnd())
                        );
                    }

                    String guidelines = doc.getString("lotteryGuidelines");
                    if (guidelines != null && !guidelines.isEmpty()) {
                        tvLotteryGuidelines.setText("Lottery Guidelines:\n" + guidelines);
                        tvLotteryGuidelines.setVisibility(View.VISIBLE);
                    }

                    if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                        Glide.with(this)
                                .load(event.getPosterUrl())
                                .centerCrop()
                                .into(ivEventPoster);
                    }
                });
    }

    /**
     * Checks Firestore to see whether this entrant currently has an active waitlist entry.
     */
    private void checkWaitingListStatus() {
        if (eventId == null) {
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .document(entrantId)
                .get()
                .addOnSuccessListener(doc -> {
                    String selectionStatus = doc.getString("selectionStatus");
                    String finalStatus = doc.getString("finalStatus");

                    isOnWaitingList = doc.exists()
                            && !"cancelled".equals(selectionStatus)
                            && !"enrolled".equals(finalStatus);

                    updateButton();
                })
                .addOnFailureListener(e -> updateButton());
    }

    /**
     * Adds this entrant to the event waitlist, enforcing capacity limits and role restrictions.
     * * <p>Before joining, this method queries the main event document to ensure:
     * <ul>
     * <li>The current user is not assigned as a co-organizer for the event.</li>
     * <li>The waitlist has not exceeded its optional maximum limit (if one is set).</li>
     * </ul>
     * Upon passing these checks, a new waitlist entry is created with a "waiting" selection status.</p>
     */
    private void joinWaitingList() {
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);

        eventRef.get().addOnSuccessListener(eventDoc -> {
            List<String> coOrganizerIds = (List<String>) eventDoc.get("coOrganizerIds");

            if (coOrganizerIds != null && coOrganizerIds.contains(entrantId)) {
                Toast.makeText(this, "Co-organizers cannot join the entrant pool.", Toast.LENGTH_LONG).show();
                return;
            }

            // enforce waitlist limit
            Long limit = eventDoc.getLong("waitlistLimit");
            Long currentWaiting = eventDoc.getLong("waitingCount");

            if (limit != null && currentWaiting != null && currentWaiting >= limit) {
                Toast.makeText(this, "The waiting list is currently full.", Toast.LENGTH_LONG).show();
                return;
            }

            Map<String, Object> waitlistEntry = new HashMap<>();
            waitlistEntry.put("deviceId", entrantId);
            waitlistEntry.put("entrantId", entrantId);
            waitlistEntry.put("joinedAt", new Date());

            waitlistEntry.put("selectionStatus", "waiting");
            waitlistEntry.put("responseStatus", null);
            waitlistEntry.put("finalStatus", null);

            eventRef.collection("waitlist")
                    .document(entrantId)
                    .set(waitlistEntry)
                    .addOnSuccessListener(unused -> {
                        eventRef.update("waitingCount", FieldValue.increment(1));
                        isOnWaitingList = true;
                        updateButton();
                        Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to join waiting list", Toast.LENGTH_SHORT).show()
                    );
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to check event role", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Removes this entrant from the waitlist, or declines a pending invitation if selected.
     */
    private void leaveWaitingList() {
        if (eventId == null) {
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitRef = eventRef.collection("waitlist").document(entrantId);

        waitRef.get().addOnSuccessListener(doc -> {
            String selectionStatus = doc.getString("selectionStatus");

            if ("selected".equals(selectionStatus)) {
                declineSelectedInvitation(eventRef, waitRef);
            } else {
                waitRef.delete().addOnSuccessListener(unused -> {
                    eventRef.update("waitingCount", FieldValue.increment(-1));
                    isOnWaitingList = false;
                    updateButton();
                    Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to leave waiting list", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    /**
     * Declines a selected invitation.
     *
     * Manual replacement flow:
     * - mark this entrant declined
     * - organizer later decides whether to draw a replacement
     */
    private void declineSelectedInvitation(DocumentReference eventRef, DocumentReference waitRef) {
        waitRef.update(
                        "selectionStatus", "selected",
                        "responseStatus", "declined",
                        "finalStatus", null,
                        "respondedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {
                    eventRef.update("selectedCount", FieldValue.increment(-1));
                    markInvitationNotificationsDeclined();
                    isOnWaitingList = false;
                    updateButton();
                    Toast.makeText(this, "Invitation declined", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to decline invitation", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Marks pending selected notifications as declined.
     */
    private void markInvitationNotificationsDeclined() {
        db.collection("entrants")
                .document(entrantId)
                .collection("notifications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("type", NotificationItem.TYPE_SELECTED)
                .whereEqualTo("actionStatus", NotificationItem.ACTION_PENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (DocumentSnapshot notificationDoc : querySnapshot.getDocuments()) {
                        batch.update(notificationDoc.getReference(),
                                "unread", false,
                                "actionRequired", false,
                                "actionStatus", NotificationItem.ACTION_DECLINED,
                                "respondedAt", FieldValue.serverTimestamp());
                    }
                    batch.commit();
                });
    }

    /**
     * Updates the button label and color based on entrant status for this event.
     */
    private void updateButton() {
        if (eventId == null) {
            btnJoinLeave.setText("Join Waiting List");
            btnJoinLeave.setEnabled(true);
            btnJoinLeave.setBackgroundTintList(ColorStateList.valueOf(0xFF000000));
            return;
        }

        if (isOnWaitingList) {
            db.collection("events")
                    .document(eventId)
                    .collection("waitlist")
                    .document(entrantId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String selectionStatus = doc.getString("selectionStatus");
                        String responseStatus = doc.getString("responseStatus");
                        String finalStatus = doc.getString("finalStatus");

                        if ("enrolled".equals(finalStatus)) {
                            btnJoinLeave.setText("Enrolled");
                            btnJoinLeave.setEnabled(false);
                            btnJoinLeave.setBackgroundTintList(ColorStateList.valueOf(0xFF6B7280));
                        } else if ("selected".equals(selectionStatus) && "pending".equals(responseStatus)) {
                            btnJoinLeave.setText("Respond to Invitation");
                            btnJoinLeave.setEnabled(true);
                            btnJoinLeave.setBackgroundTintList(ColorStateList.valueOf(0xFF4CAF50)); // make it green
                        } else {
                            btnJoinLeave.setText("Leave Waiting List");
                            btnJoinLeave.setEnabled(true);
                            btnJoinLeave.setBackgroundTintList(ColorStateList.valueOf(0xFFCC0000));
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnJoinLeave.setText("Leave Waiting List");
                        btnJoinLeave.setEnabled(true);
                        btnJoinLeave.setBackgroundTintList(ColorStateList.valueOf(0xFFCC0000));
                    });
        } else {
            btnJoinLeave.setText("Join Waiting List");
            btnJoinLeave.setEnabled(true);
            btnJoinLeave.setBackgroundTintList(ColorStateList.valueOf(0xFF000000));
        }
    }
}