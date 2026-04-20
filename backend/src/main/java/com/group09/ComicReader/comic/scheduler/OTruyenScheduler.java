package com.group09.ComicReader.comic.scheduler;

import com.group09.ComicReader.comic.service.OTruyenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OTruyenScheduler {

    private static final Logger log = LoggerFactory.getLogger(OTruyenScheduler.class);

    private final OTruyenService oTruyenService;

    public OTruyenScheduler(OTruyenService oTruyenService) {
        this.oTruyenService = oTruyenService;
    }

    /**
     * Weekly crawl job to import all comics from OTruyen.
     * Cron expression is configurable via property 'otruyen.crawl.cron'.
     * Default: run every Sunday at 03:00 UTC ("0 0 3 * * SUN").
     */
    @Async("taskExecutor")
    @Scheduled(cron = "${otruyen.crawl.cron:0 0 3 * * SUN}", zone = "${otruyen.crawl.zone:UTC}")
    public void weeklyCrawl() {
        log.info("Starting scheduled OTruyen weekly crawl");
        try {
            oTruyenService.syncOnlyNewComics();
            log.info("Scheduled OTruyen crawl finished successfully");
        } catch (Exception e) {
            log.error("Scheduled OTruyen crawl failed", e);
        }
    }
}
