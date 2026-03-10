package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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
                    startActivity(new Intent(this, OrganizerDashboardActivity.class));
                    break;
                case "entrant":
                    // TODO: replace with EntrantDashboardActivity when built
                    Toast.makeText(this, "Entrant dashboard coming soon", Toast.LENGTH_SHORT).show();
                    break;
                case "admin":
                    // TODO: replace with AdminDashboardActivity when built
                    Toast.makeText(this, "Admin dashboard coming soon", Toast.LENGTH_SHORT).show();
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
}
