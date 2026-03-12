package com.example.eventparticipation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

/**
 * Utility class for getting and validating the device ID used by the app.
 *
 * <p>This ID is used to identify an entrant without requiring a username
 * and password.</p>
 *
 * <p>Relevant user story:</p>
 * <ul>
 *     <li>US 01.07.01 - As an entrant, I want to be identified by my device so I don’t need a username and password.</li>
 * </ul>
 */
public class DeviceIdProvider {

    /** Prevents creating an instance of this utility class. */
    private DeviceIdProvider() {}

    /**
     * Returns the device ID for this app.
     *
     * <p>If the ID cannot be retrieved, returns "unknown_device".</p>
     *
     * @param context context used to access the device settings
     * @return the Android ID, or "unknown_device" if unavailable
     */
    public static String getId(Context context) {
        @SuppressLint("HardwareIds") String id = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        if (id == null || id.trim().isEmpty()) {
            return "unknown_device";
        }

        return id;
    }

    /**
     * Checks whether the given device ID is valid.
     *
     * @param id the device ID to check
     * @return true if the ID is not null, not empty, and not "unknown_device"
     */
    public static boolean isValidId(String id) {
        return id != null && !id.trim().isEmpty() && !"unknown_device".equals(id);
    }
}
