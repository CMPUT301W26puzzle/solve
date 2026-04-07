package com.example.eventparticipation.universal;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages local user sessions using SharedPreferences.
 * Replaces DeviceIdProvider for universal password authentication.
 */
public class SessionManager {
    private static final String PREF_NAME = "EventAppSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE = "user_role";

    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static SessionManager getInstance(Context context) {
        return new SessionManager(context);
    }

    public void saveSession(String userId, String role) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_ROLE, role)
                .apply();
    }

    public void logout() {
        prefs.edit().clear().apply();
    }

    /**
     * Helper method to clear the session. Functionally identical to logout().
     * Crucial for test teardowns!
     */
    public void clearSession() {
        logout();
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public boolean isLoggedIn() {
        return getUserId() != null;
    }

}