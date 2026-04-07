package com.example.eventparticipation.user;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.eventparticipation.universal.Event;
import com.example.eventparticipation.R;
import com.example.eventparticipation.universal.SelectRoleActivity;
import com.example.eventparticipation.universal.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Detailed view of an event for entrants.
 *
 * <p><b>Purpose & Role:</b> Serves as the primary information hub for an entrant.
 * It allows users to view event specifics (venue, deadline, capacity), join the
 * waiting list, and access the unified discussion board for comments.</p>
 *
 * <p>Implemented user stories:</p>
 * <ul>
 * <li>US 01.01.01 As an entrant I want to join the waiting list for a specific event.</li>
 * <li>US 01.08.02 As an entrant, I want to view comments on an event (via navigation).</li>
 * </ul>
 */
public class EntrantEventDetailActivity extends AppCompatActivity {

    private String eventId;
    private String entrantId;

    // Local cache for PDF Ticket and metadata
    private Event currentEvent;
    private String currentEntrantName = "Attendee";

    private ImageView ivEventPoster;
    private TextView tvEventName, tvEventPrice, tvTag1, tvTag2, tvTag3;
    private TextView tvEventDate, tvEventTime, tvVenueName, tvVenueAddress;
    private TextView tvCapacity, tvEnrolledWaiting, tvRegistrationDeadline, tvAbout, tvLotteryGuidelines;
    private MaterialButton btnJoinLeave, btnDownloadTicket, btnViewComments;

