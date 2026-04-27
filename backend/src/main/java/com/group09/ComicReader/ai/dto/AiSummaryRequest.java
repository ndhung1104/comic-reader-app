package com.group09.ComicReader.ai.dto;

import lombok.Data;

@Data
public class AiSummaryRequest {
    private Long comicId;
    private Long chapterId; // Optional
}
