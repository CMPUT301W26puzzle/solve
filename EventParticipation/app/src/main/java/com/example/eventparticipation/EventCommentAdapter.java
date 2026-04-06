package com.example.eventparticipation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for event comments.
 *
 * <p>Implemented user stories:</p>
 * <ul>
 *     <li>US 01.08.01 As an entrant, I want to post a comment on an event.</li>
 *     <li>US 01.08.02 As an entrant, I want to view comments on an event.</li>
 * </ul>
 */
public class EventCommentAdapter extends RecyclerView.Adapter<EventCommentAdapter.CommentViewHolder> {

    /**
     * Long-press callback for comment actions.
     */
    public interface OnCommentLongClickListener {
        /**
         * Handles a long press on a comment item.
         *
         * @param comment pressed comment
         */
        void onCommentLongClick(EventComment comment);
    }

    private final List<EventComment> comments;
    private final OnCommentLongClickListener longClickListener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());

    /**
     * Creates the adapter for event comments.
     *
     * @param comments backing comment list
     * @param longClickListener long-press callback
     */
    public EventCommentAdapter(List<EventComment> comments,
                               OnCommentLongClickListener longClickListener) {
        this.comments = comments;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        EventComment comment = comments.get(position);

        holder.tvCommentAuthor.setText(EventCommentLogic.resolveAuthorName(comment.getEntrantName()));

        Date createdAt = comment.getCreatedAt();
        holder.tvCommentDate.setText(createdAt != null ? dateFormat.format(createdAt) : "Just now");
        holder.tvCommentText.setText(comment.getText() != null ? comment.getText() : "");

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onCommentLongClick(comment);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    /**
     * View holder for a single comment item.
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView tvCommentAuthor;
        TextView tvCommentDate;
        TextView tvCommentText;

        /**
         * Binds comment row views.
         *
         * @param itemView inflated item view
         */
        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvCommentDate = itemView.findViewById(R.id.tvCommentDate);
            tvCommentText = itemView.findViewById(R.id.tvCommentText);
        }
    }
}
