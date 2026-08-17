package com.processing.cardmanagement.configuration;

import com.processing.cardmanagement.events.CardEventListener;
import com.processing.cardmanagement.events.CardEventNotifierImpl;
import com.processing.cardmanagement.mappers.CardOutboxEventDataPersistenceMapper;
import com.processing.cardmanagement.options.CardServiceDefaults;
import com.processing.cardmanagement.options.CardServiceSettings;
import com.processing.cardmanagement.options.OutboxOptions;
import com.processing.cardmanagement.repositories.*;
import com.processing.cardmanagement.services.*;
import com.processing.cardmanagement.services.retries.RetryService;
import com.processing.cardmanagement.services.retries.RetryServiceImpl;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;

import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        transactionManager.setNestedTransactionAllowed(true);

        return transactionManager;
    }

    @Bean
    public PanGenerator panGenerator() {
        return new LuhnValidator();
    }

    @Bean
    public CardEventNotifierImpl cardEventNotifier(
            OutboxEventProcessor outboxEventProcessor,
            List<CardEventListener> eventListeners
    ) {
        return new CardEventNotifierImpl(outboxEventProcessor, eventListeners);
    }

    @Bean
    public BinIssuerRepository binIssuerRepository(BinIssuerJpaRepository jpaRepository) {
        return new BinIssuerJpaAdapter(jpaRepository);
    }

    @Bean
    public BinIssuerService binIssuerService(BinIssuerRepository repository) {
        return new BinIssuerServiceImpl(repository);
    }

    @Bean
    public OutboxRepository outboxRepository(
            OutboxEventJpaRepository jpaRepository,
            CardOutboxEventDataPersistenceMapper persistenceMapper
    ) {
        return new OutboxJpaAdapter(jpaRepository, persistenceMapper);
    }

    @Bean
    public OutboxEventProcessorImpl outboxEventProcessor(
            OutboxRepository outboxRepository,
            RabbitTemplate rabbitTemplate,
            OutboxOptions outboxOptions
    ) {
        return new OutboxEventProcessorImpl(outboxRepository, rabbitTemplate, outboxOptions);
    }

    @Bean
    public RetryService retryService() {
        return new RetryServiceImpl();
    }

    @Bean
    public CardService cardService(
            CardRepository cardRepository,
            ReservationRepository reservationRepository,
            ReservationRollbackRepository reservationRollbackRepository,
            CardServiceSettings serviceConfigurationProperties,
            CardServiceDefaults defaultsConfigurationProperties,
            PanGenerator panGenerator,
            CardEventNotifierImpl cardEventNotifier,
            BinIssuerService binIssuerService,
            RetryService retryService,
            TransactionRunner transactionRunner
    ) {
        return new CardServiceImpl(
                cardRepository,
                reservationRepository,
                reservationRollbackRepository,
                serviceConfigurationProperties,
                defaultsConfigurationProperties,
                panGenerator,
                cardEventNotifier,
                binIssuerService,
                retryService,
                transactionRunner
        );
    }
}
