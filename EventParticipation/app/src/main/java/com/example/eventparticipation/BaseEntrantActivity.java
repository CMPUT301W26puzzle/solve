package com.example.eventparticipation;

import android.content.Intent;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Base activity for entrant screens with shared bottom navigation behavior.
 */
public abstract class BaseEntrantActivity extends AppCompatActivity {

    /**
     * Sets up the bottom navigation and highlights the currently selected item.
     *
     * @param selectedItemId the menu item ID of the current screen
     */
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
                openEntrantScreen(EntrantQRCodeActivity.class);
                return true;
            } else if (id == R.id.nav_notifications) {
                openEntrantScreen(EntrantNotificationsActivity.class);
                return true;
            }

            return false;
        });
    }

    /**
     * Opens the selected entrant screen and closes the current one.
     *
     * @param targetActivity the activity class to open
     */
    private void openEntrantScreen(Class<?> targetActivity) {
        if (this.getClass().equals(targetActivity)) {
            return;
        }

        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
        finish();
    }
}