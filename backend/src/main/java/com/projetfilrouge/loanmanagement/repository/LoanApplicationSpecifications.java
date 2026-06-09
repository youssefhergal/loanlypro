package com.projetfilrouge.loanmanagement.repository;

import com.projetfilrouge.loanmanagement.entity.LoanApplication;
import com.projetfilrouge.loanmanagement.entity.LoanApplicationStatus;
import com.projetfilrouge.loanmanagement.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class LoanApplicationSpecifications {

    private LoanApplicationSpecifications() {
    }

    public static Specification<LoanApplication> adminList(
            String search,
            Long advisorId,
            boolean unassignedOnly,
            LoanApplicationStatus status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), LoanApplicationStatus.DRAFT));

            Join<LoanApplication, User> applicantJoin = root.join("applicant", JoinType.INNER);
            Join<LoanApplication, User> advisorJoin = null;

            if (unassignedOnly) {
                predicates.add(cb.isNull(root.get("assignedAdvisor")));
            } else if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (!unassignedOnly && advisorId != null) {
                advisorJoin = root.join("assignedAdvisor", JoinType.INNER);
                predicates.add(cb.equal(advisorJoin.get("id"), advisorId));
            }

            if (search != null && !search.isBlank()) {
                if (advisorJoin == null) {
                    advisorJoin = root.join("assignedAdvisor", JoinType.LEFT);
                }
                String term = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("reference")), term),
                        cb.like(cb.lower(root.get("title")), term),
                        cb.like(cb.lower(root.get("purpose")), term),
                        cb.like(cb.lower(applicantJoin.get("email")), term),
                        cb.like(cb.lower(applicantJoin.get("firstName")), term),
                        cb.like(cb.lower(applicantJoin.get("lastName")), term),
                        cb.like(cb.lower(advisorJoin.get("firstName")), term),
                        cb.like(cb.lower(advisorJoin.get("lastName")), term)
                ));
            }

            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
