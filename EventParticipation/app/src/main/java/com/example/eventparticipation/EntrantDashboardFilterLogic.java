package com.example.eventparticipation;

import java.util.Date;

/**
 * Pure filtering logic for entrant event discovery.
 */
final class EntrantDashboardFilterLogic {

    static final String REGISTRATION_FILTER_ALL = "all";
    static final String REGISTRATION_FILTER_UPCOMING = "upcoming";
    static final String REGISTRATION_FILTER_OPEN = "open";
    static final String REGISTRATION_FILTER_CLOSED = "closed";

    static final String PARTICIPATION_FILTER_ALL = "all";
    static final String PARTICIPATION_FILTER_NOT_JOINED = "not_joined";
    static final String PARTICIPATION_FILTER_WAITING = "waiting";
    static final String PARTICIPATION_FILTER_SELECTED = "selected";
    static final String PARTICIPATION_FILTER_ENROLLED = "enrolled";

    private EntrantDashboardFilterLogic() {
    }

    /**
     * Matches an event against a keyword query.
     *
     * @param event event to evaluate
     * @param query search text
     * @return true if the event matches the keyword
     */
    static boolean matchesKeyword(Event event, String query) {
        String lower = query == null ? "" : query.trim().toLowerCase();
        if (lower.isEmpty()) {
            return true;
        }

        // TODO: include event description in keyword search if a description field is going to be added
        String name = event.getName() != null ? event.getName().toLowerCase() : "";
        String venueAddress = event.getVenueAddress() != null
                ? event.getVenueAddress().toLowerCase()
                : "";

        return name.contains(lower) || venueAddress.contains(lower);
    }

    /**
     * Checks whether an event matches a registration-time filter.
     *
     * @param event event to evaluate
     * @param registrationFilter active registration filter key
     * @param now current time used for comparison
     * @return true if the event matches the registration filter
     */
    static boolean matchesRegistrationFilter(Event event, String registrationFilter, Date now) {
        Date registrationStart = event.getRegistrationStart();
        Date registrationEnd = event.getRegistrationEnd();

        switch (registrationFilter) {
            case REGISTRATION_FILTER_UPCOMING:
                return registrationStart != null && now.before(registrationStart);
            case REGISTRATION_FILTER_OPEN:
                return (registrationStart == null || !now.before(registrationStart))
                        && (registrationEnd == null || !now.after(registrationEnd));
            case REGISTRATION_FILTER_CLOSED:
                return registrationEnd != null && now.after(registrationEnd);
            case REGISTRATION_FILTER_ALL:
            default:
                return true;
        }
    }

    /**
     * Checks whether an event still has waitlist spots available.
     *
     * @param event event to evaluate
     * @param registrationFilter active registration filter key
     * @param onlyAvailableSpots true when availability filtering is enabled
     * @return true if the event passes the availability rule
     */
    static boolean matchesAvailabilityFilter(Event event,
                                             String registrationFilter,
                                             boolean onlyAvailableSpots) {
        if (!onlyAvailableSpots || !REGISTRATION_FILTER_OPEN.equals(registrationFilter)) {
            return true;
        }

        Integer waitlistLimit = event.getWaitlistLimit();
        return waitlistLimit == null || event.getWaitingCount() < waitlistLimit;
    }

    /**
     * Checks whether a normalized participation status matches a filter.
     *
     * @param normalizedParticipationStatus normalized dashboard participation status
     * @param participationFilter active participation filter key
     * @return true if the status matches the participation filter
     */
    static boolean matchesParticipationFilter(String normalizedParticipationStatus,
                                              String participationFilter) {
        switch (participationFilter) {
            case PARTICIPATION_FILTER_NOT_JOINED:
                return PARTICIPATION_FILTER_NOT_JOINED.equals(normalizedParticipationStatus);
            case PARTICIPATION_FILTER_WAITING:
                return PARTICIPATION_FILTER_WAITING.equals(normalizedParticipationStatus);
            case PARTICIPATION_FILTER_SELECTED:
                return PARTICIPATION_FILTER_SELECTED.equals(normalizedParticipationStatus);
            case PARTICIPATION_FILTER_ENROLLED:
                return PARTICIPATION_FILTER_ENROLLED.equals(normalizedParticipationStatus);
            case PARTICIPATION_FILTER_ALL:
            default:
                return true;
        }
    }

    /**
     * Maps waitlist fields to a dashboard participation status.
     *
     * @param selectionStatus waitlist selection status
     * @param responseStatus waitlist response status
     * @param finalStatus waitlist final status
     * @return normalized dashboard participation status
     */
    static String resolveParticipationStatus(String selectionStatus,
                                             String responseStatus,
                                             String finalStatus) {
        if ("enrolled".equals(finalStatus)) {
            return PARTICIPATION_FILTER_ENROLLED;
        }

        if ("waiting".equals(selectionStatus)) {
            return PARTICIPATION_FILTER_WAITING;
        }

        if ("selected".equals(selectionStatus) && "pending".equals(responseStatus)) {
            return PARTICIPATION_FILTER_SELECTED;
        }

        if ("cancelled".equals(selectionStatus) || "declined".equals(responseStatus)) {
            return PARTICIPATION_FILTER_NOT_JOINED;
        }

        return PARTICIPATION_FILTER_NOT_JOINED;
    }
}
