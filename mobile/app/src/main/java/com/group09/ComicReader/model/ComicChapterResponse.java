package com.group09.ComicReader.model;

public class ComicChapterResponse {

    private Long id;
    private Long comicId;
    private Integer chapterNumber;
    private String title;
    private boolean premium;
    private Integer price;
    private boolean unlocked;
    private boolean adEligible;
    private boolean requiresEntryBannerAd;

    public Long getId() {
        return id;
    }

    public Long getComicId() {
        return comicId;
    }

    public Integer getChapterNumber() {
        return chapterNumber;
    }

    public String getTitle() {
        return title;
    }

    public boolean isPremium() {
        return premium;
    }

    public Integer getPrice() {
        return price;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public boolean isAdEligible() {
        return adEligible;
    }

    public boolean isRequiresEntryBannerAd() {
        return requiresEntryBannerAd;
    }
}
