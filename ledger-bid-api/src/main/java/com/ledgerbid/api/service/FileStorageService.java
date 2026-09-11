package com.ledgerbid.api.service;

import com.ledgerbid.api.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private final Path root;

    public FileStorageService(@Value("${ledgerbid.upload-dir:uploads}") String dir) throws IOException {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.bad("Choose a photo");
        }
        String original = file.getOriginalFilename() == null ? "photo.jpg" : file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED.contains(ext)) {
            throw ApiException.bad("Use a JPG, PNG, WEBP, or GIF photo");
        }
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Files.copy(file.getInputStream(), root.resolve(name));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save photo");
        }
        return "/api/files/" + name;
    }

    public Path resolve(String name) {
        if (name == null || name.contains("..") || name.contains("/") || name.contains("\\")) {
            throw ApiException.notFound("File not found");
        }
        Path file = root.resolve(name).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw ApiException.notFound("File not found");
        }
        return file;
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "jpg";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
