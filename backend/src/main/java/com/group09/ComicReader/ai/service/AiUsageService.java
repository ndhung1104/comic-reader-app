package com.group09.ComicReader.ai.service;

import com.group09.ComicReader.ai.config.AiGuardrailProperties;
import com.group09.ComicReader.ai.entity.AiFeature;
import com.group09.ComicReader.ai.entity.AiUsageEventEntity;
import com.group09.ComicReader.ai.entity.AiUsageStatus;
import com.group09.ComicReader.ai.repository.AiUsageEventRepository;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.common.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

@Service
public class AiUsageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiUsageService.class);
    private static final EnumSet<AiUsageStatus> QUOTA_STATUSES = EnumSet.of(
            AiUsageStatus.IN_PROGRESS,
            AiUsageStatus.SUCCEEDED,
            AiUsageStatus.FAILED
    );

    private final AiUsageEventRepository aiUsageEventRepository;
    private final UserRepository userRepository;
    private final AiGuardrailProperties aiGuardrailProperties;

    public AiUsageService(AiUsageEventRepository aiUsageEventRepository,
            UserRepository userRepository,
            AiGuardrailProperties aiGuardrailProperties) {
        this.aiUsageEventRepository = aiUsageEventRepository;
        this.userRepository = userRepository;
        this.aiGuardrailProperties = aiGuardrailProperties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UsageContext beginUsage(AiFeature feature, String referenceId, Integer requestUnits, String details) {
        if (!aiGuardrailProperties.isEnabled()) {
            return UsageContext.disabledContext();
        }

        ActorContext actorContext = resolveActorContext();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        int perActorLimit = getPerActorLimit(feature);
        if (perActorLimit > 0) {
            long actorCount = aiUsageEventRepository.countByFeatureAndActorKeyAndCreatedAtBetweenAndStatusIn(
                    feature,
                    actorContext.actorKey(),
                    startOfDay,
                    endOfDay,
                    QUOTA_STATUSES
            );
            if (actorCount >= perActorLimit) {
                saveBlockedUsage(actorContext, feature, referenceId, requestUnits, buildUserLimitMessage(feature), details);
                throw new TooManyRequestsException(buildUserLimitMessage(feature));
            }
        }

        int systemLimit = getSystemLimit(feature);
        if (systemLimit > 0) {
            long systemCount = aiUsageEventRepository.countByFeatureAndCreatedAtBetweenAndStatusIn(
                    feature,
                    startOfDay,
                    endOfDay,
                    QUOTA_STATUSES
            );
            if (systemCount >= systemLimit) {
                saveBlockedUsage(actorContext, feature, referenceId, requestUnits, buildSystemLimitMessage(feature), details);
                throw new TooManyRequestsException(buildSystemLimitMessage(feature));
            }
        }

        AiUsageEventEntity event = new AiUsageEventEntity();
        event.setRequesterUser(actorContext.user());
        event.setActorKey(actorContext.actorKey());
        event.setFeature(feature);
        event.setStatus(AiUsageStatus.IN_PROGRESS);
        event.setReferenceId(referenceId);
        event.setRequestUnits(requestUnits);
        event.setDetails(truncate(details));
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        event = aiUsageEventRepository.save(event);

        return new UsageContext(event.getId(), actorContext.userId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeSuccess(UsageContext usageContext,
            String provider,
            String model,
            Integer responseUnits,
            String details) {
        updateUsageEvent(usageContext, AiUsageStatus.SUCCEEDED, provider, model, responseUnits, details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeFailure(UsageContext usageContext,
            String provider,
            String model,
            String details) {
        updateUsageEvent(usageContext, AiUsageStatus.FAILED, provider, model, null, details);
    }

    public Long getCurrentUserIdOrNull() {
        return resolveActorContext().userId();
    }

    private void updateUsageEvent(UsageContext usageContext,
            AiUsageStatus status,
            String provider,
            String model,
            Integer responseUnits,
            String details) {
        if (usageContext == null || usageContext.isDisabled() || usageContext.eventId() == null) {
            return;
        }

        Optional<AiUsageEventEntity> existing = aiUsageEventRepository.findById(usageContext.eventId());
        if (existing.isEmpty()) {
            return;
        }

        AiUsageEventEntity event = existing.get();
        event.setStatus(status);
        event.setProvider(provider);
        event.setModel(model);
        event.setResponseUnits(responseUnits);
        event.setDetails(truncate(details));
        event.setUpdatedAt(LocalDateTime.now());
        aiUsageEventRepository.save(event);

        LOGGER.info("ai_usage feature={} status={} actor={} provider={} model={} ref={}",
                event.getFeature(),
                status,
                event.getActorKey(),
                provider,
                model,
                event.getReferenceId());
    }

    private void saveBlockedUsage(ActorContext actorContext,
            AiFeature feature,
            String referenceId,
            Integer requestUnits,
            String message,
            String details) {
        AiUsageEventEntity event = new AiUsageEventEntity();
        event.setRequesterUser(actorContext.user());
        event.setActorKey(actorContext.actorKey());
        event.setFeature(feature);
        event.setStatus(AiUsageStatus.BLOCKED);
        event.setReferenceId(referenceId);
        event.setRequestUnits(requestUnits);
        event.setDetails(truncate(message + (details == null || details.isBlank() ? "" : " | " + details)));
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        aiUsageEventRepository.save(event);

        LOGGER.warn("ai_usage_blocked feature={} actor={} reason={}", feature, actorContext.actorKey(), message);
    }

    private ActorContext resolveActorContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new ActorContext(null, null, "anonymous");
        }

        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = String.valueOf(principal);
        }

        UserEntity user = userRepository.findByEmail(username).orElse(null);
        if (user == null || user.getId() == null) {
            return new ActorContext(null, null, "anonymous");
        }
        return new ActorContext(user, user.getId(), "user:" + user.getId());
    }

    private int getPerActorLimit(AiFeature feature) {
        return switch (feature) {
            case TEXT_TRANSLATE -> aiGuardrailProperties.getTranslatePerActorPerDay();
            case OCR_TRANSLATION_JOB -> aiGuardrailProperties.getTranslationJobPerActorPerDay();
            case AUDIO_PLAYLIST -> aiGuardrailProperties.getAudioPlaylistPerActorPerDay();
            case AI_SUMMARY -> aiGuardrailProperties.getAiSummaryPerActorPerDay();
            case AI_CHAT -> aiGuardrailProperties.getChatPerActorPerDay();
        };
    }

    private int getSystemLimit(AiFeature feature) {
        return switch (feature) {
            case TEXT_TRANSLATE -> aiGuardrailProperties.getTranslatePerDay();
            case OCR_TRANSLATION_JOB -> aiGuardrailProperties.getTranslationJobPerDay();
            case AUDIO_PLAYLIST -> aiGuardrailProperties.getAudioPlaylistPerDay();
            case AI_SUMMARY -> aiGuardrailProperties.getAiSummaryPerDay();
            case AI_CHAT -> aiGuardrailProperties.getChatPerDay();
        };
    }

    private String buildUserLimitMessage(AiFeature feature) {
        return switch (feature) {
            case TEXT_TRANSLATE -> "Translation quota reached for today. Please try again tomorrow.";
            case OCR_TRANSLATION_JOB -> "OCR quota reached for today. Please try again tomorrow.";
            case AUDIO_PLAYLIST -> "Audio generation quota reached for today. Please try again tomorrow.";
            case AI_SUMMARY -> "AI Summary quota reached for today. Please try again tomorrow.";
            case AI_CHAT -> "Chat quota reached for today. Please try again tomorrow.";
        };
    }

    private String buildSystemLimitMessage(AiFeature feature) {
        return switch (feature) {
            case TEXT_TRANSLATE -> "Translation service is at capacity right now. Please try again later.";
            case OCR_TRANSLATION_JOB -> "OCR service is at capacity right now. Please try again later.";
            case AUDIO_PLAYLIST -> "Audio generation service is at capacity right now. Please try again later.";
            case AI_SUMMARY -> "AI Summary service is at capacity right now. Please try again later.";
            case AI_CHAT -> "Chat service is at capacity right now. Please try again later.";
        };
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= 1000 ? trimmed : trimmed.substring(0, 1000);
    }

    private record ActorContext(UserEntity user, Long userId, String actorKey) {
    }

    public record UsageContext(Long eventId, Long userId) {
        static UsageContext disabledContext() {
            return new UsageContext(null, null);
        }

        boolean isDisabled() {
            return eventId == null;
        }
    }
}
