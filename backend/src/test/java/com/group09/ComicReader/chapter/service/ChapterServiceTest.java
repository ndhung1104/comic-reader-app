package com.group09.ComicReader.chapter.service;

import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterFreeAdAccessRepository;
import com.group09.ComicReader.chapter.repository.ChapterPageRepository;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.common.exception.ForbiddenException;
import com.group09.ComicReader.common.storage.FileStorageService;
import com.group09.ComicReader.wallet.repository.ChapterPurchaseRepository;
import com.group09.ComicReader.wallet.repository.VipSubscriptionRepository;
import com.group09.ComicReader.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ChapterPageRepository chapterPageRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ChapterFreeAdAccessRepository chapterFreeAdAccessRepository;

    @Mock
    private ChapterPurchaseRepository purchaseRepository;

    @Mock
    private VipSubscriptionRepository vipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChapterPremiumPolicyService chapterPremiumPolicyService;

    @Mock
    private ChapterAdPolicyService chapterAdPolicyService;

    private ChapterService chapterService;

    @BeforeEach
    void setUp() {
        chapterService = new ChapterService(
                chapterRepository,
                chapterPageRepository,
                chapterFreeAdAccessRepository,
                fileStorageService,
                purchaseRepository,
                vipRepository,
                userRepository,
                chapterPremiumPolicyService,
                chapterAdPolicyService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPagesShouldReturn403WhenPremiumChapterIsLockedForAnonymousUser() {
        long chapterId = 88L;
        ChapterEntity chapter = new ChapterEntity();
        chapter.setId(chapterId);
        chapter.setPremium(true);

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));

        assertThatThrownBy(() -> chapterService.getPages(chapterId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("locked");

        verify(chapterPageRepository, never()).findByChapterIdOrderByPageNumberAsc(chapterId);
    }
}
