package com.example.eventparticipation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class EntrantNotificationAdapter extends RecyclerView.Adapter<EntrantNotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(EntrantNotification notification);
    }

    private final List<EntrantNotification> notifications;
    private final OnNotificationClickListener listener;

    public EntrantNotificationAdapter(List<EntrantNotification> notifications,
                                      OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        EntrantNotification notification = notifications.get(position);

        holder.tvMessage.setText(notification.getMessage() == null ? "" : notification.getMessage());

        if (notification.getCreatedAt() != null) {
            holder.tvDate.setText(TimeUtils.getRelativeTime(notification.getCreatedAt()));
        } else {
            holder.tvDate.setText("");
        }

        holder.viewUnreadDot.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

        if ("selected".equals(notification.getType())) {
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setText("Accept Invitation");
        } else {
            holder.btnAction.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
        holder.btnAction.setOnClickListener(v -> listener.onNotificationClick(notification));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvDate;
        View viewUnreadDot;
        MaterialButton btnAction;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvDate = itemView.findViewById(R.id.tvDate);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}
