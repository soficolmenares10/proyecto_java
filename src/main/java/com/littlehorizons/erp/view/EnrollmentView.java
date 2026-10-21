package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ConsoleUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Console screens for school-year enrollments.
 */
public class EnrollmentView extends BaseView {

    private final EnrollmentController enrollmentController;
    private final StudentController studentController;

    public EnrollmentView(EnrollmentController enrollmentController, StudentController studentController) {
        this.enrollmentController = enrollmentController;
        this.studentController = studentController;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Matrículas");
            System.out.println("1. Matricular estudiante");
            System.out.println("2. Listar matriculados por grado y año");
            System.out.println("3. Resumen de matrículas de un año");
            System.out.println("4. Historial de matrículas de un estudiante");
            System.out.println("5. Retirar estudiante");
            System.out.println("6. Graduar estudiante (grado Once)");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 6);
            switch (option) {
                case 1 -> runAndPause(this::enrollStudent);
                case 2 -> runAndPause(this::listByGradeAndYear);
                case 3 -> runAndPause(this::showYearSummary);
                case 4 -> runAndPause(this::showHistory);
                case 5 -> runAndPause(this::withdrawStudent);
                case 6 -> runAndPause(this::graduateStudent);
                default -> back = true;
            }
        }
    }

    /** Asks if the user wants to enroll a student right now (used after registering a student). */
    public void offerEnrollment(int studentId) {
        if (ConsoleUtil.confirm("¿Desea matricular al estudiante ahora?")) {
            enrollStudentFlow(studentId);
        }
    }

    /** Prints the enrollment history of one student (used by the student detail screen). */
    public void printHistoryOf(int studentId) {
        List<Enrollment> history = enrollmentController.history(studentId);
        System.out.println("\nHistorial de matrículas:");
        if (history.isEmpty()) {
            System.out.println("  (sin matrículas registradas)");
            return;
        }
        List<String[]> rows = history.stream()
                .map(e -> new String[]{String.valueOf(e.getAcademicYear()), e.getGradeName(), e.getStatus(),
                        String.valueOf(e.getEnrollmentDate()),
                        e.getWithdrawalDate() == null ? "-" : String.valueOf(e.getWithdrawalDate())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Año", "Grado", "Estado", "Fecha matrícula", "Fecha cierre"}, rows);
    }

    // ------------------------------------------------------------------ actions

    private void enrollStudent() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        ConsoleUtil.printInfo("Estudiante: " + student.getFullName() + " (" + student.getStatus() + ")");
        enrollStudentFlow(student.getId());
    }

    private void enrollStudentFlow(int studentId) {
        Grade grade = chooseGrade();
        int year = readYear();
        String notes = ConsoleUtil.readOptionalText("Observaciones", 500);
        enrollmentController.enroll(studentId, grade.getId(), year, notes);
        ConsoleUtil.printSuccess("Estudiante matriculado en " + grade.getName() + " para el año " + year + ".");
    }

    private void listByGradeAndYear() {
        Grade grade = chooseGrade();
        int year = readYear();
        List<Enrollment> enrollments = enrollmentController.listByGradeAndYear(grade.getId(), year);
        ConsoleUtil.printTitle("Matriculados en " + grade.getName() + " - " + year);
        if (enrollments.isEmpty()) {
            ConsoleUtil.printInfo("No hay estudiantes matriculados en ese grado y año.");
            return;
        }
        List<String[]> rows = enrollments.stream()
                .map(e -> new String[]{e.getStudentDocument(), e.getStudentName(), e.getStatus(),
                        String.valueOf(e.getEnrollmentDate())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Documento", "Estudiante", "Estado", "Fecha matrícula"}, rows);
        System.out.println("Total: " + enrollments.size() + " estudiantes");
    }

    private void showYearSummary() {
        int year = readYear();
        List<Enrollment> enrollments = enrollmentController.listByYear(year);
        ConsoleUtil.printTitle("Resumen de matrículas " + year);
        if (enrollments.isEmpty()) {
            ConsoleUtil.printInfo("No hay matrículas registradas en ese año.");
            return;
        }
        // Group the enrollments by grade level (sorted from Preescolar to Once).
        Map<Integer, List<Enrollment>> byGrade = enrollments.stream()
                .collect(Collectors.groupingBy(Enrollment::getGradeLevel, TreeMap::new, Collectors.toList()));
        List<String[]> rows = byGrade.values().stream()
                .map(list -> new String[]{list.get(0).getGradeName(), String.valueOf(list.size()),
                        String.valueOf(countWithStatus(list, Enrollment.ACTIVE)),
                        String.valueOf(countWithStatus(list, Enrollment.WITHDRAWN)),
                        String.valueOf(countWithStatus(list, Enrollment.GRADUATED))})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Grado", "Total", "Activos", "Retirados", "Graduados"}, rows);
        System.out.println("Total de matrículas: " + enrollments.size());
    }

    private void showHistory() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        ConsoleUtil.printInfo("Estudiante: " + student.getFullName());
        printHistoryOf(student.getId());
    }

    private void withdrawStudent() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        int year = readYear();
        Enrollment enrollment = enrollmentController.getActiveEnrollment(student.getId(), year);
        ConsoleUtil.printInfo(student.getFullName() + " - " + enrollment.getGradeName() + " (" + year + ")");
        String reason = ConsoleUtil.readOptionalText("Motivo del retiro", 500);
        if (ConsoleUtil.confirm("¿Confirma el retiro del estudiante?")) {
            enrollmentController.withdraw(student.getId(), year, reason);
            ConsoleUtil.printSuccess("Estudiante retirado correctamente.");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    private void graduateStudent() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        int year = readYear();
        Enrollment enrollment = enrollmentController.getActiveEnrollment(student.getId(), year);
        ConsoleUtil.printInfo(student.getFullName() + " - " + enrollment.getGradeName() + " (" + year + ")");
        if (ConsoleUtil.confirm("¿Confirma la graduación del estudiante?")) {
            enrollmentController.graduate(student.getId(), year);
            ConsoleUtil.printSuccess("Estudiante graduado correctamente.");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    // ------------------------------------------------------------------ helpers

    private Grade chooseGrade() {
        List<Grade> grades = enrollmentController.getGrades();
        List<String> names = grades.stream().map(Grade::getName).collect(Collectors.toList());
        String chosen = ConsoleUtil.readChoice("Grado", names);
        return grades.stream()
                .filter(grade -> grade.getName().equals(chosen))
                .findFirst()
                .orElseThrow();
    }

    private int readYear() {
        int currentYear = LocalDate.now().getYear();
        return ConsoleUtil.readInt("Año académico (por ejemplo " + currentYear + "): ", 2000, currentYear + 1);
    }

    private long countWithStatus(List<Enrollment> enrollments, String status) {
        return enrollments.stream().filter(e -> status.equals(e.getStatus())).count();
    }
}
