package com.example.eventparticipation;

import java.util.Date;

/**
 * Model class representing an event owned by an organizer.
 *
 * <p>This class is used by organizer dashboard and event management screens.
 * It stores core event information such as scheduling, capacity, counts, venue,
 * and poster image URL.</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 *     <li>US 02.04.01 - Upload event poster</li>
 *     <li>US 02.04.02 - Update event poster</li>
 * </ul>
 */
public class Event {

    /** Firestore document id of the event. */
    private String id;

    /** Organizer id that owns the event. */
    private String organizerId;

    /** Event name shown in organizer and entrant views. */
    private String name;

    /** Event start time. */
    private Date startTime;

    /** Maximum number of participants. */
    private int capacity;

    /** Registration closing time. */
    private Date registrationEnd;

    /** Download URL of the event poster image. */
    private String posterUrl;

    /** Number of entrants currently waiting. */
    private int waitingCount;

    /** Number of entrants currently selected. */
    private int selectedCount;

    /** Number of entrants currently enrolled. */
    private int enrolledCount;

    /** Venue address for the event. */
    private String venueAddress;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Event() {
        // Firestore needs empty constructor
    }

    /**
     * Creates a fully populated event instance.
     *
     * @param id event id
     * @param organizerId organizer id
     * @param name event name
     * @param startTime event start time
     * @param capacity event capacity
     * @param registrationEnd registration closing time
     * @param posterUrl poster download URL
     * @param waitingCount number of waiting entrants
     * @param selectedCount number of selected entrants
     * @param enrolledCount number of enrolled entrants
     * @param venueAddress venue address
     */
    public Event(String id,
                 String organizerId,
                 String name,
                 Date startTime,
                 int capacity,
                 Date registrationEnd,
                 String posterUrl,
                 int waitingCount,
                 int selectedCount,
                 int enrolledCount,
                 String venueAddress) {
        this.id = id;
        this.organizerId = organizerId;
        this.name = name;
        this.startTime = startTime;
        this.capacity = capacity;
        this.registrationEnd = registrationEnd;
        this.posterUrl = posterUrl;
        this.waitingCount = waitingCount;
        this.selectedCount = selectedCount;
        this.enrolledCount = enrolledCount;
        this.venueAddress = venueAddress;
    }

    /**
     * Returns the event id.
     *
     * @return event id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the event id.
     *
     * @param id event id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the organizer id.
     *
     * @return organizer id
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Sets the organizer id.
     *
     * @param organizerId organizer id
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
     * @return event start time
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Sets the event start time.
     *
     * @param startTime event start time
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the event capacity.
     *
     * @return event capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Sets the event capacity.
     *
     * @param capacity event capacity
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Returns the registration end time.
     *
     * @return registration end time
     */
    public Date getRegistrationEnd() {
        return registrationEnd;
    }

    /**
     * Sets the registration end time.
     *
     * @param registrationEnd registration end time
     */
    public void setRegistrationEnd(Date registrationEnd) {
        this.registrationEnd = registrationEnd;
    }

    /**
     * Returns the poster image URL.
     *
     * @return poster URL
     */
    public String getPosterUrl() {
        return posterUrl;
    }

    /**
     * Sets the poster image URL.
     *
     * @param posterUrl poster URL
     */
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    /**
     * Returns the number of waiting entrants.
     *
     * @return waiting count
     */
    public int getWaitingCount() {
        return waitingCount;
    }

    /**
     * Sets the number of waiting entrants.
     *
     * @param waitingCount waiting count
     */
    public void setWaitingCount(int waitingCount) {
        this.waitingCount = waitingCount;
    }

    /**
     * Returns the number of selected entrants.
     *
     * @return selected count
     */
    public int getSelectedCount() {
        return selectedCount;
    }

    /**
     * Sets the number of selected entrants.
     *
     * @param selectedCount selected count
     */
    public void setSelectedCount(int selectedCount) {
        this.selectedCount = selectedCount;
    }

    /**
     * Returns the number of enrolled entrants.
     *
     * @return enrolled count
     */
    public int getEnrolledCount() {
        return enrolledCount;
    }

    /**
     * Sets the number of enrolled entrants.
     *
     * @param enrolledCount enrolled count
     */
    public void setEnrolledCount(int enrolledCount) {
        this.enrolledCount = enrolledCount;
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
}