    private FirebaseFirestore db;
    private boolean isOnWaitingList = false;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private final SimpleDateFormat deadlineFormat = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_event_detail);

        db = FirebaseFirestore.getInstance();
        SessionManager session = SessionManager.getInstance(this);
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SelectRoleActivity.class));
            finish();
            return;
        }
        entrantId = session.getUserId();
        eventId = getIntent().getStringExtra("EVENT_ID");

        initViews();
        fetchEntrantName();
        loadEventFromIntent();
        checkWaitingListStatus();
    }

    /**
     * Fetches the entrant's display name from Firestore.
     * Used for personalizing the PDF ticket.
     */
    private void fetchEntrantName() {
        db.collection("entrants").document(entrantId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("name")) {
                currentEntrantName = doc.getString("name");
            }
        });
    }

    /**
     * Initializes UI components and binds click listeners.
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

        btnDownloadTicket = findViewById(R.id.btnDownloadTicket);
        btnDownloadTicket.setVisibility(View.GONE);
        btnDownloadTicket.setOnClickListener(v -> generateAndSavePdfTicket());

        btnViewComments = findViewById(R.id.btnViewComments);
        btnViewComments.setOnClickListener(v -> navigateToComments());

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
     * Navigates to the shared discussion board with entrant-level permissions.
     */
    private void navigateToComments() {
        Intent intent = new Intent(this, EventCommentsActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("IS_ORGANIZER", false);
        intent.putExtra("IS_ADMIN", false);
        startActivity(intent);
    }

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

    private void acceptInvitation() {
        if (eventId == null) return;
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitRef = eventRef.collection("waitlist").document(entrantId);

        waitRef.update(
                "responseStatus", "accepted",
                "finalStatus", "enrolled",
                "respondedAt", FieldValue.serverTimestamp()
        ).addOnSuccessListener(unused -> {
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

    private void loadEventFromFirestore(String eventId) {
        db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            Event event = doc.toObject(Event.class);
            if (event == null) return;

            event.setId(doc.getId());
            currentEvent = event;

            tvEventName.setText(event.getName());
            if (event.getRegistrationStart() != null) {
                tvEventDate.setText(dateFormat.format(event.getRegistrationStart()));
                tvEventTime.setText(timeFormat.format(event.getRegistrationStart()));
            }
            if (event.getVenueAddress() != null) {
                tvVenueName.setText(event.getVenueAddress());
            }

            Integer waitlistLimit = event.getWaitlistLimit();
            tvCapacity.setText("Capacity: " + (waitlistLimit == null ? "Unlimited" : waitlistLimit));
            tvEnrolledWaiting.setText(event.getEnrolledCount() + " enrolled • " + event.getWaitingCount() + " waiting");

            if (event.getRegistrationEnd() != null) {
                tvRegistrationDeadline.setText(deadlineFormat.format(event.getRegistrationEnd()));
            }

            String guidelines = doc.getString("lotteryGuidelines");
            if (guidelines != null && !guidelines.isEmpty()) {
                tvLotteryGuidelines.setText("Lottery Guidelines:\n" + guidelines);
                tvLotteryGuidelines.setVisibility(View.VISIBLE);
            }

            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                Glide.with(this).load(event.getPosterUrl()).centerCrop().into(ivEventPoster);
            }
        });
    }

    private void checkWaitingListStatus() {
        if (eventId == null) return;
        db.collection("events").document(eventId).collection("waitlist").document(entrantId).get()
                .addOnSuccessListener(doc -> {
                    String selectionStatus = doc.getString("selectionStatus");
                    String finalStatus = doc.getString("finalStatus");
                    isOnWaitingList = doc.exists() && !"cancelled".equals(selectionStatus) && !"enrolled".equals(finalStatus);
                    updateButton();
                }).addOnFailureListener(e -> updateButton());
    }

    private void joinWaitingList() {
        if (eventId == null) return;
        DocumentReference eventRef = db.collection("events").document(eventId);

        eventRef.get().addOnSuccessListener(eventDoc -> {
            List<String> coOrganizerIds = (List<String>) eventDoc.get("coOrganizerIds");
            if (coOrganizerIds != null && coOrganizerIds.contains(entrantId)) {
                Toast.makeText(this, "Co-organizers cannot join the pool.", Toast.LENGTH_LONG).show();
                return;
            }

            Long limit = eventDoc.getLong("waitlistLimit");
            Long currentWaiting = eventDoc.getLong("waitingCount");
            if (limit != null && currentWaiting != null && currentWaiting >= limit) {
                Toast.makeText(this, "The waiting list is full.", Toast.LENGTH_LONG).show();
                return;
            }

            Map<String, Object> entry = new HashMap<>();
            entry.put("deviceId", entrantId);
            entry.put("entrantId", entrantId);
            entry.put("joinedAt", new Date());
            entry.put("selectionStatus", "waiting");

            eventRef.collection("waitlist").document(entrantId).set(entry).addOnSuccessListener(unused -> {
                eventRef.update("waitingCount", FieldValue.increment(1));
                isOnWaitingList = true;
                updateButton();
                Toast.makeText(this, "Joined!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void leaveWaitingList() {
        if (eventId == null) return;
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitRef = eventRef.collection("waitlist").document(entrantId);

        waitRef.get().addOnSuccessListener(doc -> {
            String status = doc.getString("selectionStatus");
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

    private void declineSelectedInvitation(DocumentReference eventRef, DocumentReference waitRef) {
        waitRef.update("selectionStatus", "selected", "responseStatus", "declined", "respondedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(unused -> {
                    eventRef.update("selectedCount", FieldValue.increment(-1));
                    isOnWaitingList = false;
                    updateButton();
                    Toast.makeText(this, "Invitation declined", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateButton() {
        if (eventId == null) {
            btnJoinLeave.setText("Join Waiting List");
            return;
        }

        if (isOnWaitingList) {
            db.collection("events").document(eventId).collection("waitlist").document(entrantId).get()
                    .addOnSuccessListener(doc -> {
                        String finalStatus = doc.getString("finalStatus");
                        String responseStatus = doc.getString("responseStatus");
                        String selectionStatus = doc.getString("selectionStatus");

                        if ("enrolled".equals(finalStatus)) {
                            btnJoinLeave.setText("Enrolled");
                            btnJoinLeave.setEnabled(false);
                            btnDownloadTicket.setVisibility(View.VISIBLE);
                        } else if ("selected".equals(selectionStatus) && "pending".equals(responseStatus)) {
                            btnJoinLeave.setText("Respond to Invitation");
                        } else {
                            btnJoinLeave.setText("Leave Waiting List");
                        }
                    });
        } else {
            btnJoinLeave.setText("Join Waiting List");
            btnDownloadTicket.setVisibility(View.GONE);
        }
    }

    /**
     * Standard PDF generation logic using Android Graphics API.
     */
    private void generateAndSavePdfTicket() {
        if (currentEvent == null) return;
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(30f);
        canvas.drawText("EVENT TICKET", 200, 100, paint);
        paint.setTextSize(18f);
        canvas.drawText("Event: " + currentEvent.getName(), 60, 200, paint);
        canvas.drawText("Attendee: " + currentEntrantName, 60, 240, paint);

        pdfDocument.finishPage(page);
        String name = "Ticket_" + currentEvent.getName().replaceAll("\\s+", "_") + ".pdf";
        savePdfToDownloads(pdfDocument, name);
    }

    private void savePdfToDownloads(PdfDocument pdfDocument, String fileName) {
        OutputStream fos = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) fos = getContentResolver().openOutputStream(uri);
            } else {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                fos = new FileOutputStream(file);
            }
            if (fos != null) {
                pdfDocument.writeTo(fos);
                Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pdfDocument.close();
        }
    }
}