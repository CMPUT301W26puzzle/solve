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
import com.google.firebase.firestore.FieldValue;
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
 *
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

    /**
     * Launcher for the GET_CONTENT intent to pick images from the device gallery.
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
        if (etName.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Event name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            uploadAndSave();
        } else {
            Integer parsedLimit = null;
            String limitText = etWaitlistLimit.getText().toString().trim();
            if (!limitText.isEmpty()) {
                try {
                    parsedLimit = Integer.parseInt(limitText);
                } catch (NumberFormatException ignored) {}
            }

            saveToFirestore(
                    etName.getText().toString().trim(), // name
                    organizerId,                        // organizerId
                    null,                               // facilityId (default "")
                    null,                               // posterUrl
                    null,                               // qrCodeUrl (default "")
                    "University of Alberta",            // venueAddress
                    swGeo.isChecked(),                  // geolocationRequired
                    0,                                  // enrolledCount
                    0,                                  // waitingCount
                    0,                                  // selectedCount
                    53.5232,                            // venueLat
                    -113.5263,                          // venueLng
                    regStart,                           // registrationStart
                    regEnd,                             // registrationEnd
                    parsedLimit                         // waitlistLimit
            );
        }
    }

    /**
     * Uploads the selected poster to Firebase Storage and proceeds to save the
     * document upon success, passing all necessary fields to saveToFirestore.
     */
    private void uploadAndSave() {
        String path = "posters/" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(path);

        String address = "";
        Double lat = 53.5232;
        Double lng = -113.5263;

        Integer parsedLimit = null;
        String limitText = etWaitlistLimit.getText().toString().trim();
        if (!limitText.isEmpty()) {
            try {
                parsedLimit = Integer.parseInt(limitText);
            } catch (NumberFormatException ignored) {}
        }

        final Integer finalWaitlistLimit = parsedLimit;

        ref.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> saveToFirestore(
                        etName.getText().toString().trim(), // name
                        organizerId,                        // organizerId
                        null,                               // facilityId
                        uri.toString(),                     // posterUrl
                        null,                               // qrCodeUrl
                        address,                            // venueAddress
                        swGeo.isChecked(),                  // geolocationRequired
                        0,                                  // enrolledCount
                        0,                                  // waitingCount
                        0,                                  // selectedCount
                        lat,                                // venueLat
                        lng,                                // venueLng
                        regStart,                           // registrationStart
                        regEnd,                             // registrationEnd
                        finalWaitlistLimit                  // waitlistLimit
                ))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Packages the event details into a map and saves it as a new document in Firestore.
     * Every possible field from the Event model is accepted as an input parameter.
     * If any parameter is passed as null, the method applies a safe default to ensure
     * the resulting document strictly matches the Event.java model, preventing
     * NullPointerExceptions in the UI adapters.
     *
     * @param name                The display name or title of the event.
     * @param organizerId         The ID of the currently logged-in organizer.
     * @param facilityId          The ID of the facility where this event is being hosted.
     * @param posterUrl           The cloud storage download URL for the event's promotional poster.
     * @param qrCodeUrl           The cloud storage download URL for the generated QR code.
     * @param venueAddress        The physical address or location name of the event venue.
     * @param geolocationRequired Flag indicating whether an entrant must provide their geolocation to join the waitlist.
     * @param enrolledCount       The current number of entrants who have been successfully enrolled.
     * @param waitingCount        The current number of entrants waiting on the waitlist.
     * @param selectedCount       The count of entrants currently selected by the lottery.
     * @param venueLat            The double-precision latitude of the venue location.
     * @param venueLng            The double-precision longitude of the venue location.
     * @param registrationStart   The date and time when the waitlist registration period opens.
     * @param registrationEnd     The date and time when the waitlist registration period closes.
     * @param waitlistLimit       An optional cap on the maximum number of entrants allowed on the waitlist (null for unlimited).
     */
    private void saveToFirestore(
            String name,
            String organizerId,
            String facilityId,
            String posterUrl,
            String qrCodeUrl,
            String venueAddress,
            Boolean geolocationRequired,
            Integer enrolledCount,
            Integer waitingCount,
            Integer selectedCount,
            Double venueLat,
            Double venueLng,
            Date registrationStart,
            Date registrationEnd,
            Integer waitlistLimit) {

        Map<String, Object> map = new HashMap<>();

        map.put("name", name != null ? name : "");
        map.put("organizerId", organizerId != null ? organizerId : "");
        map.put("facilityId", facilityId != null ? facilityId : "");
        map.put("posterUrl", posterUrl != null ? posterUrl : "");
        map.put("qrCodeUrl", qrCodeUrl != null ? qrCodeUrl : "");
        map.put("venueAddress", venueAddress != null ? venueAddress : "");

        map.put("geolocationRequired", geolocationRequired != null ? geolocationRequired : false);

        map.put("enrolledCount", enrolledCount != null ? enrolledCount : 0);
        map.put("waitingCount", waitingCount != null ? waitingCount : 0);
        map.put("selectedCount", selectedCount != null ? selectedCount : 0);
        map.put("venueLat", venueLat != null ? venueLat : 0.0);
        map.put("venueLng", venueLng != null ? venueLng : 0.0);

        Date fallbackDate = new Date();
        map.put("registrationStart", registrationStart != null ? registrationStart : fallbackDate);
        map.put("registrationEnd", registrationEnd != null ? registrationEnd : fallbackDate);

        map.put("waitlistLimit", waitlistLimit);

        String safeOrganizerId = organizerId != null && !organizerId.isEmpty()
                ? organizerId
                : "unknown_organizer";

        String newEventId = db.collection("events").document().getId();
        map.put("id", newEventId);

        DocumentReference eventRef = db.collection("events").document(newEventId);

        eventRef.set(map)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> organizerEventMap = new HashMap<>();
                    organizerEventMap.put("eventId", newEventId);
                    organizerEventMap.put("name", name != null ? name : "");
                    organizerEventMap.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("organizers")
                            .document(safeOrganizerId)
                            .collection("events")
                            .document(newEventId)
                            .set(organizerEventMap)
                            .addOnSuccessListener(unusedIndex -> {
                                uploadQRCode(newEventId, safeOrganizerId, eventRef);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Event created, but organizer index failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Generates a QR Code Bitmap for the new Event ID, uploads it to Firebase Storage,
     * and links the resulting URL back to the Firestore document.
     */
    private void uploadQRCode(String eventId, String orgId, DocumentReference docRef) {
        try {
            Bitmap qrCode = QRCodeGenerator.generateQRCode(eventId, 500);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            qrCode.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            String qrPath = "qrcodes/" + orgId + "/" + eventId + ".png";
            StorageReference qrRef = storage.getReference().child(qrPath);

            qrRef.putBytes(data)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful() && task.getException() != null) {
                            throw task.getException();
                        }
                        return qrRef.getDownloadUrl();
                    })
                    .addOnSuccessListener(uri -> {
                        docRef.update("qrCodeUrl", uri.toString())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Event successfully created!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Event created, but failed to save QR URL.", Toast.LENGTH_SHORT).show();
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