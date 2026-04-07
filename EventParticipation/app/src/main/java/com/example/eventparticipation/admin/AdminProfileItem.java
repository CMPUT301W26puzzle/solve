package com.example.eventparticipation.admin;

import com.google.firebase.firestore.DocumentSnapshot;

/** Model representing a user profile in the admin browse list. */
public class AdminProfileItem {
    private final String profileId;
    private final String role;
    private final String name;
    private final String email;

    public AdminProfileItem(String profileId, String role, String name, String email) {
        this.profileId = profileId;
        this.role = role;
        this.name = name;
        this.email = email;
    }

    public static AdminProfileItem fromDocument(DocumentSnapshot doc, String fallbackRole) {
        String role = doc.getString("role");
        if (role == null || role.trim().isEmpty()) {
            role = fallbackRole;
        }
        return new AdminProfileItem(
                doc.getId(),
                role,
                safe(doc.getString("name")),
                safe(doc.getString("email"))
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public String getProfileId() {
        return profileId;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name == null || name.isEmpty() ? "Unnamed" : name;
    }

    public String getEmail() {
        return email;
    }
}