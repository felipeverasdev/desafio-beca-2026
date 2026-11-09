package com.beca.ms_transaction.infrastructure.config;

import com.beca.ms_transaction.application.ports.out.TransactionRepositoryPort;
import com.beca.ms_transaction.application.services.TransactionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public TransactionService transactionService(TransactionRepositoryPort transactionRepositoryPort) {
        return new TransactionService(transactionRepositoryPort);
    }
}

