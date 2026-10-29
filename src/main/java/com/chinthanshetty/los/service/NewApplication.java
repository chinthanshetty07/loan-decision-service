package com.chinthanshetty.los.service;

import com.chinthanshetty.los.domain.ProductType;

import java.math.BigDecimal;

public record NewApplication(
        ProductType productType,
        String applicantName,
        String pan,
        BigDecimal monthlyIncome,
        BigDecimal existingEmi,
        BigDecimal requestedAmount,
        int tenureMonths) {
}
