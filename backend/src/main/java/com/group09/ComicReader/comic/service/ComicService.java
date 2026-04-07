package com.group09.ComicReader.comic.service;

import com.group09.ComicReader.chapter.dto.ChapterRequest;
import com.group09.ComicReader.chapter.dto.ChapterResponse;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.comic.dto.ComicRequest;
import com.group09.ComicReader.comic.dto.ComicResponse;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.entity.ComicRatingEntity;
import com.group09.ComicReader.comic.repository.ComicRatingRepository;
import com.group09.ComicReader.comic.repository.ComicRepository;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.wallet.repository.ChapterPurchaseRepository;
import com.group09.ComicReader.wallet.repository.VipSubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ComicService {

    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPurchaseRepository purchaseRepository;
    private final VipSubscriptionRepository vipRepository;
    private final UserRepository userRepository;
    private final ComicRatingRepository comicRatingRepository;

    public ComicService(ComicRepository comicRepository,
                        ChapterRepository chapterRepository,
                        ChapterPurchaseRepository purchaseRepository,
                        VipSubscriptionRepository vipRepository,
                        UserRepository userRepository,
                        ComicRatingRepository comicRatingRepository) {
        this.comicRepository = comicRepository;
        this.chapterRepository = chapterRepository;
        this.purchaseRepository = purchaseRepository;
        this.vipRepository = vipRepository;
        this.userRepository = userRepository;
        this.comicRatingRepository = comicRatingRepository;
    }

    public Page<ComicResponse> getComics(String keyword, String category, Pageable pageable) {
        String kw = (keyword == null) ? "" : keyword;
        String cat = (category == null) ? "" : category;
        return comicRepository.searchComics(kw, cat, pageable).map(this::toComicResponse);
    }

    public ComicResponse getComic(Long comicId) {
        ComicEntity comic = getComicEntity(comicId);
        return toComicResponse(comic);
    }

    /* ========== Trending / Ranking ========== */

    public Page<ComicResponse> getTrendingComics(Pageable pageable) {
        return comicRepository.findTrending(pageable).map(this::toComicResponse);
    }

    public Page<ComicResponse> getTopRatedComics(Pageable pageable) {
        return comicRepository.findTopRated(pageable).map(this::toComicResponse);
    }

    /* ========== Related Comics ========== */

    public List<ComicResponse> getRelatedComics(Long comicId, int size) {
        ComicEntity comic = getComicEntity(comicId);
        String genresStr = comic.getGenres();
        if (genresStr == null || genresStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> genres = Arrays.stream(genresStr.split(","))
                .map(String::trim)
                .filter(g -> !g.isEmpty())
                .toList();
        if (genres.isEmpty()) {
            return new ArrayList<>();
        }
        Pageable pageable = PageRequest.of(0, size);
        Page<ComicEntity> page;
        if (genres.size() >= 3) {
            page = comicRepository.findRelatedByThreeGenres(comicId, genres.get(0), genres.get(1), genres.get(2), pageable);
        } else if (genres.size() == 2) {
            page = comicRepository.findRelatedByTwoGenres(comicId, genres.get(0), genres.get(1), pageable);
        } else {
            page = comicRepository.findRelatedByOneGenre(comicId, genres.get(0), pageable);
        }
        return page.getContent().stream().map(this::toComicResponse).toList();
    }

    /* ========== Rating ========== */

    @Transactional
    public void rateComic(Long comicId, int score) {
        ComicEntity comic = getComicEntity(comicId);
        UserEntity user = getCurrentUser();

        Optional<ComicRatingEntity> existing = comicRatingRepository.findByUserIdAndComicId(user.getId(), comicId);
        if (existing.isPresent()) {
            existing.get().setScore(score);
            comicRatingRepository.save(existing.get());
        } else {
            ComicRatingEntity rating = new ComicRatingEntity();
            rating.setUserId(user.getId());
            rating.setComicId(comicId);
            rating.setScore(score);
            comicRatingRepository.save(rating);
        }

        Double avg = comicRatingRepository.findAverageScoreByComicId(comicId);
        comicRepository.updateAverageRating(comicId, avg != null ? avg : 0);
    }

    /* ========== View Count ========== */

    @Transactional
    public void incrementViewCount(Long comicId) {
        comicRepository.incrementViewCount(comicId);
    }

    /* ========== Chapters ========== */

    public List<ChapterResponse> getChapters(Long comicId) {
        List<ChapterEntity> chapters = chapterRepository.findByComicIdOrderByChapterNumberAsc(comicId);

        Optional<UserEntity> userOptional = getCurrentUserOptional();
        boolean isVip = false;
        Set<Long> purchasedIds = Collections.emptySet();

        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            isVip = vipRepository.findActiveByUserId(user.getId(), LocalDateTime.now()).isPresent();

            List<Long> chapterIds = chapters.stream().map(ChapterEntity::getId).toList();
            purchasedIds = purchaseRepository
                    .findByUserIdAndChapterIdIn(user.getId(), chapterIds)
                    .stream()
                    .map(p -> p.getChapter().getId())
                    .collect(Collectors.toSet());
        }
        final boolean hasAuthenticatedUser = userOptional.isPresent();
        final boolean userHasVip = isVip;
        final Set<Long> userPurchasedIds = purchasedIds;

        return chapters.stream()
                .map(ch -> {
                    ChapterResponse r = toChapterResponse(ch);
                    if (!ch.isPremium()) {
                        r.setUnlocked(true);
                    } else if (!hasAuthenticatedUser) {
                        r.setUnlocked(false);
                    } else {
                        r.setUnlocked(userHasVip || userPurchasedIds.contains(ch.getId()));
                    }
                    return r;
                })
                .toList();
    }

    /* ========== Admin CRUD ========== */

    @Transactional
    public ComicResponse createComic(ComicRequest request) {
        ComicEntity comic = new ComicEntity();
        applyComicRequest(comic, request);
        ComicEntity saved = comicRepository.save(comic);
        return toComicResponse(saved);
    }

    @Transactional
    public ComicResponse updateComic(Long comicId, ComicRequest request) {
        ComicEntity comic = getComicEntity(comicId);
        applyComicRequest(comic, request);
        comic.setUpdatedAt(LocalDateTime.now());
        return toComicResponse(comicRepository.save(comic));
    }

    @Transactional
    public void deleteComic(Long comicId) {
        ComicEntity comic = getComicEntity(comicId);
        comicRepository.delete(comic);
    }

    @Transactional
    public ChapterResponse createChapter(Long comicId, ChapterRequest request) {
        ComicEntity comic = getComicEntity(comicId);
        ChapterEntity chapter = new ChapterEntity();
        chapter.setComic(comic);
        chapter.setChapterNumber(request.getChapterNumber());
        chapter.setTitle(request.getTitle());
        chapter.setLanguage("vi");
        chapter.setPremium(Boolean.TRUE.equals(request.getPremium()));
        if (request.getPrice() != null) {
            chapter.setPrice(request.getPrice());
        }
        ChapterEntity saved = chapterRepository.save(chapter);
        return toChapterResponse(saved);
    }

    /* ========== Helpers ========== */

    public ComicEntity getComicEntity(Long comicId) {
        return comicRepository.findById(comicId)
                .orElseThrow(() -> new NotFoundException("Comic not found: " + comicId));
    }

    private void applyComicRequest(ComicEntity comic, ComicRequest request) {
        comic.setTitle(request.getTitle());
        comic.setAuthor(request.getAuthor());
        comic.setSynopsis(request.getSynopsis());
        comic.setCoverUrl(request.getCoverUrl());
        comic.setStatus(request.getStatus());
        comic.setUpdatedAt(LocalDateTime.now());
    }

    private ComicResponse toComicResponse(ComicEntity entity) {
        ComicResponse response = new ComicResponse();
        response.setId(entity.getId());
        response.setTitle(entity.getTitle());
        response.setAuthor(entity.getAuthor());
        response.setSlug(entity.getSlug());

        if (entity.getGenres() != null && !entity.getGenres().trim().isEmpty()) {
            response.setGenres(Arrays.asList(entity.getGenres().split(",")));
        } else {
            response.setGenres(new ArrayList<>());
        }

        response.setSynopsis(entity.getSynopsis());
        response.setCoverUrl(entity.getCoverUrl());
        response.setStatus(entity.getStatus());
        response.setViewCount(entity.getViewCount());
        response.setAverageRating(entity.getAverageRating());
        response.setFollowerCount(entity.getFollowerCount());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private ChapterResponse toChapterResponse(ChapterEntity entity) {
        ChapterResponse response = new ChapterResponse();
        response.setId(entity.getId());
        response.setComicId(entity.getComic().getId());
        response.setChapterNumber(entity.getChapterNumber());
        response.setTitle(entity.getTitle());
        response.setLanguage(entity.getLanguage());
        response.setPremium(entity.isPremium());
        response.setPrice(entity.getPrice());
        return response;
    }

    private UserEntity getCurrentUser() {
        Optional<UserEntity> userOptional = getCurrentUserOptional();
        return userOptional.orElseThrow(() -> new RuntimeException("User not authenticated"));
    }

    private Optional<UserEntity> getCurrentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return Optional.empty();
        }

        String email;
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = principal.toString();
        }

        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email);
    }
}
