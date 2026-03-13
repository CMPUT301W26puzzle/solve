package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for the Event model class.
 */
public class EventModelTest {

    @Test
    public void setAndGetId_returnsSameValue() {
        Event event = new Event();
        event.setId("event_001");
        assertEquals("event_001", event.getId());
    }

    @Test
    public void setAndGetName_returnsSameValue() {
        Event event = new Event();
        event.setName("Tech Conference");
        assertEquals("Tech Conference", event.getName());
    }

    @Test
    public void setAndGetCapacity_returnsSameValue() {
        Event event = new Event();
        event.setCapacity(100);
        assertEquals(100, event.getCapacity());
    }

    @Test
    public void setAndGetPosterUrl_returnsSameValue() {
        Event event = new Event();
        event.setPosterUrl("https://example.com/poster.jpg");
        assertEquals("https://example.com/poster.jpg", event.getPosterUrl());
    }

    @Test
    public void setAndGetStartTime_returnsSameValue() {
        Event event = new Event();
        Date date = new Date();
        event.setStartTime(date);
        assertEquals(date, event.getStartTime());
    }

    @Test
    public void newEvent_posterUrlIsNullByDefault() {
        Event event = new Event();
        assertNull(event.getPosterUrl());
    }
}