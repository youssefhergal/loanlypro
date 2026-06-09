package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.LoanStorageException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class LoanDocumentStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final String LOAN_DIRECTORY_PREFIX = "loan-";

    private final String loanDocumentsDir;

    public LoanDocumentStorageService(
            @Value("${app.storage.loan-documents-dir:uploads/loan-documents}") String loanDocumentsDir
    ) {
        this.loanDocumentsDir = loanDocumentsDir;
    }

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
        Path destination = resolveLoanDirectory(loanId).resolve(storedFileName);
        writeFile(file, destination);
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        return new StoredUpload(
                storedFileName,
                destination.toString(),
                sanitizedOriginal,
                contentType,
                file.getSize()
        );
    }

    public DownloadedFile readFile(String storagePath, String originalFileName, String contentType) {
        Path filePath = Paths.get(storagePath);
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Le fichier physique du document est introuvable.");
        }
        try {
            byte[] content = Files.readAllBytes(filePath);
            String resolvedContentType = contentType == null ? "application/octet-stream" : contentType;
            return new DownloadedFile(originalFileName, resolvedContentType, content);
        } catch (IOException e) {
            throw new LoanStorageException("Impossible de lire le fichier document", e);
        }
    }

    public void deleteFile(String storagePath) {
        try {
            Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException e) {
            throw new LoanStorageException("Impossible de supprimer le fichier stocké", e);
        }
    }

    /**
     * Supprime le répertoire du dossier et tout son contenu (best-effort).
     */
    public void deleteLoanStorageDirectory(Long loanId) {
        Path loanDir = Paths.get(loanDocumentsDir).resolve(LOAN_DIRECTORY_PREFIX + loanId);
        if (!Files.isDirectory(loanDir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(loanDir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // best-effort
                        }
                    });
        } catch (IOException ignored) {
            // best-effort
        }
    }

    public void cleanupLoanDirectoryOrphans(Long loanId, java.util.Set<String> referencedStoredFileNames) {
        Path loanDir = Paths.get(loanDocumentsDir).resolve(LOAN_DIRECTORY_PREFIX + loanId);
        if (!Files.isDirectory(loanDir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(loanDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> !referencedStoredFileNames.contains(path.getFileName().toString()))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // best-effort
                        }
                    });
        } catch (IOException ignored) {
            // best-effort
        }
    }

    private Path resolveLoanDirectory(Long loanId) {
        Path dir = Paths.get(loanDocumentsDir).resolve(LOAN_DIRECTORY_PREFIX + loanId);
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new LoanStorageException("Impossible de créer le dossier de stockage", e);
        }
    }

    private void writeFile(MultipartFile file, Path destination) {
        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new LoanStorageException("Impossible d'enregistrer le fichier", e);
        }
    }

    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "document";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
