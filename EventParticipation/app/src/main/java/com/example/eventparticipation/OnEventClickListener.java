package com.example.eventparticipation;

/**
 * Listener interface for organizer event card actions.
 *
 * <p>Implemented by organizer-facing screens to respond when the user selects
 * an action from an event item, such as opening the manage page or entrant list.</p>
 */
public interface OnEventClickListener {

    /**
     * Called when the organizer chooses to manage the selected event.
     *
     * @param event event associated with the clicked item
     */
    void onManageClick(Event event);

    /**
     * Called when the organizer wants to view entrants of the selected event.
     *
     * @param event event associated with the clicked item
     */
    void onEntrantsClick(Event event);

    /**
     * Called when the organizer chooses to run the lottery for the selected event.
     *
     * @param event event associated with the clicked item
     */
    void onLotteryClick(Event event);

    /**
     * Called when the organizer wants to display the QR code for the selected event.
     *
     * @param event event associated with the clicked item
     */
    void onQRCodeClick(Event event);

    /**
     * Called when the organizer wants to open the event detail view.
     *
     * @param event event associated with the clicked item
     */
    void onViewClick(Event event);
}