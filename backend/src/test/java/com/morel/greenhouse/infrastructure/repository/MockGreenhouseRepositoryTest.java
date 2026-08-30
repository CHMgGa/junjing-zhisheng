package com.morel.greenhouse.infrastructure.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockGreenhouseRepositoryTest {

    @Test
    void returnsDeterministicMockDataForAllRepositoryMethods() {
        MockGreenhouseRepository repository = new MockGreenhouseRepository();

        assertEquals(3, repository.findGreenhouses().size());
        assertFalse(repository.findGreenhousesByOwner(2L).isEmpty());
        assertTrue(repository.findCurrentTelemetry(1L).isPresent());
        assertEquals(4, repository.findDevices(1L).size());
        assertEquals(3, repository.findAlerts(1L).size());
        assertEquals(3, repository.findAlertDetails(1L).size());
        assertEquals(3, repository.findTraceabilityRecords(1L).size());
        assertEquals("admin1", repository.findOperator("admin1").orElseThrow().username());
    }
}
