package com.example.eventparticipation;

import java.util.Date;

public class TimeUtils {

    public static String getRelativeTime(Date date) {
        long diffMillis = System.currentTimeMillis() - date.getTime();
        long minutes = diffMillis / (60 * 1000);
        long hours = diffMillis / (60 * 60 * 1000);
        long days = diffMillis / (24 * 60 * 60 * 1000);

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " minutes ago";
        } else if (hours < 24) {
            return hours + " hours ago";
        } else {
            return days + " days ago";
        }
    }
}
