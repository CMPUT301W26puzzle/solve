package com.example.eventparticipation;

import android.os.Bundle;
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
import java.util.Locale;
import java.util.Map;

/**
 * Event detail screen for entrants.
 *
 * <p>Displays full event information and allows the entrant to join or leave
 * the waiting list. The button state toggles based on whether the device ID
 * is already present in the event's waiting list.</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 *     <li>US 01.01.01 - Join the waiting list</li>
 *     <li>US 01.01.02 - Leave the waiting list</li>
 *     <li>US 01.05.04 - Show waiting list count</li>
 * </ul>
 */
public class EntrantEventDetailActivity extends AppCompatActivity {

    /** Firestore document ID of the event being viewed. */
    private String eventId;

    /** Current entrant id used for waitlist and notification actions. */
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
        btnJoinLeave = findViewById(R.id.btnJoinLeave);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnJoinLeave.setOnClickListener(v -> {
            if (isOnWaitingList) {
                leaveWaitingList();
            } else {
                joinWaitingList();
            }
        });
    }

    /**
     * Populates the UI using the Event passed via Intent extras.
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
     *
     * @param eventId Firestore event document ID
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

                    if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                        Glide.with(this)
                                .load(event.getPosterUrl())
                                .centerCrop()
                                .into(ivEventPoster);
                    }
                });
    }

    /**
     * Checks Firestore to see if this device is already on the waiting list,
     * then updates the button label accordingly.
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
                    String status = doc.getString("status");
                    isOnWaitingList = doc.exists()
                            && !"declined".equals(status)
                            && !"not_selected".equals(status);
                    updateButton();
                })
                .addOnFailureListener(e -> updateButton());
    }

    /**
     * Adds this device to the event's waiting list in Firestore.
     */
    private void joinWaitingList() {
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);

        Map<String, Object> waitlistEntry = new HashMap<>();
        waitlistEntry.put("deviceId", entrantId);
        waitlistEntry.put("entrantId", entrantId);
        waitlistEntry.put("joinedAt", new Date());
        waitlistEntry.put("status", "waiting");

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
    }

    /**
     * Removes this device from the event's waiting list in Firestore.
     */
    private void leaveWaitingList() {
        if (eventId == null) {
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitRef = eventRef.collection("waitlist").document(entrantId);

        waitRef.get().addOnSuccessListener(doc -> {
            String status = doc.getString("status");

            if ("selected".equals(status)) {
                declineSelectedInvitation(eventRef, waitRef);
            } else {
                waitRef.delete().addOnSuccessListener(unused -> {
                    eventRef.update("waitingCount", FieldValue.increment(-1));
                    isOnWaitingList = false;
                    updateButton();
                    Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Declines a selected invitation and triggers a replacement draw.
     */
    private void declineSelectedInvitation(DocumentReference eventRef, DocumentReference waitRef) {
        waitRef.update(
                        "status", "declined",
                        "respondedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {
                    eventRef.update("selectedCount", FieldValue.increment(-1));
                    markInvitationNotificationsDeclined();
                    triggerRedraw();
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
     * Updates the join/leave button label and color based on current waiting list status.
     */
    private void updateButton() {
        if (isOnWaitingList) {
            db.collection("events")
                    .document(eventId)
                    .collection("waitlist")
                    .document(entrantId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String status = doc.getString("status");
                        if ("selected".equals(status)) {
                            btnJoinLeave.setText("Decline Invitation");
                            btnJoinLeave.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(0xFFCC0000)
                            );
                        } else {
                            btnJoinLeave.setText("Leave Waiting List");
                            btnJoinLeave.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(0xFFCC0000)
                            );
                        }
                    });
        } else {
            btnJoinLeave.setText("Join Waiting List");
            btnJoinLeave.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF000000)
            );
        }
    }

    /**
     * Picks a random waiting entrant and promotes them to selected.
     */
    private void triggerRedraw() {
        new WaitlistController()
                .drawReplacement(eventId)
                .addOnSuccessListener(replacementId -> {
                    if (replacementId != null) {
                        Toast.makeText(
                                this,
                                "A new entrant has been selected",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Failed to draw replacement",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }
}
