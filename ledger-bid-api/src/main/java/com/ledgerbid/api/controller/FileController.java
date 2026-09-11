package com.ledgerbid.api.controller;

import com.ledgerbid.api.config.WebConfig;
import com.ledgerbid.api.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FileController {
    private final FileStorageService files;

    public FileController(FileStorageService files) {
        this.files = files;
    }

    @PostMapping("/uploads")
    public Map<String, String> upload(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        WebConfig.admin(request);
        return Map.of("url", files.store(file));
    }

    @GetMapping("/files/{name}")
    public ResponseEntity<Resource> file(@PathVariable String name) {
        Path path = files.resolve(name);
        String lower = name.toLowerCase();
        MediaType type = MediaType.IMAGE_JPEG;
        if (lower.endsWith(".png")) {
            type = MediaType.IMAGE_PNG;
        } else if (lower.endsWith(".webp")) {
            type = MediaType.parseMediaType("image/webp");
        } else if (lower.endsWith(".gif")) {
            type = MediaType.IMAGE_GIF;
        }
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(path));
    }
}
