package com.group09.ComicReader.data.remote;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TranslateApi {

    @POST("/api/v1/comics/{comicId}/translate")
    Call<ComicTranslateResponse> translateComic(
            @Path("comicId") long comicId,
            @Query("targetLang") String targetLang);

    @POST("/api/v1/translate")
    Call<TranslateTextResponse> translate(@Body Map<String, String> body);

    /** Response DTO for comic translation */
    class ComicTranslateResponse {
        private Long comicId;
        private String translatedTitle;
        private String translatedSynopsis;
        private String targetLang;

        public Long getComicId() { return comicId; }
        public String getTranslatedTitle() { return translatedTitle; }
        public String getTranslatedSynopsis() { return translatedSynopsis; }
        public String getTargetLang() { return targetLang; }
    }

    /** Response DTO for generic text translation */
    class TranslateTextResponse {
        private String originalText;
        private String translatedText;
        private String sourceLang;
        private String targetLang;

        public String getOriginalText() { return originalText; }
        public String getTranslatedText() { return translatedText; }
        public String getSourceLang() { return sourceLang; }
        public String getTargetLang() { return targetLang; }
    }
}
