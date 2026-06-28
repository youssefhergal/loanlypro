package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.DirectDebitMandate;
import com.projetfilrouge.loanmanagement.entity.IssuedDocument;
import com.projetfilrouge.loanmanagement.entity.IssuedDocumentType;
import com.projetfilrouge.loanmanagement.entity.Loan;
import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.MandateStatus;
import com.projetfilrouge.loanmanagement.entity.RepaymentPlan;
import com.projetfilrouge.loanmanagement.entity.User;
import com.projetfilrouge.loanmanagement.repository.DirectDebitMandateRepository;
import com.projetfilrouge.loanmanagement.repository.IssuedDocumentRepository;
import com.projetfilrouge.loanmanagement.repository.LoanApplicationRepository;
import com.projetfilrouge.loanmanagement.repository.LoanRepository;
import com.projetfilrouge.loanmanagement.repository.RepaymentPlanRepository;
import com.projetfilrouge.loanmanagement.service.documents.DocumentDownload;
import com.projetfilrouge.loanmanagement.service.documents.DocumentFormat;
import com.projetfilrouge.loanmanagement.service.documents.LoanDocumentPdfGenerator;
import com.projetfilrouge.loanmanagement.service.documents.LoanDocumentPdfGenerator.CreditDocumentContent;
import com.projetfilrouge.loanmanagement.service.documents.LoanDocumentPdfGenerator.KeyValueLine;
import com.projetfilrouge.loanmanagement.web.dto.response.CreditDocumentResponseDto;
import com.projetfilrouge.loanmanagement.web.exception.BusinessRuleException;
import com.projetfilrouge.loanmanagement.web.exception.ForbiddenOperationException;
import com.projetfilrouge.loanmanagement.web.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Documents crédit émis par LoanlyFans (onglet « Mon crédit »). US-6.2
 *
 * <p>La disponibilité de chaque type est calculée à partir du statut du dossier /
 * du mandat. Les PDF sont générés à la demande (pas de stockage). Une éventuelle
 * entrée {@link IssuedDocument} (seed démo US-6.4) sert uniquement à dater le
 * document ; son absence ne bloque pas la génération.</p>
 */
@Service
@RequiredArgsConstructor
public class IssuedDocumentService {

    private static final List<IssuedDocumentType> CREDIT_TYPES = List.of(
            IssuedDocumentType.APPLICATION_RECAP,
            IssuedDocumentType.OFFER,
            IssuedDocumentType.LOAN_CONTRACT,
            IssuedDocumentType.SEPA_MANDATE
    );

    private final IssuedDocumentRepository issuedDocumentRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanRepository loanRepository;
    private final RepaymentPlanRepository repaymentPlanRepository;
    private final DirectDebitMandateRepository mandateRepository;
    private final LoanDocumentPdfGenerator pdfGenerator;

