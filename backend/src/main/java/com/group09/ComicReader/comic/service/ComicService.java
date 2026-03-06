package com.group09.ComicReader.comic.service;

import com.group09.ComicReader.chapter.dto.ChapterRequest;
import com.group09.ComicReader.chapter.dto.ChapterResponse;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.comic.dto.ComicRequest;
import com.group09.ComicReader.comic.dto.ComicResponse;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.repository.ComicRepository;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.wallet.entity.ChapterPurchaseEntity;
import com.group09.ComicReader.wallet.repository.ChapterPurchaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ComicService {

    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPurchaseRepository purchaseRepository;
    private final UserRepository userRepository;

    public ComicService(ComicRepository comicRepository,
                        ChapterRepository chapterRepository,
                        ChapterPurchaseRepository purchaseRepository,
                        UserRepository userRepository) {
        this.comicRepository = comicRepository;
        this.chapterRepository = chapterRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
    }

    public Page<ComicResponse> getComics(Pageable pageable) {
        return comicRepository.findAll(pageable).map(this::toComicResponse);
    }

    public ComicResponse getComic(Long comicId) {
        ComicEntity comic = getComicEntity(comicId);
        return toComicResponse(comic);
    }

    public List<ChapterResponse> getChapters(Long comicId) {
        List<ChapterEntity> chapters = chapterRepository.findByComicIdOrderByChapterNumberAsc(comicId);

        // Get current user's purchase info
        UserEntity user = getCurrentUser();
        List<Long> chapterIds = chapters.stream().map(ChapterEntity::getId).toList();
        Set<Long> purchasedIds = purchaseRepository
                .findByUserIdAndChapterIdIn(user.getId(), chapterIds)
                .stream()
                .map(p -> p.getChapter().getId())
                .collect(Collectors.toSet());

        return chapters.stream()
                .map(ch -> {
                    ChapterResponse r = toChapterResponse(ch);
                    // Free chapters are always unlocked; premium chapters require purchase
                    r.setUnlocked(!ch.isPremium() || purchasedIds.contains(ch.getId()));
                    return r;
                })
                .toList();
    }

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
        chapter.setPremium(Boolean.TRUE.equals(request.getPremium()));
        if (request.getPrice() != null) {
            chapter.setPrice(request.getPrice());
        }
        ChapterEntity saved = chapterRepository.save(chapter);
        return toChapterResponse(saved);
    }

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
        response.setSynopsis(entity.getSynopsis());
        response.setCoverUrl(entity.getCoverUrl());
        response.setStatus(entity.getStatus());
        return response;
    }

    private ChapterResponse toChapterResponse(ChapterEntity entity) {
        ChapterResponse response = new ChapterResponse();
        response.setId(entity.getId());
        response.setComicId(entity.getComic().getId());
        response.setChapterNumber(entity.getChapterNumber());
        response.setTitle(entity.getTitle());
        response.setPremium(entity.isPremium());
        response.setPrice(entity.getPrice());
        return response;
    }

    private UserEntity getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = principal.toString();
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}

