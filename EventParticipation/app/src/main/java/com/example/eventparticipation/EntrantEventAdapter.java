package com.example.eventparticipation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * RecyclerView adapter for displaying events on the entrant dashboard.
 *
 * <p>Renders event cards with poster image, title, price, description,
 * date, location, waiting/enrolled counts, tags, and registration countdown.</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 *     <li>US 01.01.01 - Join waiting list (card tap navigates to detail)</li>
 *     <li>US 01.05.04 - Show waiting list count on card</li>
 * </ul>
 */
public class EntrantEventAdapter extends RecyclerView.Adapter<EntrantEventAdapter.EntrantEventViewHolder> {

    /** Event items to display. */
    private List<Event> events;

    /** Callback for card tap navigation. */
    private OnEntrantEventClickListener listener;

    /** Date formatter for event start time. */
    private SimpleDateFormat dateFormat;

    /**
     * Click listener interface for entrant event cards.
     */
    public interface OnEntrantEventClickListener {
        void onEventClick(Event event);
    }

    /**
     * Creates an entrant event adapter.
     *
     * @param events   event list to display
     * @param listener click callback
     */
    public EntrantEventAdapter(List<Event> events, OnEntrantEventClickListener listener) {
        this.events = events;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public EntrantEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant_event, parent, false);
        return new EntrantEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantEventViewHolder holder, int position) {
        Event event = events.get(position);

        // Title
        holder.tvEventName.setText(event.getName() != null ? event.getName() : "");

        // Price badge — show "Free" if capacity is 0 price indicator, otherwise hide for now
        // TODO: add price field to Event model when available
        holder.tvEventPrice.setVisibility(View.GONE);

        // Description — not in model yet, hide for now
        holder.tvEventDescription.setVisibility(View.GONE);

        // Date
        if (event.getRegistrationStart() != null) {
            holder.tvEventDate.setText(dateFormat.format(event.getRegistrationStart()));
        } else {
            holder.tvEventDate.setText("Date TBD");
        }

        // Location
        String location = event.getVenueAddress();
        holder.tvEventLocation.setText(location != null && !location.isEmpty() ? location : "Location TBD");

        // Enrolled / Waiting count (US 01.05.04)
        String counts = event.getEnrolledCount() + "/" + event.getCapacity()
                + " enrolled • " + event.getWaitingCount() + " waiting";
        holder.tvEventCounts.setText(counts);

        // Tags — hidden until tag field added to model
        holder.tvTag1.setVisibility(View.GONE);
        holder.tvTag2.setVisibility(View.GONE);

        // Countdown to registration end
        if (event.getRegistrationEnd() != null) {
            long diff = event.getRegistrationEnd().getTime() - new Date().getTime();
            if (diff > 0) {
                long days = TimeUnit.MILLISECONDS.toDays(diff);
                long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
                holder.tvCountdown.setText(days + "d " + hours + "h left");
                holder.tvCountdown.setVisibility(View.VISIBLE);
            } else {
                holder.tvCountdown.setText("Closed");
                holder.tvCountdown.setTextColor(0xFFAAAAAA);
                holder.tvCountdown.setVisibility(View.VISIBLE);
            }
        } else {
            holder.tvCountdown.setVisibility(View.GONE);
        }

        // Poster image
        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(holder.ivEventPoster.getContext())
                    .load(android.net.Uri.parse(event.getPosterUrl()))
                    .centerCrop()
                    .into(holder.ivEventPoster);
        }

        // Card click → event detail
        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Updates the displayed list and refreshes the RecyclerView.
     *
     * @param newEvents updated event list
     */
    public void updateEvents(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for a single entrant event card.
     */
    static class EntrantEventViewHolder extends RecyclerView.ViewHolder {

        ImageView ivEventPoster;
        TextView tvEventName;
        TextView tvEventPrice;
        TextView tvEventDescription;
        TextView tvEventDate;
        TextView tvEventLocation;
        TextView tvEventCounts;
        TextView tvTag1;
        TextView tvTag2;
        TextView tvCountdown;

        public EntrantEventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventPoster      = itemView.findViewById(R.id.ivEventPoster);
            tvEventName        = itemView.findViewById(R.id.tvEventName);
            tvEventPrice       = itemView.findViewById(R.id.tvEventPrice);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            tvEventDate        = itemView.findViewById(R.id.tvEventDate);
            tvEventLocation    = itemView.findViewById(R.id.tvEventLocation);
            tvEventCounts      = itemView.findViewById(R.id.tvEventCounts);
            tvTag1             = itemView.findViewById(R.id.tvTag1);
            tvTag2             = itemView.findViewById(R.id.tvTag2);
            tvCountdown        = itemView.findViewById(R.id.tvCountdown);
        }
    }
}
