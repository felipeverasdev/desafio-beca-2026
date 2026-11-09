package com.beca.ms_transaction.infrastructure.adapters.out.persistence.specification;

import com.beca.ms_transaction.domain.models.TransactionStatus;
import com.beca.ms_transaction.domain.models.TransactionType;
import com.beca.ms_transaction.infrastructure.adapters.out.persistence.entity.TransactionJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionSpecifications {

    public static Specification<TransactionJpaEntity> withFilters(
            UUID userId, OffsetDateTime startDate, OffsetDateTime endDate, TransactionType type) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Origem ou destino da transação
            predicates.add(cb.or(
                    cb.equal(root.get("userId"), userId),
                    cb.equal(root.get("targetUserId"), userId)
            ));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<TransactionJpaEntity> forAnalysis(
            UUID userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.equal(root.get("status"), TransactionStatus.APPROVED));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

