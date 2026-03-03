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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComicService {

    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;

    public ComicService(ComicRepository comicRepository, ChapterRepository chapterRepository) {
        this.comicRepository = comicRepository;
        this.chapterRepository = chapterRepository;
    }

    public Page<ComicResponse> getComics(Pageable pageable) {
        return comicRepository.findAll(pageable).map(this::toComicResponse);
    }

    public ComicResponse getComic(Long comicId) {
        ComicEntity comic = getComicEntity(comicId);
        return toComicResponse(comic);
    }

    public List<ChapterResponse> getChapters(Long comicId) {
        return chapterRepository.findByComicIdOrderByChapterNumberAsc(comicId).stream()
                .map(this::toChapterResponse)
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
        return response;
    }
}

