package com.ticket.notification.presentation.controller;

import com.ticket.shared.dto.ApiResponse;
import com.ticket.shared.port.CloudStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/v1/public/upload")
@RequiredArgsConstructor
public class PublicUploadController {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final Pattern SAFE_FOLDER_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,32}$");

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

        String normalizedFolder = folder != null ? folder.trim() : "";
        if (!SAFE_FOLDER_PATTERN.matcher(normalizedFolder).matches()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_UPLOAD_FOLDER",
                            "Upload folder must be 1-32 alphanumeric, hyphen or underscore characters"));
        }

        String contentType = file.getContentType();
        String normalizedContentType = contentType != null ? contentType.trim().toLowerCase() : "";
        if (!ALLOWED_IMAGE_TYPES.contains(normalizedContentType)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("UNSUPPORTED_MEDIA_TYPE",
                            "Only image files (JPEG, PNG, WebP, GIF) are allowed"));
        }

        try {
            byte[] fileBytes = file.getBytes();
            if (!matchesImageMagicBytes(fileBytes, normalizedContentType)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_IMAGE_CONTENT",
                                "File binary signature does not match declared image content type"));
            }

            String sanitizedFilename = sanitizeDisplayFilename(file.getOriginalFilename(), normalizedContentType);
            String publicUrl = cloudStoragePort.uploadFile(
                    new ByteArrayInputStream(fileBytes),
                    sanitizedFilename,
                    normalizedContentType,
                    normalizedFolder
            );

            return ResponseEntity.ok(ApiResponse.ok("Upload successful", Map.of(
                    "url", publicUrl,
                    "filename", sanitizedFilename,
                    "contentType", normalizedContentType
            )));
        } catch (Exception ex) {
            log.error("[PUBLIC UPLOAD ERROR] Failed to upload file", ex);
            // BE-SEC-008: Không trả về ex.getMessage() ra ngoài để chống lộ đường dẫn/thông tin bucket nội bộ
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("UPLOAD_FAILED", "Failed to upload file due to an internal storage error"));
        }
    }

    private static boolean matchesImageMagicBytes(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        return switch (contentType) {
            case "image/jpeg" -> (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47
                    && bytes[4] == 0x0D
                    && bytes[5] == 0x0A
                    && bytes[6] == 0x1A
                    && bytes[7] == 0x0A;
            case "image/gif" -> bytes.length >= 6
                    && bytes[0] == 'G'
                    && bytes[1] == 'I'
                    && bytes[2] == 'F'
                    && bytes[3] == '8'
                    && (bytes[4] == '7' || bytes[4] == '9')
                    && bytes[5] == 'a';
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R'
                    && bytes[1] == 'I'
                    && bytes[2] == 'F'
                    && bytes[3] == 'F'
                    && bytes[8] == 'W'
                    && bytes[9] == 'E'
                    && bytes[10] == 'B'
                    && bytes[11] == 'P';
            default -> false;
        };
    }

    private static String sanitizeDisplayFilename(String originalFilename, String contentType) {
        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
        if (originalFilename == null || originalFilename.isBlank()) {
            return "image" + ext;
        }
        String base = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (base.contains("..")) {
            base = base.replace("..", "_");
        }
        return base.isBlank() ? ("image" + ext) : base;
    }
}
