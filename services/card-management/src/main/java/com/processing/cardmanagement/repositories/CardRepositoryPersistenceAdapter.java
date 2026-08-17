package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.exceptions.MassiveCardCreationCollisionException;
import com.processing.cardmanagement.exceptions.PanCollisionException;
import com.processing.cardmanagement.mappers.CardPersistenceMapper;
import com.processing.cardmanagement.models.Card;
import com.processing.cardmanagement.models.CardStatus;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CardRepositoryPersistenceAdapter implements CardRepository {

    private final CardPersistenceMapper persistenceMapper;
    private final CardJpaRepository jpaRepository;

    @Override
    public Optional<Card> findByPan(String pan) {
        return jpaRepository
            .findByPan(pan)
            .map(persistenceMapper::toDomain);
    }

    @Override
    public Optional<Card> findByPanForUpdate(String pan) {
        return jpaRepository
            .findWithPessimisticLockByPan(pan)
            .map(persistenceMapper::toDomain);
    }

    @Override
    public List<Card> findCards(
        int limit,
        long offset,
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable Instant startDate,
        @Nullable Instant endDate
    ) {
        return jpaRepository
            .findCards(
                limit,
                offset,
                status != null ? status.name() : null,
                bin,
                issuerId,
                startDate,
                endDate
            )
            .stream()
            .map(persistenceMapper::toDomain)
            .toList();
    }

    @Override
    public List<Card> findCardsByPansForUpdate(List<String> pans) {
        return jpaRepository
            .findWithPessimisticLockByPanIn(pans)
            .stream()
            .map(persistenceMapper::toDomain)
            .toList();
    }

    @Override
    public List<Card> findCardsByBinsForUpdate(List<String> bins) {
        return jpaRepository
            .findWithPessimisticLockByBinIn(bins)
            .stream()
            .map(persistenceMapper::toDomain)
            .toList();
    }

    @Override
    public long countCardsFiltered(
        @Nullable CardStatus status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable Instant startDate,
        @Nullable Instant endDate
    ) {
        return jpaRepository.countCards(
            status != null ? status.name() : null,
            bin,
            issuerId,
            startDate,
            endDate
        );
    }

    public long countAllCards() {
        return jpaRepository.count();
    }

    @Override
    public Card create(Card card) {
        try {
            return persistenceMapper.toDomain(
                jpaRepository.saveAndFlush(persistenceMapper.toEntity(card))
            );
        } catch (DataIntegrityViolationException ex) {
            throw new PanCollisionException(card.pan());
        }
    }

    @Override
    public Card update(Card card) {
        return persistenceMapper.toDomain(
            jpaRepository.save(persistenceMapper.toEntity(card))
        );
    }

    @Override
    public List<Card> createAll(List<Card> card) {
        try {
            return jpaRepository.saveAllAndFlush(card
                    .stream()
                    .map(persistenceMapper::toEntity)
                    .toList()
                )
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
        } catch (DataIntegrityViolationException ex) {
            throw new MassiveCardCreationCollisionException();
        }
    }
}
