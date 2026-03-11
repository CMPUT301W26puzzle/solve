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
 * <p>This activity provide s a form for organizers to input event details including
 * name, waitlist capacity limit, geolocation requirements, registration dates, and
 * an optional promotional poster. It handles uploading the image to Firebase Storage
 * and saving the event document to Firestore.</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 * <li>US 02.01.01 - Create a new event</li>
 * <li>US 02.01.04 - Set a registration period</li>
 * <li>US 02.03.01 - Optionally limit waitlist capacity</li>
 * <li>US 02.04.01 - Upload an event poster</li>
 * </ul>
 */
public class CreateEventActivity extends AppCompatActivity {

    /** Input field for the event's name. */
    private EditText etName;

    /** Input field for the optional waitlist limit. */
    private EditText etWaitlistLimit;

    /** Switch to toggle whether geolocation is required to join the waitlist. */
    private SwitchCompat swGeo;

    /** View to display the selected event poster image. */
    private ImageView imgPoster;

    /** Button to open the date range picker for registration periods. */
    private MaterialButton btnDates;

    /** Button to trigger the save and upload process. */
    private MaterialButton btnSave;

    /** Hardcoded organizer ID (Temporary until authentication is implemented). */
    private final String organizerId = "organizer_demo_001";

    /** URI of the image selected from the device storage. */
    private Uri selectedImageUri;

    /** Start date of the registration period. */
    private Date regStart;

    /** End date of the registration period. */
    private Date regEnd;

    /** Reference to the Firebase Firestore database. */
    private FirebaseFirestore db;

    /** Reference to Firebase Storage for image uploads. */
    private FirebaseStorage storage;

    /** * Activity result launcher used to open the device's file picker
     * and retrieve an image for the event poster.
     */
    private final ActivityResultLauncher<String> picker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgPoster.setImageURI(uri);
                }
            });

    /**
     * Initializes the activity, sets up the content view, and connects to Firebase services.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     * being shut down then this Bundle contains the data it most
     * recently supplied. Otherwise it is null.
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
     * Binds the UI components from the layout XML to the class variables.
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
     * Sets up click listeners for the image picker, date picker, and save button.
     */
    private void setupListeners() {
        imgPoster.setOnClickListener(v -> picker.launch("image/*"));

        // Date range picker for registration period
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
     * Validates required inputs and starts the appropriate saving process based
     * on whether an image was selected for upload.
     */
    private void startSave() {
        if (etName.getText().toString().isEmpty()) {
            Toast.makeText(this, "name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            uploadAndSave();
        } else {
            saveToFirestore("");
        }
    }

    /**
     * Uploads the selected poster image to Firebase Storage. Once uploaded,
     * it retrieves the download URL and proceeds to save the event data to Firestore.
     */
    private void uploadAndSave() {
        String path = "posters/" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(path);

        ref.putFile(selectedImageUri)
                .continueWithTask(task -> ref.getDownloadUrl())
                .addOnSuccessListener(uri -> saveToFirestore(uri.toString()))
                .addOnFailureListener(e -> Toast.makeText(this, "upload failed", Toast.LENGTH_SHORT).show());
    }

    /**
     * Packages the event details into a map and saves it as a new document
     * in the Firestore events collection. Upon successful creation, it generates
     * a QR code for the event ID, uploads it to Firebase Storage, and links the URL.
     *
     * @param posterUrl The download URL of the uploaded poster, or an empty string if none.
     */
    private void saveToFirestore(String posterUrl) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", etName.getText().toString().trim());
        map.put("geolocationRequired", swGeo.isChecked());
        map.put("posterUrl", posterUrl);
        map.put("organizerId", organizerId); // Make sure organizerId is saved!

        if (regStart != null) {
            map.put("registrationStart", regStart);
            map.put("registrationEnd", regEnd);
        }

        String limit = etWaitlistLimit.getText().toString();
        if (!limit.isEmpty()) {
            map.put("waitlistLimit", Integer.parseInt(limit));
        }

        // add to the top-level "events" collection
        db.collection("events")
                .add(map)
                .addOnSuccessListener(docRef -> {
                    String newEventId = docRef.getId();

                    try {
                        // generate the qr code bitmap
                        Bitmap qrCode = QRCodeGenerator.generateQRCode(newEventId, 500);

                        // convert bitmap to byte array for firebase
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        qrCode.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] data = baos.toByteArray();

                        // path in firebase
                        StorageReference qrRef = storage.getReference().child("qrcodes/" + newEventId + ".png");

                        // 4. Upload the byte array
                        qrRef.putBytes(data)
                                .continueWithTask(task -> {
                                    if (!task.isSuccessful() && task.getException() != null) {
                                        throw task.getException();
                                    }
                                    // Get the download URL after successful upload
                                    return qrRef.getDownloadUrl();
                                })
                                .addOnSuccessListener(uri -> {
                                    // 5. Link the QR code URL to the newly created event document
                                    docRef.update("qrCodeUrl", uri.toString())
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this, "Event & QR Code created!", Toast.LENGTH_SHORT).show();
                                                finish();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Event created, but QR upload failed.", Toast.LENGTH_SHORT).show();
                                    finish();
                                });

                    } catch (com.google.zxing.WriterException e) {
                        Toast.makeText(this, "Event created, but QR generation failed.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to create event", Toast.LENGTH_SHORT).show());
    }
}