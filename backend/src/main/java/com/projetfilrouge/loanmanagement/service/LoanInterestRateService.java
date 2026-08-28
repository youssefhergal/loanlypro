package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.LoanPurpose;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Taux indicatif selon montant, durée et finalité du prêt.
 * Le crédit vert bénéficie d'une décote par rapport aux autres projets.
 */
@Service
public class LoanInterestRateService {

    private static final BigDecimal BASE_RATE = new BigDecimal("3.95");
    private static final BigDecimal MIN_RATE = new BigDecimal("2.50");
    private static final BigDecimal MAX_RATE = new BigDecimal("7.50");
    private static final BigDecimal GREEN_DISCOUNT = new BigDecimal("0.75");

    public BigDecimal calculateIndicativeRate(
            BigDecimal amount,
            int durationMonths,
            LoanPurpose purpose
    ) {
        if (amount == null || purpose == null || durationMonths <= 0) {
            return BASE_RATE.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal rate = BASE_RATE
                .add(purposeAdjustment(purpose))
                .add(amountAdjustment(amount))
                .add(durationAdjustment(durationMonths));

        return clamp(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateIndicativeRate(BigDecimal amount, int durationMonths, LoanPurpose purpose, LoanPurpose fallbackPurpose) {
        LoanPurpose effectivePurpose = purpose != null ? purpose : fallbackPurpose;
        return calculateIndicativeRate(amount, durationMonths, effectivePurpose);
    }

    private BigDecimal purposeAdjustment(LoanPurpose purpose) {
        return switch (purpose) {
            case GREEN -> GREEN_DISCOUNT.negate();
            case HOME_IMPROVEMENT -> new BigDecimal("-0.25");
            case SOFTWARE -> new BigDecimal("-0.10");
            case EDUCATION -> new BigDecimal("-0.05");
            case VEHICLE -> BigDecimal.ZERO;
            case PERSONAL -> new BigDecimal("0.05");
            case OTHER -> new BigDecimal("0.15");
        };
    }

    private BigDecimal amountAdjustment(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("75000")) >= 0) {
            return new BigDecimal("-0.35");
        }
        if (amount.compareTo(new BigDecimal("40000")) >= 0) {
            return new BigDecimal("-0.25");
        }
        if (amount.compareTo(new BigDecimal("15000")) >= 0) {
            return new BigDecimal("-0.10");
        }
        if (amount.compareTo(new BigDecimal("5000")) >= 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal("0.20");
    }

    private BigDecimal durationAdjustment(int durationMonths) {
        if (durationMonths <= 24) {
            return new BigDecimal("-0.05");
        }
        if (durationMonths <= 60) {
            return BigDecimal.ZERO;
        }
        if (durationMonths <= 120) {
            return new BigDecimal("0.20");
        }
        return new BigDecimal("0.40");
    }

    private BigDecimal clamp(BigDecimal rate) {
        if (rate.compareTo(MIN_RATE) < 0) {
            return MIN_RATE;
        }
        if (rate.compareTo(MAX_RATE) > 0) {
            return MAX_RATE;
        }
        return rate;
    }
}
