package com.projetfilrouge.loanmanagement.service.documents;

import com.projetfilrouge.loanmanagement.entity.IssuedDocumentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Génération PDF — US-6.2 (documents crédit) et US-6.3 (échéancier / prélèvements).
 *
 * <p>Le générateur est un <b>pur moteur de rendu</b> : il reçoit des modèles déjà
 * préparés (données + contrôle d'accès faits dans les services) et renvoie des
 * octets PDF. Aucune dépendance externe (voir {@link SimplePdfDocument}).</p>
 */
public interface LoanDocumentPdfGenerator {

    byte[] generateCreditDocument(CreditDocumentContent content);

    byte[] generateSchedule(SchedulePdfContent content);

    byte[] generatePayments(PaymentsPdfContent content);

    /** Ligne libellé / valeur affichée dans l'entête d'un document. */
    record KeyValueLine(String label, String value) {
    }

    /** Contenu d'un document crédit (récap, offre, contrat, mandat SEPA). */
    record CreditDocumentContent(
            IssuedDocumentType type,
            String documentTitle,
            String reference,
            String clientFullName,
            Instant issuedAt,
            List<KeyValueLine> details,
            String bodyParagraph
    ) {
    }

    /** Une ligne du tableau d'échéancier. */
    record ScheduleRow(
            int sequence,
            LocalDate dueDate,
            BigDecimal amountDue,
            BigDecimal principalPart,
            BigDecimal interestPart,
            BigDecimal remainingBalance,
            String status
    ) {
    }

    /** Contenu de l'export échéancier. */
    record SchedulePdfContent(
            String reference,
            String clientFullName,
            Instant exportedAt,
            List<KeyValueLine> summary,
            List<ScheduleRow> rows
    ) {
    }

    /** Une ligne du relevé de prélèvements. */
    record PaymentRow(
            Instant attemptedAt,
            int sequence,
            int attempt,
            BigDecimal amount,
            String status,
            String failureReason
    ) {
    }

    /** Contenu de l'export relevé de prélèvements. */
    record PaymentsPdfContent(
            String reference,
            String clientFullName,
            Instant exportedAt,
            List<PaymentRow> rows
    ) {
    }
}
