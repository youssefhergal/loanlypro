package com.projetfilrouge.loanmanagement.service.documents;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Helpers de formatage français pour les documents PDF (montants, taux, dates).
 */
public final class DocumentFormat {

    private static final Locale FR = Locale.FRANCE;
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", FR);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", FR);

    private DocumentFormat() {
    }

    public static String euro(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        return String.format(FR, "%,.2f €", amount).replace('\u00A0', ' ');
    }

    public static String percent(BigDecimal rate) {
        if (rate == null) {
            return "—";
        }
        return String.format(FR, "%,.2f %%", rate).replace('\u00A0', ' ');
    }

    public static String months(Integer count) {
        return count == null ? "—" : count + " mois";
    }

    public static String date(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    public static String date(Instant instant) {
        return instant == null ? "—" : DATE.format(instant.atZone(ZONE));
    }

    public static String dateTime(Instant instant) {
        return instant == null ? "—" : DATE_TIME.format(instant.atZone(ZONE));
    }
}
