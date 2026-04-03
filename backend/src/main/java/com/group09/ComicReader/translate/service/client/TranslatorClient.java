package com.group09.ComicReader.translate.service.client;

public interface TranslatorClient {

    String getProviderKey();

    String getCacheIdentity();

    String translate(String text, String sourceLang, String targetLang);
}
