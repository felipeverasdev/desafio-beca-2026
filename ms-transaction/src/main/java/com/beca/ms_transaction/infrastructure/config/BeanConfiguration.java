package com.beca.ms_transaction.infrastructure.config;

import com.beca.ms_transaction.application.ports.out.AccountAuthorizationPort;
import com.beca.ms_transaction.application.ports.out.CurrencyExchangePort;
import com.beca.ms_transaction.application.ports.out.TransactionRepositoryPort;
import com.beca.ms_transaction.application.ports.out.UserValidationPort;
import com.beca.ms_transaction.application.services.TransactionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BeanConfiguration {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public TransactionService transactionService(
            TransactionRepositoryPort transactionRepositoryPort,
            AccountAuthorizationPort accountAuthorizationPort,
            UserValidationPort userValidationPort,
            CurrencyExchangePort currencyExchangePort) {
        return new TransactionService(
                transactionRepositoryPort,
                accountAuthorizationPort,
                userValidationPort,
                currencyExchangePort
        );
    }
}

