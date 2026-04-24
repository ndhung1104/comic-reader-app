package com.group09.ComicReader.chapter.dto;

public class ChapterResponse {

    private Long id;
    private Long comicId;
    private Integer chapterNumber;
    private String title;
    private String language;
    private boolean premium;
    private Integer price;
    private boolean unlocked;
    private boolean adEligible;
    private boolean requiresEntryBannerAd;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getComicId() {
        return comicId;
    }

    public void setComicId(Long comicId) {
        this.comicId = comicId;
    }

    public Integer getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(Integer chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public boolean isAdEligible() {
        return adEligible;
    }

    public void setAdEligible(boolean adEligible) {
        this.adEligible = adEligible;
    }

    public boolean isRequiresEntryBannerAd() {
        return requiresEntryBannerAd;
    }

    public void setRequiresEntryBannerAd(boolean requiresEntryBannerAd) {
        this.requiresEntryBannerAd = requiresEntryBannerAd;
    }
}
