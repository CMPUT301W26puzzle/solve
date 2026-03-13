package com.example.eventparticipation;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseEntrantActivity extends AppCompatActivity {

    protected void setupBottomNav(@IdRes int selectedItemId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav == null) {
            return;
        }

        bottomNav.setSelectedItemId(selectedItemId);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == selectedItemId) {
                return true;
            }

            if (id == R.id.nav_home) {
                openEntrantScreen(EntrantDashboardActivity.class);
                return true;
            } else if (id == R.id.nav_my_events) {
                openEntrantScreen(EntrantMyEventsActivity.class);
                return true;
            } else if (id == R.id.nav_profile) {
                openEntrantScreen(ProfileActivity.class);
                return true;
            } else if (id == R.id.nav_scan) {
                Toast.makeText(this, "Scan coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_notifications) {
                Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }

    private void openEntrantScreen(Class<?> targetActivity) {
        if (this.getClass().equals(targetActivity)) {
            return;
        }

        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
        finish();
    }
}