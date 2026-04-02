package com.example.eventparticipation;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter used to display entrants in the organizer waitlist screen.
 *
 * Display logic:
 * - waiting -> Waiting
 * - selected + pending -> Pending
 * - selected + accepted -> Accepted
 * - selected + declined -> Declined
 * - enrolled -> Enrolled
 * - cancelled -> Cancelled
 */
public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.EntrantViewHolder> {

    private final List<Entrant> entrants;
    private final SimpleDateFormat dateFormat;

    public EntrantAdapter(List<Entrant> entrants) {
        this.entrants = entrants;
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
    }

    @Override
    public int getItemCount() {
        return entrants == null ? 0 : entrants.size();
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

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

    private int getDisplayStatusColor(String selectionStatus, String responseStatus, String finalStatus) {
        if ("enrolled".equals(finalStatus)) {
            return Color.parseColor("#9333EA"); // purple
        }

        if ("cancelled".equals(selectionStatus)) {
            return Color.parseColor("#6B7280"); // gray
        }

        if ("waiting".equals(selectionStatus)) {
            return Color.parseColor("#2563EB"); // blue
        }

        if ("selected".equals(selectionStatus)) {
            if ("accepted".equals(responseStatus)) {
                return Color.parseColor("#16A34A"); // green
            }
            if ("declined".equals(responseStatus)) {
                return Color.parseColor("#DC2626"); // red
            }
            return Color.parseColor("#F59E0B"); // yellow/orange for pending
        }

        return Color.parseColor("#9CA3AF");
    }

    static class EntrantViewHolder extends RecyclerView.ViewHolder {
        TextView tvEntrantName;
        TextView tvEntrantEmail;
        TextView tvRegisteredDate;
        TextView tvStatus;

        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEntrantName = itemView.findViewById(R.id.tvEntrantName);
            tvEntrantEmail = itemView.findViewById(R.id.tvEntrantEmail);
            tvRegisteredDate = itemView.findViewById(R.id.tvRegisteredDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}