package com.littlehorizons.erp.model;

/**
 * Who borrows a resource. Students and professors have separate loan tables in the database
 * (prestamos_estudiante and prestamos_profesor).
 */
public enum BorrowerType {

    STUDENT("Estudiante"),
    PROFESSOR("Profesor");

    private final String label;

    BorrowerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
