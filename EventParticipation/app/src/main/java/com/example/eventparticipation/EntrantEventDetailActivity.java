package com.example.eventparticipation;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
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
 *
 * <p>Relevant user stories:</p>
 * <ul>
 *     <li>US 01.01.01 - Join waiting list</li>
 *     <li>US 01.01.02 - Leave waiting list</li>
 *     <li>US 01.05.04 - View enrollment and waiting counts</li>
 *     <li>US 01.08.01 - Post a comment on an event</li>
 *     <li>US 01.08.02 - View comments on an event</li>
 * </ul>
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
    private TextView tvCommentsEmpty;
    private MaterialButton btnJoinLeave;
    private ImageButton btnPostComment;
    private TextInputEditText etComment;
    private RecyclerView rvComments;

    private FirebaseFirestore db;
    private boolean isOnWaitingList = false;
    private String currentEntrantName = "Anonymous entrant";
    private final List<EventComment> comments = new ArrayList<>();
    private EventCommentAdapter commentAdapter;

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
        setupCommentsRecyclerView();
        loadEventFromIntent();
        loadEntrantProfile();
        loadComments();
        checkWaitingListStatus();
    }

    /**
     * Binds layout views and sets up click listeners.
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
        tvCommentsEmpty = findViewById(R.id.tvCommentsEmpty);
        btnJoinLeave = findViewById(R.id.btnJoinLeave);
        btnPostComment = findViewById(R.id.btnPostComment);
        etComment = findViewById(R.id.etComment);
        rvComments = findViewById(R.id.rvComments);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnJoinLeave.setOnClickListener(v -> {
            if (isOnWaitingList) {
                leaveWaitingList();
            } else {
                joinWaitingList();
            }
        });

        btnPostComment.setOnClickListener(v -> postComment());
    }

    /**
     * Configures the comments RecyclerView.
     */
    private void setupCommentsRecyclerView() {
        commentAdapter = new EventCommentAdapter(comments, this::handleCommentLongPress);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
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

                    if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                        Glide.with(this)
                                .load(event.getPosterUrl())
                                .centerCrop()
                                .into(ivEventPoster);
                    }
                });
    }

    /**
     * Loads the current entrant profile name for comment posting.
     */
    private void loadEntrantProfile() {
        db.collection("entrants")
                .document(entrantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String entrantName = documentSnapshot.getString("name");
                    if (entrantName != null && !entrantName.trim().isEmpty()) {
                        currentEntrantName = entrantName.trim();
                    }
                });
    }

    /**
     * Loads comments for the current event.
     */
    private void loadComments() {
        if (eventId == null) {
            tvCommentsEmpty.setVisibility(TextView.VISIBLE);
            rvComments.setVisibility(RecyclerView.GONE);
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    comments.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        EventComment comment = doc.toObject(EventComment.class);
                        if (comment == null) {
                            continue;
                        }

                        comment.setCommentId(doc.getId());
                        comments.add(comment);
                    }

                    commentAdapter.notifyDataSetChanged();
                    updateCommentsEmptyState();
                })
                .addOnFailureListener(e -> {
                    updateCommentsEmptyState();
                    Toast.makeText(this, "Failed to load comments", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Posts a new comment for the current event.
     */
    private void postComment() {
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String commentText = EventCommentLogic.normalizeCommentText(
                etComment.getText() != null ? etComment.getText().toString() : null
        );

        if (!EventCommentLogic.isCommentTextValid(commentText)) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference commentRef = db.collection("events")
                .document(eventId)
                .collection("comments")
                .document();

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("commentId", commentRef.getId());
        commentData.put("entrantId", entrantId);
        commentData.put("entrantName", EventCommentLogic.resolveAuthorName(currentEntrantName));
        commentData.put("text", commentText);
        commentData.put("createdAt", FieldValue.serverTimestamp());

        commentRef.set(commentData)
                .addOnSuccessListener(unused -> {
                    etComment.setText("");
                    loadComments();
                    Toast.makeText(this, "Comment posted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Handles long press actions on a comment.
     *
     * @param comment pressed comment
     */
    private void handleCommentLongPress(EventComment comment) {
        if (!EventCommentLogic.canDeleteComment(entrantId, comment)) {
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete comment?")
                .setMessage("This will permanently remove your comment.")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> deleteComment(comment))
                .show();
    }

    /**
     * Deletes a comment owned by the current entrant.
     *
     * @param comment comment to delete
     */
    private void deleteComment(EventComment comment) {
        if (eventId == null || comment == null || comment.getCommentId() == null) {
            return;
        }

        db.collection("events")
                .document(eventId)
                .collection("comments")
                .document(comment.getCommentId())
                .delete()
                .addOnSuccessListener(unused -> {
                    loadComments();
                    Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete comment", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Updates visibility for the comments empty state.
     */
    private void updateCommentsEmptyState() {
        if (comments.isEmpty()) {
            tvCommentsEmpty.setVisibility(TextView.VISIBLE);
            rvComments.setVisibility(RecyclerView.GONE);
        } else {
            tvCommentsEmpty.setVisibility(TextView.GONE);
            rvComments.setVisibility(RecyclerView.VISIBLE);
        }
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
     * Adds this entrant to the event waitlist unless they are a co-organizer.
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
                Toast.makeText(
                        this,
                        "Co-organizers cannot join the entrant pool for this event",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            Map<String, Object> waitlistEntry = new HashMap<>();
            waitlistEntry.put("deviceId", entrantId);
            waitlistEntry.put("entrantId", entrantId);
            waitlistEntry.put("joinedAt", new Date());

            // New status model
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
                        } else if ("selected".equals(selectionStatus)
                                && "pending".equals(responseStatus)) {
                            btnJoinLeave.setText("Decline Invitation");
                            btnJoinLeave.setEnabled(true);
                            btnJoinLeave.setBackgroundTintList(ColorStateList.valueOf(0xFFCC0000));
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
