package com.group09.ComicReader.chapter.service;

import com.group09.ComicReader.chapter.dto.ChapterPageResponse;
import com.group09.ComicReader.chapter.dto.ChapterRequest;
import com.group09.ComicReader.chapter.dto.ChapterResponse;
import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.entity.ChapterPageEntity;
import com.group09.ComicReader.chapter.repository.ChapterPageRepository;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.common.exception.BadRequestException;
import com.group09.ComicReader.common.exception.NotFoundException;
import com.group09.ComicReader.common.storage.FileStorageService;
import com.group09.ComicReader.auth.entity.UserEntity;
import com.group09.ComicReader.auth.repository.UserRepository;
import com.group09.ComicReader.wallet.repository.ChapterPurchaseRepository;
import com.group09.ComicReader.wallet.repository.VipSubscriptionRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.group09.ComicReader.comic.dto.OTruyenChapterDetailResponseDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final ChapterPageRepository chapterPageRepository;
    private final FileStorageService fileStorageService;
    private final ChapterPurchaseRepository purchaseRepository;
    private final VipSubscriptionRepository vipRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    public ChapterService(ChapterRepository chapterRepository,
            ChapterPageRepository chapterPageRepository,
            FileStorageService fileStorageService,
            ChapterPurchaseRepository purchaseRepository,
            VipSubscriptionRepository vipRepository,
            UserRepository userRepository) {
        this.chapterRepository = chapterRepository;
        this.chapterPageRepository = chapterPageRepository;
        this.fileStorageService = fileStorageService;
        this.purchaseRepository = purchaseRepository;
        this.vipRepository = vipRepository;
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
    }

    public List<ChapterPageResponse> getPages(Long chapterId) {
        ChapterEntity chapter = getChapterEntity(chapterId);

        // Gate premium chapters: VIP users bypass, others need purchase
        if (chapter.isPremium()) {
            UserEntity user = getCurrentUser();
            boolean isVip = vipRepository.findActiveByUserId(user.getId(), LocalDateTime.now()).isPresent();
            if (!isVip) {
                boolean purchased = purchaseRepository
                        .existsByUserIdAndChapterId(user.getId(), chapter.getId());
                if (!purchased) {
                    throw new BadRequestException("Chapter is locked. Purchase it to read.");
                }
            }
        }

        List<ChapterPageEntity> pages = chapterPageRepository.findByChapterIdOrderByPageNumberAsc(chapterId);

        if (pages.isEmpty() && chapter.getOtruyenApiData() != null) {
            try {
                // Lazy Load from CDN
                String apiDataUrl = chapter.getOtruyenApiData();
                OTruyenChapterDetailResponseDTO apiResponse = restTemplate.getForObject(apiDataUrl,
                        OTruyenChapterDetailResponseDTO.class);

                if (apiResponse != null && apiResponse.getData() != null && apiResponse.getData().getItem() != null) {
                    OTruyenChapterDetailResponseDTO.Item item = apiResponse.getData().getItem();
                    String domainCdn = apiResponse.getData().getDomain_cdn();
                    String path = item.getChapter_path();

                    List<OTruyenChapterDetailResponseDTO.ImagePage> images = item.getChapter_image();
                    if (images != null) {
                        for (OTruyenChapterDetailResponseDTO.ImagePage image : images) {
                            String pageUrl = domainCdn + "/" + path + "/" + image.getImage_file();

                            ChapterPageEntity pageEntity = new ChapterPageEntity();
                            pageEntity.setChapter(chapter);
                            pageEntity.setPageNumber(image.getImage_page() != null ? image.getImage_page() : 0);
                            pageEntity.setImageUrl(pageUrl);
                            pageEntity.setCreatedAt(LocalDateTime.now());

                            chapterPageRepository.save(pageEntity);
                            pages.add(pageEntity);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to lazy load chapter pages from OTruyen: " + e.getMessage(), e);
            }
        }

        return pages.stream()
                .map(this::toPageResponse)
                .toList();
    }

    @Transactional
    public ChapterResponse updateChapter(Long chapterId, ChapterRequest request) {
        ChapterEntity chapter = getChapterEntity(chapterId);
        chapter.setChapterNumber(request.getChapterNumber());
        chapter.setTitle(request.getTitle());
        chapter.setPremium(Boolean.TRUE.equals(request.getPremium()));
        if (request.getPrice() != null) {
            chapter.setPrice(request.getPrice());
        }
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
