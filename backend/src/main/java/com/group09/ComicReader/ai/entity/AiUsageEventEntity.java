package com.group09.ComicReader.ai.entity;

import com.group09.ComicReader.auth.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage_events")
public class AiUsageEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id")
    private UserEntity requesterUser;

    @Column(name = "actor_key", nullable = false, length = 100)
    private String actorKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AiFeature feature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiUsageStatus status;

    @Column(length = 50)
    private String provider;

    @Column(length = 100)
    private String model;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "request_units")
    private Integer requestUnits;

    @Column(name = "response_units")
    private Integer responseUnits;

    @Column(length = 1000)
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserEntity getRequesterUser() {
        return requesterUser;
    }

    public void setRequesterUser(UserEntity requesterUser) {
        this.requesterUser = requesterUser;
    }

    public String getActorKey() {
        return actorKey;
    }

    public void setActorKey(String actorKey) {
        this.actorKey = actorKey;
    }

    public AiFeature getFeature() {
        return feature;
    }

    public void setFeature(AiFeature feature) {
        this.feature = feature;
    }

    public AiUsageStatus getStatus() {
        return status;
    }

    public void setStatus(AiUsageStatus status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Integer getRequestUnits() {
        return requestUnits;
    }

    public void setRequestUnits(Integer requestUnits) {
        this.requestUnits = requestUnits;
    }

    public Integer getResponseUnits() {
        return responseUnits;
    }

    public void setResponseUnits(Integer responseUnits) {
        this.responseUnits = responseUnits;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
