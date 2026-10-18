package com.littlehorizons.erp.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A guardian (parent or legal tutor) of one or more students. Maps the table "acudientes".
 */
public class Guardian {

    public static final List<String> DOCUMENT_TYPES = List.of("CC", "CE", "Pasaporte");
    public static final List<String> RELATIONSHIPS = List.of("Padre", "Madre", "Abuelo", "Abuela",
            "Tío", "Tía", "Hermano", "Hermana", "Tutor Legal");

    private int id;
    private String documentType;
    private String documentNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String secondLastName;
    private String relationship;
    private String primaryPhone;
    private String secondaryPhone;
    private String email;
    private String address;
    private String occupation;
    private boolean financialResponsible;

    /**
     * Only filled when the guardian is loaded together with a student:
     * true if this guardian is the main contact of that student.
     */
    private boolean primaryForStudent;

    public String getFullName() {
        return Stream.of(firstName, middleName, lastName, secondLastName)
                .filter(Objects::nonNull)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSecondLastName() {
        return secondLastName;
    }

    public void setSecondLastName(String secondLastName) {
        this.secondLastName = secondLastName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPrimaryPhone() {
        return primaryPhone;
    }

    public void setPrimaryPhone(String primaryPhone) {
        this.primaryPhone = primaryPhone;
    }

    public String getSecondaryPhone() {
        return secondaryPhone;
    }

    public void setSecondaryPhone(String secondaryPhone) {
        this.secondaryPhone = secondaryPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public boolean isFinancialResponsible() {
        return financialResponsible;
    }

    public void setFinancialResponsible(boolean financialResponsible) {
        this.financialResponsible = financialResponsible;
    }

    public boolean isPrimaryForStudent() {
        return primaryForStudent;
    }

    public void setPrimaryForStudent(boolean primaryForStudent) {
        this.primaryForStudent = primaryForStudent;
    }
}
