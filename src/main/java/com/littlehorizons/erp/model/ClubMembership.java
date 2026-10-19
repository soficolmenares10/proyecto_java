package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * A student that joined a club. Maps the table "membresias_club".
 */
public record ClubMembership(int id, int studentId, String studentDocument, String studentName,
                             int clubId, String clubName, LocalDate joinDate, String status,
                             LocalDate withdrawalDate, String withdrawalReason) {

    public static final String ACTIVE = "Activa";
    public static final String WITHDRAWN = "Retirada";
}
