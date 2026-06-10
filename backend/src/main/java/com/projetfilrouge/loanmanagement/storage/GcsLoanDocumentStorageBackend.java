package com.projetfilrouge.loanmanagement.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.projetfilrouge.loanmanagement.service.LoanDocumentStorageService;
import com.projetfilrouge.loanmanagement.web.exception.LoanStorageException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;

import java.util.Set;

public class GcsLoanDocumentStorageBackend implements LoanDocumentStorageBackend {

    private static final String LOAN_DIRECTORY_PREFIX = "loan-";

    private final Storage storage;
    private final String bucket;
    private final String prefix;

    public GcsLoanDocumentStorageBackend(Storage storage, String bucket, String prefix) {
        this.storage = storage;
        this.bucket = bucket;
        this.prefix = normalizePrefix(prefix);
    }

    @Override
    public LoanDocumentStorageService.StoredUpload storeUpload(
            Long loanId,
            String storedFileName,
            String sanitizedOriginal,
            String contentType,
            byte[] content
    ) {
        String objectName = objectName(loanId, storedFileName);
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectName)
                    .setContentType(contentType)
                    .build();
            storage.create(blobInfo, content);
            return new LoanDocumentStorageService.StoredUpload(
                    storedFileName,
                    objectName,
                    sanitizedOriginal,
                    contentType,
                    content.length
            );
        } catch (RuntimeException e) {
            throw new LoanStorageException("Impossible d'enregistrer le fichier sur le stockage cloud", e);
        }
    }

    @Override
    public LoanDocumentStorageService.DownloadedFile readFile(
            String storagePath,
            String originalFileName,
            String contentType
    ) {
        Blob blob = storage.get(BlobId.of(bucket, storagePath));
        if (blob == null || !blob.exists()) {
            throw new ResourceNotFoundException("Le fichier physique du document est introuvable.");
        }
        String resolvedContentType = contentType == null ? "application/octet-stream" : contentType;
        return new LoanDocumentStorageService.DownloadedFile(originalFileName, resolvedContentType, blob.getContent());
    }

    @Override
    public void deleteFile(String storagePath) {
        try {
            storage.delete(BlobId.of(bucket, storagePath));
        } catch (RuntimeException e) {
            throw new LoanStorageException("Impossible de supprimer le fichier stocké", e);
        }
    }

    @Override
    public void deleteLoanStorageDirectory(Long loanId) {
        String loanPrefix = loanPrefix(loanId);
        try {
            storage.list(bucket, Storage.BlobListOption.prefix(loanPrefix))
                    .streamAll()
                    .map(Blob::getBlobId)
                    .forEach(storage::delete);
        } catch (RuntimeException e) {
            throw new LoanStorageException("Impossible de supprimer le dossier de stockage cloud", e);
        }
    }

    @Override
    public void cleanupLoanDirectoryOrphans(Long loanId, Set<String> referencedStoredFileNames) {
        String loanPrefix = loanPrefix(loanId);
        try {
            storage.list(bucket, Storage.BlobListOption.prefix(loanPrefix))
                    .streamAll()
                    .filter(blob -> !referencedStoredFileNames.contains(fileNameFromObject(blob.getName())))
                    .map(Blob::getBlobId)
                    .forEach(storage::delete);
        } catch (RuntimeException e) {
            throw new LoanStorageException("Impossible de nettoyer les fichiers orphelins", e);
        }
    }

    private String objectName(Long loanId, String storedFileName) {
        return loanPrefix(loanId) + storedFileName;
    }

    private String loanPrefix(Long loanId) {
        return prefix + "/" + LOAN_DIRECTORY_PREFIX + loanId + "/";
    }

    private static String fileNameFromObject(String objectName) {
        int lastSlash = objectName.lastIndexOf('/');
        return lastSlash >= 0 ? objectName.substring(lastSlash + 1) : objectName;
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "loan-documents";
        }
        String trimmed = prefix.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
