package com.example.eventparticipation.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventparticipation.universal.Event;
import com.example.eventparticipation.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * A multi-purpose RecyclerView adapter for the Admin Dashboard.
 *
 * <p>Dynamically renders different UI layouts based on the type of admin item
 * being displayed (Events, Profiles, Images, or Logs) and provides callbacks
 * for moderation actions (e.g., deletion or viewing details).</p>
 */
public class AdminBrowseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /**
     * Interface definition for callbacks to be invoked when an action is taken on an image item.
     */
    public interface ImageActionListener {
        /**
         * Invoked when the administrator requests to view an image in full screen.
         * @param item The image item to be viewed.
         */
        void onViewImage(AdminImageItem item);

        /**
         * Invoked when the administrator requests to permanently delete an image.
         * @param item The image item to be deleted.
         * @param position The position of the item in the adapter.
         */
        void onDeleteImage(AdminImageItem item, int position);
    }

    /**
     * Interface definition for callbacks to be invoked when an action is taken on an event item.
     */
    public interface EventActionListener {
        /**
         * Invoked when the administrator requests to view the details of an event.
         * @param item The event item to be viewed.
         */
        void onViewEvent(AdminEventItem item);

        /**
         * Invoked when the administrator requests to permanently delete an event.
         * @param item The event item to be deleted.
         * @param position The position of the item in the adapter.
         */
        void onDeleteEvent(AdminEventItem item, int position);
    }

    /**
     * Interface definition for callbacks to be invoked when an action is taken on a user profile.
     */
    public interface ProfileActionListener {
        /**
         * Invoked when the administrator requests to permanently delete a user's profile.
         * @param item The profile item to be deleted.
         * @param position The position of the item in the adapter.
         */
        void onDeleteProfile(AdminProfileItem item, int position);

        /**
         * Invoked when the administrator requests to ban an organizer.
         * @param item The profile item belonging to the organizer to be banned.
         * @param position The position of the item in the adapter.
         */
        void onBanProfile(AdminProfileItem item, int position);
    }

    /**
     * Interface definition for callbacks to be invoked when an action is taken on an event comment.
     */
    public interface CommentActionListener {
        /**
         * Invoked when the administrator requests to delete a comment.
         * @param item The comment item to be deleted.
         * @param position The position of the item in the adapter.
         */
        void onDeleteComment(AdminCommentItem item, int position);
    }

    private static final int TYPE_EVENT = 1;
    private static final int TYPE_PROFILE = 2;
    private static final int TYPE_IMAGE = 3;
    private static final int TYPE_LOG = 4;
    private static final int TYPE_COMMENT = 5;

    private final List<Object> items;
    private final EventActionListener eventActionListener;
    private final ProfileActionListener profileActionListener;
    private final ImageActionListener imageActionListener;
    private final CommentActionListener commentActionListener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("M/d/yyyy, h:mm:ss a", Locale.getDefault());

    /**
     * Constructs the AdminBrowseAdapter with the provided data and action listeners.
     *
     * @param items The heterogeneous list of items to display (events, profiles, images, logs, comments).
     * @param imageActionListener The listener for image-related actions.
     * @param eventActionListener The listener for event-related actions.
     * @param profileActionListener The listener for profile-related actions.
     * @param commentActionListener The listener for comment-related actions.
     */
    public AdminBrowseAdapter(List<Object> items,
                              ImageActionListener imageActionListener,
                              EventActionListener eventActionListener,
                              ProfileActionListener profileActionListener,
                              CommentActionListener commentActionListener) {
        this.items = items;
        this.imageActionListener = imageActionListener;
        this.eventActionListener = eventActionListener;
        this.profileActionListener = profileActionListener;
        this.commentActionListener = commentActionListener;
    }

    /**
     * Determines the integer view type of the item at the given position to inflate the correct layout.
     *
     * @param position The position of the item in the data set.
     * @return An integer representing the item's view type.
     */
    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof AdminEventItem) return TYPE_EVENT;
        if (item instanceof AdminProfileItem) return TYPE_PROFILE;
        if (item instanceof AdminImageItem) return TYPE_IMAGE;
        if (item instanceof AdminCommentItem) return TYPE_COMMENT;
        return TYPE_LOG;
    }

    /**
     * Inflates the appropriate XML layout based on the mapped view type.
     *
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new specialized ViewHolder containing the inflated layout.
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_EVENT) {
            return new EventViewHolder(inflater.inflate(R.layout.item_admin_event, parent, false));
        }
        if (viewType == TYPE_PROFILE) {
            return new ProfileViewHolder(inflater.inflate(R.layout.item_admin_profile, parent, false));
        }
        if (viewType == TYPE_IMAGE) {
            return new ImageViewHolder(inflater.inflate(R.layout.item_admin_image, parent, false));
        }
        if (viewType == TYPE_COMMENT) {
            return new CommentViewHolder(inflater.inflate(R.layout.item_admin_profile, parent, false));
        }
        return new LogViewHolder(inflater.inflate(R.layout.item_admin_log, parent, false));
    }

    /**
     * Binds the data from the backing list to the corresponding view elements within the ViewHolder.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the item.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        if (holder instanceof EventViewHolder) {
            bindEvent((EventViewHolder) holder, (AdminEventItem) item, position);
        } else if (holder instanceof ProfileViewHolder) {
            bindProfile((ProfileViewHolder) holder, (AdminProfileItem) item, position);
        } else if (holder instanceof ImageViewHolder) {
            bindImage((ImageViewHolder) holder, (AdminImageItem) item, position);
        } else if (holder instanceof LogViewHolder) {
            bindLog((LogViewHolder) holder, (AdminNotificationLogItem) item);
        } else if (holder instanceof CommentViewHolder) {
            bindComment((CommentViewHolder) holder, (AdminCommentItem) item, position);
        }
    }

    /**
     * Returns the total number of items contained within the data set held by the adapter.
     *
     * @return The size of the items list.
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Binds event data to an EventViewHolder and attaches action listeners.
     *
     * @param holder The ViewHolder designated for event presentation.
     * @param item The AdminEventItem wrapping the core Event data.
     * @param position The position of the item in the list layout.
     */
    private void bindEvent(EventViewHolder holder, AdminEventItem item, int position) {
        Event event = item.getEvent();

        holder.tvTitle.setText(valueOrFallback(event.getName(), "Untitled event"));
        holder.tvMetaOne.setText("Organizer: " + valueOrFallback(event.getOrganizerId(), "Unknown organizer"));
        holder.tvMetaTwo.setText("Capacity: " + event.getCapacity());
        holder.tvMetaThree.setText(
                event.getRegistrationStart() != null
                        ? dateFormat.format(event.getRegistrationStart())
                        : "No date available"
        );

        holder.btnView.setOnClickListener(v -> {
            if (eventActionListener != null) {
                eventActionListener.onViewEvent(item);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (eventActionListener != null) {
                eventActionListener.onDeleteEvent(item, holder.getBindingAdapterPosition());
            }
        });

        holder.divider.setVisibility(position == getItemCount() - 1 ? View.GONE : View.VISIBLE);
    }

    /**
     * Binds image reference data to an ImageViewHolder and attaches action listeners.
     * Uses Glide to securely load image thumbnails.
     *
     * @param holder The ViewHolder designated for image presentation.
     * @param item The AdminImageItem containing URL and metadata.
     * @param position The position of the item in the list layout.
     */
    private void bindImage(ImageViewHolder holder, AdminImageItem item, int position) {
        holder.tvTitle.setText(item.getTitle());
        holder.tvMeta.setText(item.getImageType());

        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.imageView);

        holder.btnViewImage.setOnClickListener(v -> {
            if (imageActionListener != null) imageActionListener.onViewImage(item);
        });

        holder.btnDeleteImage.setOnClickListener(v -> {
            if (imageActionListener != null)
                imageActionListener.onDeleteImage(item, holder.getBindingAdapterPosition());
        });
    }

    /**
     * Binds user profile data to a ProfileViewHolder and sets up moderation options.
     *
     * @param holder The ViewHolder mapped to profile rendering.
     * @param item The AdminProfileItem encapsulating user data.
     * @param position The position of the item in the list layout.
     */
    private void bindProfile(ProfileViewHolder holder, AdminProfileItem item, int position) {
        holder.tvTitle.setText(valueOrFallback(item.getName(), "Unnamed user"));
        holder.tvMetaTwo.setText(valueOrFallback(item.getEmail(), "No email"));
        holder.tvMetaThree.setText("ID: " + valueOrFallback(item.getProfileId(), "N/A"));

        // Only show Ban button for organizers
        boolean isOrganizer = "organizer".equals(item.getRole());
        holder.btnBan.setVisibility(isOrganizer ? View.VISIBLE : View.GONE);

        holder.btnBan.setOnClickListener(v -> {
            if (profileActionListener != null)
                profileActionListener.onBanProfile(item, holder.getBindingAdapterPosition());
        });

        holder.btnRemove.setOnClickListener(v -> {
            if (profileActionListener != null)
                profileActionListener.onDeleteProfile(item, holder.getBindingAdapterPosition());
        });

        holder.divider.setVisibility(position == getItemCount() - 1 ? View.GONE : View.VISIBLE);
    }

    /**
     * Binds an immutable notification log structure into a view-only LogViewHolder.
     *
     * @param holder The ViewHolder mapped to historical notification logs.
     * @param item The Log data item.
     */
    private void bindLog(LogViewHolder holder, AdminNotificationLogItem item) {
        holder.tvTitle.setText(valueOrFallback(item.getEventName(), "Notification log"));
        holder.tvMetaOne.setText("Entrant ID: " + valueOrFallback(item.getEntrantId(), "N/A"));
        holder.tvMetaTwo.setText("Type: " + valueOrFallback(item.getType(), "N/A")
                + "   Status: " + valueOrFallback(item.getActionStatus(), "none"));
        holder.tvMetaThree.setText(item.getCreatedAt() == null
                ? "Created: N/A"
                : "Created: " + dateFormat.format(item.getCreatedAt()));
        holder.tvMessage.setText(valueOrFallback(item.getMessage(), "No message"));
    }

    /**
     * Binds an event comment to a CommentViewHolder, exposing moderation (delete) actions.
     *
     * @param holder The ViewHolder designated to render the comment.
     * @param item The AdminCommentItem encapsulating the comment text and metadata.
     * @param position The position of the item in the list layout.
     */
    private void bindComment(CommentViewHolder holder, AdminCommentItem item, int position) {
        holder.tvTitle.setText(valueOrFallback(item.getUserName(), "Anonymous"));
        holder.tvMetaTwo.setText(valueOrFallback(item.getText(), ""));
        holder.tvMetaThree.setText("Event ID: " + valueOrFallback(item.getEventId(), "N/A"));
        holder.btnRemove.setVisibility(View.VISIBLE);

        holder.btnRemove.setOnClickListener(v -> {
            if (commentActionListener != null) {
                commentActionListener.onDeleteComment(item, holder.getBindingAdapterPosition());
            }
        });

        holder.divider.setVisibility(position == getItemCount() - 1 ? View.GONE : View.VISIBLE);
    }

    /**
     * A utility method to safely process potentially null or empty text values.
     *
     * @param value The raw string retrieved from the item model.
     * @param fallback The generic placeholder to utilize if the value is missing.
     * @return Cleanly processed string output ready for rendering.
     */
    private String valueOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    /**
     * Sub-class representing structural components of a standard Event layout item.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvMetaOne;
        TextView tvMetaTwo;
        TextView tvMetaThree;
        MaterialButton btnView;
        MaterialButton btnDelete;
        View divider;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMetaOne = itemView.findViewById(R.id.tvMetaOne);
            tvMetaTwo = itemView.findViewById(R.id.tvMetaTwo);
            tvMetaThree = itemView.findViewById(R.id.tvMetaThree);
            btnView = itemView.findViewById(R.id.btnView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            divider = itemView.findViewById(R.id.divider);
        }
    }

    /**
     * Sub-class representing structural components of a User Profile layout item.
     */
    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMetaTwo, tvMetaThree;
        MaterialButton btnRemove, btnBan;
        View divider;

        ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMetaTwo = itemView.findViewById(R.id.tvMetaTwo);
            tvMetaThree = itemView.findViewById(R.id.tvMetaThree);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            btnBan = itemView.findViewById(R.id.btnBan);
            divider = itemView.findViewById(R.id.divider);
        }
    }

    /**
     * Sub-class representing structural components of an Event Comment layout item.
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvMetaTwo;
        TextView tvMetaThree;
        MaterialButton btnRemove;
        View divider;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMetaTwo = itemView.findViewById(R.id.tvMetaTwo);
            tvMetaThree = itemView.findViewById(R.id.tvMetaThree);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            divider = itemView.findViewById(R.id.divider);
        }
    }

    /**
     * Sub-class representing structural components of an Image layout item.
     */
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvTitle, tvMeta;
        MaterialButton btnViewImage, btnDeleteImage;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            btnViewImage = itemView.findViewById(R.id.btnViewImage);
            btnDeleteImage = itemView.findViewById(R.id.btnDeleteImage); // add this to your XML too
        }
    }

    /**
     * Sub-class representing structural components of an unmodifiable Log history layout item.
     */
    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvMetaOne;
        TextView tvMetaTwo;
        TextView tvMetaThree;
        TextView tvMessage;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMetaOne = itemView.findViewById(R.id.tvMetaOne);
            tvMetaTwo = itemView.findViewById(R.id.tvMetaTwo);
            tvMetaThree = itemView.findViewById(R.id.tvMetaThree);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}