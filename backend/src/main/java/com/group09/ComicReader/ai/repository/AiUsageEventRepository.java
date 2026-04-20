package com.group09.ComicReader.ai.repository;

import com.group09.ComicReader.ai.entity.AiFeature;
import com.group09.ComicReader.ai.entity.AiUsageEventEntity;
import com.group09.ComicReader.ai.entity.AiUsageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;

public interface AiUsageEventRepository extends JpaRepository<AiUsageEventEntity, Long> {

    long countByFeatureAndActorKeyAndCreatedAtBetweenAndStatusIn(
            AiFeature feature,
            String actorKey,
            LocalDateTime from,
            LocalDateTime to,
            Collection<AiUsageStatus> statuses);

    long countByFeatureAndCreatedAtBetweenAndStatusIn(
            AiFeature feature,
            LocalDateTime from,
            LocalDateTime to,
            Collection<AiUsageStatus> statuses);
}
