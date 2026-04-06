package com.example.eventparticipation;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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

    // Stored for PDF Ticket generation
    private Event currentEvent;
    private String currentEntrantName = "Attendee";

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
    private MaterialButton btnDownloadTicket; // New PDF Button

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
        fetchEntrantName(); // Fetch user's name for the ticket
        loadEventFromIntent();
        checkWaitingListStatus();
    }

    /**
     * Fetches the current user's name from Firestore for the PDF Ticket.
     */
    private void fetchEntrantName() {
        db.collection("entrants").document(entrantId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("name")) {
                currentEntrantName = doc.getString("name");
            }
        });
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

        // Initialize PDF Ticket Button
        btnDownloadTicket = findViewById(R.id.btnDownloadTicket);
        btnDownloadTicket.setVisibility(View.GONE); // Hidden by default
        btnDownloadTicket.setOnClickListener(v -> generateAndSavePdfTicket());

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
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Event event = doc.toObject(Event.class);
                    if (event == null) return;

                    event.setId(doc.getId()); // Ensure ID is set
                    currentEvent = event;     // Store locally for PDF generation

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

    private void checkWaitingListStatus() {
        if (eventId == null) return;

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

    private void leaveWaitingList() {
        if (eventId == null) return;

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

    private void markInvitationNotificationsDeclined() {
        db.collection("entrants")
                .document(entrantId)
                .collection("notifications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("type", NotificationItem.TYPE_SELECTED)
                .whereEqualTo("actionStatus", NotificationItem.ACTION_PENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) return;

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

    private void updateButton() {
        if (eventId == null) {
            btnJoinLeave.setText("Join Waiting List");
            btnJoinLeave.setEnabled(true);
            btnJoinLeave.setBackgroundTintList(ColorStateList.valueOf(0xFF000000));
            if (btnDownloadTicket != null) btnDownloadTicket.setVisibility(View.GONE);
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

                            // Show the PDF Ticket download button!
                            if (btnDownloadTicket != null) btnDownloadTicket.setVisibility(View.VISIBLE);

                        } else if ("selected".equals(selectionStatus) && "pending".equals(responseStatus)) {
                            btnJoinLeave.setText("Respond to Invitation");
                            btnJoinLeave.setEnabled(true);
                            btnJoinLeave.setBackgroundTintList(ColorStateList.valueOf(0xFF4CAF50)); // Green
                            if (btnDownloadTicket != null) btnDownloadTicket.setVisibility(View.GONE);
                        } else {
                            btnJoinLeave.setText("Leave Waiting List");
                            btnJoinLeave.setEnabled(true);
                            btnJoinLeave.setBackgroundTintList(ColorStateList.valueOf(0xFFCC0000));
                            if (btnDownloadTicket != null) btnDownloadTicket.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnJoinLeave.setText("Leave Waiting List");
                        btnJoinLeave.setEnabled(true);
                        btnJoinLeave.setBackgroundTintList(ColorStateList.valueOf(0xFFCC0000));
                    });
        } else {
            // They are not on the waiting list (or declined, etc.)
            btnJoinLeave.setText("Join Waiting List");
            btnJoinLeave.setEnabled(true);
            btnJoinLeave.setBackgroundTintList(ColorStateList.valueOf(0xFF000000));
            if (btnDownloadTicket != null) btnDownloadTicket.setVisibility(View.GONE);
        }
    }

    /**
     * Generates a PDF ticket using the Android Canvas API and saves it to the Downloads folder.
     */
    private void generateAndSavePdfTicket() {
        if (currentEvent == null) {
            Toast.makeText(this, "Event data not fully loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Create the PdfDocument and PageInfo (A4 approx size)
        PdfDocument pdfDocument = new PdfDocument();
        int pageHeight = 842;
        int pageWidth = 595;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        // 2. Setup Canvas and Paint
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // Draw White Background
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, pageWidth, pageHeight, paint);

        // Draw a decorative border
        paint.setColor(Color.parseColor("#3F51B5")); // Primary Color
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        canvas.drawRect(20, 20, pageWidth - 20, pageHeight - 20, paint);

        // Draw Header Text
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTextSize(40f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("OFFICIAL EVENT TICKET", pageWidth / 2f, 100, paint);

        // Draw Event Details
        paint.setTextSize(24f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.LEFT);

        int startX = 60;
        int startY = 200;
        int lineSpacing = 40;

        String safeEventName = currentEvent.getName() != null ? currentEvent.getName() : "Unnamed Event";
        String safeEventId = currentEvent.getId() != null ? currentEvent.getId() : eventId;

        canvas.drawText("Event Name: " + safeEventName, startX, startY, paint);
        canvas.drawText("Attendee: " + currentEntrantName, startX, startY + lineSpacing, paint);
        canvas.drawText("Status: ADMIT ONE (Enrolled)", startX, startY + (lineSpacing * 2), paint);
        canvas.drawText("Ticket ID: " + safeEventId.substring(0, Math.min(8, safeEventId.length())).toUpperCase(), startX, startY + (lineSpacing * 3), paint);

        // 3. Draw the QR Code using your existing QRCodeGenerator class
        try {
            // Generate a QR code linking to the event
            String qrData = "https://eventparticipation.com/event?id=" + safeEventId;
            Bitmap qrBitmap = QRCodeGenerator.generateQRCode(qrData, 250);

            if (qrBitmap != null) {
                // Draw the Bitmap onto the PDF Canvas
                int qrX = (pageWidth - 250) / 2; // Center horizontally
                int qrY = startY + (lineSpacing * 5);
                canvas.drawBitmap(qrBitmap, qrX, qrY, null);

                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(16f);
                paint.setColor(Color.DKGRAY);
                canvas.drawText("Scan at the door for entry", pageWidth / 2f, qrY + 280, paint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 4. Finish the page
        pdfDocument.finishPage(page);

        // 5. Save the PDF to the device's Downloads folder
        String fileName = "Ticket_" + safeEventName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
        savePdfToDownloads(pdfDocument, fileName);
    }

    /**
     * Handles saving the file using Scoped Storage (MediaStore) for Android 10+
     * or standard File I/O for older versions.
     */
    private void savePdfToDownloads(PdfDocument pdfDocument, String fileName) {
        OutputStream fos = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Modern Android (API 29+): Use MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    fos = getContentResolver().openOutputStream(uri);
                }
            } else {
                // Older Android: Save directly to external storage Downloads folder
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) downloadsDir.mkdirs();

                File file = new File(downloadsDir, fileName);
                fos = new FileOutputStream(file);
            }

            if (fos != null) {
                pdfDocument.writeTo(fos);
                Toast.makeText(this, "Ticket saved to Downloads folder!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to create file.", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving ticket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            pdfDocument.close();
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}