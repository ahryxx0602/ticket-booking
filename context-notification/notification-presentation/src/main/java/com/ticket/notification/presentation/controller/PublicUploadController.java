package com.ticket.notification.presentation.controller;

import com.ticket.shared.dto.ApiResponse;
import com.ticket.shared.port.CloudStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/public/upload")
@RequiredArgsConstructor
public class PublicUploadController {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final CloudStoragePort cloudStoragePort;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_FILE", "File upload cannot be empty"));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("FILE_TOO_LARGE", "File size exceeds the 10MB limit"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("UNSUPPORTED_MEDIA_TYPE",
                            "Only image files (JPEG, PNG, WebP, GIF) are allowed"));
        }

        try {
            String publicUrl = cloudStoragePort.uploadFile(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    contentType,
                    folder
            );

            return ResponseEntity.ok(ApiResponse.ok("Upload successful", Map.of(
                    "url", publicUrl,
                    "filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "image",
                    "contentType", contentType
            )));
        } catch (Exception ex) {
            log.error("[PUBLIC UPLOAD ERROR] Failed to upload file", ex);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("UPLOAD_FAILED", "Failed to upload file: " + ex.getMessage()));
        }
    }
}
