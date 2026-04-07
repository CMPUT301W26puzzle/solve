package com.example.eventparticipation.admin;

import com.example.eventparticipation.universal.Event;

/**
 * Data model representing an entity formatted for display and moderation
 * within the Admin Dashboard.
 */public class AdminEventItem {
    private final Event event;

    public AdminEventItem(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }
}