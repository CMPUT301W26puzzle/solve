package com.example.eventparticipation;

/** Model representing an uploaded image visible to admins. */
public class AdminImageItem {
    private final String sourceId;
    private final String title;
    private final String imageType;
    private final String imageUrl;

    public AdminImageItem(String sourceId, String title, String imageType, String imageUrl) {
        this.sourceId = sourceId;
        this.title = title;
        this.imageType = imageType;
        this.imageUrl = imageUrl;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getTitle() {
        return title == null || title.trim().isEmpty() ? "Untitled image" : title;
    }

    public String getImageType() {
        return imageType;
    }

    public String getImageUrl() {
        return imageUrl == null ? "" : imageUrl;
    }
}