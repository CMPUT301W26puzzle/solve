package com.example.eventparticipation;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Model class representing a user comment on an event.
 * * <p>This class encapsulates the data for a discussion post within an event's
 * comment section, including the author's details, the event it belongs to,
 * the text content, and a Firestore-managed timestamp.</p>
 */
public class Comment {
    private String id;
    private String eventId;
    private String userId;
    private String userName;
    private String text;

    /**
     * The timestamp of when the comment was created.
     * Annotated with @ServerTimestamp to let Firestore automatically populate
     * this field with the server time upon insertion.
     */
    @ServerTimestamp
    private Date timestamp;

    /** * Default constructor required for Firebase Firestore serialization.
     */
    public Comment() {}

    /** * Constructs a new Comment with the specified details.
     * * @param eventId  The unique identifier of the event this comment belongs to.
     * @param userId   The unique identifier of the user creating the comment.
     * @param userName The display name of the user.
     * @param text     The actual text content of the comment.
     */
    public Comment(String eventId, String userId, String userName, String text) {
        this.eventId = eventId;
        this.userId = userId;
        this.userName = userName;
        this.text = text;
    }

    /** @return The unique document ID of the comment. */
    public String getId() { return id; }

    /** @param id The unique document ID to set for the comment. */
    public void setId(String id) { this.id = id; }

    /** @return The unique ID of the event associated with this comment. */
    public String getEventId() { return eventId; }

    /** @return The unique ID of the user who posted the comment. */
    public String getUserId() { return userId; }

    /** @return The display name of the user who posted the comment. */
    public String getUserName() { return userName; }

    /** @return The text content of the comment. */
    public String getText() { return text; }

    /** @return The server-generated timestamp of the comment. */
    public Date getTimestamp() { return timestamp; }
}