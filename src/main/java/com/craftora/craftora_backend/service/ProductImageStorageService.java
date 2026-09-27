package com.craftora.craftora_backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductImageStorageService {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
    private final Path uploadDirectory = Paths.get(
            System.getenv().getOrDefault("CRAFTORA_UPLOAD_DIR", "uploads"))
            .toAbsolutePath().normalize();

    public String store(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a product image.");
        }
        if (image.getSize() > 5L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Product images must be 5 MB or smaller.");
        }
        String extension = EXTENSIONS.get(image.getContentType());
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a JPEG, PNG, or WebP image.");
        }
        Files.createDirectories(uploadDirectory);
        String filename = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(filename).normalize();
        if (!destination.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image filename.");
        }
        try (InputStream input = image.getInputStream()) {
            Files.copy(input, destination);
        }
        return "/uploads/" + filename;
    }

    public void delete(String imageUrl) throws IOException {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) return;
        String filename = imageUrl.substring("/uploads/".length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\")) return;
        Path file = uploadDirectory.resolve(filename).normalize();
        if (file.startsWith(uploadDirectory)) Files.deleteIfExists(file);
    }
}
