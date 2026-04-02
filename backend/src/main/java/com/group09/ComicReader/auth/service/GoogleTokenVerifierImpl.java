package com.group09.ComicReader.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.group09.ComicReader.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierImpl(@Value("${app.auth.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance()
        ).setAudience(Collections.singletonList(this.clientId)).build();
    }

    @Override
    public GoogleUserInfo verify(String idToken) {
        String token = idToken == null ? "" : idToken.trim();
        if (token.isEmpty()) {
            throw new BadRequestException("Missing Google idToken");
        }
        if (clientId.isEmpty()) {
            throw new BadRequestException("Google login is not configured on server");
        }

        try {
            GoogleIdToken googleIdToken = verifier.verify(token);
            if (googleIdToken == null) {
                throw new BadRequestException("Invalid Google token");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            String fullName = payload.get("name") == null ? null : payload.get("name").toString();

            if (email == null || email.trim().isEmpty()) {
                throw new BadRequestException("Google token has no email");
            }
            if (!emailVerified) {
                throw new BadRequestException("Google email is not verified");
            }

            return new GoogleUserInfo(email.trim(), fullName == null ? "" : fullName, true);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Invalid Google token");
        }
    }
}
