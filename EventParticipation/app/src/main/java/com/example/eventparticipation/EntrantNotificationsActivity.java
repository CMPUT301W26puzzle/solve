package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Entrant notifications screen.
 */
public class EntrantNotificationsActivity extends AppCompatActivity implements NotificationAdapter.Listener {

    public static final String EXTRA_TEST_MODE = "TEST_MODE";
    public static final String EXTRA_TEST_NOTIFICATIONS = "TEST_NOTIFICATIONS";

    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;

    private NotificationAdapter adapter;
    private NotificationRepository notificationRepository;
    private ListenerRegistration listenerRegistration;
    private final List<NotificationItem> notificationItems = new ArrayList<>();

    private boolean isTestMode;
    private String entrantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_notifications);

        isTestMode = getIntent().getBooleanExtra(EXTRA_TEST_MODE, false);
        entrantId = DeviceIdProvider.getId(this);
        notificationRepository = new NotificationRepository(FirebaseFirestore.getInstance());

        initViews();
        setupRecyclerView();
        setupBottomNav();

        if (isTestMode) {
            loadTestNotifications();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isTestMode) {
            startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_notifications);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_notifications) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, EntrantDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_my_events) {
                startActivity(new Intent(this, EntrantMyEventsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_scan) {
                Toast.makeText(this, "Scan coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    @SuppressWarnings("unchecked")
    private void loadTestNotifications() {
        progressBar.setVisibility(View.GONE);
        ArrayList<NotificationItem> testItems = (ArrayList<NotificationItem>) getIntent()
                .getSerializableExtra(EXTRA_TEST_NOTIFICATIONS);
        notificationItems.clear();
        if (testItems != null) {
            notificationItems.addAll(testItems);
        }
        updateUI();
    }

    private void startListening() {
        progressBar.setVisibility(View.VISIBLE);
        listenerRegistration = notificationRepository.listenForNotifications(entrantId,
                new NotificationRepository.NotificationListener() {
                    @Override
                    public void onNotificationsChanged(List<NotificationItem> items) {
                        progressBar.setVisibility(View.GONE);
                        notificationItems.clear();
                        notificationItems.addAll(items);
                        updateUI();
                    }

                    @Override
                    public void onError(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(EntrantNotificationsActivity.this,
                                "Failed to load notifications",
                                Toast.LENGTH_SHORT).show();
                        showEmptyState("Unable to load notifications right now");
                    }
                });
    }

    private void updateUI() {
        adapter.updateItems(notificationItems);
        if (notificationItems.isEmpty()) {
            showEmptyState("No notifications yet");
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState(String message) {
        tvEmptyMessage.setText(message);
        layoutEmptyState.setVisibility(View.VISIBLE);
        rvNotifications.setVisibility(View.GONE);
    }

    @Override
    public void onNotificationClicked(NotificationItem item) {
        if (item == null) {
            return;
        }

        if (!isTestMode && item.isUnread() && item.getId() != null) {
            notificationRepository.markAsRead(entrantId, item.getId());
            item.setUnread(false);
            adapter.notifyDataSetChanged();
        }

        if (item.getEventId() == null || item.getEventId().trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(this, EntrantEventDetailActivity.class);
        intent.putExtra("EVENT_ID", item.getEventId());
        intent.putExtra("EVENT_NAME", item.getEventName());
        startActivity(intent);
    }

    @Override
    public void onAcceptClicked(NotificationItem item) {
        if (item == null) {
            return;
        }

        if (isTestMode) {
            NotificationActionHelper.applyAccepted(item);
            adapter.notifyDataSetChanged();
            return;
        }

        notificationRepository.acceptInvitation(entrantId, item)
                .addOnSuccessListener(unused -> Toast.makeText(this,
                        "Invitation accepted",
                        Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        e.getMessage() != null ? e.getMessage() : "Failed to accept invitation",
                        Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDeclineClicked(NotificationItem item) {
        if (item == null) {
            return;
        }

        if (isTestMode) {
            NotificationActionHelper.applyDeclined(item);
            adapter.notifyDataSetChanged();
            return;
        }

        notificationRepository.declineInvitation(entrantId, item)
                .addOnSuccessListener(unused -> Toast.makeText(this,
                        "Invitation declined",
                        Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        e.getMessage() != null ? e.getMessage() : "Failed to decline invitation",
                        Toast.LENGTH_SHORT).show());
    }
}
