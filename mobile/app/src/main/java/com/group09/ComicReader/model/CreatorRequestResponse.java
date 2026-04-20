package com.group09.ComicReader.model;

import java.io.Serializable;

public class CreatorRequestResponse implements Serializable {
    private Long id;
    private Long userId;
    private String userEmail;
    private String message;
    private String status;
    private String createdAt;
    private String processedAt;
    private Long processedById;
    private String processedByEmail;
    private String adminMessage; // Added for audit message in UI

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }

    public Long getProcessedById() {
        return processedById;
    }

    public void setProcessedById(Long processedById) {
        this.processedById = processedById;
    }

    public String getProcessedByEmail() {
        return processedByEmail;
    }

    public void setProcessedByEmail(String processedByEmail) {
        this.processedByEmail = processedByEmail;
    }

    public String getAdminMessage() {
        return adminMessage;
    }

    public void setAdminMessage(String adminMessage) {
        this.adminMessage = adminMessage;
    }
}
