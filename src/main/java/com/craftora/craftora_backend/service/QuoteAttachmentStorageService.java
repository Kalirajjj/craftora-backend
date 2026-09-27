package com.craftora.craftora_backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuoteAttachmentStorageService {
    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".stl", ".obj", ".step", ".stp", ".3mf", ".png", ".jpg", ".jpeg");
    private final Path directory = Paths.get(System.getenv().getOrDefault("CRAFTORA_QUOTE_UPLOAD_DIR", "quote-attachments"))
            .toAbsolutePath().normalize();

    public StoredAttachment store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Attachment must be 25 MB or smaller.");
        }
        String originalName = Path.of(file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename())
                .getFileName().toString();
        int dot = originalName.lastIndexOf('.');
        String extension = dot >= 0 ? originalName.substring(dot).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attach an STL, OBJ, STEP, 3MF, PNG, or JPG file.");
        }
        Files.createDirectories(directory);
        String storageName = UUID.randomUUID() + extension;
        Path destination = directory.resolve(storageName).normalize();
        if (!destination.startsWith(directory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attachment filename.");
        }
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination);
        }
        return new StoredAttachment(originalName, storageName);
    }

    public Path resolve(String storageName) {
        Path file = directory.resolve(storageName).normalize();
        if (!file.startsWith(directory)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attachment path.");
        return file;
    }

    public record StoredAttachment(String originalName, String storageName) {}
}
