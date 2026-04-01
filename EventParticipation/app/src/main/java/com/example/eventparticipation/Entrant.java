package com.example.eventparticipation;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

/**
 * Model class representing a single entrant record stored in an event waitlist.
 *
 * <p>This class is used by Firestore object mapping and by organizer-facing UI screens
 * such as the entrant list and waitlist map. Each entrant may include profile information,
 * waitlist status, the location/address where they joined, and the timestamp of joining.</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 *     <li>US 02.02.01 - View entrants in the waiting list</li>
 *     <li>US 02.02.02 - View entrant join locations on a map</li>
 * </ul>
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

    /** Current waitlist status, for example waiting, selected, enrolled, or cancelled. */
    private String status;

    /** Human-readable address captured when the entrant joined the waitlist. */
    private String joinedAddress;

    /** Geographic coordinates captured when the entrant joined the waitlist. */
    private GeoPoint joinedLocation;

    /** Timestamp indicating when the entrant joined the waitlist. */
    private Date joinedAt;

    /** Flag indicating if the entrant has opted out of notifications. */
    private boolean optOutNotifications;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Entrant() {
    }

    /**
     * Creates a fully populated entrant object.
     *
     * @param entrantId unique identifier of the entrant
     * @param entrantName display name of the entrant
     * @param entrantEmail email address of the entrant
     * @param status current waitlist status
     * @param joinedAddress human-readable join address
     * @param joinedLocation geographic coordinates of the join location
     * @param joinedAt timestamp when the entrant joined the waitlist
     */
    public Entrant(String entrantId,
                   String entrantName,
                   String entrantEmail,
                   String status,
                   String joinedAddress,
                   GeoPoint joinedLocation,
                   Date joinedAt) {
        this.entrantId = entrantId;
        this.entrantName = entrantName;
        this.entrantEmail = entrantEmail;
        this.status = status;
        this.joinedAddress = joinedAddress;
        this.joinedLocation = joinedLocation;
        this.joinedAt = joinedAt;
    }

    /**
     * Returns the Firestore document id.
     *
     * @return waitlist document id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the Firestore document id.
     *
     * @param id waitlist document id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the entrant's unique id.
     *
     * @return entrant id
     */
    public String getEntrantId() {
        return entrantId;
    }

    /**
     * Sets the entrant id.
     *
     * @param entrantId entrant id
     */
    public void setEntrantId(String entrantId) {
        this.entrantId = entrantId;
    }

    /**
     * Returns the entrant's display name.
     *
     * @return entrant name
     */
    public String getEntrantName() {
        return entrantName;
    }

    /**
     * Sets the entrant display name.
     *
     * @param entrantName entrant name
     */
    public void setEntrantName(String entrantName) {
        this.entrantName = entrantName;
    }

    /**
     * Returns the entrant's email address.
     *
     * @return entrant email
     */
    public String getEntrantEmail() {
        return entrantEmail;
    }

    /**
     * Sets the entrant email address.
     *
     * @param entrantEmail entrant email
     */
    public void setEntrantEmail(String entrantEmail) {
        this.entrantEmail = entrantEmail;
    }

    /**
     * Returns the current waitlist status.
     *
     * @return status string
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the entrant waitlist status.
     *
     * @param status status string
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the join address recorded for the entrant.
     *
     * @return joined address
     */
    public String getJoinedAddress() {
        return joinedAddress;
    }

    /**
     * Sets the join address.
     *
     * @param joinedAddress join address
     */
    public void setJoinedAddress(String joinedAddress) {
        this.joinedAddress = joinedAddress;
    }

    /**
     * Returns the geographic location where the entrant joined.
     *
     * @return join location, or {@code null} if unavailable
     */
    public GeoPoint getJoinedLocation() {
        return joinedLocation;
    }

    /**
     * Sets the geographic join location.
     *
     * @param joinedLocation join coordinates
     */
    public void setJoinedLocation(GeoPoint joinedLocation) {
        this.joinedLocation = joinedLocation;
    }

    /**
     * Returns the timestamp when the entrant joined the waitlist.
     *
     * @return join timestamp, or {@code null} if unavailable
     */
    public Date getJoinedAt() {
        return joinedAt;
    }

    /**
     * Sets the join timestamp.
     *
     * @param joinedAt join time
     */
    public void setJoinedAt(Date joinedAt) {
        this.joinedAt = joinedAt;
    }

    /**
     * Indicates whether this entrant has a valid stored location for map display.
     *
     * @return {@code true} if {@link #joinedLocation} is not null; otherwise {@code false}
     */
    public boolean hasLocation() {
        return joinedLocation != null;
    }

    /**
     * Returns a concise debug string for logs.
     *
     * @return debug representation of the entrant
     */
    @Override
    public String toString() {
        return "Entrant{" +
                "entrantId='" + entrantId + '\'' +
                ", entrantName='" + entrantName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    /**
     * Checks if the entrant opted out of notifications.
     * @return true if opted out, false otherwise.
     */
    public boolean isOptOutNotifications() {
        return optOutNotifications;
    }

    /**
     * Sets the entrant's notification preference.
     * @param optOutNotifications true to opt out, false to receive.
     */
    public void setOptOutNotifications(boolean optOutNotifications) {
        this.optOutNotifications = optOutNotifications;
    }
}