package com.group09.ComicReader.comic.dto;

import java.util.List;

public class OTruyenDetailResponseDTO {

    private String status;
    private String message;
    private Data data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private String domainCdn;
        private Item item;

        public String getDomainCdn() {
            return domainCdn;
        }

        public String getAppDomainCdnImage() {
            return domainCdn;
        }

        public void setDomainCdn(String domainCdn) {
            this.domainCdn = domainCdn;
        }

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }
    }

    public static class Category {
        private String name;
        private String slug;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
    }

    public static class Item {
        private String _id;
        private String name;
        private String slug;
        private String content; // Synopsis
        private String thumb_url;
        private String status;
        private List<String> author; // The missing array
        private List<Category> category;
        private List<ChapterItem> chapters; // Added chapters array

        public String get_id() {
            return _id;
        }

        public void set_id(String _id) {
            this._id = _id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getThumb_url() { return thumb_url; }
        public void setThumb_url(String thumb_url) { this.thumb_url = thumb_url; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public List<String> getAuthor() {
            return author;
        }

        public void setAuthor(List<String> author) {
            this.author = author;
        }

        public List<Category> getCategory() { return category; }
        public void setCategory(List<Category> category) { this.category = category; }

        public List<ChapterItem> getChapters() {
            return chapters;
        }

        public void setChapters(List<ChapterItem> chapters) {
            this.chapters = chapters;
        }
    }

    public static class ChapterItem {
        private String server_name;
        private List<ServerData> server_data;

        public String getServer_name() {
            return server_name;
        }

        public void setServer_name(String server_name) {
            this.server_name = server_name;
        }

        public List<ServerData> getServer_data() {
            return server_data;
        }

        public void setServer_data(List<ServerData> server_data) {
            this.server_data = server_data;
        }
    }

    public static class ServerData {
        private String chapter_name;
        private String chapter_title;
        private String chapter_api_data;

        public String getChapter_name() {
            return chapter_name;
        }

        public void setChapter_name(String chapter_name) {
            this.chapter_name = chapter_name;
        }

        public String getChapter_title() {
            return chapter_title;
        }

        public void setChapter_title(String chapter_title) {
            this.chapter_title = chapter_title;
        }

        public String getChapter_api_data() {
            return chapter_api_data;
        }

        public void setChapter_api_data(String chapter_api_data) {
            this.chapter_api_data = chapter_api_data;
        }
    }
}
