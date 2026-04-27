package com.group09.ComicReader.common.controller;

import com.group09.ComicReader.common.service.ExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/report")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ExportService exportService;

    public ReportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/export/revenue")
    public ResponseEntity<String> exportRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        String csv = exportService.exportRevenueCsv(from, to);
        return createCsvResponse(csv, "revenue_report.csv");
    }

    @GetMapping("/export/content")
    public ResponseEntity<String> exportContent(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        String csv = exportService.exportContentCsv(from, to);
        return createCsvResponse(csv, "content_report.csv");
    }

    @GetMapping("/export/users")
    public ResponseEntity<String> exportUsers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        String csv = exportService.exportUserActivityCsv(from, to);
        return createCsvResponse(csv, "user_report.csv");
    }

    private ResponseEntity<String> createCsvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
