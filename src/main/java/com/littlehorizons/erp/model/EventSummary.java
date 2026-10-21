package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** One event with its number of participants. */
public record EventSummary(String eventName, LocalDate date, String type, boolean competition, long participants) {
}
