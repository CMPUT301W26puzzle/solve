package com.example.eventparticipation;

import java.util.Date;

/**
 * model class representing an event with lottery and requirement settings
 */
public class Event {
    private String id;
    private String organizerId;
    private String name;
    private Date startTime;
    private int capacity;

    // registration period start and end dates
    private Date registrationStart;
    private Date registrationEnd;

    // cloud storage link for the event poster
    private String posterUrl;

    // flag to require user location to join
    private boolean geolocationRequired;

    // optional cap on the number of waiting entrants
    private Integer waitlistLimit;

    public Event() {}

    // standard getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // added registration period accessors
    public Date getRegistrationStart() { return registrationStart; }
    public void setRegistrationStart(Date registrationStart) { this.registrationStart = registrationStart; }

    public Date getRegistrationEnd() { return registrationEnd; }
    public void setRegistrationEnd(Date registrationEnd) { this.registrationEnd = registrationEnd; }

    // added geolocation toggle accessors
    public boolean isGeolocationRequired() { return geolocationRequired; }
    public void setGeolocationRequired(boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }

    // added waitlist limit accessors
    public Integer getWaitlistLimit() { return waitlistLimit; }
    public void setWaitlistLimit(Integer waitlistLimit) { this.waitlistLimit = waitlistLimit; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
}