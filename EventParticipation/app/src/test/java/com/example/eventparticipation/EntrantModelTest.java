package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.firebase.firestore.GeoPoint;

import org.junit.Test;

/**
 * Unit tests for the Entrant model class.
 */
public class EntrantModelTest {

    @Test
    public void setAndGetEntrantName_returnsSameValue() {
        Entrant entrant = new Entrant();
        entrant.setEntrantName("Tom Lee");
        assertEquals("Tom Lee", entrant.getEntrantName());
    }

    @Test
    public void setAndGetEntrantEmail_returnsSameValue() {
        Entrant entrant = new Entrant();
        entrant.setEntrantEmail("tom@test.com");
        assertEquals("tom@test.com", entrant.getEntrantEmail());
    }

    @Test
    public void setAndGetStatus_returnsSameValue() {
        Entrant entrant = new Entrant();
        entrant.setStatus("waiting");
        assertEquals("waiting", entrant.getStatus());
    }

    @Test
    public void hasLocation_returnsFalse_whenLocationMissing() {
        Entrant entrant = new Entrant();
        entrant.setJoinedLocation(null);

        assertFalse(entrant.hasLocation());
    }

    @Test
    public void hasLocation_returnsTrue_whenLocationExists() {
        Entrant entrant = new Entrant();
        entrant.setJoinedLocation(new GeoPoint(53.5461, -113.4938));

        assertTrue(entrant.hasLocation());
    }
}