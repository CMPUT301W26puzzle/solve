package com.example.eventparticipation;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Comment {
    private String id;
    private String eventId;
    private String userId;
    private String userName;
    private String text;
    @ServerTimestamp
    private Date timestamp;

    public Comment() {} // Required for Firestore

    /** stores comment stuff **/
    public Comment(String eventId, String userId, String userName, String text) {
        this.eventId = eventId;
        this.userId = userId;
        this.userName = userName;
        this.text = text;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getText() { return text; }
    public Date getTimestamp() { return timestamp; }
}