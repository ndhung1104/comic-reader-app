package com.group09.ComicReader.comic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class OTruyenResponseDTO {

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
        @JsonProperty("APP_DOMAIN_CDN_IMAGE")
        private String appDomainCdnImage;
        private List<Item> items;

        public String getAppDomainCdnImage() {
            return appDomainCdnImage;
        }

        public void setAppDomainCdnImage(String appDomainCdnImage) {
            this.appDomainCdnImage = appDomainCdnImage;
        }

        public List<Item> getItems() {
            return items;
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }
    }

    public static class Item {
        private String name;
        private String slug;

        @JsonProperty("thumb_url")
        private String thumbUrl;
        private String status;
        private List<Category> category;

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

        public String getThumbUrl() {
            return thumbUrl;
        }

        public void setThumbUrl(String thumbUrl) {
            this.thumbUrl = thumbUrl;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<Category> getCategory() {
            return category;
        }

        public void setCategory(List<Category> category) {
            this.category = category;
        }
    }

    public static class Category {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
