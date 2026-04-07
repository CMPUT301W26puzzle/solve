package com.example.eventparticipation.universal;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for checking whether a profile exists in Firestore
 * and creating it if it does not.
 *
 * <p>This is used when a user enters the app under a specific role.</p>
 *
 * <p>Relevant user story:</p>
 * <ul>
 *     <li>US 01.07.01 - As an entrant, I want to be identified by my device so I don’t need a username and password.</li>
 * </ul>
 */
public class ProfileInitializer {
    private final FirebaseFirestore db;

    /**
     * Callback used when checking whether a profile already exists.
     */
    public interface ExistsCallback {
        void onResult(boolean exists);
        void onError(Exception e);
    }

    /**
     * Callback used after checking or creating a profile.
     */
    public interface Callback {
        /**
         * Called when the profile already exists or was created successfully.
         */
        void onSuccess();
        /**
         * Called if the profile check or creation fails.
         *
         * @param e the error that occurred
         */
        void onError(Exception e);
    }

    /**
     * Creates a new ProfileInitializer with a Firestore instance.
     */
    public ProfileInitializer() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Ensures that an entrant profile exists in Firestore.
     *
     * @param entrantId the entrant device ID
     * @param callback callback for success or failure
     */
    public void ensureEntrantProfileExists(String entrantId, Callback callback) {
        ensureProfileExists("entrants", "entrantId", entrantId, "entrant", callback);
    }

    /**
     * Ensures that an organizer profile exists in Firestore.
     *
     * @param organizerId the organizer ID
     * @param callback callback for success or failure
     */
    public void ensureOrganizerProfileExists(String organizerId, Callback callback) {
        ensureProfileExists("organizers", "organizerId", organizerId, "organizer", callback);
    }

    /**
     * Ensures that an admin profile exists in Firestore.
     *
     * @param adminId the admin ID
     * @param callback callback for success or failure
     */
    public void ensureAdminProfileExists(String adminId, Callback callback) {
        ensureProfileExists("admins", "adminId", adminId, "admin", callback);
    }

    /**
     * Checks whether an entrant profile already exists in Firestore.
     *
     * @param entrantId the entrant device ID
     * @param callback callback receiving the existence result
     */
    public void checkEntrantProfileExists(String entrantId, ExistsCallback callback) {
        checkProfileExists("entrants", entrantId, callback);
    }

    /**
     * Checks whether an organizer profile already exists in Firestore.
     *
     * @param organizerId the organizer device ID
     * @param callback callback receiving the existence result
     */
    public void checkOrganizerProfileExists(String organizerId, ExistsCallback callback) {
        checkProfileExists("organizers", organizerId, callback);
    }

    /**
     * Checks whether an admin profile already exists in Firestore.
     *
     * @param adminId the admin device ID
     * @param callback callback receiving the existence result
     */
    public void checkAdminProfileExists(String adminId, ExistsCallback callback) {
        checkProfileExists("admins", adminId, callback);
    }

    /**
     * Checks whether a profile exists in the given collection.
     * If it does not exist, a new blank profile is created.
     *
     * @param collection the Firestore collection name
     * @param idField the name of the ID field stored in the profile
     * @param id the profile ID
     * @param role the role name
     * @param callback callback for success or failure
     */
    private void ensureProfileExists(String collection, String idField, String id, String role, Callback callback) {
        db.collection(collection)
                .document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onSuccess();
                        return;
                    }
                    db.collection(collection)
                            .document(id)
                            .set(createProfile(idField, id, role))
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Deletes a profile from firestore collection.
     *
     * @param collection the Firestore collection name
     * @param id the profile ID
     * @param callback callback for success or failure
     */
    public void deleteProfile(String collection, String id, Callback callback) {
        db.collection(collection)
                .document(id)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Checks whether a profile document exists without creating it.
     *
     * @param collection the Firestore collection name
     * @param id the profile ID
     * @param callback callback receiving the existence result
     */
    private void checkProfileExists(String collection, String id, ExistsCallback callback) {
        db.collection(collection)
                .document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> callback.onResult(documentSnapshot.exists()))
                .addOnFailureListener(callback::onError);
    }

    /**
     * Creates a blank profile map for a specific role.
     *
     * @param idField the name of the ID field
     * @param id the profile ID
     * @param role the role name
     * @return a map containing the default profile fields
     */
    private Map<String, Object> createProfile(String idField, String id, String role) {
        Map<String, Object> profile = new HashMap<>();
        profile.put(idField, id);
        profile.put("role", role);
        profile.put("name", "");
        profile.put("email", "");
        profile.put("phone", "");
        return profile;
    }
}
