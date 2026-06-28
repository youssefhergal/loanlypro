package com.projetfilrouge.loanmanagement.service.documents;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation par défaut du générateur PDF (US-6.2 / US-6.3).
 *
 * <p>S'appuie sur {@link SimplePdfDocument}, un writer PDF maison sans dépendance
 * externe. Chaque appel construit un nouveau document (le writer n'est pas
 * réutilisable).</p>
 */
@Component
public class DefaultLoanDocumentPdfGenerator implements LoanDocumentPdfGenerator {

    private static final String BRAND = "LoanlyFans";

    @Override
    public byte[] generateCreditDocument(CreditDocumentContent content) {
        SimplePdfDocument pdf = new SimplePdfDocument()
                .brandHeader(BRAND, content.documentTitle())
                .keyValue("Référence dossier", content.reference())
                .keyValue("Client", content.clientFullName())
                .keyValue("Date d'émission", DocumentFormat.date(content.issuedAt()));

        if (content.details() != null && !content.details().isEmpty()) {
            pdf.sectionTitle("Détails");
            for (KeyValueLine line : content.details()) {
                pdf.keyValue(line.label(), line.value());
            }
        }

        if (content.bodyParagraph() != null && !content.bodyParagraph().isBlank()) {
            pdf.spacer(8f).paragraph(content.bodyParagraph());
        }

        pdf.footerNote(BRAND + " — document généré automatiquement le "
                + DocumentFormat.dateTime(content.issuedAt()) + ". Document non contractuel.");
        return pdf.build();
    }

    @Override
    public byte[] generateSchedule(SchedulePdfContent content) {
        SimplePdfDocument pdf = new SimplePdfDocument()
                .brandHeader(BRAND, "Échéancier de remboursement")
                .keyValue("Référence prêt", content.reference())
                .keyValue("Client", content.clientFullName())
                .keyValue("Date d'export", DocumentFormat.dateTime(content.exportedAt()));

        if (content.summary() != null) {
            pdf.sectionTitle("Caractéristiques du prêt");
            for (KeyValueLine line : content.summary()) {
                pdf.keyValue(line.label(), line.value());
            }
        }

        pdf.sectionTitle("Échéances");
        List<String> headers = List.of("N°", "Échéance", "Montant", "Capital", "Intérêts", "Restant dû", "Statut");
        float[] weights = {0.6f, 1.4f, 1.2f, 1.2f, 1.0f, 1.4f, 1.2f};
        List<List<String>> rows = new ArrayList<>();
        for (ScheduleRow row : content.rows()) {
            rows.add(List.of(
                    String.valueOf(row.sequence()),
                    DocumentFormat.date(row.dueDate()),
                    DocumentFormat.euro(row.amountDue()),
                    DocumentFormat.euro(row.principalPart()),
                    DocumentFormat.euro(row.interestPart()),
                    DocumentFormat.euro(row.remainingBalance()),
                    row.status() == null ? "" : row.status()
            ));
        }
        pdf.table(headers, rows, weights);

        pdf.footerNote(BRAND + " — échéancier généré automatiquement le "
                + DocumentFormat.dateTime(content.exportedAt()) + ".");
        return pdf.build();
    }

    @Override
    public byte[] generatePayments(PaymentsPdfContent content) {
        SimplePdfDocument pdf = new SimplePdfDocument()
                .brandHeader(BRAND, "Relevé des prélèvements")
                .keyValue("Référence prêt", content.reference())
                .keyValue("Client", content.clientFullName())
                .keyValue("Date d'export", DocumentFormat.dateTime(content.exportedAt()));

        pdf.sectionTitle("Transactions");
        List<String> headers = List.of("Date", "Échéance", "Tentative", "Montant", "Statut", "Motif");
        float[] weights = {1.6f, 1.0f, 1.0f, 1.2f, 1.1f, 2.0f};
        List<List<String>> rows = new ArrayList<>();
        for (PaymentRow row : content.rows()) {
            rows.add(List.of(
                    DocumentFormat.dateTime(row.attemptedAt()),
                    "N°" + row.sequence(),
                    String.valueOf(row.attempt()),
                    DocumentFormat.euro(row.amount()),
                    row.status() == null ? "" : row.status(),
                    row.failureReason() == null ? "" : row.failureReason()
            ));
        }
        if (rows.isEmpty()) {
            pdf.paragraph("Aucune transaction de prélèvement pour ce prêt à ce jour.");
        } else {
            pdf.table(headers, rows, weights);
        }

        pdf.footerNote(BRAND + " — relevé généré automatiquement le "
                + DocumentFormat.dateTime(content.exportedAt()) + ".");
        return pdf.build();
    }
}
