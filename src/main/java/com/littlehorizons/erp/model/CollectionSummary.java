package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.util.Map;

/** Money collected in a date range: total, number of payments, and totals by concept and by method. */
public record CollectionSummary(BigDecimal total, long payments, Map<String, BigDecimal> byConcept,
                                Map<String, BigDecimal> byMethod) {
}
