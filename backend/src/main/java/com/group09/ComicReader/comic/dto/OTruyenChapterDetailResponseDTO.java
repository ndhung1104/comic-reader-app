package com.group09.ComicReader.comic.dto;

import java.util.List;

public class OTruyenChapterDetailResponseDTO {

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
        private String domain_cdn;
        private Item item;

        public String getDomain_cdn() {
            return domain_cdn;
        }

        public void setDomain_cdn(String domain_cdn) {
            this.domain_cdn = domain_cdn;
        }

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }
    }

    public static class Item {
        private String chapter_path;
        private List<ImagePage> chapter_image;

        public String getChapter_path() {
            return chapter_path;
        }

        public void setChapter_path(String chapter_path) {
            this.chapter_path = chapter_path;
        }

        public List<ImagePage> getChapter_image() {
            return chapter_image;
        }

        public void setChapter_image(List<ImagePage> chapter_image) {
            this.chapter_image = chapter_image;
        }
    }

    public static class ImagePage {
        private Integer image_page;
        private String image_file;

        public Integer getImage_page() {
            return image_page;
        }

        public void setImage_page(Integer image_page) {
            this.image_page = image_page;
        }

        public String getImage_file() {
            return image_file;
        }

        public void setImage_file(String image_file) {
            this.image_file = image_file;
        }
    }
}
