package com.projetfilrouge.loanmanagement.storage;

import com.projetfilrouge.loanmanagement.service.LoanDocumentStorageService;

import java.util.Set;

public interface LoanDocumentStorageBackend {

    LoanDocumentStorageService.StoredUpload storeUpload(
            Long loanId,
            String storedFileName,
            String sanitizedOriginal,
            String contentType,
            byte[] content
    );

    LoanDocumentStorageService.DownloadedFile readFile(
            String storagePath,
            String originalFileName,
            String contentType
    );

    void deleteFile(String storagePath);

    void deleteLoanStorageDirectory(Long loanId);

    void cleanupLoanDirectoryOrphans(Long loanId, Set<String> referencedStoredFileNames);
}
