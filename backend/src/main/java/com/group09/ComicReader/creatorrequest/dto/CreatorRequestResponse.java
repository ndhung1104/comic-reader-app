package com.group09.ComicReader.creatorrequest.dto;

import com.group09.ComicReader.creatorrequest.entity.CreatorRequestStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreatorRequestResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String message;
    private CreatorRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Long processedById;
    private String processedByEmail;
    private String adminMessage;
}
