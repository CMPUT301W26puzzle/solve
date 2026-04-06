package com.example.eventparticipation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Comment> commentList;
    private final boolean isOrganizer;
    private final OnCommentDeleteListener deleteListener;

    public interface OnCommentDeleteListener {
        void onDeleteClick(Comment comment);
    }

    public CommentAdapter(List<Comment> commentList, boolean isOrganizer, OnCommentDeleteListener deleteListener) {
        this.commentList = commentList;
        this.isOrganizer = isOrganizer;
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

        // US 02.08.01: Only organizers can see the delete button
        if (isOrganizer) {
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

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvText;
        ImageButton btnDelete;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCommenterName);
            tvText = itemView.findViewById(R.id.tvCommentText);
            btnDelete = itemView.findViewById(R.id.btnDeleteComment);
        }
    }
}