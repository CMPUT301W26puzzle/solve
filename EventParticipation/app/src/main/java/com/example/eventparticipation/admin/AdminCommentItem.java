package com.example.eventparticipation.admin;

import com.example.eventparticipation.universal.Comment;

/** Model representing a comment in the admin browse list. */
public class AdminCommentItem {
    private final Comment comment;

    public AdminCommentItem(Comment comment) {
        this.comment = comment;
    }

    public Comment getComment() {
        return comment;
    }

    public String getId() {
        return comment.getId();
    }

    public String getEventId() {
        return comment.getEventId();
    }

    public String getUserId() {
        return comment.getUserId();
    }

    public String getUserName() {
        return comment.getUserName();
    }

    public String getText() {
        return comment.getText();
    }
}