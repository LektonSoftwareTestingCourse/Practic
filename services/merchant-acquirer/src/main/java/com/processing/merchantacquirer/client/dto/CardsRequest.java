package com.processing.merchantacquirer.client.dto;

public record CardsRequest(int limit, int offset, String status, String bin) {}
