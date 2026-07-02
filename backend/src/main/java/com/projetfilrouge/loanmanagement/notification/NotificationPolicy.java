package com.projetfilrouge.loanmanagement.notification;

import com.projetfilrouge.loanmanagement.entity.LoanApplicationEventType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Politique de notification : quels événements déclenchent un e-mail ou une alerte in-app.
 */
public final class NotificationPolicy {

    private static final Set<LoanApplicationEventType> EMAIL_EVENTS = EnumSet.of(
            LoanApplicationEventType.APPLICATION_SUBMITTED,
            LoanApplicationEventType.REVIEW_STARTED,
            LoanApplicationEventType.DOCUMENT_REJECTED,
            LoanApplicationEventType.OFFER_PROPOSED,
            LoanApplicationEventType.APPLICATION_APPROVED,
            LoanApplicationEventType.APPLICATION_REJECTED,
            LoanApplicationEventType.APPLICATION_CANCELLED,
            LoanApplicationEventType.MANDATE_ACTIVATED,
            LoanApplicationEventType.MANDATE_REVOKED,
            LoanApplicationEventType.PAYMENT_SUCCEEDED,
            LoanApplicationEventType.PAYMENT_FAILED,
            LoanApplicationEventType.INSTALLMENT_OVERDUE,
            LoanApplicationEventType.LOAN_CLOSED,
            LoanApplicationEventType.LOAN_DEFAULTED
    );

    private static final Set<LoanApplicationEventType> IN_APP_EVENTS = EnumSet.of(
            LoanApplicationEventType.APPLICATION_SUBMITTED,
            LoanApplicationEventType.REVIEW_STARTED,
            LoanApplicationEventType.DOCUMENT_REJECTED,
            LoanApplicationEventType.DOCUMENT_VALIDATED,
            LoanApplicationEventType.OFFER_PROPOSED,
            LoanApplicationEventType.APPLICATION_APPROVED,
            LoanApplicationEventType.APPLICATION_REJECTED,
            LoanApplicationEventType.APPLICATION_CANCELLED,
            LoanApplicationEventType.MANDATE_ACTIVATED,
            LoanApplicationEventType.MANDATE_REVOKED,
            LoanApplicationEventType.PAYMENT_SUCCEEDED,
            LoanApplicationEventType.PAYMENT_FAILED,
            LoanApplicationEventType.INSTALLMENT_OVERDUE,
            LoanApplicationEventType.LOAN_CLOSED,
            LoanApplicationEventType.LOAN_DEFAULTED,
            LoanApplicationEventType.ADVISOR_ASSIGNED
    );

    private static final Set<LoanApplicationEventType> ADVISOR_IN_APP_EVENTS = EnumSet.of(
            LoanApplicationEventType.APPLICATION_SUBMITTED,
            LoanApplicationEventType.DOCUMENT_UPLOADED,
            LoanApplicationEventType.OFFER_ACCEPTED,
            LoanApplicationEventType.APPLICATION_APPROVED,
            LoanApplicationEventType.APPLICATION_REJECTED,
            LoanApplicationEventType.PAYMENT_FAILED,
            LoanApplicationEventType.INSTALLMENT_OVERDUE,
            LoanApplicationEventType.LOAN_DEFAULTED,
            LoanApplicationEventType.ADVISOR_ASSIGNED
    );

    private NotificationPolicy() {
    }

    public static boolean shouldSendEmail(LoanApplicationEventType type) {
        return EMAIL_EVENTS.contains(type);
    }

    public static boolean shouldCreateInAppForClient(LoanApplicationEventType type) {
        return IN_APP_EVENTS.contains(type);
    }

    public static boolean shouldCreateInAppForAdvisor(LoanApplicationEventType type) {
        return ADVISOR_IN_APP_EVENTS.contains(type);
    }
}
