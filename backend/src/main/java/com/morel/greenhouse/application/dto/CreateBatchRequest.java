package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBatchRequest(
        @NotNull Long greenhouseId,
        @NotBlank String batchNo,
        @NotBlank String batchName,
        String cropName,
        String status,
        @NotBlank String startedAt,
        String expectedHarvestAt,
        String summary
) {
}
