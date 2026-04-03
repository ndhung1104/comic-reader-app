package com.group09.ComicReader.comic.dto;

import jakarta.validation.constraints.NotBlank;

public class ImportComicRequest {

    @NotBlank(message = "Source URL or slug is required")
    private String sourceUrl;

    private String sourceType = "OTRUYEN";

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
