package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanDocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    private LoanDocumentStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LoanDocumentStorageService(tempDir.toString());
    }

    @Test
    void validateUpload_throwsWhenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> storageService.validateUpload(file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Le fichier est obligatoire.");
    }

    @Test
    void validateUpload_throwsWhenFileTooLarge() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "big.pdf",
                "application/pdf",
                new byte[(int) (10L * 1024 * 1024) + 1]
        );

        assertThatThrownBy(() -> storageService.validateUpload(file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Le fichier dépasse la limite de 10 Mo.");
    }

    @Test
    void validateUpload_throwsWhenContentTypeNotAllowed() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.txt",
                "text/plain",
                "hello".getBytes()
        );

        assertThatThrownBy(() -> storageService.validateUpload(file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Type de fichier non autorisé. Formats acceptés : PDF, JPG, PNG.");
    }

    @Test
    void storeUpload_persistsPdfAndReturnsMetadata() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "identity.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );

        LoanDocumentStorageService.StoredUpload stored = storageService.storeUpload(42L, file);

        assertThat(stored.originalFileName()).isEqualTo("identity.pdf");
        assertThat(stored.contentType()).isEqualTo("application/pdf");
        assertThat(stored.fileSizeBytes()).isEqualTo(file.getSize());
        assertThat(Files.exists(Path.of(stored.storagePath()))).isTrue();
        assertThat(Files.readString(Path.of(stored.storagePath()))).isEqualTo("pdf-content");
    }

    @Test
    void readFile_returnsStoredContent() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scan.png",
                "image/png",
                new byte[] {1, 2, 3}
        );
        LoanDocumentStorageService.StoredUpload stored = storageService.storeUpload(7L, file);

        LoanDocumentStorageService.DownloadedFile downloaded = storageService.readFile(
                stored.storagePath(),
                stored.originalFileName(),
                stored.contentType()
        );

        assertThat(downloaded.fileName()).isEqualTo("scan.png");
        assertThat(downloaded.contentType()).isEqualTo("image/png");
        assertThat(downloaded.content()).containsExactly(1, 2, 3);
    }

    @Test
    void readFile_throwsWhenFileMissing() {
        assertThatThrownBy(() -> storageService.readFile(
                tempDir.resolve("missing.pdf").toString(),
                "missing.pdf",
                "application/pdf"
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Le fichier physique du document est introuvable.");
    }

    @Test
    void deleteFile_removesStoredFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "to-delete.pdf",
                "application/pdf",
                "data".getBytes()
        );
        LoanDocumentStorageService.StoredUpload stored = storageService.storeUpload(9L, file);
        Path path = Path.of(stored.storagePath());
        assertThat(Files.exists(path)).isTrue();

        storageService.deleteFile(stored.storagePath());

        assertThat(Files.exists(path)).isFalse();
    }
}
