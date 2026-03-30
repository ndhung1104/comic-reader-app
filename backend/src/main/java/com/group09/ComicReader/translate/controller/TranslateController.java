package com.group09.ComicReader.translate.controller;

import com.group09.ComicReader.translate.dto.ComicTranslateResponse;
import com.group09.ComicReader.translate.dto.TranslateRequest;
import com.group09.ComicReader.translate.dto.TranslateResponse;
import com.group09.ComicReader.translate.service.TranslateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TranslateController {

    private final TranslateService translateService;

    public TranslateController(TranslateService translateService) {
        this.translateService = translateService;
    }

    @PostMapping("/translate")
    public ResponseEntity<TranslateResponse> translate(@RequestBody TranslateRequest request) {
        TranslateResponse response = translateService.translate(
                request.getText(),
                request.getSourceLang(),
                request.getTargetLang()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/comics/{comicId}/translate")
    public ResponseEntity<ComicTranslateResponse> translateComic(
            @PathVariable Long comicId,
            @RequestParam(defaultValue = "vi") String targetLang) {
        ComicTranslateResponse response = translateService.translateComic(comicId, targetLang);
        return ResponseEntity.ok(response);
    }
}
