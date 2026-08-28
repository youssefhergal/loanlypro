package com.projetfilrouge.loanmanagement.service.documents;

/**
 * Ancien squelette du générateur PDF — remplacé par
 * {@link DefaultLoanDocumentPdfGenerator} (US-6.2 / US-6.3).
 *
 * <p>N'est volontairement plus annoté {@code @Component} : il n'est donc pas
 * enregistré comme bean Spring (un seul bean implémente l'interface). Conservé
 * comme implémentation de repli neutre ; ce fichier peut être supprimé.</p>
 *
 * @deprecated utiliser {@link DefaultLoanDocumentPdfGenerator}.
 */
@Deprecated(forRemoval = true)
public class StubLoanDocumentPdfGenerator implements LoanDocumentPdfGenerator {

    @Override
    public byte[] generateCreditDocument(CreditDocumentContent content) {
        return new SimplePdfDocument()
                .brandHeader("LoanlyPro", content.documentTitle())
                .paragraph("Document indisponible.")
                .build();
    }

    @Override
    public byte[] generateSchedule(SchedulePdfContent content) {
        return new SimplePdfDocument()
                .brandHeader("LoanlyPro", "Échéancier")
                .paragraph("Document indisponible.")
                .build();
    }

    @Override
    public byte[] generatePayments(PaymentsPdfContent content) {
        return new SimplePdfDocument()
                .brandHeader("LoanlyPro", "Relevé des prélèvements")
                .paragraph("Document indisponible.")
                .build();
    }
}
