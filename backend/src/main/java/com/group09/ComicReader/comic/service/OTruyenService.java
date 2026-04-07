package com.group09.ComicReader.comic.service;

import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.comic.dto.OTruyenDetailResponseDTO;
import com.group09.ComicReader.comic.dto.OTruyenResponseDTO;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.repository.ComicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OTruyenService {

    private static final Logger log = LoggerFactory.getLogger(OTruyenService.class);

    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;
    private final RestTemplate restTemplate;

    public OTruyenService(ComicRepository comicRepository, ChapterRepository chapterRepository) {
        this.comicRepository = comicRepository;
        this.chapterRepository = chapterRepository;
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

                            // Map Author array to CSV string or set default
                            if (detailItem.getAuthor() != null && !detailItem.getAuthor().isEmpty()) {
                                comic.setAuthor(String.join(", ", detailItem.getAuthor()));
                            } else {
                                comic.setAuthor("Unknown");
                            }

                            // Optional Synopsis map
                            if (detailItem.getContent() != null && !detailItem.getContent().trim().isEmpty()) {
                                // Extract raw text if HTML (simplistic replace) or just store it natively
                                comic.setSynopsis(detailItem.getContent().replaceAll("<[^>]*>", "").trim());
                            } else {
                                comic.setSynopsis("Synced from OTruyen API");
                            }
                        } else {
                            comic.setAuthor("Unknown");
                            comic.setSynopsis("Synced from OTruyen API");
                        }

                        // Sleep to not DDOS the API provider during large syncs
                        Thread.sleep(200);

                    } catch (Exception e) {
                        log.error("Failed to fetch detail for slug: {}", item.getSlug(), e);
                        comic.setAuthor("Unknown");
                        comic.setSynopsis("Synced from OTruyen API");
                    }

                    // Combine image domain and path
                    String coverUrl = imageDomain + "/uploads/comics/" + item.getThumbUrl();
                    comic.setCoverUrl(coverUrl);

                    comic.setStatus(item.getStatus() != null ? item.getStatus() : "ongoing");

                    // Convert categories list to comma-separated string
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

                    // Now process detailed chapter array if we have one
                    try {
                        OTruyenDetailResponseDTO detailResponse = restTemplate.getForObject(
                                "https://otruyenapi.com/v1/api/truyen-tranh/" + item.getSlug(),
                                OTruyenDetailResponseDTO.class);
                        if (detailResponse != null && detailResponse.getData() != null
                                && detailResponse.getData().getItem() != null) {
                            List<OTruyenDetailResponseDTO.ChapterItem> chapters = detailResponse.getData().getItem()
                                    .getChapters();
                            if (chapters != null && !chapters.isEmpty()) {
                                // OTruyen typically has multiple servers. We pick the first one which is
                                // usually sv1.
                                List<OTruyenDetailResponseDTO.ServerData> serverData = chapters.get(0).getServer_data();
                                if (serverData != null) {
                                    for (int i = 0; i < serverData.size(); i++) {
                                        OTruyenDetailResponseDTO.ServerData cData = serverData.get(i);

                                        ChapterEntity chapter = new ChapterEntity();
                                        chapter.setComic(savedComic);
                                        chapter.setChapterNumber(i + 1); // Ensure sequentially numbered index

                                        String title = "Chapter " + (i + 1);
                                        if (cData.getChapter_name() != null
                                                && !cData.getChapter_name().trim().isEmpty()) {
                                            title = cData.getChapter_name();
                                        }
                                        chapter.setTitle(title);
                                        chapter.setLanguage("vi");
                                        chapter.setOtruyenApiData(cData.getChapter_api_data());
                                        chapter.setPremium(false); // OTruyen data defaults to free
                                        chapter.setPrice(0);
                                        chapter.setCreatedAt(LocalDateTime.now());
                                        chapter.setUpdatedAt(LocalDateTime.now());

                                        chapterRepository.save(chapter);
                                    }
                                    log.info("Saved {} chapters for comic: {}", serverData.size(), item.getName());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse chapters for comic: {}", item.getName(), e);
                    }
                }
            }
        }
    }
}
