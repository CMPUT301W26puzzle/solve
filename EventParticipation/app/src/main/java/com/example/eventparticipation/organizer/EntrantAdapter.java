package com.example.eventparticipation.organizer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventparticipation.universal.Entrant;
import com.example.eventparticipation.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter used to display entrants in organizer waitlist screens.
 *
 * <p>This adapter displays entrant identity information and a single status badge.
 * It also supports row click callbacks so the parent screen can trigger organizer
 * actions such as cancelling invitations.</p>
 *
 * <p>Status display logic:
 * <ul>
 *     <li>waiting -> Waiting</li>
 *     <li>selected + pending -> Pending</li>
 *     <li>selected + accepted -> Accepted</li>
 *     <li>selected + declined -> Declined</li>
 *     <li>enrolled -> Enrolled</li>
 *     <li>cancelled -> Cancelled</li>
 * </ul>
 */
public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.EntrantViewHolder> {

    /**
     * Listener for row click events.
     */
    public interface OnEntrantClickListener {
        /**
         * Called when an entrant row is tapped.
         *
         * @param entrant tapped entrant
         */
        void onEntrantClick(Entrant entrant);
    }

    /** Entrants displayed by this adapter. */
    private final List<Entrant> entrants;

    /** Date formatter used for joined-at display. */
    private final SimpleDateFormat dateFormat;

    /** Optional row click listener. */
    private final OnEntrantClickListener listener;

    /**
     * Creates a new adapter instance.
     *
     * @param entrants displayed entrant list
     * @param listener optional click listener for row taps
     */
    public EntrantAdapter(List<Entrant> entrants, OnEntrantClickListener listener) {
        this.entrants = entrants;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant, parent, false);
        return new EntrantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        Entrant entrant = entrants.get(position);

        holder.tvEntrantName.setText(safe(entrant.getEntrantName()));
        holder.tvEntrantEmail.setText(safe(entrant.getEntrantEmail()));

        if (entrant.getJoinedAt() != null) {
            holder.tvRegisteredDate.setText("Joined: " + dateFormat.format(entrant.getJoinedAt()));
        } else {
            holder.tvRegisteredDate.setText("Joined: -");
        }

        String displayStatus = getDisplayStatusText(
                entrant.getSelectionStatus(),
                entrant.getResponseStatus(),
                entrant.getFinalStatus()
        );

        int statusColor = getDisplayStatusColor(
                entrant.getSelectionStatus(),
                entrant.getResponseStatus(),
                entrant.getFinalStatus()
        );

        holder.tvStatus.setText(displayStatus);
        holder.tvStatus.setBackgroundColor(statusColor);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEntrantClick(entrant);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entrants == null ? 0 : entrants.size();
    }

    /**
     * Returns a user-facing status string for the badge.
     *
     * @param selectionStatus entrant selection status
     * @param responseStatus entrant response status
     * @param finalStatus entrant final status
     * @return display text for the badge
     */
    private String getDisplayStatusText(String selectionStatus, String responseStatus, String finalStatus) {
        if ("enrolled".equals(finalStatus)) {
            return "Enrolled";
        }

        if ("cancelled".equals(selectionStatus)) {
            return "Cancelled";
        }

        if ("waiting".equals(selectionStatus)) {
            return "Waiting";
        }

        if ("selected".equals(selectionStatus)) {
            if ("accepted".equals(responseStatus)) {
                return "Accepted";
            }
            if ("declined".equals(responseStatus)) {
                return "Declined";
            }
            return "Pending";
        }

        return "Unknown";
    }

    /**
     * Returns badge color based on entrant status.
     *
     * @param selectionStatus entrant selection status
     * @param responseStatus entrant response status
     * @param finalStatus entrant final status
     * @return color integer for badge background
     */
    private int getDisplayStatusColor(String selectionStatus, String responseStatus, String finalStatus) {
        if ("enrolled".equals(finalStatus)) {
            return Color.parseColor("#9333EA");
        }

        if ("cancelled".equals(selectionStatus)) {
            return Color.parseColor("#6B7280");
        }

        if ("waiting".equals(selectionStatus)) {
            return Color.parseColor("#2563EB");
        }

        if ("selected".equals(selectionStatus)) {
            if ("accepted".equals(responseStatus)) {
                return Color.parseColor("#16A34A");
            }
            if ("declined".equals(responseStatus)) {
                return Color.parseColor("#DC2626");
            }
            return Color.parseColor("#F59E0B");
        }

        return Color.parseColor("#9CA3AF");
    }

    /**
     * Returns a non-null safe string for display.
     *
     * @param value possibly null value
     * @return dash when null, otherwise original value
     */
    private String safe(String value) {
        return value == null ? "-" : value;
    }

    /**
     * ViewHolder for a single entrant row.
     */
    static class EntrantViewHolder extends RecyclerView.ViewHolder {

        /** Entrant display name. */
        TextView tvEntrantName;

        /** Entrant email. */
        TextView tvEntrantEmail;

        /** Joined-at display label. */
        TextView tvRegisteredDate;

        /** Status badge. */
        TextView tvStatus;

        /**
         * Creates a view holder for an entrant row.
         *
         * @param itemView inflated row view
         */
        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEntrantName = itemView.findViewById(R.id.tvEntrantName);
            tvEntrantEmail = itemView.findViewById(R.id.tvEntrantEmail);
            tvRegisteredDate = itemView.findViewById(R.id.tvRegisteredDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}