    @Transactional(readOnly = true)
    public List<CreditDocumentResponseDto> listCreditDocumentsForClient(String clientEmail) {
        List<LoanApplication> applications = loanApplicationRepository.findByApplicantEmail(clientEmail);

        Map<String, IssuedDocument> issuedIndex = issuedDocumentRepository
                .findByLoanApplication_Applicant_EmailOrderByIssuedAtDesc(clientEmail).stream()
                .filter(doc -> doc.getLoanApplication() != null)
                .collect(Collectors.toMap(
                        doc -> indexKey(doc.getLoanApplication().getId(), doc.getDocumentType()),
                        doc -> doc,
                        (a, b) -> a));

        // Tri par dossier décroissant (le plus récent en premier).
        applications.sort((a, b) -> Long.compare(b.getId(), a.getId()));

        List<CreditDocumentResponseDto> result = new ArrayList<>();
        for (LoanApplication application : applications) {
            Loan loan = loanRepository.findByLoanApplicationId(application.getId()).orElse(null);
            for (IssuedDocumentType type : CREDIT_TYPES) {
                result.add(buildDto(application, loan, type, issuedIndex));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public DocumentDownload downloadCreditDocument(String clientEmail, IssuedDocumentType type, Long referenceId) {
        if (type == IssuedDocumentType.SEPA_MANDATE) {
            return downloadMandate(clientEmail, referenceId);
        }
        return downloadApplicationDocument(clientEmail, type, referenceId);
    }

    /* ----------------------------------------------------------------- listing */

    private CreditDocumentResponseDto buildDto(
            LoanApplication application,
            Loan loan,
            IssuedDocumentType type,
            Map<String, IssuedDocument> issuedIndex
    ) {
        Availability availability = availability(application, loan, type);
        IssuedDocument issued = issuedIndex.get(indexKey(application.getId(), type));
        Instant issuedAt = issued != null ? issued.getIssuedAt() : derivedIssuedAt(application, type);

        boolean isMandate = type == IssuedDocumentType.SEPA_MANDATE;

        return CreditDocumentResponseDto.builder()
                .documentType(type)
                .title(title(type))
                .reference(application.getReference())
                .loanApplicationId(application.getId())
                // Pour le mandat, le téléchargement se fait par loanId ; sinon par loanApplicationId.
                .loanId(isMandate && loan != null ? loan.getId() : null)
                .issuedAt(availability.available() ? issuedAt : null)
                .available(availability.available())
                .unavailableReason(availability.reason())
                .build();
    }

    private Availability availability(LoanApplication application, Loan loan, IssuedDocumentType type) {
        LoanApplicationStatus status = application.getStatus();
        return switch (type) {
            case APPLICATION_RECAP -> status == LoanApplicationStatus.DRAFT
                    ? Availability.denied("Disponible après soumission du dossier")
                    : Availability.granted();
            case OFFER -> (status == LoanApplicationStatus.OFFER_PENDING || status == LoanApplicationStatus.APPROVED)
                    ? Availability.granted()
                    : Availability.denied("Disponible une fois l'offre proposée");
            case LOAN_CONTRACT -> status == LoanApplicationStatus.APPROVED
                    ? Availability.granted()
                    : Availability.denied("Disponible après approbation");
            case SEPA_MANDATE -> hasActiveMandate(loan)
                    ? Availability.granted()
                    : Availability.denied("Disponible après activation du mandat SEPA");
        };
    }

    private boolean hasActiveMandate(Loan loan) {
        return loan != null
                && mandateRepository.findByLoanIdAndStatus(loan.getId(), MandateStatus.ACTIVE).isPresent();
    }

    private Instant derivedIssuedAt(LoanApplication application, IssuedDocumentType type) {
        return switch (type) {
            case APPLICATION_RECAP -> application.getSubmittedAt();
            case OFFER, LOAN_CONTRACT -> application.getDecidedAt();
            case SEPA_MANDATE -> null;
        };
    }

    /* ----------------------------------------------------------------- download */

    private DocumentDownload downloadApplicationDocument(
            String clientEmail, IssuedDocumentType type, Long applicationId
    ) {
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable"));
        assertApplicantOwnership(application, clientEmail);

        Loan loan = loanRepository.findByLoanApplicationId(application.getId()).orElse(null);
        Availability availability = availability(application, loan, type);
        if (!availability.available()) {
            throw new BusinessRuleException("Document non disponible pour ce dossier.");
        }

        CreditDocumentContent content = buildContent(application, loan, null, type);
        byte[] pdf = pdfGenerator.generateCreditDocument(content);
        return new DocumentDownload(fileName(type, application.getReference()), "application/pdf", pdf);
    }

    private DocumentDownload downloadMandate(String clientEmail, Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Prêt introuvable"));
        if (!loan.getBorrower().getEmail().equalsIgnoreCase(clientEmail)) {
            throw new ForbiddenOperationException("Accès refusé à ce prêt.");
        }
        DirectDebitMandate mandate = mandateRepository
                .findByLoanIdAndStatus(loan.getId(), MandateStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("Aucun mandat SEPA actif pour ce prêt."));

        CreditDocumentContent content = buildContent(
                loan.getLoanApplication(), loan, mandate, IssuedDocumentType.SEPA_MANDATE);
        byte[] pdf = pdfGenerator.generateCreditDocument(content);
        return new DocumentDownload(
                fileName(IssuedDocumentType.SEPA_MANDATE, loan.getLoanApplication().getReference()),
                "application/pdf",
                pdf);
    }

    private void assertApplicantOwnership(LoanApplication application, String clientEmail) {
        if (!application.getApplicant().getEmail().equalsIgnoreCase(clientEmail)) {
            throw new ForbiddenOperationException("Accès refusé à ce dossier.");
        }
    }

    /* -------------------------------------------------------------- pdf content */

    private CreditDocumentContent buildContent(
            LoanApplication application, Loan loan, DirectDebitMandate mandate, IssuedDocumentType type
    ) {
        List<KeyValueLine> details = new ArrayList<>();
        switch (type) {
            case APPLICATION_RECAP -> {
                details.add(new KeyValueLine("Montant demandé", DocumentFormat.euro(application.getRequestedAmount())));
                details.add(new KeyValueLine("Durée demandée", DocumentFormat.months(application.getRequestedDurationMonths())));
                details.add(new KeyValueLine("Objet", application.getPurpose()));
                details.add(new KeyValueLine("Statut du dossier", application.getStatus().name()));
            }
            case OFFER, LOAN_CONTRACT -> {
                details.add(new KeyValueLine("Montant accordé", DocumentFormat.euro(
                        application.getApprovedAmount() != null
                                ? application.getApprovedAmount()
                                : application.getRequestedAmount())));
                details.add(new KeyValueLine("Durée", DocumentFormat.months(
                        application.getApprovedDurationMonths() != null
                                ? application.getApprovedDurationMonths()
                                : application.getRequestedDurationMonths())));
                details.add(new KeyValueLine("Taux annuel", DocumentFormat.percent(application.getInterestRate())));
                monthlyPayment(loan).ifPresent(amount ->
                        details.add(new KeyValueLine("Mensualité", DocumentFormat.euro(amount))));
            }
            case SEPA_MANDATE -> {
                if (mandate != null) {
                    details.add(new KeyValueLine("Titulaire", mandate.getPaymentMethod().getHolderName()));
                    details.add(new KeyValueLine("IBAN", mandate.getPaymentMethod().getIbanMasked()));
                    details.add(new KeyValueLine("Statut du mandat", mandate.getStatus().name()));
                }
                details.add(new KeyValueLine("Référence prêt", application.getReference()));
            }
        }

        User applicant = application.getApplicant();
        return new CreditDocumentContent(
                type,
                title(type),
                application.getReference(),
                applicant.getFirstName() + " " + applicant.getLastName(),
                Instant.now(),
                details,
                bodyParagraph(type));
    }

    private Optional<java.math.BigDecimal> monthlyPayment(Loan loan) {
        if (loan == null) {
            return Optional.empty();
        }
        return repaymentPlanRepository.findByLoanId(loan.getId()).map(RepaymentPlan::getMonthlyPayment);
    }

    /* ----------------------------------------------------------------- labels */

    private String title(IssuedDocumentType type) {
        return switch (type) {
            case APPLICATION_RECAP -> "Récapitulatif de la demande";
            case OFFER -> "Offre de prêt";
            case LOAN_CONTRACT -> "Contrat de prêt";
            case SEPA_MANDATE -> "Mandat de prélèvement SEPA";
        };
    }

    private String fileName(IssuedDocumentType type, String reference) {
        String prefix = switch (type) {
            case APPLICATION_RECAP -> "recapitulatif";
            case OFFER -> "offre";
            case LOAN_CONTRACT -> "contrat";
            case SEPA_MANDATE -> "mandat-sepa";
        };
        return prefix + "-" + reference + ".pdf";
    }

    private String bodyParagraph(IssuedDocumentType type) {
        return switch (type) {
            case APPLICATION_RECAP -> "Ce récapitulatif reprend les informations principales de votre demande de prêt "
                    + "telles qu'enregistrées sur la plateforme LoanlyFans.";
            case OFFER -> "Cette offre présente les conditions proposées pour votre prêt. Elle est fournie à titre "
                    + "informatif dans le cadre du projet de démonstration.";
            case LOAN_CONTRACT -> "Ce document récapitule l'engagement contractuel lié à votre prêt. Document non "
                    + "contractuel généré automatiquement.";
            case SEPA_MANDATE -> "Ce mandat autorise LoanlyFans à présenter des prélèvements SEPA sur le compte "
                    + "indiqué, conformément au plan de remboursement de votre prêt.";
        };
    }

    private String indexKey(Long applicationId, IssuedDocumentType type) {
        return applicationId + ":" + type.name();
    }

    private record Availability(boolean available, String reason) {
        static Availability granted() {
            return new Availability(true, null);
        }

        static Availability denied(String reason) {
            return new Availability(false, reason);
        }
    }
}
