package com.processing.cardmanagement.repositories;

import com.processing.cardmanagement.models.CardEntity;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;

public interface CardCriteriaBuilderJpaRepository {

    List<CardEntity> findCards(
        int limit,
        long offset,
        @Nullable String status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable Instant startDate,
        @Nullable Instant endDate
    );

    long countCards(
        @Nullable String status,
        @Nullable String bin,
        @Nullable String issuerId,
        @Nullable Instant startDate,
        @Nullable Instant endDate
    );
}
