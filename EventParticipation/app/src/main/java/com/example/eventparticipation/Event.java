package com.example.eventparticipation;

import java.util.Date;

/**
 * Model class representing an event with lottery and requirement settings.
 *
 * <p>Acts as the primary Data Transfer Object (DTO) for Firebase Firestore,
 * encapsulating all details of an event including its waitlist restrictions,
 * promotional materials, and schedule.</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 * <li>US 01.01.03 - View a list of events</li>
 * <li>US 02.01.01 - Create a new event</li>
 * <li>US 02.01.04 - Set a registration period</li>
 * <li>US 02.03.01 - Optionally limit waitlist capacity</li>
 * </ul>
 */
public class Event {

    /** The unique Firestore document ID for this event. */
    private String id;

    /** The ID of the facility where this event is being hosted. */
    private String facilityId;

    /** The unique identifier of the organizer who created the event. */
    private String organizerId;

    /** The display name or title of the event. */
    private String name;

    /** The date and time when the event is scheduled to start. */
    private Date startTime;

    /** The maximum number of attendees allowed to enroll in the event. */
    private int capacity;

    /** The date and time when the waitlist registration period opens. */
    private Date registrationStart;

    /** The date and time when the waitlist registration period closes. */
    private Date registrationEnd;

    /** The cloud storage download URL for the event's promotional poster image. */
    private String posterUrl;

    /** Flag indicating whether an entrant must provide their geolocation to join the waitlist. */
    private boolean geolocationRequired;

    /** An optional cap on the maximum number of entrants allowed on the waitlist. Null if unlimited. */
    private Integer waitlistLimit;

    /** The physical address or location name of the event venue. */
    private String venueAddress;

    /** The current number of entrants who have been successfully enrolled in the event. */
    private int enrolledCount;

    /** The current number of entrants waiting on the waitlist. */
    private int waitingCount;

    /** The count of entrants currently selected by the lottery. */
    private int selectedCount;

    /** The latitude of the event venue. */
    private Double venueLat;

    /** The longitude of the event venue.  */
    private Double venueLng;

    /** * Default constructor required for Firebase Firestore object mapping.
     */
    public Event() {}

    /**
     * Retrieves the unique event ID.
     *
     * @return The unique Firestore document ID of the event.
     */
    public String getId() { return id; }

    /**
     * Sets the unique event ID.
     *
     * @param id The unique Firestore document ID to set.
     */
    public void setId(String id) { this.id = id; }

    /**
     * Retrieves the facility ID associated with this event.
     *
     * @return The facility ID.
     */
    public String getFacilityId() { return facilityId; }

    /**
     * Sets the facility ID for this event.
     *
     * @param facilityId The facility ID to set.
     */
    public void setFacilityId(String facilityId) { this.facilityId = facilityId; }

    /**
     * Retrieves the ID of the organizer who created the event.
     *
     * @return The organizer's ID.
     */
    public String getOrganizerId() { return organizerId; }

    /**
     * Sets the ID of the organizer for this event.
     *
     * @param organizerId The organizer's ID to set.
     */
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    /**
     * Retrieves the name or title of the event.
     *
     * @return The event name.
     */
    public String getName() { return name; }

    /**
     * Sets the name or title of the event.
     *
     * @param name The event name to set.
     */
    public void setName(String name) { this.name = name; }

    /**
     * Retrieves the scheduled start time of the event.
     *
     * @return The event's start time as a {@link Date} object.
     */
    public Date getStartTime() { return startTime; }

    /**
     * Sets the scheduled start time of the event.
     *
     * @param startTime The start time to set.
     */
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    /**
     * Retrieves the opening date and time for event registration.
     *
     * @return The registration start date.
     */
    public Date getRegistrationStart() { return registrationStart; }

    /**
     * Sets the opening date and time for event registration.
     *
     * @param registrationStart The registration start date to set.
     */
    public void setRegistrationStart(Date registrationStart) { this.registrationStart = registrationStart; }

