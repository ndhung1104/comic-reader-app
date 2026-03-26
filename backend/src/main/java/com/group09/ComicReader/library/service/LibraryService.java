package com.group09.ComicReader.library.service;

import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.repository.ComicRepository;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.library.dto.FollowStatusResponse;
import com.group09.ComicReader.library.dto.FollowedComicResponse;
import com.group09.ComicReader.library.dto.ReadingHistoryRequest;
import com.group09.ComicReader.library.dto.RecentReadResponse;
import com.group09.ComicReader.library.entity.FollowedComicEntity;
import com.group09.ComicReader.library.entity.ReadingHistoryEntity;
import com.group09.ComicReader.library.repository.FollowedComicRepository;
import com.group09.ComicReader.library.repository.ReadingHistoryRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LibraryService {

    private final FollowedComicRepository followedComicRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;

    public LibraryService(FollowedComicRepository followedComicRepository,
                          ReadingHistoryRepository readingHistoryRepository,
                          ComicRepository comicRepository,
                          ChapterRepository chapterRepository,
                          UserRepository userRepository) {
        this.followedComicRepository = followedComicRepository;
        this.readingHistoryRepository = readingHistoryRepository;
        this.comicRepository = comicRepository;
        this.chapterRepository = chapterRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<FollowedComicResponse> getFollowedComics() {
        UserEntity user = getCurrentUser();
        List<FollowedComicEntity> follows = followedComicRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<FollowedComicResponse> responses = new ArrayList<>();
        for (FollowedComicEntity follow : follows) {
            ReadingHistoryEntity history = readingHistoryRepository
                    .findByUserIdAndComicId(user.getId(), follow.getComic().getId())
                    .orElse(null);
            responses.add(toFollowedComicResponse(follow.getComic(), history));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<RecentReadResponse> getRecentReads() {
        UserEntity user = getCurrentUser();
        List<ReadingHistoryEntity> history = readingHistoryRepository.findByUserIdOrderByLastReadAtDesc(user.getId());
        List<RecentReadResponse> responses = new ArrayList<>();
        for (ReadingHistoryEntity item : history) {
            responses.add(toRecentReadResponse(item));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public FollowStatusResponse getFollowStatus(Long comicId) {
        UserEntity user = getCurrentUser();
        return new FollowStatusResponse(followedComicRepository.existsByUserIdAndComicId(user.getId(), comicId));
    }

    @Transactional
    public FollowStatusResponse followComic(Long comicId) {
        UserEntity user = getCurrentUser();
        if (followedComicRepository.existsByUserIdAndComicId(user.getId(), comicId)) {
            return new FollowStatusResponse(true);
        }

        ComicEntity comic = comicRepository.findById(comicId)
                .orElseThrow(() -> new NotFoundException("Comic not found: " + comicId));

        FollowedComicEntity follow = new FollowedComicEntity();
        follow.setUser(user);
        follow.setComic(comic);
        followedComicRepository.save(follow);
        return new FollowStatusResponse(true);
    }

    @Transactional
    public FollowStatusResponse unfollowComic(Long comicId) {
        UserEntity user = getCurrentUser();
        followedComicRepository.findByUserIdAndComicId(user.getId(), comicId)
                .ifPresent(followedComicRepository::delete);
        return new FollowStatusResponse(false);
    }

    @Transactional
    public RecentReadResponse recordReadingHistory(ReadingHistoryRequest request) {
        if (request.getComicId() == null) {
            throw new BadRequestException("comicId is required");
        }
        if (request.getChapterId() == null) {
            throw new BadRequestException("chapterId is required");
        }

        UserEntity user = getCurrentUser();
        ComicEntity comic = comicRepository.findById(request.getComicId())
                .orElseThrow(() -> new NotFoundException("Comic not found: " + request.getComicId()));
        ChapterEntity chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new NotFoundException("Chapter not found: " + request.getChapterId()));

        if (!chapter.getComic().getId().equals(comic.getId())) {
            throw new BadRequestException("Chapter does not belong to comic");
        }

        ReadingHistoryEntity history = readingHistoryRepository.findByUserIdAndComicId(user.getId(), comic.getId())
                .orElseGet(ReadingHistoryEntity::new);
        history.setUser(user);
        history.setComic(comic);
        history.setChapter(chapter);
        history.setPageNumber(request.getPageNumber());
        history.setLastReadAt(LocalDateTime.now());
        ReadingHistoryEntity saved = readingHistoryRepository.save(history);
        return toRecentReadResponse(saved);
    }

    private FollowedComicResponse toFollowedComicResponse(ComicEntity comic, ReadingHistoryEntity history) {
        FollowedComicResponse response = new FollowedComicResponse();
        response.setComicId(comic.getId());
        response.setTitle(comic.getTitle());
        response.setAuthor(comic.getAuthor());
        response.setCoverUrl(comic.getCoverUrl());
        response.setTotalChapters(chapterRepository.countByComicId(comic.getId()));
        if (history != null && history.getChapter() != null) {
            response.setLastReadChapterNumber(history.getChapter().getChapterNumber());
            response.setLastReadChapterTitle(history.getChapter().getTitle());
        }
        return response;
    }

    private RecentReadResponse toRecentReadResponse(ReadingHistoryEntity history) {
        RecentReadResponse response = new RecentReadResponse();
        response.setComicId(history.getComic().getId());
        response.setTitle(history.getComic().getTitle());
        response.setAuthor(history.getComic().getAuthor());
        response.setCoverUrl(history.getComic().getCoverUrl());
        response.setTotalChapters(chapterRepository.countByComicId(history.getComic().getId()));
        response.setChapterId(history.getChapter().getId());
        response.setChapterNumber(history.getChapter().getChapterNumber());
        response.setChapterTitle(history.getChapter().getTitle());
        response.setPageNumber(history.getPageNumber());
        response.setLastReadAt(history.getLastReadAt());
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
