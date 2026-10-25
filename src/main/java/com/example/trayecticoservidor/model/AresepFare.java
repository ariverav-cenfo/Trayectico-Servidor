package com.example.trayecticoservidor.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AresepFare(
        String routeCode,
        String routeName,
        String branchCode,
        String branchName,
        String fractionCode,
        String fractionName,
        BigDecimal averageTripKm,
        BigDecimal regularFare,
        BigDecimal seniorFare,
        String resolution,
        LocalDate resolutionDate,
        String gazetteNumber,
        String gazetteSupplement,
        LocalDate gazetteDate,
        LocalDate effectiveDate,
        String caseNumber,
        String operators,
        String serviceType,
        String province) {
}
