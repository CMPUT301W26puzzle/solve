package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private static final String DEVICE_ID = "device_demo_001";

    private RecyclerView rvNotifications;
    private EntrantNotificationAdapter adapter;
    private final List<EntrantNotification> notifications = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();

        ImageButton btnBack = findViewById(R.id.btnBack);
        rvNotifications = findViewById(R.id.rvNotifications);

        btnBack.setVisibility(View.GONE);
        btnBack.setOnClickListener(v -> finish());

        adapter = new EntrantNotificationAdapter(notifications, notification -> {
            markAsRead(notification);

            Intent intent = new Intent(this, EntrantEventDetailActivity.class);
            intent.putExtra("EVENT_ID", notification.getEventId());
            intent.putExtra("ORGANIZER_ID", notification.getOrganizerId());
            intent.putExtra("EVENT_NAME", notification.getEventName());
            intent.putExtra("NOTIFICATION_ID", notification.getId());
            startActivity(intent);
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        setupBottomNav();
        loadNotifications();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_notifications);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, EntrantDashboardActivity.class));
                return true;
            } else if (id == R.id.nav_scan) {
                Toast.makeText(this, "Scan coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_notifications) {
                return true;
            } else if (id == R.id.nav_my_events) {
                Toast.makeText(this, "My Events coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }

    private void loadNotifications() {
        db.collection("users")
                .document(DEVICE_ID)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notifications.clear();

                    queryDocumentSnapshots.forEach(doc -> {
                        EntrantNotification notification = doc.toObject(EntrantNotification.class);
                        notification.setId(doc.getId());
                        notifications.add(notification);
                    });

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show());
    }

    private void markAsRead(EntrantNotification notification) {
        if (notification == null || notification.getId() == null) {
            return;
        }

        db.collection("users")
                .document(DEVICE_ID)
                .collection("notifications")
                .document(notification.getId())
                .update("read", true);
    }
}
