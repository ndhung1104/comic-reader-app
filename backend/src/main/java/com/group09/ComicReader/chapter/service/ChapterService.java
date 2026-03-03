package com.group09.ComicReader.chapter.service;

import com.group09.ComicReader.chapter.dto.ChapterPageResponse;
import com.group09.ComicReader.chapter.dto.ChapterRequest;
import com.group09.ComicReader.chapter.dto.ChapterResponse;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.entity.ChapterPageEntity;
import com.group09.ComicReader.chapter.repository.ChapterPageRepository;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.common.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final FileStorageService fileStorageService;

    public ChapterService(ChapterRepository chapterRepository,
                          ChapterPageRepository chapterPageRepository,
                          FileStorageService fileStorageService) {
        this.chapterRepository = chapterRepository;
        this.chapterPageRepository = chapterPageRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<ChapterPageResponse> getPages(Long chapterId) {
        return chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId).stream()
                .map(this::toPageResponse)
                .toList();
    }

    @Transactional
    public ChapterResponse updateChapter(Long chapterId, ChapterRequest request) {
        ChapterEntity chapter = getChapterEntity(chapterId);
        chapter.setChapterNumber(request.getChapterNumber());
        chapter.setTitle(request.getTitle());
        chapter.setPremium(Boolean.TRUE.equals(request.getPremium()));
        chapter.setUpdatedAt(LocalDateTime.now());
        return toChapterResponse(chapterRepository.save(chapter));
    }

    @Transactional
    public void deleteChapter(Long chapterId) {
        ChapterEntity chapter = getChapterEntity(chapterId);
        chapterRepository.delete(chapter);
    }

    @Transactional
    public List<ChapterPageResponse> uploadPages(Long chapterId, MultipartFile[] files) {
        ChapterEntity chapter = getChapterEntity(chapterId);
        int startIndex = chapterPageRepository.countByChapterId(chapterId);
        List<ChapterPageResponse> result = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            String imageUrl = fileStorageService.storeChapterPage(chapterId, files[i]);

            ChapterPageEntity page = new ChapterPageEntity();
            page.setChapter(chapter);
            page.setPageNumber(startIndex + i + 1);
            page.setImageUrl(imageUrl);

            ChapterPageEntity saved = chapterPageRepository.save(page);
            result.add(toPageResponse(saved));
        }

        return result;
    }

    public ChapterEntity getChapterEntity(Long chapterId) {
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NotFoundException("Chapter not found: " + chapterId));
    }

    private ChapterPageResponse toPageResponse(ChapterPageEntity entity) {
        ChapterPageResponse response = new ChapterPageResponse();
        response.setId(entity.getId());
        response.setChapterId(entity.getChapter().getId());
        response.setPageNumber(entity.getPageNumber());
        response.setImageUrl(entity.getImageUrl());
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

