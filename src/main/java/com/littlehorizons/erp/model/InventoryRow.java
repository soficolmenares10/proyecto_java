package com.littlehorizons.erp.model;

/** How many resources there are of a category in a status. */
public record InventoryRow(String category, String status, long count) {
}
