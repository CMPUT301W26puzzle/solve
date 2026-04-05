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
import com.google.android.material.materialswitch.MaterialSwitch;
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
 *
 * <p>Relevant user stories:</p>
 * <ul>
 * <li>US 01.02.01 - As an entrant, I want to provide my personal information such as name, email and optional phone number in the app</li>
 * <li>US 01.02.02 - As an entrant I want to update information such as name, email and contact information on my profile</li>
 * <li>US 01.02.04 - As an entrant, I want to delete my profile if I no longer wish to use the app</li>
 * </ul>
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

    private MaterialCardView cardNotificationSettings;
    private BottomNavigationView bottomNavigation;
    private MaterialSwitch switchOptOut;
    private TextView tvProfileTitle;
    private TextView tvProfileSubtitle;
    private TextView tvDeleteAccountSubtitle;

    private FirebaseFirestore db;
    private String profileId;
    private String role;
    private boolean hasExistingProfileData = false;
    private boolean isBindingOptOutPreference = false;

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
        setupOptOutToggle();
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
        cardNotificationSettings = findViewById(R.id.cardNotificationSettings);
        switchOptOut = findViewById(R.id.switchOptOut);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
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
            cardNotificationSettings.setVisibility(View.VISIBLE);
            cardDeleteAccount.setVisibility(View.VISIBLE);
            tvDeleteAccountSubtitle.setText("Remove your profile and registrations.");
        } else if ("organizer".equals(role)) {
            if (bottomNavigation != null) {
                bottomNavigation.setVisibility(View.VISIBLE);
            }
            setupOrganizerBottomNav(R.id.nav_profile);
            cardNotificationSettings.setVisibility(View.GONE);
            cardDeleteAccount.setVisibility(View.GONE);
            cardCoOrganizerAccess.setVisibility(View.GONE);
        } else {
            if (bottomNavigation != null) {
                bottomNavigation.setVisibility(View.GONE);
            }
            cardNotificationSettings.setVisibility(View.GONE);
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
                    Boolean isOptedOut = documentSnapshot.getBoolean("optOutNotifications");

                    if (name != null) {
                        etName.setText(name);
                    }

                    if (email != null) {
                        etEmail.setText(email);
                    }

                    if (phone != null) {
                        etPhone.setText(phone);
                    }

                    if (isEntrantRole()) {
                        isBindingOptOutPreference = true;
                        switchOptOut.setChecked(isOptedOut != null && isOptedOut);
                        isBindingOptOutPreference = false;
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
        if (isEntrantRole()) {
            profile.put("optOutNotifications", switchOptOut.isChecked());
        }

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

    /**
     * Checks whether the phone number contains exactly 10 digits.
     *
     * @param phone the phone number entered by the user
     * @return true if the phone number contains exactly 10 digits; false otherwise
     */
    private boolean isValidPhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 10;
    }

    /**
     * Saves entrant notification preferences when the switch is toggled by the user.
     */
    private void setupOptOutToggle() {
        if (!isEntrantRole()) {
            return;
        }

        switchOptOut.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isBindingOptOutPreference) {
                return;
            }

            db.collection("entrants")
                    .document(profileId)
                    .update("optOutNotifications", isChecked)
                    .addOnSuccessListener(unused -> {
                        String message = isChecked
                                ? "Notifications disabled"
                                : "Notifications enabled";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to update settings", Toast.LENGTH_SHORT).show()
                    );
        });
    }

    /**
     * Shows a confirmation dialog before deleting the current account.
     */
    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Are you sure you want to delete this account?")
                .setMessage("This will permanently remove your profile and registrations.")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .show();
    }

    /**
     * Deletes the current entrant profile and returns the app to the initial state.
     */
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

                    // TODO: update waitingCount if needed?? update some other lists maybe?

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

    /**
     * Resets the app flow to the initial screen after account deletion.
     */
    private void resetAppState() {
        Intent intent = new Intent(this, SelectRoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Resolves the active profile role from the launch intent.
     *
     * @return normalized role value
     */
    private String resolveRole() {
        String requestedRole = getIntent().getStringExtra(EXTRA_ROLE);
        if ("organizer".equals(requestedRole) || "admin".equals(requestedRole) || "entrant".equals(requestedRole)) {
            return requestedRole;
        }
        return "entrant";
    }

    /**
     * Resolves the current profile id.
     *
     * @param testEntrantId optional test override id
     * @return resolved profile id
     */
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

    /**
     * Checks whether the current profile is an entrant.
     *
     * @return true if the role is entrant
     */
    private boolean isEntrantRole() {
        return "entrant".equals(role);
    }

    /**
     * Returns the Firestore collection for the current role.
     *
     * @return collection name
     */
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

    /**
     * Returns the id field name for the current role.
     *
     * @return Firestore id field name
     */
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
