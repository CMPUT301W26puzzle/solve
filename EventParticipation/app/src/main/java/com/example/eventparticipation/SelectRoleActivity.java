package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/**
 * Role selection screen shown on app launch.
 *
 * <p>User taps Entrant, Organizer, or Admin card and presses Continue
 * to be routed to the appropriate dashboard.</p>
 */
public class SelectRoleActivity extends AppCompatActivity {

    private CardView cardEntrant, cardOrganizer, cardAdmin;
    private RadioButton rbEntrant, rbOrganizer, rbAdmin;
    private Button btnContinue;
    private ProgressBar btnProgress;


    /** Currently selected role: "entrant", "organizer", or "admin". Null if none selected. */
    private String selectedRole = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_role);

        cardEntrant  = findViewById(R.id.cardEntrant);
        cardOrganizer = findViewById(R.id.cardOrganizer);
        cardAdmin    = findViewById(R.id.cardAdmin);
        rbEntrant    = findViewById(R.id.rbEntrant);
        rbOrganizer  = findViewById(R.id.rbOrganizer);
        rbAdmin      = findViewById(R.id.rbAdmin);
        btnContinue  = findViewById(R.id.btnContinue);
        btnProgress  = findViewById(R.id.btnProgress);


        cardEntrant.setOnClickListener(v -> selectRole("entrant"));
        cardOrganizer.setOnClickListener(v -> selectRole("organizer"));
        cardAdmin.setOnClickListener(v -> selectRole("admin"));

        btnContinue.setOnClickListener(v -> {
            if (selectedRole == null) {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            switch (selectedRole) {
                case "organizer":
                    openOrganizerFlow();
                    break;
                case "entrant":
                    openEntrantFlow();
                    break;
                case "admin":
                    openAdminFlow();
                    break;
            }
        });
    }

    /**
     * Updates the selected role and toggles the radio button visuals accordingly.
     *
     * @param role one of "entrant", "organizer", or "admin"
     */
    private void selectRole(String role) {
        selectedRole = role;

        // Uncheck all
        rbEntrant.setChecked(false);
        rbOrganizer.setChecked(false);
        rbAdmin.setChecked(false);

        // Reset card strokes
        cardEntrant.setCardBackgroundColor(0xFFFFFFFF);
        cardOrganizer.setCardBackgroundColor(0xFFFFFFFF);
        cardAdmin.setCardBackgroundColor(0xFFFFFFFF);

        // Check selected
        switch (role) {
            case "entrant":
                rbEntrant.setChecked(true);
                cardEntrant.setCardBackgroundColor(0xFFEEF2FF);
                break;
            case "organizer":
                rbOrganizer.setChecked(true);
                cardOrganizer.setCardBackgroundColor(0xFFEEF2FF);
                break;
            case "admin":
                rbAdmin.setChecked(true);
                cardAdmin.setCardBackgroundColor(0xFFEEF2FF);
                break;
        }
    }

    /**
     * Starts the entrant flow.
     *
     * <p>This method gets the device ID, validates it, checks whether
     * the entrant profile already exists in Firestore, and either routes
     * the user to onboarding or directly to the dashboard.</p>
     */
    private void openEntrantFlow() {
        String entrantId = DeviceIdProvider.getId(this);

        if (!DeviceIdProvider.isValidId(entrantId)) {
            Toast.makeText(this, "Failed to get device ID", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        new ProfileInitializer().checkEntrantProfileExists(
                entrantId,
                new ProfileInitializer.ExistsCallback() {
                    @Override
                    public void onResult(boolean exists) {
                        setLoading(false);
                        if (exists) {
                            startActivity(new Intent(SelectRoleActivity.this, EntrantDashboardActivity.class));
                        } else {
                            openProfileSetup("entrant", entrantId);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        setLoading(false);
                        Toast.makeText(SelectRoleActivity.this,
                                "Failed to check entrant profile",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Starts the organizer flow.
     *
     * <p>This method gets the device ID, validates it, checks whether
     * the organizer profile already exists in Firestore, and either routes
     * the user to onboarding or directly to the dashboard.</p>
     */
    private void openOrganizerFlow() {
        String organizerId = DeviceIdProvider.getId(this);

        if (!DeviceIdProvider.isValidId(organizerId)) {
            Toast.makeText(this, "Failed to get device ID", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        new ProfileInitializer().checkOrganizerProfileExists(
                organizerId,
                new ProfileInitializer.ExistsCallback() {
                    @Override
                    public void onResult(boolean exists) {
                        setLoading(false);
                        if (exists) {
                            startActivity(new Intent(SelectRoleActivity.this, OrganizerDashboardActivity.class));
                        } else {
                            openProfileSetup("organizer", organizerId);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        setLoading(false);
                        Toast.makeText(SelectRoleActivity.this,
                                "Failed to check organizer profile",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Starts the admin flow.
     *
     * <p>This method gets the device ID, validates it, checks whether
     * the admin profile already exists in Firestore, and either routes
     * the user to onboarding or shows the placeholder admin state.</p>
     */
    private void openAdminFlow() {
        String adminId = DeviceIdProvider.getId(this);

        if (!DeviceIdProvider.isValidId(adminId)) {
            Toast.makeText(this, "Failed to get device ID", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        new ProfileInitializer().checkAdminProfileExists(
                adminId,
                new ProfileInitializer.ExistsCallback() {
                    @Override
                    public void onResult(boolean exists) {
                        setLoading(false);
                        if (exists) {
                            Toast.makeText(SelectRoleActivity.this,
                                    "Admin dashboard coming soon",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            openProfileSetup("admin", adminId);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        setLoading(false);
                        Toast.makeText(SelectRoleActivity.this,
                                "Failed to check admin profile",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void openProfileSetup(String role, String profileId) {
        Intent intent = new Intent(this, ProfileSetupActivity.class);
        intent.putExtra(ProfileSetupActivity.EXTRA_ROLE, role);
        intent.putExtra(ProfileSetupActivity.EXTRA_PROFILE_ID, profileId);
        startActivity(intent);
    }

    private void setLoading(boolean isLoading) {
        btnContinue.setEnabled(!isLoading);
        btnContinue.setText(isLoading ? "" : "Continue");
        btnProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        cardEntrant.setEnabled(!isLoading);
        cardOrganizer.setEnabled(!isLoading);
        cardAdmin.setEnabled(!isLoading);
    }
}
