package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.List;

/**
 * A loanable resource of the school (book, lab equipment, sports gear, device).
 * Maps the table "recursos".
 */
public class Resource {

    public static final String AVAILABLE = "Disponible";
    public static final String LOANED = "Prestado";
    public static final String MAINTENANCE = "En Mantenimiento";
    public static final String DISCARDED = "Dado de Baja";
    public static final List<String> STATUSES = List.of(AVAILABLE, LOANED, MAINTENANCE, DISCARDED);
    /** Statuses that a person can set by hand ("Prestado" is only set by a loan). */
    public static final List<String> MANUAL_STATUSES = List.of(AVAILABLE, MAINTENANCE, DISCARDED);

    private int id;
    private String code;
    private String name;
    private int categoryId;
    private String categoryName;
    private String description;
    private LocalDate acquisitionDate;
    private String location;
    private String status;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(LocalDate acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
