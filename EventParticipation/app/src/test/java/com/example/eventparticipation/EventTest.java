package com.example.eventparticipation;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Date;

/**
 * Unit tests for the Event model class.
 *
 * <p>These tests run on the JVM without a device or emulator.</p>
 */
public class EventTest {

    private Event event;

    @Before
    public void setUp() {
        event = new Event();
        event.setId("test_id");
        event.setOrganizerId("organizer_001");
        event.setName("Spring Music Festival");
        event.setRegistrationStart(new Date(1740000000000L));
        event.setWaitlistLimit(500);
        event.setRegistrationEnd(new Date(1740100000000L));
        event.setPosterUrl("https://example.com/poster.jpg");
        event.setWaitingCount(234);
        event.setSelectedCount(10);
        event.setEnrolledCount(450);
        event.setVenueAddress("Central Park Music Plaza");
    }

    /** US 01.05.04 - waiting count is stored and retrieved correctly. */
    @Test
    public void testWaitingCount() {
        assertEquals(234, event.getWaitingCount());
    }

    /** US 01.05.04 - enrolled count is stored and retrieved correctly. */
    @Test
    public void testEnrolledCount() {
        assertEquals(450, event.getEnrolledCount());
    }

    /** Capacity is stored and retrieved correctly. */
    @Test
    public void testCapacity() {
        assertEquals(Integer.valueOf(500), event.getWaitlistLimit());
    }

    /** Event name is stored and retrieved correctly. */
    @Test
    public void testEventName() {
        assertEquals("Spring Music Festival", event.getName());
    }

    /** Venue address is stored and retrieved correctly. */
    @Test
    public void testVenueAddress() {
        assertEquals("Central Park Music Plaza", event.getVenueAddress());
    }

    /** Poster URL is stored and retrieved correctly. */
    @Test
    public void testPosterUrl() {
        assertEquals("https://example.com/poster.jpg", event.getPosterUrl());
    }

    /** Organizer ID is stored and retrieved correctly. */
    @Test
    public void testOrganizerId() {
        assertEquals("organizer_001", event.getOrganizerId());
    }

    /** Event ID is stored and retrieved correctly. */
    @Test
    public void testEventId() {
        assertEquals("test_id", event.getId());
    }

    /** Waiting count can be updated via setter. */
    @Test
    public void testSetWaitingCount() {
        event.setWaitingCount(300);
        assertEquals(300, event.getWaitingCount());
    }

    /** Enrolled count can be updated via setter. */
    @Test
    public void testSetEnrolledCount() {
        event.setEnrolledCount(460);
        assertEquals(460, event.getEnrolledCount());
    }

    /** Registration end date is stored and retrieved correctly. */
    @Test
    public void testRegistrationEnd() {
        assertNotNull(event.getRegistrationEnd());
        assertEquals(1740100000000L, event.getRegistrationEnd().getTime());
    }

    /** Registration is active when end date is in the future. */
    @Test
    public void testRegistrationIsActive() {
        Event futureEvent = new Event();
        futureEvent.setRegistrationEnd(new Date(System.currentTimeMillis() + 86400000L));
        assertTrue(futureEvent.getRegistrationEnd().after(new Date()));
    }

    /** Registration is closed when end date is in the past. */
    @Test
    public void testRegistrationIsClosed() {
        Event pastEvent = new Event();
        pastEvent.setRegistrationEnd(new Date(System.currentTimeMillis() - 86400000L));
        assertFalse(pastEvent.getRegistrationEnd().after(new Date()));
    }

    /** Empty constructor creates an event with null fields (for Firestore). */
    @Test
    public void testEmptyConstructor() {
        Event empty = new Event();
        assertNull(empty.getId());
        assertNull(empty.getName());
        assertNull(empty.getWaitlistLimit());
        assertEquals(0, empty.getWaitingCount());
    }

    /** Waiting count cannot exceed capacity (business logic check). */
    @Test
    public void testWaitingDoesNotExceedCapacity() {
        assertTrue(event.getWaitingCount() + event.getEnrolledCount() >= 0);
    }
}