package com.morel.greenhouse.domain.traceability;

import java.time.LocalDate;

public record TraceabilityRecord(
        Long id,
        Long greenhouseId,
        String batchNo,
        String operation,
        String operator,
        LocalDate operationDate,
        String note
) {
}
