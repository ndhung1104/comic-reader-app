package com.group09.ComicReader.common.error;

import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ErrorParserTest {

    @Test
    public void parseHttpError_tokenExpiredFor401And403() {
        ErrorParser.ParsedError unauthorized = ErrorParser.parseHttpError(401, null, "fallback");
        ErrorParser.ParsedError forbidden = ErrorParser.parseHttpError(403, null, "fallback");

        assertEquals(AppError.TOKEN_EXPIRED, unauthorized.getError());
        assertEquals(AppError.TOKEN_EXPIRED, forbidden.getError());
        assertTrue(ErrorParser.isTokenExpiredMessage(unauthorized.getMessage()));
        assertTrue(ErrorParser.isTokenExpiredMessage(forbidden.getMessage()));
    }

    @Test
    public void parseHttpError_prefersMessageInJsonBody() {
        String body = "{\"error\":\"Wallet balance is not enough\"}";

        ErrorParser.ParsedError parsed = ErrorParser.parseHttpError(400, body, "fallback");

        assertEquals(AppError.BAD_REQUEST, parsed.getError());
        assertEquals("Wallet balance is not enough", parsed.getMessage());
    }

    @Test
    public void parseThrowable_networkMappedToNetworkError() {
        ErrorParser.ParsedError parsed = ErrorParser.parseThrowable(new ConnectException("refused"), "fallback");

        assertEquals(AppError.NETWORK, parsed.getError());
        assertEquals("refused", parsed.getMessage());
    }

    @Test
    public void parseThrowable_fallbackMessageUsedWhenThrowableMessageMissing() {
        ErrorParser.ParsedError parsed = ErrorParser.parseThrowable(new IOException(), "fallback");

        assertEquals(AppError.NETWORK, parsed.getError());
        assertEquals("fallback", parsed.getMessage());
    }
}
