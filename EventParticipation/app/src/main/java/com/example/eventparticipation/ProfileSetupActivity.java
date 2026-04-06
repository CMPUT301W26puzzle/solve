package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Onboarding screen shown when the current device has no profile in Firestore yet.
 */
public class ProfileSetupActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "extra_role";
    public static final String EXTRA_PROFILE_ID = "extra_profile_id";

    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private MaterialButton btnSaveProfile;
    private ProgressBar progressBar;
    private TextView tvTitle;
    private TextView tvSubtitle;

    private FirebaseFirestore db;
    private String role;
    private String profileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        db = FirebaseFirestore.getInstance();
        role = getIntent().getStringExtra(EXTRA_ROLE);
        profileId = getIntent().getStringExtra(EXTRA_PROFILE_ID);

        if (!isSupportedRole(role) || !DeviceIdProvider.isValidId(profileId)) {
            Toast.makeText(this, "Failed to start profile setup", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        bindCopy();
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvProfileSetupTitle);
        tvSubtitle = findViewById(R.id.tvProfileSetupSubtitle);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressBar);
    }

    private void bindCopy() {
        String roleLabel = Character.toUpperCase(role.charAt(0)) + role.substring(1);
        tvTitle.setText("Complete your " + roleLabel + " profile");
        tvSubtitle.setText("New users need a name and email before continuing.");
    }

    /**
     * Validates input, merges the new profile data into Firestore, and saves
     * the local session before routing the user to their dashboard.
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

        if (!phone.isEmpty() && !isValidPhone(phone)) {
            etPhone.setError("Enter a 10-digit phone number");
            etPhone.requestFocus();
            return;
        }

        setLoading(true);

        Map<String, Object> profile = new HashMap<>();
        profile.put(getIdField(role), profileId);
        profile.put("role", role);
        profile.put("name", name);
        profile.put("email", email);
        profile.put("phone", phone);

        db.collection(getCollection(role))
                .document(profileId)
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // NEW: Save the session so the user remains logged in for future app launches
                    SessionManager.getInstance(this).saveSession(profileId, role);

                    setLoading(false);
                    openDestination();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void openDestination() {
        Intent intent;
        if ("organizer".equals(role)) {
            intent = new Intent(this, OrganizerDashboardActivity.class);
        } else if ("admin".equals(role)) {
            Toast.makeText(this, "Admin dashboard coming soon", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else {
            intent = new Intent(this, EntrantDashboardActivity.class);
        }

        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        btnSaveProfile.setEnabled(!isLoading);
        btnSaveProfile.setText(isLoading ? "" : "Continue");
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void clearErrors() {
        etName.setError(null);
        etEmail.setError(null);
        etPhone.setError(null);
    }

    private String getInputText(TextInputEditText editText) {
        if (editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private boolean isValidPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 10;
    }

    private boolean isSupportedRole(String role) {
        return "entrant".equals(role) || "organizer".equals(role) || "admin".equals(role);
    }

    private String getCollection(String role) {
        if ("organizer".equals(role)) {
            return "organizers";
        }
        if ("admin".equals(role)) {
            return "admins";
        }
        return "entrants";
    }

    private String getIdField(String role) {
        if ("organizer".equals(role)) {
            return "organizerId";
        }
        if ("admin".equals(role)) {
            return "adminId";
        }
        return "entrantId";
    }
}