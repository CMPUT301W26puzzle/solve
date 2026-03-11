package com.example.eventparticipation;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
* Controller/Activity screen for handling the creation of a new event.
*
* <p>Provides an interface for organizers to define event parameters, including
* lottery settings, registration windows, and geolocation requirements. It generates
* a unique promotional QR code linked to the event and manages storage for posters.</p>

* <p>Relevant user stories:</p>
* <ul>
* <li>US 02.01.01 Create a new event and generate a unique promotional QR code</li>
* <li>US 02.01.04 Set a registration period</li>
* <li>US 02.03.01 Optionally limit waitlist capacity</li>
* <li>US 02.04.01 Upload an event poster</li>
* </ul>
*/
public class CreateEventActivity extends AppCompatActivity {
    /** Input field for the event's display name. */
    private EditText etName;

    /** Input field for the maximum number of entrants allowed on the waitlist. */
    private EditText etWaitlistLimit;

    /** Switch to toggle the requirement for entrants to provide geolocation data. */
    private SwitchCompat swGeo;

    /** View to preview the selected event poster image. */
    private ImageView imgPoster;

    /** Button to trigger the Material Design date range picker. */
    private MaterialButton btnDates;

    /** Button to validate input and commit the event to the cloud. */
    private MaterialButton btnSave;

    /** Hardcoded organizer ID for demo purposes. */
    private final String organizerId = "organizer_demo_001";

    /** Local URI of the poster image selected by the user. */
    private Uri selectedImageUri;

    /** The start date of the registration period. */
    private Date regStart;

    /** The end date of the registration period. */
    private Date regEnd;

    /** Entry point for Firestore database operations. */
    private FirebaseFirestore db;

    /** Entry point for Firebase Storage operations. */
    private FirebaseStorage storage;

    /** * Launcher for the GET_CONTENT intent to pick images from the device gallery.
     */
    private final ActivityResultLauncher<String> picker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgPoster.setImageURI(uri);
                }
            });

    /**
     * Standard onCreate method to initialize view references, Firebase instances,
     * and UI listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();
        setupListeners();
    }

    /**
     * Binds view components from the XML layout to class-level variables.
     */
    private void initViews() {
        etName = findViewById(R.id.etEventName);
        etWaitlistLimit = findViewById(R.id.etWaitlistLimit);
        swGeo = findViewById(R.id.switchGeolocation);
        imgPoster = findViewById(R.id.imgEventPoster);
        btnDates = findViewById(R.id.btnDateRange);
        btnSave = findViewById(R.id.btnSaveEvent);
    }

    /**
     * Configures interaction logic for buttons and interactive views.
     */
    private void setupListeners() {
        imgPoster.setOnClickListener(v -> picker.launch("image/*"));

        btnDates.setOnClickListener(v -> {
            MaterialDatePicker<androidx.core.util.Pair<Long, Long>> datePicker =
                    MaterialDatePicker.Builder.dateRangePicker()
                            .setTitleText(R.string.select_registration_period)
                            .build();
            datePicker.show(getSupportFragmentManager(), "RANGE");
            datePicker.addOnPositiveButtonClickListener(selection -> {
                regStart = new Date(selection.first);
                regEnd = new Date(selection.second);
                btnDates.setText(R.string.period_set);
            });
        });

        btnSave.setOnClickListener(v -> startSave());
    }

    /**
     * Validates required inputs and initiates the save process.
     */
    private void startSave() {
        if (etName.getText().toString().isEmpty()) {
            Toast.makeText(this, "Event name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            uploadAndSave();
        } else {
            // TODO: unhardcode these
            int capacity = 0;
            String address = "";
            Double lat = 53.5232;
            Double lng = -113.5263;

            saveToFirestore("", organizerId, capacity, address, lat, lng);
        }
    }

    /**
     * Uploads the selected poster to Firebase Storage and proceeds to save the
     * document upon success.
     */
    private void uploadAndSave() {
        String path = "posters/" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(path);
        // TODO: unhardcode these
        int capacity = 0;
        String address = "";
        Double lat = 53.5232;
        Double lng = -113.5263;

        ref.putFile(selectedImageUri)
                .continueWithTask(task -> ref.getDownloadUrl())
                .addOnSuccessListener(uri -> saveToFirestore(uri.toString(), organizerId, capacity, address, lat, lng))
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show());
    }

    /**
     * Packages the event details into a map and saves it as a new document.
     * All fields are dynamically passed or extracted from the UI, and types are strictly
     * enforced to match the Event.java model to prevent deserialization crashes.
     *
     * @param posterUrl          The download URL of the event poster.
     * @param currentOrganizerId The ID of the currently logged-in organizer.
     * @param capacity           The maximum number of attendees allowed.
     * @param venueAddress       The string representation of the venue location.
     * @param venueLat           The double-precision latitude.
     * @param venueLng           The double-precision longitude.
     */
    private void saveToFirestore(String posterUrl,
                                 String currentOrganizerId,
                                 int capacity,
                                 String venueAddress,
                                 Double venueLat,
                                 Double venueLng) {

        Map<String, Object> map = new HashMap<>();
        map.put("name", etName.getText().toString().trim());
        map.put("organizerId", currentOrganizerId);
        map.put("posterUrl", posterUrl);
        map.put("geolocationRequired", swGeo.isChecked());
        map.put("venueAddress", venueAddress);
        map.put("venueLat", venueLat);
        map.put("venueLng", venueLng);
        map.put("capacity", capacity);
        map.put("waitingCount", 0);
        map.put("selectedCount", 0);
        map.put("enrolledCount", 0);

        if (regStart != null) {
            map.put("registrationStart", regStart);
            map.put("registrationEnd", regEnd);
        }

        // map.put("startTime", eventStartTime);

        // optional waitlist limit
        String limitText = etWaitlistLimit.getText().toString().trim();
        if (!limitText.isEmpty()) {
            try {
                map.put("waitlistLimit", Integer.parseInt(limitText));
            } catch (NumberFormatException e) {
                map.put("waitlistLimit", null); // Safe fallback
            }
        } else {
            map.put("waitlistLimit", null);
        }

        // save to the nested subcollection: /organizers/{organizerId}/events/
        db.collection("organizers")
                .document(currentOrganizerId)
                .collection("events")
                .add(map)
                .addOnSuccessListener(docRef -> {
                    String newEventId = docRef.getId();
                    // generate and upload the QR Code
                    uploadQRCode(newEventId, currentOrganizerId, docRef);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Generates a QR Code Bitmap for the new Event ID, uploads it to Firebase Storage,
     * and links the resulting URL back to the Firestore document.
     */
    private void uploadQRCode(String eventId, String orgId, com.google.firebase.firestore.DocumentReference docRef) {
        try {
            // generate the QR Code bitmap
            Bitmap qrCode = QRCodeGenerator.generateQRCode(eventId, 500);

            // convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            qrCode.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            // set storage path (e.g., qrcodes/organizer123/eventABC.png)
            String qrPath = "qrcodes/" + orgId + "/" + eventId + ".png";
            StorageReference qrRef = storage.getReference().child(qrPath);

            // upload and retrieve URL
            qrRef.putBytes(data)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful() && task.getException() != null) {
                            throw task.getException();
                        }
                        return qrRef.getDownloadUrl();
                    })
                    .addOnSuccessListener(uri -> {
                        // update the Firestore document with the qrCodeUrl
                        docRef.update("qrCodeUrl", uri.toString())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Event successfully created!", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Event created, but QR upload failed.", Toast.LENGTH_SHORT).show();
                        finish();
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Event created, but QR generation failed.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}