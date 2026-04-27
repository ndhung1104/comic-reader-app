package com.group09.ComicReader.ai.dto;

import com.group09.ComicReader.common.entity.ModerationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiSummaryResponse {
    private Long id;
    private Long comicId;
    private Long chapterId;
    private String content;
    private ModerationStatus status;
    private String moderationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
