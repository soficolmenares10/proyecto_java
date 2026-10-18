package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Enrollment of a student in a grade for one academic year. Maps the table "matriculas".
 * The student and grade descriptions are filled by the DAO to make listings easy.
 */
public class Enrollment {

    public static final String ACTIVE = "Activo";
    public static final String GRADUATED = "Graduado";
    public static final String WITHDRAWN = "Retirado";
    public static final List<String> STATUSES = List.of(ACTIVE, GRADUATED, WITHDRAWN);

    private int id;
    private int studentId;
    private String studentName;
    private String studentDocument;
    private int gradeId;
    private String gradeName;
    private int gradeLevel;
    private int academicYear;
    private String status;
    private LocalDate enrollmentDate;
    private LocalDate withdrawalDate;
    private String notes;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentDocument() {
        return studentDocument;
    }

    public void setStudentDocument(String studentDocument) {
        this.studentDocument = studentDocument;
    }

    public int getGradeId() {
        return gradeId;
    }

    public void setGradeId(int gradeId) {
        this.gradeId = gradeId;
    }

    public String getGradeName() {
        return gradeName;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    public int getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(int gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public int getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(int academicYear) {
        this.academicYear = academicYear;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public LocalDate getWithdrawalDate() {
        return withdrawalDate;
    }

    public void setWithdrawalDate(LocalDate withdrawalDate) {
        this.withdrawalDate = withdrawalDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
