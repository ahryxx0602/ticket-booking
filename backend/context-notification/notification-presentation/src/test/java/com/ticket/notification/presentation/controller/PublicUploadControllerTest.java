package com.ticket.notification.presentation.controller;

import com.ticket.shared.port.CloudStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("BE-SEC-008: PublicUploadController Content Signature, Folder Validation & Error Sanitization Tests")
class PublicUploadControllerTest {

    @Mock
    private CloudStoragePort cloudStoragePort;

    @InjectMocks
    private PublicUploadController controller;

    private MockMvc mockMvc;

    // Minimal valid PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("BE-SEC-008: Valid PNG image with matching magic bytes and safe folder succeeds")
    void uploadImage_WhenValidPngAndFolder_ShouldReturn200() throws Exception {
        MockMultipartFile pngFile = new MockMultipartFile(
                "file", "banner.png", "image/png", VALID_PNG_BYTES
        );
        when(cloudStoragePort.uploadFile(any(), eq("banner.png"), eq("image/png"), eq("banners")))
                .thenReturn("https://mock-storage.ticketbooking.local/banners/uuid.png");

        mockMvc.perform(multipart("/api/v1/public/upload")
                        .file(pngFile)
                        .param("folder", "banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").value("https://mock-storage.ticketbooking.local/banners/uuid.png"));
    }

    @Test
    @DisplayName("BE-SEC-008: Disguised HTML/script file claiming image/png MIME type must be rejected by magic-byte check")
    void uploadImage_WhenContentDoesNotMatchMagicBytes_ShouldReturn400InvalidImageContent() throws Exception {
        MockMultipartFile fakePng = new MockMultipartFile(
                "file", "exploit.png", "image/png", "<script>alert(1)</script>".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/public/upload")
                        .file(fakePng)
                        .param("folder", "general"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMAGE_CONTENT"));

        verify(cloudStoragePort, never()).uploadFile(any(), any(), any(), any());
    }

    @Test
    @DisplayName("BE-SEC-008: Folder parameter with path traversal (../) must be rejected with 400 INVALID_UPLOAD_FOLDER")
    void uploadImage_WhenFolderContainsPathTraversal_ShouldReturn400() throws Exception {
        MockMultipartFile pngFile = new MockMultipartFile(
                "file", "banner.png", "image/png", VALID_PNG_BYTES
        );

        mockMvc.perform(multipart("/api/v1/public/upload")
                        .file(pngFile)
                        .param("folder", "../../private-bucket"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UPLOAD_FOLDER"));

        verify(cloudStoragePort, never()).uploadFile(any(), any(), any(), any());
    }

    @Test
    @DisplayName("BE-SEC-008: Storage adapter exception must return generic error message without leaking internal exception text")
    void uploadImage_WhenStorageThrowsException_ShouldNotLeakInternalErrorMessage() throws Exception {
        MockMultipartFile pngFile = new MockMultipartFile(
                "file", "banner.png", "image/png", VALID_PNG_BYTES
        );
        when(cloudStoragePort.uploadFile(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("SecretBucket:/var/credentials/service-account.json permission denied"));

        mockMvc.perform(multipart("/api/v1/public/upload")
                        .file(pngFile)
                        .param("folder", "banners"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("UPLOAD_FAILED"))
                .andExpect(jsonPath("$.message", not(containsString("SecretBucket"))))
                .andExpect(jsonPath("$.message", not(containsString("service-account.json"))));
    }
}
