package com.group09.ComicReader.config;

import com.group09.ComicReader.comic.service.OTruyenService;
import com.group09.ComicReader.comic.repository.ComicRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import java.util.concurrent.Executor;
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
    // allow disabling automatic startup sync
    private final boolean startupSyncEnabled;
    private final Executor taskExecutor;

    public DatabaseInitializer(OTruyenService oTruyenService, ComicRepository comicRepository,
            @Qualifier("taskExecutor") Executor taskExecutor,
            @Value("${otruyen.crawl.startup-enabled:true}") boolean startupSyncEnabled) {
        this.oTruyenService = oTruyenService;
        this.comicRepository = comicRepository;
        this.taskExecutor = taskExecutor;
        this.startupSyncEnabled = startupSyncEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        long comicCount = comicRepository.count();
        if (comicCount == 0 && startupSyncEnabled) {
            log.info("No comics found post-initialization. Triggering background OTruyen sync (non-blocking)...");
            // Run in background so the application can serve requests while the crawl runs
            taskExecutor.execute(() -> {
                try {
                    oTruyenService.syncComicsFromOTruyen();
                    log.info("Automatic startup OTruyen sync (background) completed.");
                } catch (Exception e) {
                    log.error("Failed to auto-sync comics during startup", e);
                }
            });
        } else {
            log.info(
                    "Database already populated with {} comics or startup sync disabled. Skipping automatic OTruyen synchronization.",
                    comicCount);
        }
    }
}
