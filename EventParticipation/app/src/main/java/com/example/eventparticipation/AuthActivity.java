package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthActivity extends AppCompatActivity {

    private TextView tvAuthTitle, tvAuthSubtitle;
    private LinearLayout layoutRegisterFields;
    private EditText etName, etPhone, etEmail, etPassword;
    private MaterialButton btnSubmitAuth, btnToggleMode;

    private boolean isLoginMode = true;
    private String role;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        db = FirebaseFirestore.getInstance();
        role = getIntent().getStringExtra("ROLE");
        if (role == null) role = "entrant"; // Default

        initViews();
        updateUI();

        btnToggleMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUI();
        });

        btnSubmitAuth.setOnClickListener(v -> {
            if (isLoginMode) performLogin();
            else performRegistration();
        });
    }

    private void initViews() {
        tvAuthTitle = findViewById(R.id.tvAuthTitle);
        tvAuthSubtitle = findViewById(R.id.tvAuthSubtitle);
        layoutRegisterFields = findViewById(R.id.layoutRegisterFields);
        etName = findViewById(R.id.etAuthName);
        etPhone = findViewById(R.id.etAuthPhone);
        etEmail = findViewById(R.id.etAuthEmail);
        etPassword = findViewById(R.id.etAuthPassword);
        btnSubmitAuth = findViewById(R.id.btnSubmitAuth);
        btnToggleMode = findViewById(R.id.btnToggleMode);
    }

    private void updateUI() {
        String displayRole = Character.toUpperCase(role.charAt(0)) + role.substring(1);

        if (isLoginMode) {
            tvAuthTitle.setText(displayRole + " Login");
            layoutRegisterFields.setVisibility(View.GONE);
            btnSubmitAuth.setText("Login");
            btnToggleMode.setText("Need an account? Register here");
        } else {
            tvAuthTitle.setText(displayRole + " Registration");
            layoutRegisterFields.setVisibility(View.VISIBLE);
            btnSubmitAuth.setText("Create Account");
            btnToggleMode.setText("Already have an account? Login here");
        }
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitAuth.setEnabled(false);

        db.collection(getCollectionName()).whereEqualTo("email", email).whereEqualTo("password", pass).limit(1).get()
                .addOnSuccessListener(query -> {
                    btnSubmitAuth.setEnabled(true);
                    if (!query.isEmpty()) {
                        String userId = query.getDocuments().get(0).getId();
                        SessionManager.getInstance(this).saveSession(userId, role);
                        routeToDashboard();
                    } else {
                        Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnSubmitAuth.setEnabled(true);
                    Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void performRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Name, Email, and Password required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitAuth.setEnabled(false);

        // Check if email already exists
        db.collection(getCollectionName()).whereEqualTo("email", email).limit(1).get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        btnSubmitAuth.setEnabled(true);
                        Toast.makeText(this, "An account with this email already exists", Toast.LENGTH_LONG).show();
                    } else {
                        createNewUser(name, email, pass, phone);
                    }
                });
    }

    private void createNewUser(String name, String email, String pass, String phone) {
        String newId = UUID.randomUUID().toString(); // Generate a secure unique ID

        Map<String, Object> user = new HashMap<>();
        user.put(getIdFieldName(), newId);
        user.put("name", name);
        user.put("email", email);
        user.put("password", pass); // Prototype: Storing as plain text
        user.put("phone", phone);
        user.put("role", role);

        db.collection(getCollectionName()).document(newId).set(user)
                .addOnSuccessListener(aVoid -> {
                    SessionManager.getInstance(this).saveSession(newId, role);
                    Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show();
                    routeToDashboard();
                })
                .addOnFailureListener(e -> {
                    btnSubmitAuth.setEnabled(true);
                    Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void routeToDashboard() {
        Intent intent;
        if ("organizer".equals(role)) {
            intent = new Intent(this, OrganizerDashboardActivity.class);
        } else if ("admin".equals(role)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(this, EntrantDashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getCollectionName() {
        if ("organizer".equals(role)) return "organizers";
        if ("admin".equals(role)) return "admins";
        return "entrants";
    }

    private String getIdFieldName() {
        if ("organizer".equals(role)) return "organizerId";
        if ("admin".equals(role)) return "adminId";
        return "entrantId";
    }
}