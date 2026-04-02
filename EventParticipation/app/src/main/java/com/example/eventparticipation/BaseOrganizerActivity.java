package com.example.eventparticipation;

import android.content.Intent;

import androidx.annotation.IdRes;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Base activity for organizer screens with shared bottom navigation behavior.
 *
 * <p>This extends {@link BaseEntrantActivity} so shared profile screens can opt into
 * either entrant or organizer navigation without duplicating helper logic.</p>
 */
public abstract class BaseOrganizerActivity extends BaseEntrantActivity {

    /**
     * Sets up the organizer bottom navigation and highlights the current destination.
     *
     * @param selectedItemId the menu item ID of the current screen
     */
    protected void setupOrganizerBottomNav(@IdRes int selectedItemId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav == null) {
            return;
        }

        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.organizer_bottom_nav);
        bottomNav.setSelectedItemId(selectedItemId);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == selectedItemId) {
                return true;
            }

            if (id == R.id.nav_dashboard) {
                openOrganizerScreen(OrganizerDashboardActivity.class);
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra(ProfileActivity.EXTRA_ROLE, "organizer");
                intent.putExtra(ProfileActivity.EXTRA_PROFILE_ID, DeviceIdProvider.getId(this));
                startActivity(intent);
                finish();
                return true;
            }

            return false;
        });
    }

    /**
     * Opens the selected organizer screen and closes the current one.
     *
     * @param targetActivity the activity class to open
     */
    private void openOrganizerScreen(Class<?> targetActivity) {
        if (this.getClass().equals(targetActivity)) {
            return;
        }

        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
        finish();
    }
}
