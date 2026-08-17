package com.processing.common.dto.cardmanagement;

import com.processing.common.dto.annotations.Bin;
import com.processing.common.dto.annotations.Pan;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkUpdateRequest(
        @Nullable
        List<@Bin String> bins,

        @Nullable
        List<@Pan String> pans,

        @NotNull
        CardModelStatus status
) {
}
