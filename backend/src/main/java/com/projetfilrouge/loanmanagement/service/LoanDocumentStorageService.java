package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
public class LoanDocumentStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

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

    public record DownloadedFile(String fileName, String contentType, byte[] content) {}

    public void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Le fichier est obligatoire.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleException("Le fichier dépasse la limite de 10 Mo.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        boolean allowed = contentType.equals("application/pdf")
                || contentType.equals("image/jpeg")
                || contentType.equals("image/png");
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
        return new StoredUpload(
                storedFileName,
                destination.toString(),
                sanitizedOriginal,
                file.getContentType(),
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
            throw new RuntimeException("Impossible de lire le fichier document", e);
        }
    }

    public void deleteFile(String storagePath) {
        try {
            Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException e) {
            throw new RuntimeException("Impossible de supprimer le fichier stocké", e);
        }
    }

    /**
     * Supprime le répertoire du dossier et tout son contenu (best-effort).
     */
    public void deleteLoanStorageDirectory(Long loanId) {
        Path loanDir = Paths.get(loanDocumentsDir).resolve("loan-" + loanId);
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
        Path loanDir = Paths.get(loanDocumentsDir).resolve("loan-" + loanId);
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
        Path dir = Paths.get(loanDocumentsDir).resolve("loan-" + loanId);
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier de stockage", e);
        }
    }

    private void writeFile(MultipartFile file, Path destination) {
        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'enregistrer le fichier", e);
        }
    }

    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "document";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
