package com.example.eventparticipation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

    public interface ImageClickListener {
        void onImageClick(AdminImageItem item);
    }

    public interface EventActionListener {
        void onViewEvent(AdminEventItem item);
        void onDeleteEvent(AdminEventItem item, int position);
    }

    public interface ProfileActionListener {
        void onDeleteProfile(AdminProfileItem item, int position);
    }

    public interface CommentActionListener {
        void onDeleteComment(AdminCommentItem item, int position);
    }

    private static final int TYPE_EVENT = 1;
    private static final int TYPE_PROFILE = 2;
    private static final int TYPE_IMAGE = 3;
    private static final int TYPE_LOG = 4;
    private static final int TYPE_COMMENT = 5;

    private final List<Object> items;
    private final ImageClickListener imageClickListener;
    private final EventActionListener eventActionListener;
    private final ProfileActionListener profileActionListener;
    private final CommentActionListener commentActionListener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("M/d/yyyy, h:mm:ss a", Locale.getDefault());

    public AdminBrowseAdapter(List<Object> items,
                              ImageClickListener imageClickListener,
                              EventActionListener eventActionListener,
                              ProfileActionListener profileActionListener,
                              CommentActionListener commentActionListener) {
        this.items = items;
        this.imageClickListener = imageClickListener;
        this.eventActionListener = eventActionListener;
        this.profileActionListener = profileActionListener;
        this.commentActionListener = commentActionListener;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof AdminEventItem) return TYPE_EVENT;
        if (item instanceof AdminProfileItem) return TYPE_PROFILE;
        if (item instanceof AdminImageItem) return TYPE_IMAGE;
        if (item instanceof AdminCommentItem) return TYPE_COMMENT;
        return TYPE_LOG;
    }

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

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        if (holder instanceof EventViewHolder) {
            bindEvent((EventViewHolder) holder, (AdminEventItem) item, position);
        } else if (holder instanceof ProfileViewHolder) {
            bindProfile((ProfileViewHolder) holder, (AdminProfileItem) item, position);
        } else if (holder instanceof ImageViewHolder) {
            bindImage((ImageViewHolder) holder, (AdminImageItem) item);
        } else if (holder instanceof LogViewHolder) {
            bindLog((LogViewHolder) holder, (AdminNotificationLogItem) item);
        } else if (holder instanceof CommentViewHolder) {
            bindComment((CommentViewHolder) holder, (AdminCommentItem) item, position);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

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

    private void bindProfile(ProfileViewHolder holder, AdminProfileItem item, int position) {
        holder.tvTitle.setText(valueOrFallback(item.getName(), "Unnamed user"));
        holder.tvMetaTwo.setText(valueOrFallback(item.getEmail(), "No email"));
        holder.tvMetaThree.setText("Joined: " + valueOrFallback(item.getProfileId(), "N/A"));
        holder.btnRemove.setVisibility(View.VISIBLE);

        holder.btnRemove.setOnClickListener(v -> {
            if (profileActionListener != null) {
                profileActionListener.onDeleteProfile(item, holder.getBindingAdapterPosition());
            }
        });

        holder.divider.setVisibility(position == getItemCount() - 1 ? View.GONE : View.VISIBLE);
    }

    private void bindImage(ImageViewHolder holder, AdminImageItem item) {
        holder.tvTitle.setText(item.getTitle());
        holder.tvMeta.setText(item.getImageType());

        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.imageView);

        holder.btnViewImage.setOnClickListener(v -> imageClickListener.onImageClick(item));
    }

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

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

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

    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvMetaTwo;
        TextView tvMetaThree;
        MaterialButton btnRemove;
        View divider;

        ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMetaTwo = itemView.findViewById(R.id.tvMetaTwo);
            tvMetaThree = itemView.findViewById(R.id.tvMetaThree);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            divider = itemView.findViewById(R.id.divider);
        }
    }

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

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvTitle;
        TextView tvMeta;
        MaterialButton btnViewImage;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            btnViewImage = itemView.findViewById(R.id.btnViewImage);
        }
    }

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