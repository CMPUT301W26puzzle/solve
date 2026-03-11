package com.example.eventparticipation;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    /** Organizer ID that owns this event. */
    private String organizerId;

    /** Temporary hardcoded device/user ID until auth is implemented. */
    private static final String DEVICE_ID = "device_demo_001";

    private ImageView ivEventPoster;
    private TextView tvEventName, tvEventPrice;
    private TextView tvTag1, tvTag2, tvTag3;
    private TextView tvEventDate, tvEventTime;
    private TextView tvVenueName, tvVenueAddress;
    private TextView tvCapacity, tvEnrolledWaiting;
    private TextView tvRegistrationDeadline;
    private TextView tvAbout;
    private MaterialButton btnJoinLeave;

    private FirebaseFirestore db;
    private boolean isOnWaitingList = false;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private SimpleDateFormat deadlineFormat = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_event_detail);

        db = FirebaseFirestore.getInstance();

        eventId    = getIntent().getStringExtra("EVENT_ID");
        organizerId = getIntent().getStringExtra("ORGANIZER_ID");

        initViews();
        loadEventFromIntent();
        checkWaitingListStatus();
    }

    /**
     * Binds layout views and sets up back button and join/leave button.
     */
    private void initViews() {
        ivEventPoster        = findViewById(R.id.ivEventPoster);
        tvEventName          = findViewById(R.id.tvEventName);
        tvEventPrice         = findViewById(R.id.tvEventPrice);
        tvTag1               = findViewById(R.id.tvTag1);
        tvTag2               = findViewById(R.id.tvTag2);
        tvTag3               = findViewById(R.id.tvTag3);
        tvEventDate          = findViewById(R.id.tvEventDate);
        tvEventTime          = findViewById(R.id.tvEventTime);
        tvVenueName          = findViewById(R.id.tvVenueName);
        tvVenueAddress       = findViewById(R.id.tvVenueAddress);
        tvCapacity           = findViewById(R.id.tvCapacity);
        tvEnrolledWaiting    = findViewById(R.id.tvEnrolledWaiting);
        tvRegistrationDeadline = findViewById(R.id.tvRegistrationDeadline);
        tvAbout              = findViewById(R.id.tvAbout);
        btnJoinLeave         = findViewById(R.id.btnJoinLeave);

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
        // For now populate with the hardcoded test data passed via intent
        // When Firestore is wired, replace this with a db.get() call
        tvEventName.setText(getIntent().getStringExtra("EVENT_NAME") != null
                ? getIntent().getStringExtra("EVENT_NAME") : "Event");

        // Price badge — hardcoded Free for now
        tvEventPrice.setText("Free");

        // Date / time
        tvEventDate.setText("See event details");
        tvEventTime.setText("");

        // Venue
        String venue = getIntent().getStringExtra("VENUE_ADDRESS");
        tvVenueName.setText(venue != null && !venue.isEmpty() ? venue : "Venue TBD");
        tvVenueAddress.setText("");

        // Capacity / counts
        int capacity = getIntent().getIntExtra("CAPACITY", 0);
        int enrolled = getIntent().getIntExtra("ENROLLED_COUNT", 0);
        int waiting  = getIntent().getIntExtra("WAITING_COUNT", 0);
        tvCapacity.setText("Capacity: " + capacity);
        tvEnrolledWaiting.setText(enrolled + " enrolled • " + waiting + " waiting");

        // Deadline
        tvRegistrationDeadline.setText("N/A");

        // About
        tvAbout.setText("N/A");

        if (eventId != null) {
            loadEventFromFirestore(eventId, organizerId);
        }
    }

    /**
     * Loads full event data from Firestore using the event and organizer IDs.
     *
     * @param eventId     Firestore event document ID
     * @param organizerId organizer document ID
     */
    private void loadEventFromFirestore(String eventId, String organizerId) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    Event event = doc.toObject(Event.class);
                    if (event == null) return;

                    tvEventName.setText(event.getName());

                    if (event.getStartTime() != null) {
                        tvEventDate.setText(dateFormat.format(event.getStartTime()));
                        tvEventTime.setText(timeFormat.format(event.getStartTime()));
                    }

                    if (event.getVenueAddress() != null) {
                        tvVenueName.setText(event.getVenueAddress());
                    }

                    tvCapacity.setText("Capacity: " + event.getCapacity());
                    tvEnrolledWaiting.setText(event.getEnrolledCount() + " enrolled • "
                            + event.getWaitingCount() + " waiting");

                    if (event.getRegistrationEnd() != null) {
                        tvRegistrationDeadline.setText(deadlineFormat.format(event.getRegistrationEnd()));
                    }

                    if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                        Glide.with(this).load(event.getPosterUrl()).centerCrop().into(ivEventPoster);
                    }
                });
    }

    /**
     * Checks Firestore to see if this device is already on the waiting list,
     * then updates the button label accordingly.
     */
    private void checkWaitingListStatus() {
        if (eventId == null) return;

        db.collection("events").document(eventId)
                .collection("waitingList").document(DEVICE_ID)
                .get()
                .addOnSuccessListener(doc -> {
                    isOnWaitingList = doc.exists();
                    updateButton();
                })
                .addOnFailureListener(e -> updateButton());
    }

    /**
     * Adds this device to the event's waiting list in Firestore (US 01.01.01).
     */
    private void joinWaitingList() {
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);

        // Add device to waitingList subcollection
        eventRef.collection("waitingList").document(DEVICE_ID)
                .set(new java.util.HashMap<String, Object>() {{
                    put("deviceId", DEVICE_ID);
                    put("joinedAt", new Date());
                    put("status", "waiting");
                }})
                .addOnSuccessListener(unused -> {
                    // Increment waiting count atomically
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
     * Removes this device from the event's waiting list in Firestore (US 01.01.02).
     */
    private void leaveWaitingList() {
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);

        eventRef.collection("waitingList").document(DEVICE_ID)
                .delete()
                .addOnSuccessListener(unused -> {
                    // Decrement waiting count atomically
                    eventRef.update("waitingCount", FieldValue.increment(-1));
                    isOnWaitingList = false;
                    updateButton();
                    Toast.makeText(this, "Left waiting list", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Failed to leave waiting list", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Updates the join/leave button label and color based on current waiting list status.
     */
    private void updateButton() {
        if (isOnWaitingList) {
            btnJoinLeave.setText("Leave Waiting List");
            btnJoinLeave.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFCC0000));
        } else {
            btnJoinLeave.setText("Join Waiting List");
            btnJoinLeave.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF000000));
        }
    }
}
