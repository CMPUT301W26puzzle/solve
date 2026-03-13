package com.example.eventparticipation;

import java.io.Serializable;
import java.util.Date;

/**
 * Model representing a single entrant notification stored in Firestore.
 */
public class NotificationItem implements Serializable {

    public static final String TYPE_SELECTED = "selected";
    public static final String TYPE_NOT_SELECTED = "not_selected";

    public static final String ACTION_PENDING = "pending";
    public static final String ACTION_ACCEPTED = "accepted";
    public static final String ACTION_DECLINED = "declined";
    public static final String ACTION_NONE = "none";

    private String id;
    private String entrantId;
    private String eventId;
    private String eventName;
    private String type;
    private String message;
    private boolean unread = true;
    private boolean actionRequired = false;
    private String actionStatus = ACTION_NONE;
    private Date createdAt;

    public NotificationItem() {
    }

    public NotificationItem(String id,
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntrantId() {
        return entrantId;
    }

    public void setEntrantId(String entrantId) {
        this.entrantId = entrantId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    public boolean isActionRequired() {
        return actionRequired;
    }

    public void setActionRequired(boolean actionRequired) {
        this.actionRequired = actionRequired;
    }

    public String getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(String actionStatus) {
        this.actionStatus = actionStatus;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
