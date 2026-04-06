package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Entry point Activity where users select their intended app role.
 * Entrants use passwordless Device ID authentication, while Organizers
 * and Admins use standard Email/Password authentication.
 */
public class SelectRoleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Check if user is already logged in!
        SessionManager session = SessionManager.getInstance(this);

        if (session.isLoggedIn()) {
            String userId = session.getUserId();
            String role = session.getRole();

            FirebaseFirestore.getInstance()
                    .collection(getCollection(role))
                    .document(userId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            // Fix for stale sessions
                            session.logout();
                            setContentView(R.layout.activity_select_role);
                            setupRoleButtons();
                        } else {
                            routeToDashboard(role);
                        }
                    })
                    .addOnFailureListener(e -> {
                        session.logout();
                        setContentView(R.layout.activity_select_role);
                        setupRoleButtons();
                    });
            return;
        }

        // 2. If not logged in, show the role selection screen
        setContentView(R.layout.activity_select_role);
        setupRoleButtons();
    }

    private void setupRoleButtons() {
        android.view.View btnEntrant = findViewById(R.id.btnEntrant);
        android.view.View btnOrganizer = findViewById(R.id.btnOrganizer);
        android.view.View btnAdmin = findViewById(R.id.btnAdmin);

        btnEntrant.setOnClickListener(v -> handleEntrantLogin());
        btnOrganizer.setOnClickListener(v -> goToAuth("organizer"));
        btnAdmin.setOnClickListener(v -> goToAuth("admin"));
    }
    /**
     * Executes the passwordless device ID flow for Entrants.
     * Checks if their hardware ID exists in Firestore. If yes, it logs them in directly.
     * If no, it routes them to the Profile Setup screen to collect their basic info.
     */
    private void handleEntrantLogin() {
        String deviceId = DeviceIdProvider.getId(this);

        if (!DeviceIdProvider.isValidId(deviceId)) {
            android.widget.Toast.makeText(this, "Failed to get Device ID", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        ProfileInitializer initializer = new ProfileInitializer();
        initializer.checkEntrantProfileExists(deviceId, new ProfileInitializer.ExistsCallback() {
            @Override
            public void onResult(boolean exists) {
                if (exists) {
                    // Profile exists! Save session and go straight to dashboard.
                    SessionManager.getInstance(SelectRoleActivity.this).saveSession(deviceId, "entrant");
                    routeToDashboard("entrant");
                } else {
                    // First time user. Send to Profile Setup.
                    Intent intent = new Intent(SelectRoleActivity.this, ProfileSetupActivity.class);
                    intent.putExtra(ProfileSetupActivity.EXTRA_ROLE, "entrant");
                    intent.putExtra(ProfileSetupActivity.EXTRA_PROFILE_ID, deviceId);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onError(Exception e) {
                android.widget.Toast.makeText(SelectRoleActivity.this, "Network error. Try again.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToAuth(String role) {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra("ROLE", role);
        startActivity(intent);
    }

    private String getCollection(String role) {
        switch (role) {
            case "organizer": return "organizers";
            case "admin": return "admins";
            default: return "entrants";
        }
    }

    private void routeToDashboard(String role) {
        Intent intent;
        if ("organizer".equals(role)) {
            intent = new Intent(this, OrganizerDashboardActivity.class);
        } else if ("admin".equals(role)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(this, EntrantDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }
}