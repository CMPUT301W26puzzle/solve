package com.example.eventparticipation;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.WriteBatch;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView btnEvents;
    private TextView btnProfiles;
    private TextView btnImages;
    private TextView btnOrganizers;
    private TextView btnLogs;

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

    private void confirmDeleteProfile(AdminProfileItem item, int position) {
        if (item == null || item.getProfileId() == null) {
            Toast.makeText(this, "Profile unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete profile?")
                .setMessage("This will permanently delete \"" + safe(item.getName()) + "\".")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile(item, position))
                .show();
    }

    private void deleteProfile(AdminProfileItem item, int position) {
        if (item == null || item.getProfileId() == null || item.getProfileId().trim().isEmpty()) {
            Toast.makeText(this, "Invalid profile", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        String collection = item.getRole() + "s";
        // "entrant" → "entrants", "organizer" → "organizers", etc.

        db.collection(collection)
                .document(item.getProfileId())
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

    private void bindViews() {
        btnEvents = findViewById(R.id.btnTabEvents);
        btnProfiles = findViewById(R.id.btnTabProfiles);
        btnImages = findViewById(R.id.btnTabImages);
        btnOrganizers = findViewById(R.id.btnTabOrganizers);
        btnLogs = findViewById(R.id.btnTabLogs);

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

    private void setupRecycler() {
        adapter = new AdminBrowseAdapter(
                items,
                this::openImagePreview,
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
                }
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnEvents.setOnClickListener(v -> selectTab("events"));
        btnProfiles.setOnClickListener(v -> selectTab("profiles"));
        btnImages.setOnClickListener(v -> selectTab("images"));
        btnOrganizers.setOnClickListener(v -> selectTab("organizers"));
        btnLogs.setOnClickListener(v -> selectTab("logs"));
    }

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
        } else {
            loadNotificationLogs();
        }
    }

    private void updateTabStyles() {
        setTabStyle(btnEvents, "events".equals(activeTab));
        setTabStyle(btnProfiles, "profiles".equals(activeTab));
        setTabStyle(btnImages, "images".equals(activeTab));
        setTabStyle(btnOrganizers, "organizers".equals(activeTab));
        setTabStyle(btnLogs, "logs".equals(activeTab));
    }

    private void setTabStyle(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tab.setTextColor(ContextCompat.getColor(this, selected ? android.R.color.black : android.R.color.darker_gray));
    }

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
        } else {
            tvSectionTitle.setText("Notification Logs");
            tvSectionSubtitle.setText("Review logs of all notifications sent to entrants");
        }
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    private void showItems(List<?> newItems, String emptyMessage) {
        items.clear();
        items.addAll(newItems);
        adapter.notifyDataSetChanged();

        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(newItems.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(newItems.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmpty.setText(emptyMessage);
    }

    private void showLoadError(String message, Exception e) {
        showItems(Collections.emptyList(), message);
        Toast.makeText(this, e != null ? e.getMessage() : message, Toast.LENGTH_SHORT).show();
    }

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

    private int compareDatesDesc(Date first, Date second) {
        long firstTime = first != null ? first.getTime() : 0L;
        long secondTime = second != null ? second.getTime() : 0L;
        return Long.compare(secondTime, firstTime);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void openImagePreview(AdminImageItem item) {
        if (item == null || item.getImageUrl().isEmpty()) {
            Toast.makeText(this, "Image unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        ImagePreviewDialogFragment.newInstance(item.getTitle(), item.getImageUrl())
                .show(getSupportFragmentManager(), "image_preview");
    }
}