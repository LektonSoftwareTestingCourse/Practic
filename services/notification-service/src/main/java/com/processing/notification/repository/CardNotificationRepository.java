package com.processing.notification.repository;

import com.processing.notification.model.CardNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link CardNotification}.
 */
public interface CardNotificationRepository extends JpaRepository<CardNotification, UUID> {
}
