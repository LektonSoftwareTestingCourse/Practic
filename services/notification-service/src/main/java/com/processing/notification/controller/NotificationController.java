package com.processing.notification.controller;

import com.processing.notification.model.CardNotification;
import com.processing.notification.repository.CardNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for viewing received card notifications.
 *
 * <p>Provides test endpoints to verify that events from Card Management
 * are being properly consumed and persisted.</p>
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final CardNotificationRepository repository;

    /**
     * Returns a paginated list of received card notifications,
     * ordered by {@code receivedAt} descending.
     *
     * @param page zero-based page index (default 0)
     * @param size page size (default 50)
     * @return page of {@link CardNotification}
     */
    @GetMapping
    public Page<CardNotification> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return repository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt")));
    }
}
