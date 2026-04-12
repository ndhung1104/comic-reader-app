package com.group09.ComicReader.model;

public class Genre {
    public enum LayoutType {
        LARGE,  // Action: col-span-2, row-span-2
        MEDIUM, // Webtoon: col-span-2
        SMALL  // Others: col-span-1
    }

    private final int id;
    private final String name;
    private final String imageUrl;
    private final int titleCount;
    private final LayoutType layoutType;
    private final String badge;
    private final String description;
    private final String categoryId;
    private final boolean hasCompleted;
    private final boolean hasOngoing;

    public Genre(int id, String name, String imageUrl, int titleCount, LayoutType layoutType, String badge, String description) {
        this(id, name, imageUrl, titleCount, layoutType, badge, description, name, true, true);
    }

    public Genre(int id, String name, String imageUrl, int titleCount, LayoutType layoutType, String badge,
                 String description, String categoryId, boolean hasCompleted, boolean hasOngoing) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.titleCount = titleCount;
        this.layoutType = layoutType;
        this.badge = badge;
        this.description = description;
        this.categoryId = categoryId;
        this.hasCompleted = hasCompleted;
        this.hasOngoing = hasOngoing;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public int getTitleCount() { return titleCount; }
    public LayoutType getLayoutType() { return layoutType; }
    public boolean isLarge() { return layoutType == LayoutType.LARGE; }
    public boolean isMedium() { return layoutType == LayoutType.MEDIUM; }
    public String getBadge() { return badge; }
    public String getDescription() { return description; }
    public String getCategoryId() { return categoryId; }
    public boolean hasCompleted() { return hasCompleted; }
    public boolean hasOngoing() { return hasOngoing; }
}
