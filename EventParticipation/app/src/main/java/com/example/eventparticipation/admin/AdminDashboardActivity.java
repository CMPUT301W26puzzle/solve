package com.example.eventparticipation.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.eventparticipation.universal.Comment;
import com.example.eventparticipation.universal.Event;
import com.example.eventparticipation.organizer.ManageEventActivity;
import com.example.eventparticipation.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Main control panel for Administrator users.
 *
 * <p>Provides a tabbed interface allowing admins to browse and moderate events,
 * user profiles, uploaded images, and system notification logs.</p>
 *
 * <p>Relevant user stories:</p>
 * <ul>
 * <li>US 03.01.01 Remove events</li>
 * <li>US 03.02.01 Remove profiles</li>
 * <li>US 03.03.01 Remove uploaded images</li>
 * <li>US 03.04.01 Browse events</li>
 * <li>US 03.05.01 Browse profiles</li>
 * <li>US 03.06.01 Browse uploaded images</li>
 * <li>US 03.08.01 Review notification logs</li>
 * </ul>
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private TextView btnEvents;
    private TextView btnProfiles;
    private TextView btnImages;
    private TextView btnOrganizers;
    private TextView btnLogs;
    private TextView btnComments;

    private TextView tvEventCount;
    private TextView tvUserCount;
    private TextView tvImageCount;

    private TextView tvSectionTitle;
    private TextView tvSectionSubtitle;

    private View progressBar;
    private TextView tvEmpty;
    private RecyclerView recyclerView;

    private FirebaseFirestore db;

    private final List<Object> items = new ArrayList<>();
    private AdminBrowseAdapter adapter;

    private String activeTab = "profiles";

    /**
     * Initializes the activity, binds views, sets up the RecyclerView,
     * and loads initial dashboard data.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();

        bindViews();
        setupRecycler();
        setupListeners();
        loadDashboardCounts();
        selectTab("profiles");
    }

    /**
     * Navigates to the ManageEventActivity for a specified event item.
     *
     * @param item The selected admin event item.
     */
    private void openEvent(AdminEventItem item) {
        if (item == null || item.getEvent() == null) {
            Toast.makeText(this, "Event unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        Event event = item.getEvent();

        Intent intent = new Intent(this, ManageEventActivity.class);
        intent.putExtra("EVENT_ID", event.getId());
        intent.putExtra("ORGANIZER_ID", event.getOrganizerId());
        startActivity(intent);
    }

    /**
     * Displays a confirmation dialog before permanently deleting an event.
     *
     * @param item     The event item slated for deletion.
     * @param position The position of the event in the RecyclerView.
     */
    private void confirmDeleteEvent(AdminEventItem item, int position) {
        if (item == null || item.getEvent() == null) {
            Toast.makeText(this, "Event unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        Event event = item.getEvent();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete event?")
                .setMessage("This will permanently delete \"" + safe(event.getName()) + "\".")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent(item, position))
                .show();
    }

    /**
     * Permanently removes an event and its waitlist dependencies from Firestore.
     *
     * @param item     The event item to delete.
     * @param position The position of the item in the adapter list.
     */
    private void deleteEvent(AdminEventItem item, int position) {
        Event event = item.getEvent();
        if (event == null || event.getId() == null || event.getId().trim().isEmpty()) {
            Toast.makeText(this, "Invalid event", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        String eventId = event.getId();
        String organizerId = event.getOrganizerId();

        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .get()
                .addOnSuccessListener(waitlistSnapshot -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();

                    for (DocumentSnapshot waitDoc : waitlistSnapshot.getDocuments()) {
                        batch.delete(waitDoc.getReference());
                    }

                    batch.delete(db.collection("events").document(eventId));

                    if (organizerId != null && !organizerId.trim().isEmpty()) {
                        batch.delete(
                                db.collection("organizers")
                                        .document(organizerId)
                                        .collection("events")
                                        .document(eventId)
                        );
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                deleteEventStorageFiles(event);
                                removeEventFromList(position);
                                loadDashboardCounts();
                                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Failed to delete event", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to load event dependencies", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Cleans up orphaned images (poster, QR code) from Firebase Storage
     * when an event is deleted.
     *
     * @param event The event being deleted.
     */
    private void deleteEventStorageFiles(Event event) {
        if (event == null) return;

        String posterUrl = event.getPosterUrl();
        if (posterUrl != null && !posterUrl.trim().isEmpty()) {
            try {
                com.google.firebase.storage.FirebaseStorage.getInstance()
                        .getReferenceFromUrl(posterUrl)
                        .delete();
            } catch (Exception ignored) {
            }
        }

        try {
            com.google.firebase.storage.FirebaseStorage.getInstance()
                    .getReference()
                    .child("qrcodes/" + safe(event.getOrganizerId()) + "/" + safe(event.getId()) + ".png")
                    .delete();
        } catch (Exception ignored) {
        }
    }

    /**
     * Removes a deleted event from the UI lists and handles empty states.
     *
     * @param position The position of the deleted event in the RecyclerView.
     */
    private void removeEventFromList(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, items.size() - position);
        }

        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText("No events found");
    }

    /**
     * Displays a confirmation dialog before deleting or banning a user profile.
     *
     * @param item     The profile item selected for deletion/banning.
     * @param position The position of the profile in the RecyclerView.
     */
    private void confirmDeleteProfile(AdminProfileItem item, int position) {
        if (item == null || item.getProfileId() == null) {
            Toast.makeText(this, "Profile unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        // Offer Ban vs Delete only for organizers
        if ("organizer".equals(item.getRole())) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Action for \"" + safe(item.getName()) + "\"")
                    .setMessage("Ban hides the organizer and their events. Delete permanently removes them.")
                    .setNeutralButton("Cancel", null)
                    .setNegativeButton("Delete", (dialog, which) -> deleteProfile(item, position))
                    .setPositiveButton("Ban", (dialog, which) -> banOrganizer(item, position))
                    .show();
        } else {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete profile?")
                    .setMessage("This will permanently delete \"" + safe(item.getName()) + "\".")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> deleteProfile(item, position))
                    .show();
        }
    }

    /**
     * Flags an organizer profile as banned in Firestore and hides their active events.
     *
     * @param item     The organizer profile to be banned.
     * @param position The position of the profile in the RecyclerView.
     */
    private void banOrganizer(AdminProfileItem item, int position) {
        showLoading(true);
        String organizerId = item.getProfileId();

        // Mark organizer as banned in Firestore
        db.collection("organizers")
                .document(organizerId)
                .update("banned", true)
                .addOnSuccessListener(unused -> hideOrganizerEvents(organizerId, () -> {
                    removeProfileFromList(position);
                    loadDashboardCounts();
                    Toast.makeText(this, "Organizer banned and events hidden", Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to ban organizer", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates all events belonging to a specific organizer to be hidden from public view.
     *
     * @param organizerId The ID of the targeted organizer.
     * @param onComplete  A callback to execute upon successful hiding of the events.
     */
    private void hideOrganizerEvents(String organizerId, Runnable onComplete) {
        db.collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.update(doc.getReference(), "hidden", true);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> onComplete.run())
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Organizer banned but events could not be hidden", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to fetch organizer events", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Prompts the admin to confirm the deletion of an uploaded image.
     *
     * @param item     The image item selected.
     * @param position The position of the item in the list.
     */
    private void confirmDeleteImage(AdminImageItem item, int position) {
        if (item == null || item.getImageUrl().isEmpty()) {
            Toast.makeText(this, "Image unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete image?")
                .setMessage("This will permanently delete the image for \"" + item.getTitle() + "\".")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteImage(item, position))
                .show();
    }

    /**
     * Deletes a physical file (image) from Firebase Storage.
     *
     * @param item     The image item containing the cloud storage URL.
     * @param position The position of the item in the RecyclerView.
     */
    private void deleteImage(AdminImageItem item, int position) {
        showLoading(true);

        try {
            com.google.firebase.storage.FirebaseStorage.getInstance()
                    .getReferenceFromUrl(item.getImageUrl())
                    .delete()
                    .addOnSuccessListener(unused -> clearImageReference(item, position))
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Failed to delete image from storage", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            showLoading(false);
            Toast.makeText(this, "Invalid image URL", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Detaches the cleared image URL from the respective parent document in Firestore.
     *
     * @param item     The image item referencing the parent document.
     * @param position The list position to update.
     */
    private void clearImageReference(AdminImageItem item, int position) {
        String field;
        if ("Event poster".equals(item.getImageType())) {
            field = "posterUrl";
        } else if ("Event QR code".equals(item.getImageType())) {
            field = "qrCodeUrl";
        } else {
            // For profile images, adjust collection/field as needed
            field = "profileImageUrl";
        }

        db.collection("events")
                .document(item.getSourceId())
                .update(field, "") // clear the URL
                .addOnSuccessListener(unused -> {
                    removeImageFromList(position);
                    loadDashboardCounts();
                    Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Image deleted from storage but reference not cleared", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Removes a deleted image from the UI lists and updates empty states.
     *
     * @param position The position of the deleted image.
     */
    private void removeImageFromList(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, items.size() - position);
        }

        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText("No uploaded images found");
    }

    /**
     * Permanently deletes a user's profile from their respective collection.
     *
     * @param item     The profile to be removed.
     * @param position The position of the profile in the list.
     */
    private void deleteProfile(AdminProfileItem item, int position) {
        if (item == null || item.getProfileId() == null || item.getProfileId().trim().isEmpty()) {
            Toast.makeText(this, "Invalid profile", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        String profileId = item.getProfileId();
        String collection = item.getRole() + "s";

        if ("organizer".equals(item.getRole())) {
            deleteOrganizerAndEvents(item, position);
        } else {
            db.collection(collection)
                    .document(profileId)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        removeProfileFromList(position);
                        loadDashboardCounts();
                        Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Fully purges an organizer and cascades the deletion to all their associated events.
     *
     * @param item     The organizer profile to delete.
     * @param position The list position of the item.
     */
    private void deleteOrganizerAndEvents(AdminProfileItem item, int position) {
        String organizerId = item.getProfileId();

        db.collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setId(doc.getId());
                            deleteEventStorageFiles(event);
                        }
                        batch.delete(doc.getReference());
                    }

                    // Delete the organizer document
                    batch.delete(db.collection("organizers").document(organizerId));

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                removeProfileFromList(position);
                                loadDashboardCounts();
                                Toast.makeText(this, "Organizer and their events deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Failed to complete deletion", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to fetch organizer events", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the UI to reflect a deleted profile.
     *
     * @param position The index of the deleted element in the list.
     */
    private void removeProfileFromList(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, items.size() - position);
        }

        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText("No profiles found");
    }

    /**
     * Asks the admin for confirmation before destroying a user comment.
     *
     * @param item     The comment in question.
     * @param position The index of the comment in the view list.
     */
    private void confirmDeleteComment(AdminCommentItem item, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete comment?")
                .setMessage("Are you sure you want to remove this comment?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteComment(item, position))
                .show();
    }

    /**
     * Executes the deletion of a specific event comment from the Firestore backend.
     *
     * @param item     The comment data.
     * @param position List index to animate out upon success.
     */
    private void deleteComment(AdminCommentItem item, int position) {
        showLoading(true);
        db.collection("events").document(item.getEventId())
                .collection("comments").document(item.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    if (position >= 0 && position < items.size()) {
                        items.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, items.size() - position);
                    }
                    showLoading(false);
                    Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show();
                    if (items.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("No comments found");
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to delete comment", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Binds layout views to internal class references.
     */
    private void bindViews() {
        btnEvents = findViewById(R.id.btnTabEvents);
        btnProfiles = findViewById(R.id.btnTabProfiles);
        btnImages = findViewById(R.id.btnTabImages);
        btnOrganizers = findViewById(R.id.btnTabOrganizers);
        btnLogs = findViewById(R.id.btnTabLogs);
        btnComments = findViewById(R.id.btnTabComments);

        tvEventCount = findViewById(R.id.tvEventCount);
        tvUserCount = findViewById(R.id.tvUserCount);
        tvImageCount = findViewById(R.id.tvImageCount);

        tvSectionTitle = findViewById(R.id.tvSectionTitle);
        tvSectionSubtitle = findViewById(R.id.tvSectionSubtitle);

        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmptyState);
        recyclerView = findViewById(R.id.rvAdminItems);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    /**
     * Attaches the RecyclerView adapter and configures action callback listeners
     * for varying item types in the list.
     */
    private void setupRecycler() {
        adapter = new AdminBrowseAdapter(
                items,
                new AdminBrowseAdapter.ImageActionListener() {
                    @Override
                    public void onViewImage(AdminImageItem item) {
                        openImagePreview(item);
                    }

                    @Override
                    public void onDeleteImage(AdminImageItem item, int position) {
                        confirmDeleteImage(item, position);
                    }
                },
                new AdminBrowseAdapter.EventActionListener() {
                    @Override
                    public void onViewEvent(AdminEventItem item) {
                        openEvent(item);
                    }

                    @Override
                    public void onDeleteEvent(AdminEventItem item, int position) {
                        confirmDeleteEvent(item, position);
                    }
                },
                new AdminBrowseAdapter.ProfileActionListener() {
                    @Override
                    public void onDeleteProfile(AdminProfileItem item, int position) {
                        confirmDeleteProfile(item, position);
                    }
                    @Override
                    public void onBanProfile(AdminProfileItem item, int position) {
                        banOrganizer(item, position);
                    }
                },
                new AdminBrowseAdapter.CommentActionListener() {
                    @Override
                    public void onDeleteComment(AdminCommentItem item, int position) {
                        confirmDeleteComment(item, position);
                    }
                }
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Configures click interactions for top navigation tabs.
     */
    private void setupListeners() {
        btnEvents.setOnClickListener(v -> selectTab("events"));
        btnProfiles.setOnClickListener(v -> selectTab("profiles"));
        btnImages.setOnClickListener(v -> selectTab("images"));
        btnOrganizers.setOnClickListener(v -> selectTab("organizers"));
        btnLogs.setOnClickListener(v -> selectTab("logs"));
        btnComments.setOnClickListener(v -> selectTab("comments"));
    }

    /**
     * Updates UI and fetches target data associated with the active navigation tab.
     *
     * @param tab The identifier of the selected tab context (e.g., "events", "profiles").
     */
    private void selectTab(@NonNull String tab) {
        activeTab = tab;
        updateTabStyles();
        updateSectionHeader(tab);

        if ("events".equals(tab)) {
            loadEvents();
        } else if ("profiles".equals(tab)) {
            loadProfiles(false);
        } else if ("images".equals(tab)) {
            loadImages();
        } else if ("organizers".equals(tab)) {
            loadProfiles(true);
        } else if ("comments".equals(tab)) {
            loadComments();
        } else {
            loadNotificationLogs();
        }
    }

    /**
     * Loops over navigation tabs to apply selected/unselected visual states.
     */
    private void updateTabStyles() {
        setTabStyle(btnEvents, "events".equals(activeTab));
        setTabStyle(btnProfiles, "profiles".equals(activeTab));
        setTabStyle(btnImages, "images".equals(activeTab));
        setTabStyle(btnOrganizers, "organizers".equals(activeTab));
        setTabStyle(btnLogs, "logs".equals(activeTab));
        setTabStyle(btnComments, "comments".equals(activeTab));
    }

    /**
     * Adjusts the background and text color of a navigation tab element.
     *
     * @param tab      The tab TextView to modify.
     * @param selected True if the tab is active.
     */
    private void setTabStyle(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tab.setTextColor(ContextCompat.getColor(this, selected ? android.R.color.black : android.R.color.darker_gray));
    }

    /**
     * Modifies the title headers to match the selected category context.
     *
     * @param tab The target tab identifier string.
     */
    private void updateSectionHeader(String tab) {
        if ("events".equals(tab)) {
            tvSectionTitle.setText("All Events");
            tvSectionSubtitle.setText("Browse all events in the system");
        } else if ("profiles".equals(tab)) {
            tvSectionTitle.setText("All User Profiles");
            tvSectionSubtitle.setText("Manage user accounts and remove violators");
        } else if ("images".equals(tab)) {
            tvSectionTitle.setText("Uploaded Images");
            tvSectionSubtitle.setText("Browse uploaded event images for moderation");
        } else if ("organizers".equals(tab)) {
            tvSectionTitle.setText("Organizer Profiles");
            tvSectionSubtitle.setText("Review organizer accounts");
        } else if ("comments".equals(tab)) {
            tvSectionTitle.setText("All Comments");
            tvSectionSubtitle.setText("Browse and moderate all event comments");
        } else {
            tvSectionTitle.setText("Notification Logs");
            tvSectionSubtitle.setText("Review logs of all notifications sent to entrants");
        }
    }

    /**
     * Toggles visibility rules between the main content list and loading spinner.
     *
     * @param loading Boolean deciding whether to show progress or active content.
     */
    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    /**
     * Populates the RecyclerView adapter with freshly pulled items. Displays an empty
     * state overlay if the results list is blank.
     *
     * @param newItems     The downloaded data to mount in the list.
     * @param emptyMessage The prompt to show if there are 0 items found.
     */
    private void showItems(List<?> newItems, String emptyMessage) {
        items.clear();
        items.addAll(newItems);
        adapter.notifyDataSetChanged();

        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(newItems.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(newItems.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText(emptyMessage);
    }

    /**
     * Exposes errors to the admin and halts progress indicators gracefully.
     *
     * @param message Internal error summary.
     * @param e       The actual Java/Firestore Exception.
     */
    private void showLoadError(String message, Exception e) {
        showItems(Collections.emptyList(), message);
        Toast.makeText(this, e != null ? e.getMessage() : message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Iterates system totals (total active events, images, users) to populate the
     * main status dashboard cards.
     */
    private void loadDashboardCounts() {
        db.collection("events").get().addOnSuccessListener(snapshot ->
                tvEventCount.setText(String.valueOf(snapshot.size())));

        db.collection("events").get().addOnSuccessListener(snapshot -> {
            int imageCount = 0;
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String posterUrl = safe(doc.getString("posterUrl"));
                String qrCodeUrl = safe(doc.getString("qrCodeUrl"));
                if (!posterUrl.isEmpty()) imageCount++;
                if (!qrCodeUrl.isEmpty()) imageCount++;
            }
            tvImageCount.setText(String.valueOf(imageCount));
        });

        final int[] totalUsers = {0};

        db.collection("entrants").get().addOnSuccessListener(entrantDocs -> {
            totalUsers[0] += entrantDocs.size();
            db.collection("organizers").get().addOnSuccessListener(organizerDocs -> {
                totalUsers[0] += organizerDocs.size();
                db.collection("admins").get().addOnSuccessListener(adminDocs -> {
                    totalUsers[0] += adminDocs.size();
                    tvUserCount.setText(String.valueOf(totalUsers[0]));
                });
            });
        });
    }

    /**
     * Fetches all events systematically sorted by most-recent registration start times.
     */
    private void loadEvents() {
        showLoading(true);
        db.collection("events")
                .orderBy("registrationStart", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AdminEventItem> result = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event == null) continue;
                        event.setId(doc.getId());
                        result.add(new AdminEventItem(event));
                    }
                    showItems(result, "No events found");
                })
                .addOnFailureListener(e -> showLoadError("Unable to load events", e));
    }

    /**
     * Fetches distinct user models from Entrant, Organizer, and Admin collections.
     *
     * @param organizersOnly Boolean flag that restricts queries solely to the Organizers collection.
     */
    private void loadProfiles(boolean organizersOnly) {
        showLoading(true);
        List<AdminProfileItem> result = new ArrayList<>();

        if (organizersOnly) {
            db.collection("organizers").get()
                    .addOnSuccessListener(organizerDocs -> {
                        for (DocumentSnapshot doc : organizerDocs.getDocuments()) {
                            result.add(AdminProfileItem.fromDocument(doc, "organizer"));
                        }
                        result.sort(Comparator.comparing(AdminProfileItem::getName));
                        showItems(result, "No organizer profiles found");
                    })
                    .addOnFailureListener(e -> showLoadError("Unable to load organizer profiles", e));
            return;
        }

        db.collection("entrants").get()
                .addOnSuccessListener(entrantDocs -> {
                    for (DocumentSnapshot doc : entrantDocs.getDocuments()) {
                        result.add(AdminProfileItem.fromDocument(doc, "entrant"));
                    }

                    db.collection("organizers").get()
                            .addOnSuccessListener(organizerDocs -> {
                                for (DocumentSnapshot doc : organizerDocs.getDocuments()) {
                                    result.add(AdminProfileItem.fromDocument(doc, "organizer"));
                                }

                                db.collection("admins").get()
                                        .addOnSuccessListener(adminDocs -> {
                                            for (DocumentSnapshot doc : adminDocs.getDocuments()) {
                                                result.add(AdminProfileItem.fromDocument(doc, "admin"));
                                            }

                                            result.sort(Comparator.comparing(AdminProfileItem::getRole)
                                                    .thenComparing(AdminProfileItem::getName));
                                            showItems(result, "No profiles found");
                                        })
                                        .addOnFailureListener(e -> showLoadError("Unable to load admin profiles", e));
                            })
                            .addOnFailureListener(e -> showLoadError("Unable to load organizer profiles", e));
                })
                .addOnFailureListener(e -> showLoadError("Unable to load entrant profiles", e));
    }

    /**
     * Scans active events for valid poster or QR Code URL nodes and exposes them for review.
     */
    private void loadImages() {
        showLoading(true);
        db.collection("events").get()
                .addOnSuccessListener(snapshot -> {
                    List<AdminImageItem> result = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String eventName = safe(doc.getString("name"));
                        String posterUrl = safe(doc.getString("posterUrl"));
                        String qrCodeUrl = safe(doc.getString("qrCodeUrl"));

                        if (!posterUrl.isEmpty()) {
                            result.add(new AdminImageItem(doc.getId(), eventName, "Event poster", posterUrl));
                        }

                        if (!qrCodeUrl.isEmpty()) {
                            result.add(new AdminImageItem(doc.getId(), eventName, "Event QR code", qrCodeUrl));
                        }
                    }
                    showItems(result, "No uploaded images found");
                })
                .addOnFailureListener(e -> showLoadError("Unable to load images", e));
    }

    /**
     * Executes a Firestore collection-group query strictly filtering for comments mapped to events.
     */
    private void loadComments() {
        showLoading(true);
        db.collectionGroup("comments")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AdminCommentItem> result = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment != null) {
                            comment.setId(doc.getId());
                            result.add(new AdminCommentItem(comment));
                        }
                    }
                    showItems(result, "No comments found");
                })
                .addOnFailureListener(e -> showLoadError("Unable to load comments", e));
    }

    /**
     * Executes a robust collection-group lookup across the database to extract and review
     * active or historical notifications that organziers pushed to entrants.
     */
    private void loadNotificationLogs() {
        showLoading(true);
        db.collectionGroup("notifications")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<AdminNotificationLogItem> result = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String entrantId = safe(doc.getString("entrantId"));
                        if (entrantId.isEmpty()
                                && doc.getReference().getParent() != null
                                && doc.getReference().getParent().getParent() != null) {
                            entrantId = doc.getReference().getParent().getParent().getId();
                        }

                        Date createdAt = doc.getDate("createdAt");

                        result.add(new AdminNotificationLogItem(
                                doc.getId(),
                                entrantId,
                                safe(doc.getString("eventId")),
                                safe(doc.getString("eventName")),
                                safe(doc.getString("type")),
                                safe(doc.getString("message")),
                                Boolean.TRUE.equals(doc.getBoolean("unread")),
                                Boolean.TRUE.equals(doc.getBoolean("actionRequired")),
                                safe(doc.getString("actionStatus")),
                                createdAt
                        ));
                    }

                    result.sort((a, b) -> compareDatesDesc(a.getCreatedAt(), b.getCreatedAt()));
                    showItems(result, "No notification logs found");
                })
                .addOnFailureListener(e -> showLoadError("Unable to load notification logs", e));
    }

    /**
     * Standard Date comparison block for prioritizing newer notification logs.
     *
     * @param first  Primary date input.
     * @param second Secondary date block to contrast.
     * @return Integer indicating sort adjustment hierarchy (-1, 0, 1).
     */
    private int compareDatesDesc(Date first, Date second) {
        long firstTime = first != null ? first.getTime() : 0L;
        long secondTime = second != null ? second.getTime() : 0L;
        return Long.compare(secondTime, firstTime);
    }

    /**
     * Basic utility helper mechanism protecting against database NullPointerExceptions.
     *
     * @param value Potential null-sourced input string.
     * @return Trimmed clean string or static empty string block.
     */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Triggers a preview dialog box enabling admins to expand and inspect an image URI.
     *
     * @param item AdminImageItem structure carrying URL and visual data.
     */
    private void openImagePreview(AdminImageItem item) {
        if (item == null || item.getImageUrl().isEmpty()) {
            Toast.makeText(this, "Image unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        ImagePreviewDialogFragment.newInstance(item.getTitle(), item.getImageUrl())
                .show(getSupportFragmentManager(), "image_preview");
    }
}