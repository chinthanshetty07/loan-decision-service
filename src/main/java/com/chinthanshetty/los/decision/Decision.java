package com.chinthanshetty.los.decision;

import com.chinthanshetty.los.domain.ApplicationStatus;

import java.math.BigDecimal;
import java.util.List;

public record Decision(ApplicationStatus status, List<String> reasons, BigDecimal proposedEmi, BigDecimal foir) {
}
