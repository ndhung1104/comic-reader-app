package com.group09.ComicReader.comic.service;

import com.group09.ComicReader.comic.dto.OTruyenResponseDTO;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.repository.ComicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OTruyenService {

    private final ComicRepository comicRepository;
    private final RestTemplate restTemplate;

    public OTruyenService(ComicRepository comicRepository) {
        this.comicRepository = comicRepository;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public void syncComicsFromOTruyen() {
        String url = "https://otruyenapi.com/v1/api/home";
        OTruyenResponseDTO response = restTemplate.getForObject(url, OTruyenResponseDTO.class);

        if (response != null && response.getData() != null && response.getData().getItems() != null) {
            String imageDomain = response.getData().getAppDomainCdnImage();
            List<OTruyenResponseDTO.Item> items = response.getData().getItems();

            for (OTruyenResponseDTO.Item item : items) {
                if (comicRepository.findBySlug(item.getSlug()).isEmpty()) {
                    ComicEntity comic = new ComicEntity();
                    comic.setTitle(item.getName());
                    comic.setSlug(item.getSlug());
                    comic.setAuthor("Unknown"); // Setting default as OTruyen home API lacks author

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

                    comic.setSynopsis("Synced from OTruyen API");
                    comic.setCreatedAt(LocalDateTime.now());
                    comic.setUpdatedAt(LocalDateTime.now());

                    comicRepository.save(comic);
                }
            }
        }
    }
}
