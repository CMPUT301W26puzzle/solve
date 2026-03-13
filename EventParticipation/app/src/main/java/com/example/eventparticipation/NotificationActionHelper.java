package com.example.eventparticipation;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Pure helper methods used by the notification UI and tests.
 */
public class NotificationActionHelper {

    private NotificationActionHelper() {
    }

    public static String buildSelectedMessage(String eventName) {
        String safeEventName = eventName == null || eventName.trim().isEmpty()
                ? "the event"
                : eventName.trim();
        return "🎉 Congratulations! You have been selected as a replacement for "
                + safeEventName + ". Click to accept your invitation!";
    }

    public static String buildNotSelectedMessage(String eventName) {
        String safeEventName = eventName == null || eventName.trim().isEmpty()
                ? "the event"
                : eventName.trim();
        return "You were not selected in this draw for " + safeEventName + ". You remain on the waiting list.";
    }

    public static boolean shouldShowAcceptAction(NotificationItem item) {
        return item != null
                && NotificationItem.TYPE_SELECTED.equals(item.getType())
                && item.isActionRequired()
                && NotificationItem.ACTION_PENDING.equals(item.getActionStatus());
    }

    public static boolean shouldShowDeclineAction(NotificationItem item) {
        return shouldShowAcceptAction(item);
    }

    public static String getPrimaryActionLabel(NotificationItem item) {
        return shouldShowAcceptAction(item) ? "Accept Invitation" : "";
    }

    public static String getActionStateLabel(NotificationItem item) {
        if (item == null) {
            return "";
        }

        if (NotificationItem.ACTION_ACCEPTED.equals(item.getActionStatus())) {
            return "Invitation accepted";
        }

        if (NotificationItem.ACTION_DECLINED.equals(item.getActionStatus())) {
            return "Invitation declined";
        }

        return "";
    }

    public static void applyAccepted(NotificationItem item) {
        if (item == null) {
            return;
        }
        item.setUnread(false);
        item.setActionRequired(false);
        item.setActionStatus(NotificationItem.ACTION_ACCEPTED);
    }

    public static void applyDeclined(NotificationItem item) {
        if (item == null) {
            return;
        }
        item.setUnread(false);
        item.setActionRequired(false);
        item.setActionStatus(NotificationItem.ACTION_DECLINED);
    }

    public static String formatRelativeTime(Date createdAt, long nowMillis) {
        if (createdAt == null) {
            return "Just now";
        }

        long diff = Math.max(0L, nowMillis - createdAt.getTime());
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (minutes < 1) {
            return "Just now";
        }
        if (minutes == 1) {
            return "1 minute ago";
        }
        if (minutes < 60) {
            return minutes + " minutes ago";
        }
        if (hours == 1) {
            return "1 hour ago";
        }
        if (hours < 24) {
            return hours + " hours ago";
        }
        if (days == 1) {
            return "1 day ago";
        }
        return days + " days ago";
    }
}
