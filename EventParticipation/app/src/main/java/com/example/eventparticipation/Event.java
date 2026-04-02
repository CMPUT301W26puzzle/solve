package com.example.eventparticipation;

import java.util.Date;

/**
 * Model class representing an event stored in Firestore.
 *
 * <p>This class acts as the primary Data Transfer Object (DTO) for top-level event
 * documents in the {@code events} collection. It contains event metadata, venue data,
 * registration period fields, and derived waitlist counts used by dashboard cards.</p>
 *
 * <p>Relevant derived count fields:
 * <ul>
 *     <li>{@code waitingCount}: entrants still waiting on the waitlist</li>
 *     <li>{@code selectedCount}: entrants selected but not yet enrolled</li>
 *     <li>{@code enrolledCount}: entrants who completed enrollment</li>
 * </ul>
 */
public class Event {

    /** Unique Firestore document ID for this event. */
    private String id;

    /** Facility ID associated with this event, if any. */
    private String facilityId;

    /** Organizer ID who owns the event. */
    private String organizerId;

    /** Human-readable event name. */
    private String name;

    /** Event start time. */
    private Date startTime;

    /** Maximum number of attendees who can enroll in the event. */
    private int capacity;

    /** Registration start time for the waitlist. */
    private Date registrationStart;

    /** Registration end time for the waitlist. */
    private Date registrationEnd;

    /** Download URL of the poster image stored in Firebase Storage. */
    private String posterUrl;

    /** Whether geolocation is required to join the waitlist. */
    private boolean geolocationRequired;

    /** Optional waitlist size limit. Null means unlimited. */
    private Integer waitlistLimit;

    /** Venue display address. */
    private String venueAddress;

    /** Number of entrants currently enrolled in the event. */
    private int enrolledCount;

    /** Number of entrants still waiting on the waitlist. */
    private int waitingCount;

    /** Number of entrants currently selected but not yet enrolled. */
    private int selectedCount;

    /** Venue latitude. */
    private Double venueLat;

    /** Venue longitude. */
    private Double venueLng;

    /**
     * Required empty constructor for Firestore object mapping.
     */
    public Event() {
    }

    /**
     * Returns the Firestore document ID.
     *
     * @return event ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the Firestore document ID.
     *
     * @param id event ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the facility ID.
     *
     * @return facility ID
     */
    public String getFacilityId() {
        return facilityId;
    }

    /**
     * Sets the facility ID.
     *
     * @param facilityId facility ID
     */
    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    /**
     * Returns the organizer ID.
     *
     * @return organizer ID
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Sets the organizer ID.
     *
     * @param organizerId organizer ID
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Returns the event name.
     *
     * @return event name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the event name.
     *
     * @param name event name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the event start time.
     *
     * @return start time
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Sets the event start time.
     *
     * @param startTime start time
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the event capacity.
     *
     * @return capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Sets the event capacity.
     *
     * @param capacity capacity
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Returns the registration start time.
     *
     * @return registration start
     */
    public Date getRegistrationStart() {
        return registrationStart;
    }

    /**
     * Sets the registration start time.
     *
     * @param registrationStart registration start
     */
    public void setRegistrationStart(Date registrationStart) {
        this.registrationStart = registrationStart;
    }

    /**
     * Returns the registration end time.
     *
     * @return registration end
     */
    public Date getRegistrationEnd() {
        return registrationEnd;
    }

    /**
     * Sets the registration end time.
     *
     * @param registrationEnd registration end
     */
    public void setRegistrationEnd(Date registrationEnd) {
        this.registrationEnd = registrationEnd;
    }

    /**
     * Returns the poster URL.
     *
     * @return poster URL
     */
    public String getPosterUrl() {
        return posterUrl;
    }

    /**
     * Sets the poster URL.
     *
     * @param posterUrl poster URL
     */
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    /**
     * Returns whether geolocation is required.
     *
     * @return true if required, false otherwise
     */
    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    /**
     * Sets whether geolocation is required.
     *
     * @param geolocationRequired whether geolocation is required
     */
    public void setGeolocationRequired(boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
    }

    /**
     * Returns the optional waitlist limit.
     *
     * @return waitlist limit, or null if unlimited
     */
    public Integer getWaitlistLimit() {
        return waitlistLimit;
    }

    /**
     * Sets the optional waitlist limit.
     *
     * @param waitlistLimit waitlist limit
     */
    public void setWaitlistLimit(Integer waitlistLimit) {
        this.waitlistLimit = waitlistLimit;
    }

    /**
     * Returns the venue address.
     *
     * @return venue address
     */
    public String getVenueAddress() {
        return venueAddress;
    }

    /**
     * Sets the venue address.
     *
     * @param venueAddress venue address
     */
    public void setVenueAddress(String venueAddress) {
        this.venueAddress = venueAddress;
    }

    /**
     * Returns the enrolled count.
     *
     * @return enrolled count
     */
    public int getEnrolledCount() {
        return enrolledCount;
    }

    /**
     * Sets the enrolled count.
     *
     * @param enrolledCount enrolled count
     */
    public void setEnrolledCount(int enrolledCount) {
        this.enrolledCount = enrolledCount;
    }

    /**
     * Returns the waiting count.
     *
     * @return waiting count
     */
    public int getWaitingCount() {
        return waitingCount;
    }

    /**
     * Sets the waiting count.
     *
     * @param waitingCount waiting count
     */
    public void setWaitingCount(int waitingCount) {
        this.waitingCount = waitingCount;
    }

    /**
     * Returns the selected count.
     *
     * @return selected count
     */
    public int getSelectedCount() {
        return selectedCount;
    }

    /**
     * Sets the selected count.
     *
     * @param selectedCount selected count
     */
    public void setSelectedCount(int selectedCount) {
        this.selectedCount = selectedCount;
    }

    /**
     * Returns the venue latitude.
     *
     * @return venue latitude
     */
    public Double getVenueLat() {
        return venueLat;
    }

    /**
     * Sets the venue latitude.
     *
     * @param venueLat venue latitude
     */
    public void setVenueLat(Double venueLat) {
        this.venueLat = venueLat;
    }

    /**
     * Returns the venue longitude.
     *
     * @return venue longitude
     */
    public Double getVenueLng() {
        return venueLng;
    }

    /**
     * Sets the venue longitude.
     *
     * @param venueLng venue longitude
     */
    public void setVenueLng(Double venueLng) {
        this.venueLng = venueLng;
    }
}