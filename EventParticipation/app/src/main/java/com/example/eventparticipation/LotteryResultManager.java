package com.example.eventparticipation;

import android.content.Context;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LotteryResultManager {

    private final FirebaseFirestore db;

    public LotteryResultManager() {
        db = FirebaseFirestore.getInstance();
    }

    public void markEntrantSelected(Context context,
                                    String organizerId,
                                    String eventId,
                                    String entrantDeviceId,
                                    String eventName) {

        DocumentReference waitlistRef = db.collection("organizers")
                .document(organizerId)
                .collection("events")
                .document(eventId)
                .collection("waitlist")
                .document(entrantDeviceId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "selected");

        waitlistRef.update(updates).addOnSuccessListener(unused -> {
            createEntrantNotification(
                    context,
                    entrantDeviceId,
                    eventId,
                    organizerId,
                    eventName,
                    "selected",
                    "Congratulations! You have been selected as a replacement for the event. Click to accept your invitation!"
            );
        });
    }

    public void markEntrantNotSelected(Context context,
                                       String organizerId,
                                       String eventId,
                                       String entrantDeviceId,
                                       String eventName) {

        DocumentReference waitlistRef = db.collection("organizers")
                .document(organizerId)
                .collection("events")
                .document(eventId)
                .collection("waitlist")
                .document(entrantDeviceId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "not_selected");

        waitlistRef.update(updates).addOnSuccessListener(unused -> {
            createEntrantNotification(
                    context,
                    entrantDeviceId,
                    eventId,
                    organizerId,
                    eventName,
                    "not_selected",
                    "Thank you for your interest. You were not selected for this event."
            );
        });
    }

    private void createEntrantNotification(Context context,
                                           String entrantDeviceId,
                                           String eventId,
                                           String organizerId,
                                           String eventName,
                                           String type,
                                           String message) {

        Map<String, Object> notificationMap = new HashMap<>();
        notificationMap.put("eventId", eventId);
        notificationMap.put("organizerId", organizerId);
        notificationMap.put("eventName", eventName);
        notificationMap.put("type", type);
        notificationMap.put("message", message);
        notificationMap.put("status", "pending");
        notificationMap.put("read", false);
        notificationMap.put("createdAt", new Date());

        db.collection("users")
                .document(entrantDeviceId)
                .collection("notifications")
                .add(notificationMap)
                .addOnSuccessListener(docRef -> {
                    NotificationHelper.showPhoneNotification(
                            context,
                            "Event Update",
                            message,
                            Math.abs(docRef.getId().hashCode())
                    );
                });
    }
}
