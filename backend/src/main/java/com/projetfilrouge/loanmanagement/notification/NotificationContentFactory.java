package com.projetfilrouge.loanmanagement.notification;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;

public final class NotificationContentFactory {

    private NotificationContentFactory() {
    }

    public static NotificationContent forEvent(LoanApplicationEventType type, LoanApplication loan) {
        return forEvent(type, loan, NotificationAudience.CLIENT);
    }

    public static NotificationContent forEvent(
            LoanApplicationEventType type,
            LoanApplication loan,
            NotificationAudience audience
    ) {
        String reference = loan.getReference() != null ? loan.getReference() : "votre dossier";
        return audience == NotificationAudience.ADVISOR
                ? forAdvisorEvent(type, reference)
                : forClientEvent(type, reference);
    }

    private static NotificationContent forClientEvent(LoanApplicationEventType type, String reference) {
        return switch (type) {
            case APPLICATION_SUBMITTED -> new NotificationContent(
                    "Demande soumise",
                    "Votre demande " + reference + " a été transmise à votre conseiller."
            );
            case REVIEW_STARTED -> new NotificationContent(
                    "Analyse en cours",
                    "Votre dossier " + reference + " est en cours d'instruction."
            );
            case DOCUMENT_REJECTED -> new NotificationContent(
                    "Document à corriger",
                    "Un document de votre dossier " + reference + " nécessite une action de votre part."
            );
            case DOCUMENT_VALIDATED -> new NotificationContent(
                    "Document validé",
                    "Un document de votre dossier " + reference + " a été validé."
            );
            case DOCUMENT_UPLOADED -> new NotificationContent(
                    "Nouveau document",
                    "Le client a déposé un document sur le dossier " + reference + "."
            );
            case OFFER_PROPOSED -> new NotificationContent(
                    "Contre-offre reçue",
                    "Votre conseiller vous a proposé une contre-offre pour " + reference + "."
            );
            case APPLICATION_APPROVED -> new NotificationContent(
                    "Demande approuvée",
                    "Félicitations ! Votre demande " + reference + " a été approuvée."
            );
            case APPLICATION_REJECTED -> new NotificationContent(
                    "Demande refusée",
                    "Votre demande " + reference + " a été refusée."
            );
            case APPLICATION_CANCELLED -> new NotificationContent(
                    "Demande annulée",
                    "La demande " + reference + " a été annulée."
            );
            case MANDATE_ACTIVATED -> new NotificationContent(
                    "Mandat activé",
                    "Votre mandat de prélèvement SEPA est actif pour le prêt lié à " + reference + "."
            );
            case MANDATE_REVOKED -> new NotificationContent(
                    "Mandat révoqué",
                    "Le mandat de prélèvement du prêt lié à " + reference + " a été révoqué."
            );
            case PAYMENT_SUCCEEDED -> new NotificationContent(
                    "Prélèvement réussi",
                    "Un prélèvement a été effectué avec succès pour " + reference + "."
            );
            case PAYMENT_FAILED -> new NotificationContent(
                    "Échec de prélèvement",
                    "Un prélèvement a échoué pour " + reference + ". Vérifiez votre mandat ou votre solde."
            );
            case INSTALLMENT_OVERDUE -> new NotificationContent(
                    "Échéance en retard",
                    "Une échéance est en retard pour " + reference + "."
            );
            case LOAN_CLOSED -> new NotificationContent(
                    "Prêt soldé",
                    "Votre prêt lié à " + reference + " est entièrement remboursé."
            );
            case LOAN_DEFAULTED -> new NotificationContent(
                    "Prêt en défaut",
                    "Le prêt lié à " + reference + " est passé en situation de défaut."
            );
            case ADVISOR_ASSIGNED -> new NotificationContent(
                    "Conseiller assigné",
                    "Un conseiller a été assigné au dossier " + reference + "."
            );
            default -> new NotificationContent(
                    "Mise à jour dossier",
                    "Une mise à jour est disponible pour " + reference + "."
            );
        };
    }

    private static NotificationContent forAdvisorEvent(LoanApplicationEventType type, String reference) {
        return switch (type) {
            case APPLICATION_SUBMITTED -> new NotificationContent(
                    "Nouvelle demande soumise",
                    "Le dossier " + reference + " vient d'être soumis par le client."
            );
            case DOCUMENT_UPLOADED -> new NotificationContent(
                    "Nouveau document",
                    "Le client a déposé un document sur le dossier " + reference + "."
            );
            case APPLICATION_APPROVED -> new NotificationContent(
                    "Dossier approuvé",
                    "Le dossier " + reference + " a été approuvé."
            );
            case APPLICATION_REJECTED -> new NotificationContent(
                    "Dossier refusé",
                    "Le dossier " + reference + " a été refusé."
            );
            case PAYMENT_FAILED -> new NotificationContent(
                    "Échec de prélèvement",
                    "Un prélèvement a échoué pour le dossier " + reference + "."
            );
            case INSTALLMENT_OVERDUE -> new NotificationContent(
                    "Échéance en retard",
                    "Une échéance est en retard sur le dossier " + reference + "."
            );
            case LOAN_DEFAULTED -> new NotificationContent(
                    "Prêt en défaut",
                    "Le prêt lié au dossier " + reference + " est passé en situation de défaut."
            );
            case ADVISOR_ASSIGNED -> new NotificationContent(
                    "Nouveau dossier affecté",
                    "Le dossier " + reference + " vous a été assigné."
            );
            case OFFER_ACCEPTED -> new NotificationContent(
                    "Contre-offre acceptée",
                    "Le client a accepté votre contre-offre pour le dossier " + reference + "."
            );
            default -> new NotificationContent(
                    "Mise à jour dossier",
                    "Une mise à jour est disponible sur le dossier " + reference + "."
            );
        };
    }
}
