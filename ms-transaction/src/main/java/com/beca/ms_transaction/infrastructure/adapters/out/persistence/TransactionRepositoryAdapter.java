package com.beca.ms_transaction.infrastructure.adapters.out.persistence;

import com.beca.ms_transaction.application.ports.out.TransactionRepositoryPort;
import com.beca.ms_transaction.domain.models.Transaction;
import com.beca.ms_transaction.domain.models.TransactionType;
import com.beca.ms_transaction.infrastructure.adapters.out.persistence.entity.TransactionJpaEntity;
import com.beca.ms_transaction.infrastructure.adapters.out.persistence.repository.SpringDataTransactionRepository;
import com.beca.ms_transaction.infrastructure.adapters.out.persistence.specification.TransactionSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TransactionRepositoryAdapter implements TransactionRepositoryPort {

    private final SpringDataTransactionRepository repository;

    public TransactionRepositoryAdapter(SpringDataTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity entity = toEntity(transaction);
        TransactionJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Page<Transaction> search(UUID userId, OffsetDateTime startDate, OffsetDateTime endDate,
                                    TransactionType type, Pageable pageable) {
        Specification<TransactionJpaEntity> spec = TransactionSpecifications.withFilters(userId, startDate, endDate, type);
        return repository.findAll(spec, pageable).map(this::toDomain);
    }

    @Override
    public List<Transaction> findForAnalysis(UUID userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        Specification<TransactionJpaEntity> spec = TransactionSpecifications.forAnalysis(userId, startDate, endDate);
        return repository.findAll(spec).stream()
                .map(this::toDomain)
                .toList();
    }

    private Transaction toDomain(TransactionJpaEntity entity) {
        return new Transaction(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getStatus(),
                entity.getAmount(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getTargetUserId(),
                entity.getRejectionReason(),
                entity.getCreatedAt()
        );
    }

    private TransactionJpaEntity toEntity(Transaction domain) {
        return new TransactionJpaEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getType(),
                domain.getStatus(),
                domain.getAmount(),
                domain.getCategory(),
                domain.getDescription(),
                domain.getTargetUserId(),
                domain.getRejectionReason(),
                domain.getCreatedAt()
        );
    }
}
