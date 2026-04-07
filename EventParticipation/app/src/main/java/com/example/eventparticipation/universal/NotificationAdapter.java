package com.example.eventparticipation.universal;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventparticipation.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RecyclerView adapter for displaying entrant notifications.
 *
 * <p>This adapter is responsible for:
 * <ul>
 *     <li>binding notification message, relative time, and unread state</li>
 *     <li>showing or hiding Accept / Decline actions based on notification type</li>
 *     <li>rendering actionable invitations with invitation-style visuals</li>
 *     <li>forwarding item and button clicks to the host activity/fragment</li>
 * </ul>
 *
 * <p>Supported actionable invitation types:
 * <ul>
 *     <li>{@link NotificationItem#TYPE_SELECTED}</li>
 *     <li>{@link NotificationItem#TYPE_PRIVATE_INVITE}</li>
 *     <li>{@link NotificationItem#TYPE_COORGANIZER_INVITATION}</li>
 * </ul>
 *
 * <p>All button visibility and label logic is delegated to
 * {@link NotificationActionHelper} so the UI behavior stays centralized.</p>
 */
public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    /**
     * Callback interface used by the host screen to react to notification actions.
     */
    public interface Listener {

        /**
         * Called when the notification card itself is tapped.
         *
         * @param item clicked notification
         */
        void onNotificationClicked(NotificationItem item);

        /**
         * Called when the primary accept button is tapped.
         *
         * @param item notification being accepted
         */
        void onAcceptClicked(NotificationItem item);

        /**
         * Called when the decline button is tapped.
         *
         * @param item notification being declined
         */
        void onDeclineClicked(NotificationItem item);
    }

    private final Listener listener;
    private final List<NotificationItem> items = new ArrayList<>();

    /**
     * Creates a new adapter.
     *
     * @param listener listener that receives card and action button callbacks
     */
    public NotificationAdapter(Listener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the current data set and refreshes the list.
     *
     * @param newItems latest notifications to display
     */
    public void updateItems(List<NotificationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    /**
     * Inflates a single notification row.
     *
     * @param parent parent view group
     * @param viewType adapter view type
     * @return view holder for a notification item
     */
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    /**
     * Binds a notification item to the row UI.
     *
     * <p>This includes:
     * <ul>
     *     <li>message text</li>
     *     <li>relative timestamp</li>
     *     <li>unread indicator</li>
     *     <li>action state label</li>
     *     <li>Accept / Decline buttons</li>
     *     <li>visual styling based on notification type</li>
     * </ul>
     *
     * @param holder row view holder
     * @param position adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = items.get(position);

        bindBasicContent(holder, item);
        bindActionState(holder, item);
        bindActionButtons(holder, item);
        bindVisualStyle(holder, item);
        bindClickListeners(holder, item);
    }

    /**
     * Returns the number of notifications currently displayed.
     *
     * @return item count
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Binds the basic message, timestamp, and unread indicator.
     *
     * @param holder row view holder
     * @param item notification to render
     */
    private void bindBasicContent(@NonNull NotificationViewHolder holder,
                                  NotificationItem item) {
        holder.tvMessage.setText(item.getMessage() != null ? item.getMessage() : "Notification");
        holder.tvTime.setText(
                NotificationActionHelper.formatRelativeTime(
                        item.getCreatedAt(),
                        new Date().getTime()
                )
        );
        holder.unreadDot.setVisibility(item.isUnread() ? View.VISIBLE : View.GONE);
    }

    /**
     * Binds the action state label shown after a notification has been accepted
     * or declined.
     *
     * <p>Examples include:
     * <ul>
     *     <li>Invitation accepted</li>
     *     <li>Invitation declined</li>
     *     <li>Co-organizer invitation accepted</li>
     * </ul>
     *
     * @param holder row view holder
     * @param item notification to render
     */
    private void bindActionState(@NonNull NotificationViewHolder holder,
                                 NotificationItem item) {
        String actionState = NotificationActionHelper.getActionStateLabel(item);
        holder.tvActionState.setVisibility(actionState.isEmpty() ? View.GONE : View.VISIBLE);
        holder.tvActionState.setText(actionState);
    }

    /**
     * Binds action buttons using centralized helper logic.
     *
     * <p>This ensures that only supported pending invitation types show
     * Accept / Decline controls.</p>
     *
     * @param holder row view holder
     * @param item notification to render
     */
    private void bindActionButtons(@NonNull NotificationViewHolder holder,
                                   NotificationItem item) {
        boolean showAccept = NotificationActionHelper.shouldShowAcceptAction(item);
        boolean showDecline = NotificationActionHelper.shouldShowDeclineAction(item);

        holder.btnAccept.setVisibility(showAccept ? View.VISIBLE : View.GONE);
        holder.btnDecline.setVisibility(showDecline ? View.VISIBLE : View.GONE);
        holder.btnAccept.setText(NotificationActionHelper.getPrimaryActionLabel(item));
    }

    /**
     * Applies icon, border, and action-state colors based on notification type.
     *
     * <p>Actionable invitation types are rendered with invitation styling so they
     * visually stand out from passive informational notifications.</p>
     *
     * @param holder row view holder
     * @param item notification to render
     */
    private void bindVisualStyle(@NonNull NotificationViewHolder holder,
                                 NotificationItem item) {
        if (isActionableInvitation(item)) {
            applyInvitationStyle(holder);
        } else {
            applyStandardNotificationStyle(holder);
        }
    }

    /**
     * Attaches click handlers for the notification row and action buttons.
     *
     * @param holder row view holder
     * @param item notification to render
     */
    private void bindClickListeners(@NonNull NotificationViewHolder holder,
                                    NotificationItem item) {
        holder.itemView.setOnClickListener(v -> listener.onNotificationClicked(item));
        holder.btnAccept.setOnClickListener(v -> listener.onAcceptClicked(item));
        holder.btnDecline.setOnClickListener(v -> listener.onDeclineClicked(item));
    }

    /**
     * Returns whether the notification should be rendered as an actionable invitation.
     *
     * <p>Currently this includes:
     * <ul>
     *     <li>event selection invitations</li>
     *     <li>co-organizer invitations</li>
     * </ul>
     *
     * @param item notification to inspect
     * @return true if the item is an actionable invitation type
     */
    private boolean isActionableInvitation(NotificationItem item) {
        if (item == null || item.getType() == null) {
            return false;
        }

        return NotificationItem.TYPE_SELECTED.equals(item.getType())
                || NotificationItem.TYPE_PRIVATE_INVITE.equals(item.getType())
                || NotificationItem.TYPE_COORGANIZER_INVITATION.equals(item.getType());
    }

    /**
     * Applies the visual style used for actionable invitations.
     *
     * <p>This style intentionally highlights the notification as an item that
     * may require entrant action.</p>
     *
     * @param holder row view holder
     */
    private void applyInvitationStyle(@NonNull NotificationViewHolder holder) {
        holder.icon.setImageResource(R.drawable.entrantlogo);
        holder.card.setStrokeColor(
                holder.itemView.getResources().getColor(R.color.green_600)
        );
        holder.tvActionState.setTextColor(Color.parseColor("#16A34A"));
    }

    /**
     * Applies the default visual style used for passive notifications.
     *
     * @param holder row view holder
     */
    private void applyStandardNotificationStyle(@NonNull NotificationViewHolder holder) {
        holder.icon.setImageResource(R.drawable.belllogo);
        holder.card.setStrokeColor(
                holder.itemView.getResources().getColor(R.color.gray_300)
        );
        holder.tvActionState.setTextColor(Color.parseColor("#6B7280"));
    }

    /**
     * View holder for a single notification row.
     */
    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView icon;
        View unreadDot;
        TextView tvMessage;
        TextView tvTime;
        TextView tvActionState;
        MaterialButton btnAccept;
        MaterialButton btnDecline;

        /**
         * Binds row views from {@code item_notification.xml}.
         *
         * @param itemView inflated row view
         */
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