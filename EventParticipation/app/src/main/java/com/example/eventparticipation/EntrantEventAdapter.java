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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * RecyclerView adapter for displaying events to entrants on their dashboard.
 *
 * <p>Shows event details like poster, name, date, location, and registration status.</p>
 */
public class EntrantEventAdapter extends RecyclerView.Adapter<EntrantEventAdapter.EventViewHolder> {

    private final List<Event> events;
    private final OnEntrantEventClickListener listener;
    private final SimpleDateFormat dateFormat;

    /**
     * Interface for handling clicks on entrant events.
     */
    public interface OnEntrantEventClickListener {
        void onEventClick(Event event);
    }

    public EntrantEventAdapter(List<Event> events, OnEntrantEventClickListener listener) {
        this.events = events;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvEventName.setText(event.getName());
        holder.tvEventPrice.setText("Free"); // Default to Free as price is not in Event model

        // Description is not in the model, using a placeholder or empty
        holder.tvEventDescription.setVisibility(View.GONE);

        if (event.getStartTime() != null) {
            holder.tvEventDate.setText(dateFormat.format(event.getStartTime()));
        } else {
            holder.tvEventDate.setText("Date TBD");
        }

        holder.tvEventLocation.setText(event.getVenueAddress() != null ? event.getVenueAddress() : "Venue TBD");

        String counts = event.getEnrolledCount() + "/" + event.getCapacity() + " enrolled • " +
                event.getWaitingCount() + " waiting";
        holder.tvEventCounts.setText(counts);

        // Handle countdown if registration end is available
        if (event.getRegistrationEnd() != null) {
            long diff = event.getRegistrationEnd().getTime() - System.currentTimeMillis();
            if (diff > 0) {
                long days = TimeUnit.MILLISECONDS.toDays(diff);
                long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
                holder.tvCountdown.setText(days + "d " + hours + "h left");
                holder.tvCountdown.setVisibility(View.VISIBLE);
            } else {
                holder.tvCountdown.setText("Registration Closed");
                holder.tvCountdown.setVisibility(View.VISIBLE);
            }
        } else {
            holder.tvCountdown.setVisibility(View.GONE);
        }

        // Tags - can be static for now as not in model
        holder.tvTag1.setText("Event");
        holder.tvTag2.setVisibility(View.GONE);

        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(event.getPosterUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(holder.ivEventPoster);
        } else {
            holder.ivEventPoster.setImageResource(R.drawable.ic_image_placeholder);
        }

        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEventPoster;
        TextView tvEventName, tvEventPrice, tvEventDescription;
        TextView tvEventDate, tvEventLocation, tvEventCounts;
        TextView tvTag1, tvTag2, tvCountdown;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventPoster = itemView.findViewById(R.id.ivEventPoster);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventPrice = itemView.findViewById(R.id.tvEventPrice);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvEventLocation = itemView.findViewById(R.id.tvEventLocation);
            tvEventCounts = itemView.findViewById(R.id.tvEventCounts);
            tvTag1 = itemView.findViewById(R.id.tvTag1);
            tvTag2 = itemView.findViewById(R.id.tvTag2);
            tvCountdown = itemView.findViewById(R.id.tvCountdown);
        }
    }
}
