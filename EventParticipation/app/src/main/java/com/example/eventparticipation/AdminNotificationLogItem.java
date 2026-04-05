package com.example.eventparticipation;

import java.util.Date;

/** Model representing a notification sent to an entrant. */
public class AdminNotificationLogItem {
    private final String id;
    private final String entrantId;
    private final String eventId;
    private final String eventName;
    private final String type;
    private final String message;
    private final boolean unread;
    private final boolean actionRequired;
    private final String actionStatus;
    private final Date createdAt;

    public AdminNotificationLogItem(String id,
                                    String entrantId,
                                    String eventId,
                                    String eventName,
                                    String type,
                                    String message,
                                    boolean unread,
                                    boolean actionRequired,
                                    String actionStatus,
                                    Date createdAt) {
        this.id = id;
        this.entrantId = entrantId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.type = type;
        this.message = message;
        this.unread = unread;
        this.actionRequired = actionRequired;
        this.actionStatus = actionStatus;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getEntrantId() { return entrantId; }
    public String getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public boolean isUnread() { return unread; }
    public boolean isActionRequired() { return actionRequired; }
    public String getActionStatus() { return actionStatus; }
    public Date getCreatedAt() { return createdAt; }
}