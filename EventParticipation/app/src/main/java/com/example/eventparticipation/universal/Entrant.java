package com.example.eventparticipation.universal;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

/**
 * Model class representing a single entrant record stored in an event waitlist.
 *
 * Status design:
 * - selectionStatus: waiting / selected / cancelled
 * - responseStatus: pending / accepted / declined
 * - finalStatus: enrolled
 */
public class Entrant {

    /** Firestore document id for this waitlist entry. */
    private String id;

    /** Unique identifier of the entrant user. */
    private String entrantId;

    /** Display name of the entrant. */
    private String entrantName;

    /** Email address of the entrant. */
    private String entrantEmail;

    /**
     * Legacy status field kept temporarily for backward compatibility.
     * Old values might be: waiting, selected, enrolled, cancelled.
     */
    private String status;

    /** Selection layer status: waiting / selected / cancelled */
    private String selectionStatus;

    /** Response layer status for selected entrants: pending / accepted / declined */
    private String responseStatus;

    /** Final layer status: enrolled */
    private String finalStatus;

    /** Human-readable address captured when the entrant joined the waitlist. */
    private String joinedAddress;

    /** Geographic coordinates captured when the entrant joined the waitlist. */
    private GeoPoint joinedLocation;

    /** Timestamp indicating when the entrant joined the waitlist. */
    private Date joinedAt;

    /** Timestamp indicating when the entrant was selected. */
    private Date selectedAt;

    /** Timestamp indicating when the entrant responded to the selection. */
    private Date respondedAt;

    /** Timestamp indicating when the entrant was enrolled. */
    private Date enrolledAt;

    /** Timestamp indicating when the entrant was cancelled. */
    private Date cancelledAt;

    /** Flag indicating if the entrant has opted out of notifications. */
    private boolean optOutNotifications;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Entrant() {
    }

    /**
     * Basic constructor.
     */
    public Entrant(String entrantId,
                   String entrantName,
                   String entrantEmail,
                   String selectionStatus,
                   String responseStatus,
                   String finalStatus,
                   String joinedAddress,
                   GeoPoint joinedLocation,
                   Date joinedAt) {
        this.entrantId = entrantId;
        this.entrantName = entrantName;
        this.entrantEmail = entrantEmail;
        this.selectionStatus = selectionStatus;
        this.responseStatus = responseStatus;
        this.finalStatus = finalStatus;
        this.joinedAddress = joinedAddress;
        this.joinedLocation = joinedLocation;
        this.joinedAt = joinedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntrantId() {
        return entrantId;
    }

    public void setEntrantId(String entrantId) {
        this.entrantId = entrantId;
    }

    public String getEntrantName() {
        return entrantName;
    }

    public void setEntrantName(String entrantName) {
        this.entrantName = entrantName;
    }

    public String getEntrantEmail() {
        return entrantEmail;
    }

    public void setEntrantEmail(String entrantEmail) {
        this.entrantEmail = entrantEmail;
    }

    /**
     * Legacy getter. Prefer using getSelectionStatus(), getResponseStatus(), getFinalStatus().
     */
    public String getStatus() {
        return status;
    }

    /**
     * Legacy setter. Prefer using setSelectionStatus(), setResponseStatus(), setFinalStatus().
     */
    public void setStatus(String status) {
        this.status = status;
    }

    public String getSelectionStatus() {
        // New field first
        if (selectionStatus != null && !selectionStatus.isEmpty()) {
            return selectionStatus;
        }

        // Backward compatibility with old single-field status
        if ("waiting".equals(status)) return "waiting";
        if ("selected".equals(status)) return "selected";
        if ("cancelled".equals(status)) return "cancelled";
        if ("enrolled".equals(status)) return "selected"; // enrolled implies was selected

        return null;
    }

    public void setSelectionStatus(String selectionStatus) {
        this.selectionStatus = selectionStatus;
    }

    public String getResponseStatus() {
        // New field first
        if (responseStatus != null && !responseStatus.isEmpty()) {
            return responseStatus;
        }

        // Backward compatibility
        if ("selected".equals(status)) return "pending";
        if ("enrolled".equals(status)) return "accepted";

        return null;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getFinalStatus() {
        // New field first
        if (finalStatus != null && !finalStatus.isEmpty()) {
            return finalStatus;
        }

        // Backward compatibility
        if ("enrolled".equals(status)) return "enrolled";

        return null;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

    public String getJoinedAddress() {
        return joinedAddress;
    }

    public void setJoinedAddress(String joinedAddress) {
        this.joinedAddress = joinedAddress;
    }

    public GeoPoint getJoinedLocation() {
        return joinedLocation;
    }

    public void setJoinedLocation(GeoPoint joinedLocation) {
        this.joinedLocation = joinedLocation;
    }

    public Date getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Date joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Date getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(Date selectedAt) {
        this.selectedAt = selectedAt;
    }

    public Date getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Date respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Date getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(Date enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public boolean hasLocation() {
        return joinedLocation != null;
    }

    public boolean isOptOutNotifications() {
        return optOutNotifications;
    }

    public void setOptOutNotifications(boolean optOutNotifications) {
        this.optOutNotifications = optOutNotifications;
    }

    @Override
    public String toString() {
        return "Entrant{" +
                "entrantId='" + entrantId + '\'' +
                ", entrantName='" + entrantName + '\'' +
                ", selectionStatus='" + getSelectionStatus() + '\'' +
                ", responseStatus='" + getResponseStatus() + '\'' +
                ", finalStatus='" + getFinalStatus() + '\'' +
                '}';
    }
}