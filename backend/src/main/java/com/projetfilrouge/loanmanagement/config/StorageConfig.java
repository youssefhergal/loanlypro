package com.projetfilrouge.loanmanagement.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.projetfilrouge.loanmanagement.storage.GcsLoanDocumentStorageBackend;
import com.projetfilrouge.loanmanagement.storage.LocalLoanDocumentStorageBackend;
import com.projetfilrouge.loanmanagement.storage.LoanDocumentStorageBackend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
    LoanDocumentStorageBackend localLoanDocumentStorageBackend(
            @Value("${app.storage.loan-documents-dir:uploads/loan-documents}") String loanDocumentsDir
    ) {
        return new LocalLoanDocumentStorageBackend(loanDocumentsDir);
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "gcs")
    Storage googleCloudStorage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "gcs")
    LoanDocumentStorageBackend gcsLoanDocumentStorageBackend(
            Storage googleCloudStorage,
            @Value("${app.storage.gcs-bucket}") String bucket,
            @Value("${app.storage.gcs-prefix:loan-documents}") String prefix
    ) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("Le bucket GCS est obligatoire lorsque app.storage.type=gcs.");
        }
        return new GcsLoanDocumentStorageBackend(googleCloudStorage, bucket, prefix);
    }
}
