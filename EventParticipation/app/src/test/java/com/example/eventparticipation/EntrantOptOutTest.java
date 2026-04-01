package com.example.eventparticipation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the Entrant model class, specifically targeting notification preferences.
 **/
public class EntrantOptOutTest {

    /**
     * US 01.04.03: Opt out of receiving notifications.
     * Verifies that a newly created entrant is opted into notifications by default.
     */
    @Test
    public void newEntrant_defaultOptOutIsFalse() {
        Entrant entrant = new Entrant();
        assertFalse("By default, entrants should receive notifications", entrant.isOptOutNotifications());
    }

    /**
     * US 01.04.03: Opt out of receiving notifications.
     * Verifies that the opt-out preference can be updated and retrieved correctly.
     */
    @Test
    public void setOptOut_updatesStatus() {
        Entrant entrant = new Entrant();

        entrant.setOptOutNotifications(true);
        assertTrue("Entrant should be able to opt-out of notifications", entrant.isOptOutNotifications());

        entrant.setOptOutNotifications(false);
        assertFalse("Entrant should be able to opt back into notifications", entrant.isOptOutNotifications());
    }
}