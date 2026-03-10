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
 * <p>Each row shows entrant name, email, join date, and current status.</p>
 */
public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.EntrantViewHolder> {

    /** Source list used by the adapter. */
    private final List<Entrant> entrants;

    /** Date formatter used for rendering join dates. */
    private final SimpleDateFormat dateFormat;

    /**
     * Creates an adapter for the provided entrant list.
     *
     * @param entrants list of entrants to display
     */
    public EntrantAdapter(List<Entrant> entrants) {
        this.entrants = entrants;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    /**
     * Inflates a single entrant item view.
     *
     * @param parent parent view group
     * @param viewType item view type
     * @return new view holder instance
     */
    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant, parent, false);
        return new EntrantViewHolder(view);
    }

    /**
     * Binds entrant data to the row at the given position.
     *
     * @param holder row view holder
     * @param position adapter position
     */
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

        holder.tvStatus.setText(getStatusText(entrant.getStatus()));
        holder.tvStatus.setBackgroundColor(getStatusColor(entrant.getStatus()));
    }

    /**
     * Returns the total number of rows displayed by the adapter.
     *
     * @return entrant count
     */
    @Override
    public int getItemCount() {
        return entrants.size();
    }

    /**
     * Converts a possibly null string into a UI-safe display value.
     *
     * @param value raw text value
     * @return original value, or {@code "-"} when null
     */
    private String safe(String value) {
        return value == null ? "-" : value;
    }

    /**
     * Maps internal waitlist status values to user-friendly labels.
     *
     * @param status raw status from Firestore
     * @return display label for the status
     */
    private String getStatusText(String status) {
        if (status == null) return "Unknown";
        switch (status) {
            case "waiting":
                return "Waiting";
            case "selected":
                return "Selected";
            case "enrolled":
                return "Enrolled";
            case "cancelled":
                return "Cancelled";
            case "not_selected":
                return "Not Selected";
            default:
                return status;
        }
    }

    /**
     * Maps waitlist status values to badge background colors.
     *
     * @param status raw status from Firestore
     * @return parsed color integer associated with the status
     */
    private int getStatusColor(String status) {
        if (status == null) return Color.parseColor("#9CA3AF");
        switch (status) {
            case "waiting":
                return Color.parseColor("#2563EB");
            case "selected":
                return Color.parseColor("#16A34A");
            case "enrolled":
                return Color.parseColor("#9333EA");
            case "cancelled":
                return Color.parseColor("#DC2626");
            case "not_selected":
                return Color.parseColor("#6B7280");
            default:
                return Color.parseColor("#9CA3AF");
        }
    }

    /**
     * ViewHolder representing one entrant row in the list.
     */
    static class EntrantViewHolder extends RecyclerView.ViewHolder {

        /** Name label. */
        TextView tvEntrantName;

        /** Email label. */
        TextView tvEntrantEmail;

        /** Join date label. */
        TextView tvRegisteredDate;

        /** Status badge label. */
        TextView tvStatus;

        /**
         * Creates a view holder bound to a row view.
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