package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity for viewing and modifying user profile information.
 *
 * <p>Allows users to update their contact details, manage notification
 * preferences, access co-organizer features, or permanently delete their account.</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 * <li>US 01.02.02 Update profile information</li>
 * <li>US 01.02.04 Delete profile</li>
 * <li>US 01.04.03 Opt out of notifications</li>
 * </ul>
 */
public class ProfileActivity extends BaseOrganizerActivity {

    public static final String EXTRA_TEST_ENTRANT_ID = "extra_test_entrant_id";

    private TextInputEditText etName, etEmail, etPhone;
    private MaterialButton btnSaveChanges, btnDeleteAccount, btnCoOrganizerDashboard, btnLogout;

    private MaterialCardView cardDeleteAccount, cardCoOrganizerAccess, cardNotificationSettings;
    private BottomNavigationView bottomNavigation;
    private MaterialSwitch switchOptOut;
    private TextView tvProfileTitle, tvProfileSubtitle, tvDeleteAccountSubtitle;

    private FirebaseFirestore db;
    private String profileId;
    private String role;
    private boolean hasExistingProfileData = false;
    private boolean isBindingOptOutPreference = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Get the Session
        SessionManager session = SessionManager.getInstance(this);
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SelectRoleActivity.class));
            finish();
            return;
        }

        // 2. Assign Role and ID Securely
        profileId = session.getUserId();
        role = session.getRole();

        // Handle testing overrides
        String testEntrantId = getIntent().getStringExtra(EXTRA_TEST_ENTRANT_ID);
        if (testEntrantId != null && !testEntrantId.isEmpty()) {
            profileId = testEntrantId;
        }

        setContentView(R.layout.activity_profile);
        db = FirebaseFirestore.getInstance();

        initViews();
        configureUiForRole();
        setupOptOutToggle();
        loadProfile();

        // 3. Setup standard click listeners
        btnSaveChanges.setOnClickListener(v -> saveProfile());

        btnDeleteAccount.setOnClickListener(v -> {
            if (isEntrantRole()) showDeleteAccountDialog();
        });

        btnCoOrganizerDashboard.setOnClickListener(v -> {
            startActivity(new Intent(this, CoOrganizerDashboardActivity.class));
        });

        // 4. Handle Logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                session.logout();
                Intent intent = new Intent(this, SelectRoleActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void initViews() {
        tvProfileTitle = findViewById(R.id.tvProfileTitle);
        tvProfileSubtitle = findViewById(R.id.tvProfileSubtitle);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnCoOrganizerDashboard = findViewById(R.id.btnCoOrganizerDashboard);
        btnLogout = findViewById(R.id.btnLogout); // Bind Logout Button

        cardDeleteAccount = findViewById(R.id.cardDeleteAccount);
        cardCoOrganizerAccess = findViewById(R.id.cardCoOrganizerAccess);
        cardNotificationSettings = findViewById(R.id.cardNotificationSettings);
        switchOptOut = findViewById(R.id.switchOptOut);
        tvDeleteAccountSubtitle = findViewById(R.id.tvDeleteAccountSubtitle);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void configureUiForRole() {
        String titleRole = Character.toUpperCase(role.charAt(0)) + role.substring(1);
        tvProfileTitle.setText(titleRole + " Profile");
        tvProfileSubtitle.setText("Manage your account settings");

        cardCoOrganizerAccess.setVisibility(isEntrantRole() ? View.VISIBLE : View.GONE);

        if (isEntrantRole()) {
            if (bottomNavigation != null) bottomNavigation.setVisibility(View.VISIBLE);
            setupBottomNav(R.id.nav_profile);
            cardNotificationSettings.setVisibility(View.VISIBLE);
            cardDeleteAccount.setVisibility(View.VISIBLE);
            tvDeleteAccountSubtitle.setText("Remove your profile and registrations.");
        } else if ("organizer".equals(role)) {
            if (bottomNavigation != null) bottomNavigation.setVisibility(View.VISIBLE);
            setupOrganizerBottomNav(R.id.nav_profile);
            cardNotificationSettings.setVisibility(View.GONE);
            cardDeleteAccount.setVisibility(View.GONE);
            cardCoOrganizerAccess.setVisibility(View.GONE);
        } else {
            if (bottomNavigation != null) bottomNavigation.setVisibility(View.GONE);
            cardNotificationSettings.setVisibility(View.GONE);
            cardDeleteAccount.setVisibility(View.GONE);
            cardCoOrganizerAccess.setVisibility(View.GONE);
        }
    }

    private void loadProfile() {
        db.collection(getCollectionName()).document(profileId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        handleDeletedProfile();
                        return;
                    }

                    String name = documentSnapshot.getString("name");
                    String email = documentSnapshot.getString("email");
                    String phone = documentSnapshot.getString("phone");
                    Boolean isOptedOut = documentSnapshot.getBoolean("optOutNotifications");

                    if (name != null) etName.setText(name);
                    if (email != null) etEmail.setText(email);
                    if (phone != null) etPhone.setText(phone);

                    if (isEntrantRole()) {
                        isBindingOptOutPreference = true;
                        switchOptOut.setChecked(isOptedOut != null && isOptedOut);
                        isBindingOptOutPreference = false;
                    }

                    hasExistingProfileData = (name != null && !name.trim().isEmpty()) ||
                            (email != null && !email.trim().isEmpty()) ||
                            (phone != null && !phone.trim().isEmpty());

                    btnSaveChanges.setText(hasExistingProfileData ? "Update" : "Save Changes");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void handleDeletedProfile() {
        Toast.makeText(this, "Your account has been removed", Toast.LENGTH_LONG).show();

        // Clear session
        SessionManager.getInstance(this).logout();

        // Redirect to entry screen
        Intent intent = new Intent(this, SelectRoleActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void saveProfile() {
        String name = getInputText(etName);
        String email = getInputText(etEmail);
        String phone = getInputText(etPhone);

        clearErrors();

        if (name.isEmpty()) { etName.setError("Name is required"); etName.requestFocus(); return; }
        if (email.isEmpty()) { etEmail.setError("Email is required"); etEmail.requestFocus(); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("Enter a valid email"); etEmail.requestFocus(); return; }
        if (!phone.isEmpty() && !isValidPhone(phone)) { etPhone.setError("Enter a 10-digit phone number"); etPhone.requestFocus(); return; }

        Map<String, Object> profile = new HashMap<>();
        profile.put(getIdFieldName(), profileId);
        profile.put("role", role);
        profile.put("name", name);
        profile.put("email", email);
        profile.put("phone", phone);

        if (isEntrantRole()) {
            profile.put("optOutNotifications", switchOptOut.isChecked());
        }

        db.collection(getCollectionName()).document(profileId).set(profile, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    hasExistingProfileData = true;
                    btnSaveChanges.setText("Update");
                    Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show());
    }

    private void clearErrors() {
        etName.setError(null);
        etEmail.setError(null);
        etPhone.setError(null);
    }

    private String getInputText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private boolean isValidPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 10;
    }

    private void setupOptOutToggle() {
        if (!isEntrantRole()) return;
        switchOptOut.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isBindingOptOutPreference) return;
            db.collection("entrants").document(profileId).update("optOutNotifications", isChecked)
                    .addOnSuccessListener(unused -> Toast.makeText(this, isChecked ? "Notifications disabled" : "Notifications enabled", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update settings", Toast.LENGTH_SHORT).show());
        });
    }

    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete this account?")
                .setMessage("This will permanently remove your profile and registrations.")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .show();
    }

    private void deleteAccount() {
        if (!isEntrantRole()) return;

        btnDeleteAccount.setEnabled(false);
        btnDeleteAccount.setText("Deleting...");
        if (btnSaveChanges != null) btnSaveChanges.setEnabled(false);

        db.collection("events").get()
                .addOnSuccessListener(eventsSnapshot -> {
                    WriteBatch batch = db.batch();
                    batch.delete(db.collection("entrants").document(profileId));

                    for (com.google.firebase.firestore.QueryDocumentSnapshot eventDoc : eventsSnapshot) {
                        batch.delete(eventDoc.getReference().collection("waitlist").document(profileId));
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                                SessionManager.getInstance(this).logout();
                                resetAppState();
                            })
                            .addOnFailureListener(e -> resetDeleteButtonState());
                })
                .addOnFailureListener(e -> resetDeleteButtonState());
    }

    private void resetDeleteButtonState() {
        btnDeleteAccount.setEnabled(true);
        btnDeleteAccount.setText("Delete account");
        if (btnSaveChanges != null) btnSaveChanges.setEnabled(true);
        Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
    }

    private void resetAppState() {
        Intent intent = new Intent(this, SelectRoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isEntrantRole() {
        return "entrant".equals(role);
    }

    private String getCollectionName() {
        switch (role) {
            case "organizer": return "organizers";
            case "admin": return "admins";
            default: return "entrants";
        }
    }

    private String getIdFieldName() {
        switch (role) {
            case "organizer": return "organizerId";
            case "admin": return "adminId";
            default: return "entrantId";
        }
    }
}