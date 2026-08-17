package com.processing.controller;


import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.common.dto.authorization.AuthorizationResponse;
import com.processing.service.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Внутренний REST-эндпоинт маршрутизации транзакций (вызывается Gateway).
 */
@RestController
@RequestMapping("/api/internal")
public class RouteController {


    private static final Logger LOG = LoggerFactory.getLogger(RouteController.class);


    private final RouteService routeService;


    /**
     * @param routeService сервис оркестрации маршрутизации
     */
    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }


    /**
     * Принимает запрос авторизации, выполняет маршрутизацию и возвращает ответ.
     *
     * @param request тело {@link AuthorizationRequest} от Gateway
     * @return {@link AuthorizationResponse} для передачи клиенту
     */
    @PostMapping("/route")
    public ResponseEntity<AuthorizationResponse> routeTransaction(
            @RequestBody AuthorizationRequest request) {
        LOG.info("Received: STAN={}, PAN={}", request.stan(), maskPan(request.pan()));
        AuthorizationResponse response = routeService.route(request);
        return ResponseEntity.ok(response);
    }


    /**
     * Маскирует PAN для безопасного логирования (первые 4 + **** + последние 4).
     *
     * @param pan номер карты
     * @return замаскированный PAN
     */
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) {
            return "****";
        }
        return pan.substring(0, 4) + "****" + pan.substring(pan.length() - 4);
    }
}
