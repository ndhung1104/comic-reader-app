package com.group09.ComicReader.comic.service;

import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.chapter.service.ChapterPremiumPolicyService;
import com.group09.ComicReader.comic.dto.ComicResponse;
import com.group09.ComicReader.comic.dto.OTruyenDetailResponseDTO;
import com.group09.ComicReader.comic.dto.OTruyenResponseDTO;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.repository.ComicRepository;
import com.group09.ComicReader.common.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OTruyenService {

    private static final Logger log = LoggerFactory.getLogger(OTruyenService.class);

    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPremiumPolicyService chapterPremiumPolicyService;
    private final RestTemplate restTemplate;

    public OTruyenService(ComicRepository comicRepository,
                          ChapterRepository chapterRepository,
                          ChapterPremiumPolicyService chapterPremiumPolicyService) {
        this.comicRepository = comicRepository;
        this.chapterRepository = chapterRepository;
        this.chapterPremiumPolicyService = chapterPremiumPolicyService;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public void syncComicsFromOTruyen() {
        String url = "https://otruyenapi.com/v1/api/danh-sach/dang-phat-hanh";
        OTruyenResponseDTO response = restTemplate.getForObject(url, OTruyenResponseDTO.class);

        if (response != null && response.getData() != null && response.getData().getItems() != null) {
            String imageDomain = response.getData().getAppDomainCdnImage();
            List<OTruyenResponseDTO.Item> items = response.getData().getItems();

            for (OTruyenResponseDTO.Item item : items) {
                if (comicRepository.findBySlug(item.getSlug()).isEmpty()) {
                    ComicEntity comic = new ComicEntity();
                    comic.setTitle(item.getName());
                    comic.setSlug(item.getSlug());

                    // Fetch details to get author and synopsis
                    String detailUrl = "https://otruyenapi.com/v1/api/truyen-tranh/" + item.getSlug();
                    try {
                        OTruyenDetailResponseDTO detailResponse = restTemplate.getForObject(detailUrl,
                                OTruyenDetailResponseDTO.class);
                        if (detailResponse != null && detailResponse.getData() != null
                                && detailResponse.getData().getItem() != null) {
                            OTruyenDetailResponseDTO.Item detailItem = detailResponse.getData().getItem();

                            if (detailItem.getAuthor() != null && !detailItem.getAuthor().isEmpty()) {
                                comic.setAuthor(String.join(", ", detailItem.getAuthor()));
                            } else {
                                comic.setAuthor("Unknown");
                            }

                            if (detailItem.getContent() != null && !detailItem.getContent().trim().isEmpty()) {
                                comic.setSynopsis(detailItem.getContent().replaceAll("<[^>]*>", "").trim());
                            } else {
                                comic.setSynopsis("Synced from OTruyen API");
                            }
                        } else {
                            comic.setAuthor("Unknown");
                            comic.setSynopsis("Synced from OTruyen API");
                        }

                        Thread.sleep(200);

                    } catch (Exception e) {
                        log.error("Failed to fetch detail for slug: {}", item.getSlug(), e);
                        comic.setAuthor("Unknown");
                        comic.setSynopsis("Synced from OTruyen API");
                    }

                    String coverUrl = imageDomain + "/uploads/comics/" + item.getThumbUrl();
                    comic.setCoverUrl(coverUrl);

                    comic.setStatus(item.getStatus() != null ? item.getStatus() : "ongoing");

                    if (item.getCategory() != null) {
                        String genres = item.getCategory().stream()
                                .map(OTruyenResponseDTO.Category::getName)
                                .collect(Collectors.joining(","));
                        comic.setGenres(genres);
                    }

                    comic.setCreatedAt(LocalDateTime.now());
                    comic.setUpdatedAt(LocalDateTime.now());

                    ComicEntity savedComic = comicRepository.save(comic);
                    log.info("Saved comic: {}", item.getName());

                    try {
                        OTruyenDetailResponseDTO detailResponse = restTemplate.getForObject(
                                "https://otruyenapi.com/v1/api/truyen-tranh/" + item.getSlug(),
                                OTruyenDetailResponseDTO.class);
                        if (detailResponse != null && detailResponse.getData() != null
                                && detailResponse.getData().getItem() != null) {
                            importChaptersForComic(savedComic, detailResponse.getData().getItem());
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse chapters for comic: {}", item.getName(), e);
                    }
                }
            }
        }
    }

    // ── Single Comic Import ────────────────────────────────

    @Transactional
    public ComicResponse importSingleComic(String sourceUrlOrSlug) {
        String slug = extractSlug(sourceUrlOrSlug);

        if (comicRepository.findBySlug(slug).isPresent()) {
            throw new BadRequestException("Comic with slug '" + slug + "' already exists in the database");
        }

        String detailUrl = "https://otruyenapi.com/v1/api/truyen-tranh/" + slug;
        OTruyenDetailResponseDTO detailResponse;
        try {
            detailResponse = restTemplate.getForObject(detailUrl, OTruyenDetailResponseDTO.class);
        } catch (Exception e) {
            log.error("Failed to fetch comic from OTruyen for slug: {}", slug, e);
            throw new BadRequestException("Failed to fetch comic from OTruyen. Please check the URL/slug and try again.");
        }

        if (detailResponse == null || detailResponse.getData() == null
                || detailResponse.getData().getItem() == null) {
            throw new BadRequestException("Comic not found on OTruyen for slug: " + slug);
        }

        OTruyenDetailResponseDTO.Item detailItem = detailResponse.getData().getItem();
        String cdnDomain = detailResponse.getData().getAppDomainCdnImage() != null
                ? detailResponse.getData().getAppDomainCdnImage()
                : "https://img.otruyenapi.com";

        ComicEntity comic = new ComicEntity();
        comic.setTitle(detailItem.getName() != null ? detailItem.getName() : slug);
        comic.setSlug(detailItem.getSlug() != null ? detailItem.getSlug() : slug);

        if (detailItem.getAuthor() != null && !detailItem.getAuthor().isEmpty()) {
            comic.setAuthor(String.join(", ", detailItem.getAuthor()));
        } else {
            comic.setAuthor("Unknown");
        }

        if (detailItem.getContent() != null && !detailItem.getContent().trim().isEmpty()) {
            comic.setSynopsis(detailItem.getContent().replaceAll("<[^>]*>", "").trim());
        } else {
            comic.setSynopsis("Imported from OTruyen");
        }

        if (detailItem.getThumb_url() != null && !detailItem.getThumb_url().isEmpty()) {
            comic.setCoverUrl(cdnDomain + "/uploads/comics/" + detailItem.getThumb_url());
        }

        comic.setStatus(detailItem.getStatus() != null ? detailItem.getStatus() : "ongoing");

        if (detailItem.getCategory() != null && !detailItem.getCategory().isEmpty()) {
            String genres = detailItem.getCategory().stream()
                    .map(OTruyenDetailResponseDTO.Category::getName)
                    .collect(Collectors.joining(","));
            comic.setGenres(genres);
        }

        comic.setCreatedAt(LocalDateTime.now());
        comic.setUpdatedAt(LocalDateTime.now());
        ComicEntity savedComic = comicRepository.save(comic);
        log.info("Imported comic: {} (slug: {})", comic.getTitle(), slug);

        int chapterCount = importChaptersForComic(savedComic, detailItem);
        log.info("Imported {} chapters for comic: {}", chapterCount, comic.getTitle());

        return toComicResponse(savedComic);
    }

    // ── Helpers ─────────────────────────────────────────────

    private int importChaptersForComic(ComicEntity comic, OTruyenDetailResponseDTO.Item detailItem) {
        List<OTruyenDetailResponseDTO.ChapterItem> chapters = detailItem.getChapters();
        if (chapters == null || chapters.isEmpty()) {
            return 0;
        }

        List<OTruyenDetailResponseDTO.ServerData> serverData = chapters.get(0).getServer_data();
        if (serverData == null || serverData.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < serverData.size(); i++) {
            OTruyenDetailResponseDTO.ServerData cData = serverData.get(i);

            ChapterEntity chapter = new ChapterEntity();
            chapter.setComic(comic);
            chapter.setChapterNumber(i + 1);

            String title = "Chapter " + (i + 1);
            if (cData.getChapter_name() != null && !cData.getChapter_name().trim().isEmpty()) {
                title = cData.getChapter_name();
            }
            chapter.setTitle(title);
            chapter.setOtruyenApiData(cData.getChapter_api_data());
            chapter.setPremium(false);
            chapter.setPrice(0);
            chapter.setCreatedAt(LocalDateTime.now());
            chapter.setUpdatedAt(LocalDateTime.now());

            chapterRepository.save(chapter);
            count++;
        }

        chapterPremiumPolicyService.applyLastThreePremiumPolicy(comic.getId());
        return count;
    }

    private String extractSlug(String sourceUrlOrSlug) {
        if (sourceUrlOrSlug == null || sourceUrlOrSlug.trim().isEmpty()) {
            throw new BadRequestException("Source URL or slug cannot be empty");
        }

        String input = sourceUrlOrSlug.trim();

        if (input.startsWith("http://") || input.startsWith("https://")) {
            String[] parts = input.split("/");
            if (parts.length > 0) {
                return parts[parts.length - 1];
            }
        }

        return input;
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
}
