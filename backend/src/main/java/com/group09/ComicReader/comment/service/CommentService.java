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
import org.springframework.data.domain.PageImpl;
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

    private static final int MAX_REPLY_DEPTH = 4; // depth 0..4 (5 levels)

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

        CommentEntity parent = null;
        if (request.getParentCommentId() != null) {
            parent = getCommentEntity(request.getParentCommentId());
            if (parent.getComic() == null || parent.getComic().getId() == null
                    || !parent.getComic().getId().equals(comicId)) {
                throw new BadRequestException("Parent comment does not belong to this comic");
            }
            if (parent.isHidden()) {
                throw new BadRequestException("Cannot reply to a hidden comment");
            }
            if (parent.isLocked()) {
                throw new BadRequestException("This comment is locked");
            }
        }

        ChapterEntity chapter = null;
        if (request.getChapterId() != null) {
            chapter = chapterService.getChapterEntity(request.getChapterId());
            if (chapter.getComic() == null || chapter.getComic().getId() == null
                    || !chapter.getComic().getId().equals(comicId)) {
                throw new BadRequestException("Chapter does not belong to this comic");
            }
        }

        if (parent != null) {
            ChapterEntity parentChapter = parent.getChapter();
            if (parentChapter == null && chapter != null) {
                throw new BadRequestException("Reply chapter mismatch");
            }
            if (parentChapter != null && chapter == null) {
                throw new BadRequestException("Reply chapter mismatch");
            }
            if (parentChapter != null && chapter != null && !parentChapter.getId().equals(chapter.getId())) {
                throw new BadRequestException("Reply chapter mismatch");
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

        if (parent != null) {
            int depth = parent.getDepth() + 1;
            if (depth > MAX_REPLY_DEPTH) {
                throw new BadRequestException("Max reply depth reached");
            }
            comment.setParentComment(parent);
            comment.setDepth(depth);
            CommentEntity root = parent.getRootComment() != null ? parent.getRootComment() : parent;
            comment.setRootComment(root);
            comment.setChapter(parent.getChapter());
        }

        CommentEntity saved = commentRepository.save(comment);
        if (saved.getParentComment() == null && saved.getRootComment() == null) {
            saved.setRootComment(saved);
            saved = commentRepository.save(saved);
        }
        return toCommentResponse(saved);
    }

    public Page<CommentResponse> getCommentsByComicPaged(Long comicId, Long chapterId, Pageable pageable) {
        comicService.getComicEntity(comicId); // validate comic exists
        Page<CommentEntity> roots = commentRepository.findRootCommentsPaged(comicId, chapterId, pageable);
        if (roots.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, roots.getTotalElements());
        }

        List<Long> rootIds = roots.getContent().stream().map(CommentEntity::getId).toList();
        List<CommentEntity> threadComments = commentRepository.findThreadCommentsForRoots(comicId, chapterId, rootIds);
        List<CommentResponse> flattened = flattenThreadsInRootOrder(roots.getContent(), threadComments);
        return new PageImpl<>(flattened, pageable, roots.getTotalElements());
    }

    private List<CommentResponse> flattenThreadsInRootOrder(List<CommentEntity> roots, List<CommentEntity> threadComments) {
        java.util.Map<Long, CommentEntity> byId = new java.util.HashMap<>();
        for (CommentEntity c : threadComments) {
            byId.put(c.getId(), c);
        }

        java.util.Map<Long, java.util.List<CommentEntity>> childrenByParentId = new java.util.HashMap<>();
        for (CommentEntity c : threadComments) {
            if (c.getParentComment() == null) continue;
            Long parentId = c.getParentComment().getId();
            childrenByParentId.computeIfAbsent(parentId, k -> new java.util.ArrayList<>()).add(c);
        }

        for (java.util.List<CommentEntity> children : childrenByParentId.values()) {
            children.sort(java.util.Comparator.comparing(CommentEntity::getCreatedAt));
        }

        java.util.List<CommentResponse> result = new java.util.ArrayList<>();
        for (CommentEntity root : roots) {
            CommentEntity rootFromThread = byId.getOrDefault(root.getId(), root);
            dfsAppend(rootFromThread, childrenByParentId, result);
        }
        return result;
    }

    private void dfsAppend(CommentEntity node,
            java.util.Map<Long, java.util.List<CommentEntity>> childrenByParentId,
            java.util.List<CommentResponse> out) {
        out.add(toCommentResponse(node));
        java.util.List<CommentEntity> children = childrenByParentId.get(node.getId());
        if (children == null || children.isEmpty()) return;
        for (CommentEntity child : children) {
            dfsAppend(child, childrenByParentId, out);
        }
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
        response.setAvatarUrl(entity.getUser().getAvatarUrl());
        response.setContent(entity.getContent());
        response.setHidden(entity.isHidden());
        response.setLocked(entity.isLocked());
        response.setSourceType(entity.getSourceType());
        response.setCreatedAt(entity.getCreatedAt());

        response.setDepth(entity.getDepth());
        if (entity.getParentComment() != null) {
            response.setParentCommentId(entity.getParentComment().getId());
        }
        if (entity.getRootComment() != null) {
            response.setRootCommentId(entity.getRootComment().getId());
        }

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
