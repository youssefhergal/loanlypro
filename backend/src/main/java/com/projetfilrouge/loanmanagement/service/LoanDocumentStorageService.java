package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.storage.LoanDocumentStorageBackend;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LoanDocumentStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private final LoanDocumentStorageBackend storageBackend;

    public record StoredUpload(
            String storedFileName,
            String storagePath,
            String originalFileName,
            String contentType,
            long fileSizeBytes
    ) {}

    public record DownloadedFile(String fileName, String contentType, byte[] content) {
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof DownloadedFile that)) {
                return false;
            }
            return Objects.equals(fileName, that.fileName)
                    && Objects.equals(contentType, that.contentType)
                    && Arrays.equals(content, that.content);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(fileName);
            result = 31 * result + Objects.hashCode(contentType);
            result = 31 * result + Arrays.hashCode(content);
            return result;
        }

        @Override
        public String toString() {
            return "DownloadedFile[fileName=" + fileName
                    + ", contentType=" + contentType
                    + ", content=" + Arrays.toString(content)
                    + "]";
        }
    }

    public void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Le fichier est obligatoire.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleException("Le fichier dépasse la limite de 10 Mo.");
        }
        String contentType = file.getContentType();
        boolean allowed = "application/pdf".equals(contentType)
                || "image/jpeg".equals(contentType)
                || "image/png".equals(contentType);
        if (!allowed) {
            throw new BusinessRuleException("Type de fichier non autorisé. Formats acceptés : PDF, JPG, PNG.");
        }
    }

    public StoredUpload storeUpload(Long loanId, MultipartFile file) {
        validateUpload(file);
        String sanitizedOriginal = sanitize(file.getOriginalFilename());
        String storedFileName = java.util.UUID.randomUUID() + "-" + sanitizedOriginal;
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        byte[] content = readMultipartContent(file);
        return storageBackend.storeUpload(loanId, storedFileName, sanitizedOriginal, contentType, content);
    }

    public DownloadedFile readFile(String storagePath, String originalFileName, String contentType) {
        return storageBackend.readFile(storagePath, originalFileName, contentType);
    }

    public void deleteFile(String storagePath) {
        storageBackend.deleteFile(storagePath);
    }

    public void deleteLoanStorageDirectory(Long loanId) {
        storageBackend.deleteLoanStorageDirectory(loanId);
    }

    public void cleanupLoanDirectoryOrphans(Long loanId, Set<String> referencedStoredFileNames) {
        storageBackend.cleanupLoanDirectoryOrphans(loanId, referencedStoredFileNames);
    }

    private byte[] readMultipartContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessRuleException("Impossible de lire le fichier téléversé.");
        }
    }

    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "document";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
