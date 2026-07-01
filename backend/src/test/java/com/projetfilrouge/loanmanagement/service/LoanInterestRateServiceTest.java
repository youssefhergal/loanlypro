package com.projetfilrouge.loanmanagement.service;

import com.projetfilrouge.loanmanagement.entity.LoanPurpose;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LoanInterestRateServiceTest {

    private final LoanInterestRateService service = new LoanInterestRateService();

    @Test
    void calculateIndicativeRate_vehicle15000Over48Months_matchesLegacyDefault() {
        BigDecimal rate = service.calculateIndicativeRate(
                new BigDecimal("15000"),
                48,
                LoanPurpose.VEHICLE
        );

        assertThat(rate).isEqualByComparingTo("3.85");
    }

    @Test
    void calculateIndicativeRate_greenIsLowerThanVehicle() {
        BigDecimal green = service.calculateIndicativeRate(
                new BigDecimal("15000"),
                48,
                LoanPurpose.GREEN
        );
        BigDecimal vehicle = service.calculateIndicativeRate(
                new BigDecimal("15000"),
                48,
                LoanPurpose.VEHICLE
        );

        assertThat(green).isLessThan(vehicle);
    }

    @Test
    void calculateIndicativeRate_longerDurationIncreasesRate() {
        BigDecimal shortLoan = service.calculateIndicativeRate(
                new BigDecimal("20000"),
                36,
                LoanPurpose.PERSONAL
        );
        BigDecimal longLoan = service.calculateIndicativeRate(
                new BigDecimal("20000"),
                144,
                LoanPurpose.PERSONAL
        );

        assertThat(longLoan).isGreaterThan(shortLoan);
    }
}
