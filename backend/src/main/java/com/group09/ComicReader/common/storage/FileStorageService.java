package com.group09.ComicReader.common.storage;

import com.group09.ComicReader.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp"
    );

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot initialize upload directory", exception);
        }
    }

    public String storeChapterPage(Long chapterId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        String originalName = file.getOriginalFilename() == null ? "page.jpg" : file.getOriginalFilename();
        String extension = extractExtension(originalName);
        String filename = UUID.randomUUID() + extension;

        Path chapterDir = uploadRoot.resolve("chapter-" + chapterId).normalize();
        Path targetFile = chapterDir.resolve(filename).normalize();

        if (!targetFile.startsWith(chapterDir)) {
            throw new BadRequestException("Invalid file path");
        }

        try {
            Files.createDirectories(chapterDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store file", exception);
        }

        return "/uploads/chapter-" + chapterId + "/" + filename;
    }

    public String storeUserAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }

        String originalName = file.getOriginalFilename() == null ? "avatar.jpg" : file.getOriginalFilename();
        String extension = extractExtension(originalName).toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported image type");
        }

        String filename = UUID.randomUUID() + extension;

        Path userDir = uploadRoot.resolve("avatars").resolve("user-" + userId).normalize();
        Path targetFile = userDir.resolve(filename).normalize();

        if (!targetFile.startsWith(userDir)) {
            throw new BadRequestException("Invalid file path");
        }

        try {
            Files.createDirectories(userDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store file", exception);
        }

        return "/uploads/avatars/user-" + userId + "/" + filename;
    }

    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) {
            return ".jpg";
        }
        return filename.substring(lastDot);
    }
}

