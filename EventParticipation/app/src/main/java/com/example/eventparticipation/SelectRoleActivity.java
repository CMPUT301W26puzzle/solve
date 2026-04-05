package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class SelectRoleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Check if user is already logged in!
        SessionManager session = SessionManager.getInstance(this);
        if (session.isLoggedIn()) {
            routeToDashboard(session.getRole());
            return;
        }

        // 2. If not logged in, show the role selection screen
        setContentView(R.layout.activity_select_role);

        MaterialButton btnEntrant = findViewById(R.id.btnEntrant);
        MaterialButton btnOrganizer = findViewById(R.id.btnOrganizer);
        MaterialButton btnAdmin = findViewById(R.id.btnAdmin);

        // 3. Route all clicks to the new AuthActivity
        btnEntrant.setOnClickListener(v -> goToAuth("entrant"));
        btnOrganizer.setOnClickListener(v -> goToAuth("organizer"));
        btnAdmin.setOnClickListener(v -> goToAuth("admin"));
    }

    private void goToAuth(String role) {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra("ROLE", role);
        startActivity(intent);
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