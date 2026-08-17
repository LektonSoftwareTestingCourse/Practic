package com.processing.common.dto.cardmanagement;

import com.processing.common.dto.annotations.Bin;
import com.processing.common.dto.annotations.IssuerId;

public record CreateBinIssuerRequest(
        @Bin
        String bin,

        @IssuerId
        String issuerId
) {
}
