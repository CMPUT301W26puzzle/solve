package com.example.eventparticipation;

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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * handles the creation of a new event.
 */
public class CreateEventActivity extends AppCompatActivity {
    private EditText etName, etWaitlistLimit;
    private SwitchCompat swGeo;
    private ImageView imgPoster;
    private MaterialButton btnDates, btnSave;

    private final String organizerId = "organizer_demo_001";
    private Uri selectedImageUri;
    private Date regStart, regEnd;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // image picker for the event poster
    private final ActivityResultLauncher<String> picker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgPoster.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etEventName);
        etWaitlistLimit = findViewById(R.id.etWaitlistLimit);
        swGeo = findViewById(R.id.switchGeolocation);
        imgPoster = findViewById(R.id.imgEventPoster);
        btnDates = findViewById(R.id.btnDateRange);
        btnSave = findViewById(R.id.btnSaveEvent);
    }

    private void setupListeners() {
        imgPoster.setOnClickListener(v -> picker.launch("image/*"));

        // date range picker for registration period
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

    private void uploadAndSave() {
        String path = "posters/" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(path);

        ref.putFile(selectedImageUri)
                .continueWithTask(task -> ref.getDownloadUrl())
                .addOnSuccessListener(uri -> saveToFirestore(uri.toString()))
                .addOnFailureListener(e -> Toast.makeText(this, "upload failed", Toast.LENGTH_SHORT).show());
    }

    /**
     * saves the final data to firestore.
     */
    private void saveToFirestore(String posterUrl) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", etName.getText().toString().trim());
        map.put("geolocationRequired", swGeo.isChecked());
        map.put("posterUrl", posterUrl);

        if (regStart != null) {
            map.put("registrationStart", regStart);
            map.put("registrationEnd", regEnd);
        }

        String limit = etWaitlistLimit.getText().toString();
        if (!limit.isEmpty()) {
            map.put("waitlistLimit", Integer.parseInt(limit));
        }

        db.collection("organizers").document(organizerId).collection("events")
                .add(map)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "event created", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}