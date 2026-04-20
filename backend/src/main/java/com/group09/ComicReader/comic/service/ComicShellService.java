package com.group09.ComicReader.comic.service;

import com.group09.ComicReader.comic.dto.OTruyenResponseDTO;
import com.group09.ComicReader.comic.entity.ComicEntity;
import com.group09.ComicReader.comic.repository.ComicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class ComicShellService {

    private final ComicRepository comicRepository;

    public ComicShellService(ComicRepository comicRepository) {
        this.comicRepository = comicRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ComicEntity saveShell(OTruyenResponseDTO.Item item, String imageDomain) {
        ComicEntity comic = new ComicEntity();
        comic.setTitle(item.getName() != null ? item.getName() : item.getSlug());
        comic.setSlug(item.getSlug());
        comic.setAuthor("Unknown");

        if (imageDomain != null && item.getThumbUrl() != null) {
            comic.setCoverUrl(imageDomain + "/uploads/comics/" + item.getThumbUrl());
        }

        comic.setStatus(item.getStatus() != null ? item.getStatus() : "ongoing");

        if (item.getCategory() != null && !item.getCategory().isEmpty()) {
            String genres = item.getCategory().stream()
                    .map(OTruyenResponseDTO.Category::getName)
                    .collect(Collectors.joining(","));
            comic.setGenres(genres);
        }

        comic.setCreatedAt(LocalDateTime.now());
        comic.setUpdatedAt(LocalDateTime.now());

        ComicEntity saved = comicRepository.saveAndFlush(comic);
        return saved;
    }
}