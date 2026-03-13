package com.example.eventparticipation;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity for viewing and updating entrant profile information.
 *
 * <p>Relevant user stories:</p>
 * <ul>
 *     <li>US 01.02.01 - As an entrant, I want to provide my personal information such as name, email and optional phone number in the app</li>
 *     <li>US 01.02.02 - As an entrant I want to update information such as name, email and contact information on my profile</li>
 * </ul>
 */
public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private MaterialButton btnSaveChanges;

    private FirebaseFirestore db;
    private String entrantId;
    private boolean hasExistingProfileData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        entrantId = DeviceIdProvider.getId(this);

        initViews();

        if (!DeviceIdProvider.isValidId(entrantId)) {
            Toast.makeText(this, "Failed to get device ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProfile();

        btnSaveChanges.setOnClickListener(v -> saveProfile());
    }

    /**
     * Binds the layout views.
     */
    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    /**
     * Loads the current entrant profile from Firestore.
     */
    private void loadProfile() {
        db.collection("entrants")
                .document(entrantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        btnSaveChanges.setText("Save Changes");
                        return;
                    }

                    String name = documentSnapshot.getString("name");
                    String email = documentSnapshot.getString("email");
                    String phone = documentSnapshot.getString("phone");

                    if (name != null) {
                        etName.setText(name);
                    }

                    if (email != null) {
                        etEmail.setText(email);
                    }

                    if (phone != null) {
                        etPhone.setText(phone);
                    }

                    hasExistingProfileData =
                            (name != null && !name.trim().isEmpty()) ||
                                    (email != null && !email.trim().isEmpty()) ||
                                    (phone != null && !phone.trim().isEmpty());

                    btnSaveChanges.setText(hasExistingProfileData ? "Update" : "Save Changes");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Validates and saves the profile data to Firestore.
     */
    private void saveProfile() {
        String name = getInputText(etName);
        String email = getInputText(etEmail);
        String phone = getInputText(etPhone);

        clearErrors();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("entrantId", entrantId);
        profile.put("role", "entrant");
        profile.put("name", name);
        profile.put("email", email);
        profile.put("phone", phone);

        db.collection("entrants")
                .document(entrantId)
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    hasExistingProfileData = true;
                    btnSaveChanges.setText("Update");
                    Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Clears existing field errors.
     */
    private void clearErrors() {
        etName.setError(null);
        etEmail.setError(null);
        etPhone.setError(null);
    }

    /**
     * Returns trimmed text from an input field.
     *
     * @param editText input field
     * @return trimmed text, or an empty string if null
     */
    private String getInputText(TextInputEditText editText) {
        if (editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }
}