package com.example.eventparticipation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Adapter for events accessible to the current co-organizer.
 */
public class CoOrganizerEventAdapter extends RecyclerView.Adapter<CoOrganizerEventAdapter.ViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final List<Event> events;
    private final OnEventClickListener listener;

    public CoOrganizerEventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_co_organizer_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvEventName.setText(event.getName() == null ? "Event" : event.getName());
        holder.tvVenue.setText(event.getVenueAddress() == null ? "Venue TBD" : event.getVenueAddress());
        holder.tvCounts.setText(event.getEnrolledCount() + " enrolled • " + event.getWaitingCount() + " waiting");

        holder.btnManage.setOnClickListener(v -> listener.onEventClick(event));
        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    @Override
    public int getItemCount() {
        return events == null ? 0 : events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName;
        TextView tvVenue;
        TextView tvCounts;
        MaterialButton btnManage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvVenue = itemView.findViewById(R.id.tvVenue);
            tvCounts = itemView.findViewById(R.id.tvCounts);
            btnManage = itemView.findViewById(R.id.btnManageEvent);
        }
    }
}
