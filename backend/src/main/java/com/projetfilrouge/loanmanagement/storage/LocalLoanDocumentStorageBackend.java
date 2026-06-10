package com.projetfilrouge.loanmanagement.storage;

import com.projetfilrouge.loanmanagement.service.LoanDocumentStorageService;
import com.projetfilrouge.loanmanagement.web.exception.LoanStorageException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

public class LocalLoanDocumentStorageBackend implements LoanDocumentStorageBackend {

    private static final String LOAN_DIRECTORY_PREFIX = "loan-";

    private final String loanDocumentsDir;

    public LocalLoanDocumentStorageBackend(String loanDocumentsDir) {
        this.loanDocumentsDir = loanDocumentsDir;
    }

    @Override
    public LoanDocumentStorageService.StoredUpload storeUpload(
            Long loanId,
            String storedFileName,
            String sanitizedOriginal,
            String contentType,
            byte[] content
    ) {
        Path destination = resolveLoanDirectory(loanId).resolve(storedFileName);
        writeFile(content, destination);
        return new LoanDocumentStorageService.StoredUpload(
                storedFileName,
                destination.toString(),
                sanitizedOriginal,
                contentType,
                content.length
        );
    }

    @Override
    public LoanDocumentStorageService.DownloadedFile readFile(
            String storagePath,
            String originalFileName,
            String contentType
    ) {
        Path filePath = Paths.get(storagePath);
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Le fichier physique du document est introuvable.");
        }
        try {
            byte[] fileContent = Files.readAllBytes(filePath);
            String resolvedContentType = contentType == null ? "application/octet-stream" : contentType;
            return new LoanDocumentStorageService.DownloadedFile(originalFileName, resolvedContentType, fileContent);
        } catch (IOException e) {
            throw new LoanStorageException("Impossible de lire le fichier document", e);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        try {
            Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException e) {
            throw new LoanStorageException("Impossible de supprimer le fichier stocké", e);
        }
    }

    @Override
    public void deleteLoanStorageDirectory(Long loanId) {
        Path loanDir = loanDirectory(loanId);
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

    @Override
    public void cleanupLoanDirectoryOrphans(Long loanId, Set<String> referencedStoredFileNames) {
        Path loanDir = loanDirectory(loanId);
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

    private Path loanDirectory(Long loanId) {
        return Paths.get(loanDocumentsDir).resolve(LOAN_DIRECTORY_PREFIX + loanId);
    }

    private Path resolveLoanDirectory(Long loanId) {
        Path dir = loanDirectory(loanId);
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new LoanStorageException("Impossible de créer le dossier de stockage", e);
        }
    }

    private void writeFile(byte[] content, Path destination) {
        try {
            Files.write(destination, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new LoanStorageException("Impossible d'enregistrer le fichier", e);
        }
    }
}
