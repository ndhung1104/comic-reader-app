package com.group09.ComicReader.comic.service;

import com.group09.ComicReader.chapter.entity.ChapterEntity;
import com.group09.ComicReader.chapter.repository.ChapterRepository;
import com.group09.ComicReader.chapter.service.ChapterPremiumPolicyService;
import com.group09.ComicReader.comic.dto.OTruyenDetailResponseDTO;
import com.group09.ComicReader.comic.dto.OTruyenResponseDTO;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.repository.ComicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OTruyenAsyncService {

    private static final Logger log = LoggerFactory.getLogger(OTruyenAsyncService.class);

    private final ComicRepository comicRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterPremiumPolicyService chapterPremiumPolicyService;
    private final RestTemplate restTemplate;

    public OTruyenAsyncService(ComicRepository comicRepository,
            ChapterRepository chapterRepository,
            ChapterPremiumPolicyService chapterPremiumPolicyService) {
        this.comicRepository = comicRepository;
        this.chapterRepository = chapterRepository;
        this.chapterPremiumPolicyService = chapterPremiumPolicyService;
        this.restTemplate = new RestTemplate();
    }

    @Async("taskExecutor")
    @Transactional
    public void lazyLoadComicDetails(Long comicId, String slug) {
        log.info("Async: lazy loading details for comic {} (id={})", slug, comicId);
        try {
            String detailUrl = "https://otruyenapi.com/v1/api/truyen-tranh/" + slug;
            OTruyenDetailResponseDTO detailResponse = restTemplate.getForObject(detailUrl,
                    OTruyenDetailResponseDTO.class);

            if (detailResponse == null || detailResponse.getData() == null
                    || detailResponse.getData().getItem() == null) {
                log.warn("No detail data for slug {}", slug);
                return;
            }

            OTruyenDetailResponseDTO.Item detailItem = detailResponse.getData().getItem();

            ComicEntity comic = comicRepository.findById(comicId).orElse(null);
            if (comic == null) {
                log.warn("Comic id {} not found in DB during lazy load", comicId);
                return;
            }

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

            if ((comic.getCoverUrl() == null || comic.getCoverUrl().isEmpty())
                    && detailResponse.getData().getAppDomainCdnImage() != null
                    && detailItem.getThumb_url() != null) {
                comic.setCoverUrl(detailResponse.getData().getAppDomainCdnImage() + "/uploads/comics/"
                        + detailItem.getThumb_url());
            }

            comic.setUpdatedAt(LocalDateTime.now());
            comicRepository.save(comic);

            // import chapters
            List<OTruyenDetailResponseDTO.ChapterItem> chapters = detailItem.getChapters();
            if (chapters == null || chapters.isEmpty())
                return;

            List<OTruyenDetailResponseDTO.ServerData> serverData = chapters.get(0).getServer_data();
            if (serverData == null || serverData.isEmpty())
                return;

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
                chapter.setLanguage("vi");
                chapter.setOtruyenApiData(cData.getChapter_api_data());
                chapter.setPremium(false);
                chapter.setPrice(0);
                chapter.setCreatedAt(LocalDateTime.now());
                chapter.setUpdatedAt(LocalDateTime.now());

                chapterRepository.save(chapter);
                count++;
            }

            if (count > 0) {
                chapterPremiumPolicyService.applyLastThreePremiumPolicy(comic.getId());
            }

            log.info("Lazy loaded {} chapters for comic {}", count, comic.getTitle());

        } catch (Exception e) {
            log.error("Failed lazy load for slug {}", slug, e);
        }
    }
}
