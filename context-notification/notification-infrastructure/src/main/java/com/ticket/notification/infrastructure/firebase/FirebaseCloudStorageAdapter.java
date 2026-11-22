package com.ticket.notification.infrastructure.firebase;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;
import com.ticket.shared.port.CloudStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseCloudStorageAdapter implements CloudStoragePort {

    private final FirebaseProperties properties;

    @Override
    public String uploadFile(InputStream inputStream, String originalFilename, String contentType, String folder) {
        if (!properties.isEnabled() || FirebaseApp.getApps().isEmpty()) {
            String mockUrl = "https://mock-storage.ticketbooking.local/" + folder + "/" + UUID.randomUUID() + "-" + originalFilename;
            log.info("[MOCK FIREBASE STORAGE] Firebase disabled. Generated mock URL: {}", mockUrl);
            return mockUrl;
        }

        try {
            Bucket bucket = StorageClient.getInstance().bucket();
            if (bucket == null) {
                throw new IllegalStateException("Firebase Storage bucket is not configured");
            }

            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String sanitizedFolder = (folder != null && !folder.trim().isEmpty()) ? folder.trim() : "uploads";
            String blobName = sanitizedFolder + "/" + UUID.randomUUID() + extension;

            String mimeType = (contentType != null && !contentType.trim().isEmpty()) ? contentType : "application/octet-stream";

            Blob blob = bucket.create(blobName, inputStream, mimeType);

            // Tạo link truy cập công khai dạng Firebase Storage download URL
            String encodedBlobName = URLEncoder.encode(blobName, StandardCharsets.UTF_8).replace("+", "%20");
            String publicUrl = String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                    bucket.getName(), encodedBlobName);

            log.info("[FIREBASE STORAGE UPLOAD] Uploaded {} to bucket {} -> {}", blobName, bucket.getName(), publicUrl);
            return publicUrl;
        } catch (Exception ex) {
            log.error("[FIREBASE STORAGE ERROR] Failed to upload file: {}", originalFilename, ex);
            throw new RuntimeException("Could not upload file to Firebase Storage: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        if (!properties.isEnabled() || FirebaseApp.getApps().isEmpty() || fileUrl == null) {
            return false;
        }

        try {
            Bucket bucket = StorageClient.getInstance().bucket();
            if (bucket == null) return false;

            // Trích xuất blobName từ url: https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{blobName}?alt=media
            if (fileUrl.contains("/o/")) {
                String encodedPart = fileUrl.substring(fileUrl.indexOf("/o/") + 3);
                if (encodedPart.contains("?")) {
                    encodedPart = encodedPart.substring(0, encodedPart.indexOf("?"));
                }
                String blobName = java.net.URLDecoder.decode(encodedPart, StandardCharsets.UTF_8);
                Blob blob = bucket.get(blobName);
                if (blob != null) {
                    return blob.delete();
                }
            }
            return false;
        } catch (Exception ex) {
            log.error("[FIREBASE STORAGE DELETE ERROR] Failed to delete file: {}", fileUrl, ex);
            return false;
        }
    }
}
