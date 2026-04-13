package com.group09.ComicReader.config;

import com.group09.ComicReader.comic.service.OTruyenService;
import com.group09.ComicReader.comic.repository.ComicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final OTruyenService oTruyenService;
    private final ComicRepository comicRepository;

    public DatabaseInitializer(OTruyenService oTruyenService, ComicRepository comicRepository) {
        this.oTruyenService = oTruyenService;
        this.comicRepository = comicRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        long comicCount = comicRepository.count();
        if (comicCount == 0) {
            log.info("No comics found post-initialization. Triggering automatic OTruyen synchronization...");
            new Thread(() -> {
                try {
                    oTruyenService.syncComicsFromOTruyen();
                } catch (Exception e) {
                    log.error("Failed to auto-sync comics", e);
                }
            }).start();
        } else {
            log.info("Database already populated with {} comics. Skipping automatic OTruyen synchronization.",
                    comicCount);
        }
    }
}
