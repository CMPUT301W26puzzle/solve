package com.example.eventparticipation;

import java.util.Date;

/**
 * Model class representing a single event comment.
 *
 * <p>Implemented user stories:</p>
 * <ul>
 *     <li>US 01.08.01 As an entrant, I want to post a comment on an event.</li>
 *     <li>US 01.08.02 As an entrant, I want to view comments on an event.</li>
 * </ul>
 */
public class EventComment {

    /** Firestore document id of the comment. */
    private String commentId;

    /** Entrant id of the comment author. */
    private String entrantId;

    /** Display name of the comment author. */
    private String entrantName;

    /** Comment body text. */
    private String text;

    /** Creation timestamp of the comment. */
    private Date createdAt;

    /**
     * Required empty constructor for Firestore mapping.
     */
    public EventComment() {
    }

    /**
     * Returns the Firestore comment id.
     *
     * @return comment id
     */
    public String getCommentId() {
        return commentId;
    }

    /**
     * Sets the Firestore comment id.
     *
     * @param commentId comment id
     */
    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    /**
     * Returns the entrant id of the author.
     *
     * @return entrant id
     */
    public String getEntrantId() {
        return entrantId;
    }

    /**
     * Sets the entrant id of the author.
     *
     * @param entrantId entrant id
     */
    public void setEntrantId(String entrantId) {
        this.entrantId = entrantId;
    }

    /**
     * Returns the display name of the author.
     *
     * @return entrant name
     */
    public String getEntrantName() {
        return entrantName;
    }

    /**
     * Sets the display name of the author.
     *
     * @param entrantName entrant name
     */
    public void setEntrantName(String entrantName) {
        this.entrantName = entrantName;
    }

    /**
     * Returns the comment body.
     *
     * @return comment text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the comment body.
     *
     * @param text comment text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Returns the comment creation time.
     *
     * @return creation date
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the comment creation time.
     *
     * @param createdAt creation date
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
