package com.projetfilrouge.loanmanagement.web.dto.request;

import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;

public record AdminApplicationListQuery(
        String search,
        Long advisorId,
        boolean unassignedOnly,
        LoanApplicationStatus status,
        AdminLoanListSort sort,
        int page,
        int size
) {
}
