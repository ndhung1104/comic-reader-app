package com.group09.ComicReader.model;

import java.io.Serializable;
import java.util.List;

public class CreatorRequestPageResponse implements Serializable {
    private List<CreatorRequestResponse> items;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;

    public List<CreatorRequestResponse> getItems() {
        return items;
    }

    public void setItems(List<CreatorRequestResponse> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
}
