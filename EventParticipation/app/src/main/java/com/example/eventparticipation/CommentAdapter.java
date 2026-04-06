package com.example.eventparticipation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Unified RecyclerView adapter for displaying and managing event comments.
 *
 * <p><b>Purpose & Role:</b> Maps {@link Comment} data objects to the UI. It dynamically adjusts
 * the visibility of moderation controls (like the delete button) based on the current
 * user's role (Entrant, Organizer, or Admin) and comment authorship.</p>
 *
 * <p>Implemented user stories:</p>
 * <ul>
 * <li>US 01.08.02 As an entrant, I want to view comments on an event.</li>
 * <li>US 02.08.01 As an organizer, I want to view and delete entrant comments on my event.</li>
 * <li>US 03.10.01 As an administrator, I want to remove event comments that violate app policy.</li>
 * </ul>
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Comment> commentList;
    private final String currentUserId;
    private final boolean isOrganizer;
    private final boolean isAdmin;
    private final OnCommentDeleteListener deleteListener;

    /**
     * Interface definition for a callback to be invoked when a comment's delete button is clicked.
     */
    public interface OnCommentDeleteListener {
        /**
         * Called when a comment is selected for deletion.
         *
         * @param comment The comment object to be deleted.
         */
        void onDeleteClick(Comment comment);
    }

    /**
     * Constructs a new CommentAdapter with specified permission flags.
     *
     * @param commentList   The backing list of comments to display.
     * @param currentUserId The Firestore ID of the currently authenticated user (used to verify authorship).
     * @param isOrganizer   True if the current user is an organizer managing this event.
     * @param isAdmin       True if the current user is an administrator moderating the platform.
     * @param deleteListener Callback triggered when a user attempts to delete a comment.
     */
    public CommentAdapter(List<Comment> commentList, String currentUserId, boolean isOrganizer, boolean isAdmin, OnCommentDeleteListener deleteListener) {
        this.commentList = commentList;
        this.currentUserId = currentUserId;
        this.isOrganizer = isOrganizer;
        this.isAdmin = isAdmin;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.tvName.setText(comment.getUserName());
        holder.tvText.setText(comment.getText());

        // Permission Logic: Can delete if they are an Admin, the Organizer, or the original Author
        boolean isAuthor = comment.getUserId() != null && comment.getUserId().equals(currentUserId);

        if (isAdmin || isOrganizer || isAuthor) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> deleteListener.onDeleteClick(comment));
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    /**
     * View holder for a single comment item.
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvText;
        ImageButton btnDelete;

        /**
         * Binds comment row views.
         *
         * @param itemView inflated item view.
         */
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCommenterName);
            tvText = itemView.findViewById(R.id.tvCommentText);
            btnDelete = itemView.findViewById(R.id.btnDeleteComment);
        }
    }
}