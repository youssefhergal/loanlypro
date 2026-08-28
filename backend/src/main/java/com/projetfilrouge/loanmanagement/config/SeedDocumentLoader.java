package com.projetfilrouge.loanmanagement.config;

import com.projetfilrouge.loanmanagement.entity.LoanDocumentType;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Charge les 5 justificatifs déposés dans {@code classpath:seed/documents/}
 * pour les réutiliser lors du seed bulk (prod ou local).
 */
@Component
public class SeedDocumentLoader {

    public record SeedDocument(
            LoanDocumentType type,
            String originalFileName,
            String contentType,
            byte[] content
    ) {}

    private static final Map<LoanDocumentType, String> CLASSPATH_BY_TYPE = Map.of(
            LoanDocumentType.IDENTITY, "seed/documents/Pièce d'identité DEMO.png",
            LoanDocumentType.PAYSLIPS, "seed/documents/Bulletin_Salaire_DEMO_Realiste.pdf",
            LoanDocumentType.TAX_NOTICE, "seed/documents/Avis_Imposition_DEMO_Realiste.pdf",
            LoanDocumentType.BANK_STATEMENTS, "seed/documents/Releve_Bancaire_DEMO.pdf",
            LoanDocumentType.PROOF_OF_ADDRESS, "seed/documents/Justificatif de domicile_DEMO_fake.png"
    );

    private final List<SeedDocument> documents;

    public SeedDocumentLoader() {
        this.documents = CLASSPATH_BY_TYPE.entrySet().stream()
                .map(entry -> load(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<SeedDocument> getAll() {
        return documents;
    }

    private static SeedDocument load(LoanDocumentType type, String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        if (!resource.exists()) {
            throw new BusinessRuleException("Fichier seed introuvable : " + classpathLocation);
        }
        String fileName = classpathLocation.substring(classpathLocation.lastIndexOf('/') + 1);
        String contentType = contentTypeFor(fileName);
        try (InputStream input = resource.getInputStream()) {
            byte[] content = input.readAllBytes();
            return new SeedDocument(type, fileName, contentType, content);
        } catch (IOException e) {
            throw new BusinessRuleException("Impossible de lire le fichier seed : " + classpathLocation);
        }
    }

    private static String contentTypeFor(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}
