package com.processing.merchantacquirer.service.dto;

import com.processing.common.dto.authorization.AuthorizationRequest;
import com.processing.merchantacquirer.domain.entity.AcquirerFee;

public record RequestFeeData(
        AuthorizationRequest authorizationRequest,
        AcquirerFee fee
) {
}
