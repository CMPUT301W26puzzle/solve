package com.example.eventparticipation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for entrant dashboard filtering logic.
 */
public class EntrantDashboardFilterLogicTest {

    private Date now;
    private Event openAvailableEvent;
    private Event openFullEvent;
    private Event upcomingEvent;
    private Event closedEvent;
    private Event unlimitedOpenEvent;

    /**
     * Creates reusable event fixtures for dashboard filter tests.
     */
    @Before
    public void setUp() {
        now = new Date();

        openAvailableEvent = buildEvent(
                "Campus Hackathon",
                "University of Alberta",
                offsetFromNow(-60 * 60 * 1000L),
                offsetFromNow(60 * 60 * 1000L),
                2,
                5
        );

        openFullEvent = buildEvent(
                "Startup Night",
                "Edmonton Tower",
                offsetFromNow(-60 * 60 * 1000L),
                offsetFromNow(60 * 60 * 1000L),
                5,
                5
        );

        upcomingEvent = buildEvent(
                "Future Expo",
                "Downtown Hall",
                offsetFromNow(60 * 60 * 1000L),
                offsetFromNow(2 * 60 * 60 * 1000L),
                0,
                10
        );

        closedEvent = buildEvent(
                "Past Meetup",
                "Old Strathcona",
                offsetFromNow(-2 * 60 * 60 * 1000L),
                offsetFromNow(-60 * 60 * 1000L),
                1,
                10
        );

        unlimitedOpenEvent = buildEvent(
                "Open House",
                "Innovation Centre",
                offsetFromNow(-60 * 60 * 1000L),
                offsetFromNow(60 * 60 * 1000L),
                999,
                null
        );
    }

    /**
     * Builds an event fixture with registration and waitlist data.
     *
     * @param name event name
     * @param venueAddress venue address
     * @param registrationStart registration start time
     * @param registrationEnd registration end time
     * @param waitingCount current waiting count
     * @param waitlistLimit optional waitlist limit
     * @return configured event fixture
     */
    private Event buildEvent(String name,
                             String venueAddress,
                             Date registrationStart,
                             Date registrationEnd,
                             int waitingCount,
                             Integer waitlistLimit) {
        Event event = new Event();
        event.setName(name);
        event.setVenueAddress(venueAddress);
        event.setRegistrationStart(registrationStart);
        event.setRegistrationEnd(registrationEnd);
        event.setWaitingCount(waitingCount);
        event.setWaitlistLimit(waitlistLimit);
        return event;
    }

    /**
     * Returns a date offset from the shared test clock.
     *
     * @param millis offset in milliseconds
     * @return offset date
     */
    private Date offsetFromNow(long millis) {
        return new Date(System.currentTimeMillis() + millis);
    }

