package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicRecordController;
import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.ReportCardRow;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectAverage;
import com.littlehorizons.erp.util.ConsoleUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Console screens for grades (notas), report cards and averages.
 */
public class AcademicRecordView extends BaseView {

    private final AcademicRecordController recordController;
    private final StudentController studentController;
    private final ProfessorController professorController;
    private final SubjectSelector subjectSelector;

    public AcademicRecordView(AcademicRecordController recordController, StudentController studentController,
                              ProfessorController professorController, SubjectSelector subjectSelector) {
        this.recordController = recordController;
        this.studentController = studentController;
        this.professorController = professorController;
        this.subjectSelector = subjectSelector;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Calificaciones");
            System.out.println("1. Registrar o corregir una nota");
            System.out.println("2. Ver las notas y el promedio de un curso");
            System.out.println("3. Boletín de un estudiante (promedios)");
            System.out.println("4. Historial académico de un estudiante");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 4);
            switch (option) {
                case 1 -> runAndPause(this::registerGrade);
                case 2 -> runAndPause(this::showCourse);
                case 3 -> runAndPause(this::showReportCard);
                case 4 -> runAndPause(this::showHistory);
                default -> back = true;
            }
        }
    }

    private void registerGrade() {
        ConsoleUtil.printTitle("Registrar nota");
        Professor professor = professorController.getByDocument(ConsoleUtil.readDocument("Documento del profesor: "));
        ConsoleUtil.printInfo("Profesor: " + professor.getFullName());
        Subject subject = subjectSelector.choose(false);
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        ConsoleUtil.printInfo("Estudiante: " + student.getFullName());
        int year = readAcademicYear();
        String period = readPeriod(true);
        BigDecimal value = ConsoleUtil.readGrade("Nota (0.0 a 5.0): ");
        String notes = ConsoleUtil.readOptionalText("Observaciones", 1000);

        boolean created = recordController.registerGrade(student.getId(), subject.getId(), professor.getId(),
                year, period, value, notes);
        ConsoleUtil.printSuccess("Nota " + (created ? "registrada" : "actualizada") + ": " + value.toPlainString()
                + " en " + subject.getName() + " (período " + period + ").");
    }

    private void showCourse() {
        Subject subject = subjectSelector.choose(false);
        int year = readAcademicYear();
        List<String> options = new ArrayList<>(AcademicRecord.REGULAR_PERIODS);
        options.add("Todos");
        String chosen = ConsoleUtil.readChoice("Período", options);
        boolean allPeriods = chosen.equals("Todos");

        ConsoleUtil.printTitle(subject.getName() + " - " + year + (allPeriods ? "" : " - período " + chosen));
        if (!allPeriods) {
            List<AcademicRecord> grades = recordController.courseGrades(subject.getId(), year, chosen);
            if (grades.isEmpty()) {
                ConsoleUtil.printInfo("Todavía no hay notas en ese período.");
                return;
            }
            List<String[]> rows = grades.stream()
                    .map(r -> new String[]{r.studentDocument(), r.studentName(), r.value().toPlainString()})
                    .collect(Collectors.toList());
            ConsoleUtil.printTable(new String[]{"Documento", "Estudiante", "Nota"}, rows);
        }
        SubjectAverage average = recordController.courseAverage(subject.getId(), year, allPeriods ? null : chosen);
        if (average.average() == null) {
            ConsoleUtil.printInfo("No hay notas para calcular el promedio.");
        } else {
            System.out.println("Promedio del curso en " + average.subjectName() + ": " + average.average()
                    + "  (" + average.gradesCount() + " notas)");
        }
    }

    private void showReportCard() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        int year = readAcademicYear();
        List<ReportCardRow> report = recordController.reportCard(student.getId(), year);
        ConsoleUtil.printTitle("Boletín " + year + " - " + student.getFullName());
        if (report.isEmpty()) {
            ConsoleUtil.printInfo("El estudiante no tiene notas en " + year + ".");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (ReportCardRow row : report) {
            rows.add(new String[]{row.subjectName(),
                    show(row.gradeByPeriod().get("I")), show(row.gradeByPeriod().get("II")),
                    show(row.gradeByPeriod().get("III")), show(row.gradeByPeriod().get("IV")),
                    show(row.average())});
        }
        // Last row: the general average of each period (all subjects together).
        String[] periodAverages = new String[6];
        periodAverages[0] = "PROMEDIO DEL PERÍODO";
        for (int i = 0; i < AcademicRecord.REGULAR_PERIODS.size(); i++) {
            Optional<BigDecimal> average = recordController.studentPeriodAverage(student.getId(), year,
                    AcademicRecord.REGULAR_PERIODS.get(i));
            periodAverages[i + 1] = average.map(BigDecimal::toPlainString).orElse("-");
        }
        periodAverages[5] = recordController.studentGeneralAverage(student.getId(), year)
                .map(BigDecimal::toPlainString).orElse("-");
        rows.add(periodAverages);
        ConsoleUtil.printTable(new String[]{"Materia", "I", "II", "III", "IV", "Promedio"}, rows);
    }

    private void showHistory() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        List<AcademicRecord> history = recordController.history(student.getId());
        ConsoleUtil.printTitle("Historial académico - " + student.getFullName());
        if (history.isEmpty()) {
            ConsoleUtil.printInfo("El estudiante no tiene notas registradas.");
            return;
        }
        List<String[]> rows = history.stream()
                .map(r -> new String[]{String.valueOf(r.academicYear()), r.period(), r.subjectName(),
                        r.value().toPlainString()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Año", "Período", "Materia", "Nota"}, rows);
        System.out.println("Total: " + history.size() + " notas");
    }
}
