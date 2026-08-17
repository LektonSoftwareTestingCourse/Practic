package com.processing.merchantacquirer.client.dto;

import java.util.List;

public record CardsResponse(int total, List<CardDataResponse> cards) {}
