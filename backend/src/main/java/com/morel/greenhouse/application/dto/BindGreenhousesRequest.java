package com.morel.greenhouse.application.dto;

import java.util.List;

public record BindGreenhousesRequest(
        List<Long> greenhouseIds
) {
}
