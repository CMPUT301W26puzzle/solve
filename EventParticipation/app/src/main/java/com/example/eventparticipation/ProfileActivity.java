package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity for viewing and updating profile information.
 *
 * Supports:
 * - save/update profile
 * - delete entrant account
 * - test shortcut into co-organizer-accessible Manage Event
 */
public class ProfileActivity extends BaseOrganizerActivity {

    public static final String EXTRA_ROLE = "extra_role";
    public static final String EXTRA_PROFILE_ID = "extra_profile_id";
    public static final String EXTRA_TEST_ENTRANT_ID = "extra_test_entrant_id";

    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private MaterialButton btnSaveChanges;
    private MaterialButton btnDeleteAccount;
    private MaterialButton btnCoOrganizerDashboard;

    private MaterialCardView cardDeleteAccount;
    private MaterialCardView cardCoOrganizerAccess;

    private BottomNavigationView bottomNavigation;
    private TextView tvProfileTitle;
    private TextView tvProfileSubtitle;
    private TextView tvDeleteAccountSubtitle;

    private FirebaseFirestore db;
    private String profileId;
    private String role;
    private boolean hasExistingProfileData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        String testEntrantId = getIntent().getStringExtra(EXTRA_TEST_ENTRANT_ID);
        role = resolveRole();
        profileId = resolveProfileId(testEntrantId);

        initViews();

        if (!DeviceIdProvider.isValidId(profileId)) {
            Toast.makeText(this, "Failed to get device ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        configureUiForRole();
        loadProfile();

        btnSaveChanges.setOnClickListener(v -> saveProfile());

        btnDeleteAccount.setOnClickListener(v -> {
            if (isEntrantRole()) {
                showDeleteAccountDialog();
            }
        });


        btnCoOrganizerDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(this, CoOrganizerDashboardActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Binds layout views.
     */
    private void initViews() {
        tvProfileTitle = findViewById(R.id.tvProfileTitle);
        tvProfileSubtitle = findViewById(R.id.tvProfileSubtitle);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnCoOrganizerDashboard = findViewById(R.id.btnCoOrganizerDashboard);

        cardDeleteAccount = findViewById(R.id.cardDeleteAccount);
        cardCoOrganizerAccess = findViewById(R.id.cardCoOrganizerAccess);

        tvDeleteAccountSubtitle = findViewById(R.id.tvDeleteAccountSubtitle);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void configureUiForRole() {
        String titleRole = Character.toUpperCase(role.charAt(0)) + role.substring(1);
        tvProfileTitle.setText(titleRole + " Profile");
        tvProfileSubtitle.setText("Manage your account settings");

        // For testing, show co-organizer access card for entrant role
        cardCoOrganizerAccess.setVisibility(isEntrantRole() ? View.VISIBLE : View.GONE);

        if (isEntrantRole()) {
            if (bottomNavigation != null) {
                bottomNavigation.setVisibility(View.VISIBLE);
            }
            setupBottomNav(R.id.nav_profile);

            cardDeleteAccount.setVisibility(View.VISIBLE);
            tvDeleteAccountSubtitle.setText("Remove your profile and registrations.");
        } else if ("organizer".equals(role)) {
            if (bottomNavigation != null) {
                bottomNavigation.setVisibility(View.VISIBLE);
            }
            setupOrganizerBottomNav(R.id.nav_profile);

            cardDeleteAccount.setVisibility(View.GONE);
            cardCoOrganizerAccess.setVisibility(View.GONE);
        } else {
            if (bottomNavigation != null) {
                bottomNavigation.setVisibility(View.GONE);
            }
            cardDeleteAccount.setVisibility(View.GONE);
            cardCoOrganizerAccess.setVisibility(View.GONE);
        }
    }

    /**
     * Loads current profile data from Firestore.
     */
    private void loadProfile() {
        db.collection(getCollectionName())
                .document(profileId)
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
                            (name != null && !name.trim().isEmpty())
                                    || (email != null && !email.trim().isEmpty())
                                    || (phone != null && !phone.trim().isEmpty());

                    btnSaveChanges.setText(hasExistingProfileData ? "Update" : "Save Changes");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Validates and saves profile data.
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

        Map<String, Object> profile = new HashMap<>();
        profile.put(getIdFieldName(), profileId);
        profile.put("role", role);
        profile.put("name", name);
        profile.put("email", email);
        profile.put("phone", phone);

        db.collection(getCollectionName())
                .document(profileId)
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    hasExistingProfileData = true;
                    btnSaveChanges.setText("Update");
                    Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show());
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

    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Are you sure you want to delete this account?")
                .setMessage("This will permanently remove your profile and registrations.")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .show();
    }

    private void deleteAccount() {
        if (!isEntrantRole()) {
            return;
        }

        btnDeleteAccount.setEnabled(false);
        btnDeleteAccount.setText("Deleting...");

        if (btnSaveChanges != null) {
            btnSaveChanges.setEnabled(false);
        }

        if (!DeviceIdProvider.isValidId(profileId)) {
            btnDeleteAccount.setEnabled(true);
            btnDeleteAccount.setText("Delete account");

            if (btnSaveChanges != null) {
                btnSaveChanges.setEnabled(true);
            }

            Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events")
                .get()
                .addOnSuccessListener(eventsSnapshot -> {
                    WriteBatch batch = db.batch();

                    batch.delete(db.collection("entrants").document(profileId));

                    for (com.google.firebase.firestore.QueryDocumentSnapshot eventDoc : eventsSnapshot) {
                        batch.delete(
                                eventDoc.getReference()
                                        .collection("waitlist")
                                        .document(profileId)
                        );
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                                resetAppState();
                            })
                            .addOnFailureListener(e -> {
                                btnDeleteAccount.setEnabled(true);
                                btnDeleteAccount.setText("Delete account");

                                if (btnSaveChanges != null) {
                                    btnSaveChanges.setEnabled(true);
                                }

                                Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnDeleteAccount.setEnabled(true);
                    btnDeleteAccount.setText("Delete account");

                    if (btnSaveChanges != null) {
                        btnSaveChanges.setEnabled(true);
                    }

                    Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                });
    }

    private void resetAppState() {
        Intent intent = new Intent(this, SelectRoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String resolveRole() {
        String requestedRole = getIntent().getStringExtra(EXTRA_ROLE);
        if ("organizer".equals(requestedRole) || "admin".equals(requestedRole) || "entrant".equals(requestedRole)) {
            return requestedRole;
        }
        return "entrant";
    }

    private String resolveProfileId(String testEntrantId) {
        if (testEntrantId != null) {
            return testEntrantId;
        }

        String requestedProfileId = getIntent().getStringExtra(EXTRA_PROFILE_ID);
        if (requestedProfileId != null && !requestedProfileId.trim().isEmpty()) {
            return requestedProfileId;
        }

        return DeviceIdProvider.getId(this);
    }

    private boolean isEntrantRole() {
        return "entrant".equals(role);
    }

    private String getCollectionName() {
        switch (role) {
            case "organizer":
                return "organizers";
            case "admin":
                return "admins";
            case "entrant":
            default:
                return "entrants";
        }
    }

    private String getIdFieldName() {
        switch (role) {
            case "organizer":
                return "organizerId";
            case "admin":
                return "adminId";
            case "entrant":
            default:
                return "entrantId";
        }
    }
}