package com.example.eventparticipation;

/** Wrapper used by the admin event browse list. */
public class AdminEventItem {
    private final Event event;

    public AdminEventItem(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }
}