    /**
     * Retrieves the closing date and time for event registration.
     *
     * @return The registration end date.
     */
    public Date getRegistrationEnd() { return registrationEnd; }

    /**
     * Sets the closing date and time for event registration.
     *
     * @param registrationEnd The registration end date to set.
     */
    public void setRegistrationEnd(Date registrationEnd) { this.registrationEnd = registrationEnd; }

    /**
     * Checks whether geolocation is required to join the event's waitlist.
     *
     * @return {@code true} if geolocation is required, {@code false} otherwise.
     */
    public boolean isGeolocationRequired() { return geolocationRequired; }

    /**
     * Sets the geolocation requirement for joining the waitlist.
     *
     * @param geolocationRequired {@code true} to require geolocation, {@code false} otherwise.
     */
    public void setGeolocationRequired(boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }

    /**
     * Retrieves the optional limit on the number of entrants allowed on the waitlist.
     *
     * @return The waitlist limit, or {@code null} if there is no limit.
     */
    public Integer getWaitlistLimit() { return waitlistLimit; }

    /**
     * Sets the limit on the number of entrants allowed on the waitlist.
     *
     * @param waitlistLimit The maximum number of entrants, or {@code null} for unlimited.
     */
    public void setWaitlistLimit(Integer waitlistLimit) { this.waitlistLimit = waitlistLimit; }

    /**
     * Retrieves the download URL for the event's promotional poster.
     *
     * @return The poster URL as a String.
     */
    public String getPosterUrl() { return posterUrl; }

    /**
     * Sets the download URL for the event's promotional poster.
     *
     * @param posterUrl The poster URL to set.
     */
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    /**
     * Retrieves the maximum attendee capacity for the event.
     *
     * @return The event capacity.
     */
    public int getCapacity() { return capacity; }

    /**
     * Sets the maximum attendee capacity for the event.
     *
     * @param capacity The event capacity to set.
     */
    public void setCapacity(int capacity) { this.capacity = capacity; }

    /**
     * Retrieves the physical address or location name of the venue.
     *
     * @return The venue address.
     */
    public String getVenueAddress() { return venueAddress; }

    /**
     * Sets the physical address or location name of the venue.
     *
     * @param venueAddress The venue address to set.
     */
    public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }

    /**
     * Retrieves the number of entrants currently enrolled in the event.
     *
     * @return The count of enrolled entrants.
     */
    public int getEnrolledCount() { return enrolledCount;  }

    /**
     * Sets the number of entrants currently enrolled in the event.
     *
     * @param enrolledCount The new enrolled count.
     */
    public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }

    /**
     * Retrieves the number of entrants currently waiting on the waitlist.
     *
     * @return The count of waiting entrants.
     */
    public int getWaitingCount() { return waitingCount; }

    /**
     * Sets the number of entrants currently waiting on the waitlist.
     *
     * @param waitingCount The new waitlist count.
     */
    public void setWaitingCount(int waitingCount) { this.waitingCount = waitingCount; }

    /**
     * Retrieves the number of entrants currently selected from the waitlist.
     *
     * @return The count of selected entrants.
     */
    public int getSelectedCount() { return selectedCount; }

    /**
     * Sets the number of entrants currently selected for the event.
     *
     * @param selectedCount The new selected count.
     */
    public void setSelectedCount(int selectedCount) { this.selectedCount = selectedCount; }

    /**
     * Retrieves the venue latitude.
     *
     * @return The venue latitude.
     */
    public Double getVenueLat() { return venueLat; }

    /**
     * Sets the event latitude.
     *
     * @param venueLat The venue latitude.
     */
    public void setVenueLat(Double venueLat) { this.venueLat = venueLat; }

    /**
     * Retrieves the venue longitude.
     *
     * @return The venue longitude.
     */
    public Double getVenueLng() { return venueLng; }

    /**
     * Sets the event longitude.
     *
     * @param venueLng The venue longitude.
     */
    public void setVenueLng(Double venueLng) { this.venueLng = venueLng; }
}