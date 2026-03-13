package com.example.eventparticipation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying events on the organizer dashboard.
 *
 * <p>The adapter renders basic event information and forwards card actions through
 * {@link OnEventClickListener}.</p>
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    /** Event items displayed in the dashboard. */
    private final List<Event> events;

    /** Callback listener handling event card actions. */
    private final OnEventClickListener listener;

    /** Formatter used for displaying event dates. */
    private final SimpleDateFormat dateFormat;

    /**
     * Creates an event adapter.
     *
     * @param events event list to display
     * @param listener listener for click actions
     */
    public EventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    /**
     * Inflates a single organizer event item view.
     *
     * @param parent parent view group
     * @param viewType item view type
     * @return new event view holder
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_event, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds event data and click listeners to the row at the given position.
     *
     * @param holder row view holder
     * @param position adapter position
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);


        holder.tvEventName.setText(event.getName());


        if (event.getRegistrationStart() != null && event.getRegistrationEnd() != null) {

            String start = dateFormat.format(event.getRegistrationStart());
            String end = dateFormat.format(event.getRegistrationEnd());

            holder.tvEventDate.setText(start + " - " + end);

        } else {
            holder.tvEventDate.setText("Date TBD");
        }

        holder.tvEventCapacity.setText("Capacity: " + event.getCapacity());

        holder.btnManageEvent.setOnClickListener(v -> listener.onManageClick(event));
        holder.btnViewEntrants.setOnClickListener(v -> listener.onEntrantsClick(event));
        holder.btnRunLottery.setOnClickListener(v -> listener.onLotteryClick(event));
        holder.btnShowQR.setOnClickListener(v -> listener.onQRCodeClick(event));
        holder.btnViewEvent.setOnClickListener(v -> listener.onViewClick(event));
    }

    /**
     * Returns the number of event rows displayed by the adapter.
     *
     * @return event count
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for a single organizer event card.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {

        /** Event name label. */
        TextView tvEventName;

        /** Event date label. */
        TextView tvEventDate;

        /** Event capacity label. */
        TextView tvEventCapacity;

        /** Button opening event management. */
        MaterialButton btnManageEvent;

        /** Button opening entrant list. */
        LinearLayout btnViewEntrants;

        /** Button triggering lottery flow. */
        LinearLayout btnRunLottery;

        /** Button opening QR code view. */
        LinearLayout btnShowQR;

        /** Button opening event detail view. */
        LinearLayout btnViewEvent;

        /**
         * Creates a ViewHolder for the organizer event card.
         *
         * @param itemView inflated item view
         */
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvEventCapacity = itemView.findViewById(R.id.tvEventCapacity);
            btnManageEvent = itemView.findViewById(R.id.btnManageEvent);
            btnViewEntrants = itemView.findViewById(R.id.btnViewEntrants);
            btnRunLottery = itemView.findViewById(R.id.btnRunLottery);
            btnShowQR = itemView.findViewById(R.id.btnShowQR);
            btnViewEvent = itemView.findViewById(R.id.btnViewEvent);
        }
    }
}