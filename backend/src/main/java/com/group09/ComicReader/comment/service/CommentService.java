package com.group09.ComicReader.comment.service;

import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.service.ChapterService;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.service.ComicService;
import com.group09.ComicReader.comment.dto.CommentRequest;
import com.group09.ComicReader.comment.dto.CommentResponse;
import com.group09.ComicReader.comment.entity.CommentEntity;
import com.group09.ComicReader.comment.repository.CommentRepository;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ComicService comicService;
    private final UserRepository userRepository;
    private final ChapterService chapterService;

    public CommentService(CommentRepository commentRepository,
            ComicService comicService,
            UserRepository userRepository,
            ChapterService chapterService) {
        this.commentRepository = commentRepository;
        this.comicService = comicService;
        this.userRepository = userRepository;
        this.chapterService = chapterService;
    }

    public List<CommentResponse> getCommentsByComic(Long comicId) {
        comicService.getComicEntity(comicId); // validate comic exists
        return commentRepository.findByComicIdAndHiddenFalseOrderByCreatedAtDesc(comicId)
                .stream()
                .map(this::toCommentResponse)
                .toList();
    }

    @Transactional
    public CommentResponse createComment(Long comicId, CommentRequest request) {
        ComicEntity comic = comicService.getComicEntity(comicId);
        UserEntity user = getCurrentUser();

        ChapterEntity chapter = null;
        if (request.getChapterId() != null) {
            chapter = chapterService.getChapterEntity(request.getChapterId());
            if (chapter.getComic() == null || chapter.getComic().getId() == null
                    || !chapter.getComic().getId().equals(comicId)) {
                throw new BadRequestException("Chapter does not belong to this comic");
            }
        }

        CommentEntity comment = new CommentEntity();
        comment.setComic(comic);
        comment.setChapter(chapter);
        comment.setUser(user);
        comment.setContent(request.getContent());
        if (request.getSourceType() != null && !request.getSourceType().trim().isEmpty()) {
            comment.setSourceType(request.getSourceType().trim());
        }

        CommentEntity saved = commentRepository.save(comment);
        return toCommentResponse(saved);
    }

    public Page<CommentResponse> getCommentsByComicPaged(Long comicId, Long chapterId, Pageable pageable) {
        comicService.getComicEntity(comicId); // validate comic exists
        if (chapterId != null) {
            return commentRepository.findByComicIdAndChapterIdAndHiddenFalseOrderByCreatedAtDesc(comicId, chapterId, pageable)
                    .map(this::toCommentResponse);
        }
        return commentRepository.findByComicIdAndHiddenFalseOrderByCreatedAtDesc(comicId, pageable)
                .map(this::toCommentResponse);
    }

    // --- Admin methods ---

    public Page<CommentResponse> getAllCommentsByComic(Long comicId, Pageable pageable) {
        comicService.getComicEntity(comicId); // validate
        return commentRepository.findByComicIdOrderByCreatedAtDesc(comicId, pageable)
                .map(this::toCommentResponse);
    }

    @Transactional
    public CommentResponse hideComment(Long commentId) {
        CommentEntity comment = getCommentEntity(commentId);
        comment.setHidden(true);
        comment.setUpdatedAt(LocalDateTime.now());
        return toCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse unhideComment(Long commentId) {
        CommentEntity comment = getCommentEntity(commentId);
        comment.setHidden(false);
        comment.setUpdatedAt(LocalDateTime.now());
        return toCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse lockComment(Long commentId) {
        CommentEntity comment = getCommentEntity(commentId);
        comment.setLocked(true);
        comment.setUpdatedAt(LocalDateTime.now());
        return toCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse unlockComment(Long commentId) {
        CommentEntity comment = getCommentEntity(commentId);
        comment.setLocked(false);
        comment.setUpdatedAt(LocalDateTime.now());
        return toCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long commentId) {
        CommentEntity comment = getCommentEntity(commentId);
        commentRepository.delete(comment);
    }

    private CommentEntity getCommentEntity(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));
    }

    private CommentResponse toCommentResponse(CommentEntity entity) {
        CommentResponse response = new CommentResponse();
        response.setId(entity.getId());
        response.setComicId(entity.getComic().getId());
        response.setUserId(entity.getUser().getId());
        response.setUsername(entity.getUser().getFullName());
        response.setContent(entity.getContent());
        response.setHidden(entity.isHidden());
        response.setLocked(entity.isLocked());
        response.setSourceType(entity.getSourceType());
        response.setCreatedAt(entity.getCreatedAt());

        if (entity.getChapter() != null) {
            response.setChapterId(entity.getChapter().getId());
            response.setChapterNumber(entity.getChapter().getChapterNumber());
            response.setChapterTitle(entity.getChapter().getTitle());
        }
        return response;
    }

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = principal.toString();
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
