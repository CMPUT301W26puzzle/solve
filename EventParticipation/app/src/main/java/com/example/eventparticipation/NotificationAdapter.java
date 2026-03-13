package com.example.eventparticipation;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RecyclerView adapter for entrant notifications.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface Listener {
        void onNotificationClicked(NotificationItem item);
        void onAcceptClicked(NotificationItem item);
        void onDeclineClicked(NotificationItem item);
    }

    private final Listener listener;
    private final List<NotificationItem> items = new ArrayList<>();

    public NotificationAdapter(Listener listener) {
        this.listener = listener;
    }

    public void updateItems(List<NotificationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
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
        NotificationItem item = items.get(position);

        holder.tvMessage.setText(item.getMessage() != null ? item.getMessage() : "Notification");
        holder.tvTime.setText(NotificationActionHelper.formatRelativeTime(item.getCreatedAt(), new Date().getTime()));
        holder.unreadDot.setVisibility(item.isUnread() ? View.VISIBLE : View.GONE);

        String actionState = NotificationActionHelper.getActionStateLabel(item);
        holder.tvActionState.setVisibility(actionState.isEmpty() ? View.GONE : View.VISIBLE);
        holder.tvActionState.setText(actionState);

        boolean showAccept = NotificationActionHelper.shouldShowAcceptAction(item);
        boolean showDecline = NotificationActionHelper.shouldShowDeclineAction(item);

        holder.btnAccept.setVisibility(showAccept ? View.VISIBLE : View.GONE);
        holder.btnDecline.setVisibility(showDecline ? View.VISIBLE : View.GONE);
        holder.btnAccept.setText(NotificationActionHelper.getPrimaryActionLabel(item));

        if (NotificationItem.TYPE_SELECTED.equals(item.getType())) {
            holder.icon.setImageResource(R.drawable.entrantlogo);
            holder.card.setStrokeColor(holder.itemView.getResources().getColor(R.color.green_600));
            holder.tvActionState.setTextColor(Color.parseColor("#16A34A"));
        } else {
            holder.icon.setImageResource(R.drawable.belllogo);
            holder.card.setStrokeColor(holder.itemView.getResources().getColor(R.color.gray_300));
            holder.tvActionState.setTextColor(Color.parseColor("#6B7280"));
        }

        holder.itemView.setOnClickListener(v -> listener.onNotificationClicked(item));
        holder.btnAccept.setOnClickListener(v -> listener.onAcceptClicked(item));
        holder.btnDecline.setOnClickListener(v -> listener.onDeclineClicked(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView icon;
        View unreadDot;
        TextView tvMessage;
        TextView tvTime;
        TextView tvActionState;
        MaterialButton btnAccept;
        MaterialButton btnDecline;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.notificationCard);
            icon = itemView.findViewById(R.id.ivNotificationIcon);
            unreadDot = itemView.findViewById(R.id.viewUnreadDot);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            tvActionState = itemView.findViewById(R.id.tvActionState);
            btnAccept = itemView.findViewById(R.id.btnAcceptInvitation);
            btnDecline = itemView.findViewById(R.id.btnDeclineInvitation);
        }
    }
}