    @Test
    public void matchesKeyword_emptyQuery_returnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesKeyword(openAvailableEvent, ""));
    }

    @Test
    public void matchesKeyword_nameContainsQuery_returnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesKeyword(openAvailableEvent, "hack"));
    }

    @Test
    public void matchesKeyword_venueContainsQuery_returnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesKeyword(openAvailableEvent, "alberta"));
    }

    @Test
    public void matchesKeyword_ignoresCase_returnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesKeyword(openAvailableEvent, "HACKATHON"));
    }

    @Test
    public void matchesKeyword_missingQuery_returnsFalse() {
        assertFalse(EntrantDashboardFilterLogic.matchesKeyword(openAvailableEvent, "music"));
    }

    @Test
    public void matchesRegistrationFilter_all_returnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesRegistrationFilter(
                openAvailableEvent,
                EntrantDashboardFilterLogic.REGISTRATION_FILTER_ALL,
                now
        ));
    }

    @Test
    public void matchesRegistrationFilter_upcomingBeforeStart_returnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesRegistrationFilter(
                upcomingEvent,
                EntrantDashboardFilterLogic.REGISTRATION_FILTER_UPCOMING,
                now
        ));
    }

    @Test
    public void matchesRegistrationFilter_openWithinWindow_returnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesRegistrationFilter(
                openAvailableEvent,
                EntrantDashboardFilterLogic.REGISTRATION_FILTER_OPEN,
                now
        ));
    }

    @Test
    public void matchesRegistrationFilter_closedAfterEnd_returnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesRegistrationFilter(
                closedEvent,
                EntrantDashboardFilterLogic.REGISTRATION_FILTER_CLOSED,
                now
        ));
    }

    @Test
    public void matchesRegistrationFilter_openAtStartBoundary_returnsTrue() {
        Date boundary = openAvailableEvent.getRegistrationStart();

        assertTrue(EntrantDashboardFilterLogic.matchesRegistrationFilter(
                openAvailableEvent,
                EntrantDashboardFilterLogic.REGISTRATION_FILTER_OPEN,
                boundary
        ));
    }

    @Test
    public void matchesRegistrationFilter_openAtEndBoundary_returnsTrue() {
        Date boundary = openAvailableEvent.getRegistrationEnd();

        assertTrue(EntrantDashboardFilterLogic.matchesRegistrationFilter(
                openAvailableEvent,
                EntrantDashboardFilterLogic.REGISTRATION_FILTER_OPEN,
                boundary
        ));
    }

    @Test
    public void matchesAvailabilityFilter_disabled_returnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesAvailabilityFilter(
                openFullEvent,
                EntrantDashboardFilterLogic.REGISTRATION_FILTER_OPEN,
                false
        ));
    }

    @Test
    public void matchesAvailabilityFilter_nonOpenRegistration_returnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesAvailabilityFilter(
                upcomingEvent,
                EntrantDashboardFilterLogic.REGISTRATION_FILTER_UPCOMING,
                true
        ));
    }

    @Test
    public void matchesAvailabilityFilter_unlimitedWaitlist_returnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesAvailabilityFilter(
                unlimitedOpenEvent,
                EntrantDashboardFilterLogic.REGISTRATION_FILTER_OPEN,
                true
        ));
    }

    @Test
    public void matchesAvailabilityFilter_belowLimit_returnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesAvailabilityFilter(
                openAvailableEvent,
                EntrantDashboardFilterLogic.REGISTRATION_FILTER_OPEN,
                true
        ));
    }

    @Test
    public void matchesAvailabilityFilter_atLimit_returnsFalse() {
        assertFalse(EntrantDashboardFilterLogic.matchesAvailabilityFilter(
                openFullEvent,
                EntrantDashboardFilterLogic.REGISTRATION_FILTER_OPEN,
                true
        ));
    }

    @Test
    public void resolveParticipationStatus_waitingReturnsWaiting() {
        assertTrue(EntrantDashboardFilterLogic.PARTICIPATION_FILTER_WAITING.equals(
                EntrantDashboardFilterLogic.resolveParticipationStatus("waiting", null, null)
        ));
    }

    @Test
    public void resolveParticipationStatus_selectedPendingReturnsSelected() {
        assertTrue(EntrantDashboardFilterLogic.PARTICIPATION_FILTER_SELECTED.equals(
                EntrantDashboardFilterLogic.resolveParticipationStatus("selected", "pending", null)
        ));
    }

    @Test
    public void resolveParticipationStatus_enrolledReturnsEnrolled() {
        assertTrue(EntrantDashboardFilterLogic.PARTICIPATION_FILTER_ENROLLED.equals(
                EntrantDashboardFilterLogic.resolveParticipationStatus("selected", "accepted", "enrolled")
        ));
    }

    @Test
    public void resolveParticipationStatus_declinedReturnsNotJoined() {
        assertTrue(EntrantDashboardFilterLogic.PARTICIPATION_FILTER_NOT_JOINED.equals(
                EntrantDashboardFilterLogic.resolveParticipationStatus("selected", "declined", null)
        ));
    }

    @Test
    public void resolveParticipationStatus_cancelledReturnsNotJoined() {
        assertTrue(EntrantDashboardFilterLogic.PARTICIPATION_FILTER_NOT_JOINED.equals(
                EntrantDashboardFilterLogic.resolveParticipationStatus("cancelled", null, null)
        ));
    }

    @Test
    public void matchesParticipationFilter_allReturnsTrue() {
        assertTrue(EntrantDashboardFilterLogic.matchesParticipationFilter(
                EntrantDashboardFilterLogic.PARTICIPATION_FILTER_WAITING,
                EntrantDashboardFilterLogic.PARTICIPATION_FILTER_ALL
        ));
    }

    @Test
    public void matchesParticipationFilter_waitingMatchesWaiting() {
        assertTrue(EntrantDashboardFilterLogic.matchesParticipationFilter(
                EntrantDashboardFilterLogic.PARTICIPATION_FILTER_WAITING,
                EntrantDashboardFilterLogic.PARTICIPATION_FILTER_WAITING
        ));
    }

    @Test
    public void matchesParticipationFilter_selectedDoesNotMatchWaiting() {
        assertFalse(EntrantDashboardFilterLogic.matchesParticipationFilter(
                EntrantDashboardFilterLogic.PARTICIPATION_FILTER_SELECTED,
                EntrantDashboardFilterLogic.PARTICIPATION_FILTER_WAITING
        ));
    }
}
