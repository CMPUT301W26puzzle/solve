package com.example.eventparticipation.organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventparticipation.R;
import com.example.eventparticipation.universal.Event;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Adapter for rendering a list of events accessible to the current co-organizer.
 * * <p>This adapter is responsible for displaying the event cards on the co-organizer
 * dashboard, mapping event data (like name, venue, and participation counts) to the
 * respective view components.</p>
 */
public class CoOrganizerEventAdapter extends RecyclerView.Adapter<CoOrganizerEventAdapter.ViewHolder> {

    /**
     * Interface definition for a callback to be invoked when an event card or its
     * manage button is clicked.
     */
    public interface OnEventClickListener {
        /**
         * Called when a specific event item has been clicked.
         * * @param event The event associated with the clicked UI element.
         */
        void onEventClick(Event event);
    }

    private final List<Event> events;
    private final OnEventClickListener listener;

    /**
     * Initializes the adapter with a dataset and a click listener.
     * * @param events   The list of events the co-organizer has access to.
     * @param listener The callback interface for handling user interactions with the event cards.
     */
    public CoOrganizerEventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    /**
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent an item.
     * * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_co_organizer_event, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * * <p>This method updates the contents of the {@link ViewHolder#itemView} to reflect the item at the
     * given position, binding event metadata and attaching click listeners to interact with it.</p>
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvEventName.setText(event.getName() == null ? "Event" : event.getName());
        holder.tvVenue.setText(event.getVenueAddress() == null ? "Venue TBD" : event.getVenueAddress());
        holder.tvCounts.setText(event.getEnrolledCount() + " enrolled • " + event.getWaitingCount() + " waiting");

        holder.btnManage.setOnClickListener(v -> listener.onEventClick(event));
        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * * @return The total number of events in the list. Returns 0 if the list is null.
     */
    @Override
    public int getItemCount() {
        return events == null ? 0 : events.size();
    }

    /**
     * Represents the view architecture for a single Co-Organizer Event item.
     * Caches UI references to prevent expensive layout lookups during scroll rendering.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName;
        TextView tvVenue;
        TextView tvCounts;
        MaterialButton btnManage;

        /**
         * Binds the XML layout IDs to the local View objects.
         * * @param itemView The root layout view of the list item.
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvVenue = itemView.findViewById(R.id.tvVenue);
            tvCounts = itemView.findViewById(R.id.tvCounts);
            btnManage = itemView.findViewById(R.id.btnManageEvent);
        }
    }
}