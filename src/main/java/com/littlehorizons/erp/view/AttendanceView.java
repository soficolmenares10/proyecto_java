package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AttendanceController;
import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.AbsenceAlert;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.util.ConsoleUtil;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Console screens for daily attendance and absence reports.
 */
public class AttendanceView extends BaseView {

    private final AttendanceController attendanceController;
    private final StudentController studentController;
    private final ProfessorController professorController;
    private final SubjectSelector subjectSelector;

    public AttendanceView(AttendanceController attendanceController, StudentController studentController,
                          ProfessorController professorController, SubjectSelector subjectSelector) {
        this.attendanceController = attendanceController;
        this.studentController = studentController;
        this.professorController = professorController;
        this.subjectSelector = subjectSelector;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Control de asistencia");
            System.out.println("1. Registrar la asistencia de una clase");
            System.out.println("2. Ver la asistencia de una clase");
            System.out.println("3. Ver la asistencia de un estudiante");
            System.out.println("4. Reporte de inasistencias preocupantes");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 4);
            switch (option) {
                case 1 -> runAndPause(this::registerClassAttendance);
                case 2 -> runAndPause(this::showClassAttendance);
                case 3 -> runAndPause(this::showStudentAttendance);
                case 4 -> runAndPause(this::showAbsenceAlerts);
                default -> back = true;
            }
        }
    }

    private void registerClassAttendance() {
        ConsoleUtil.printTitle("Registrar asistencia de una clase");
        Professor professor = professorController.getByDocument(ConsoleUtil.readDocument("Documento del profesor: "));
        Subject subject = subjectSelector.choose(true);
        LocalDate date = ConsoleUtil.readDate("Fecha de la clase");

        List<SubjectEnrollment> students = attendanceController.studentsOfClass(subject.getId(), date);
        if (students.isEmpty()) {
            ConsoleUtil.printInfo("No hay estudiantes inscritos en " + subject.getName() + " en " + date.getYear() + ".");
            return;
        }
        List<String[]> rows = students.stream()
                .map(s -> new String[]{s.studentDocument(), s.studentName()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Documento", "Estudiante"}, rows);
        ConsoleUtil.printInfo("Todos quedan como Presente. Escriba solo los estudiantes con novedad.");

        Map<Integer, String> exceptions = new LinkedHashMap<>();
        while (true) {
            String document = ConsoleUtil.readOptionalDocument("Documento del estudiante con novedad (ENTER para terminar): ");
            if (document == null) {
                break;
            }
            Optional<SubjectEnrollment> student = students.stream()
                    .filter(s -> s.studentDocument().equals(document))
                    .findFirst();
            if (student.isEmpty()) {
                ConsoleUtil.printError("Ese documento no está en la lista de la clase.");
                continue;
            }
            String status = ConsoleUtil.readChoice("Estado de " + student.get().studentName(), Attendance.EXCEPTIONS);
            exceptions.put(student.get().studentId(), status);
            ConsoleUtil.printInfo("Novedad anotada. Total de novedades: " + exceptions.size());
        }

        Map<String, Long> counts = attendanceController.registerClass(subject.getId(), professor.getId(), date, exceptions);
        ConsoleUtil.printSuccess("Asistencia guardada para " + students.size() + " estudiantes.");
        counts.forEach((status, count) -> System.out.println("  " + status + ": " + count));
    }

    private void showClassAttendance() {
        Subject subject = subjectSelector.choose(false);
        LocalDate date = ConsoleUtil.readDate("Fecha de la clase");
        List<Attendance> attendances = attendanceController.classAttendance(subject.getId(), date);
        ConsoleUtil.printTitle(subject.getName() + " - " + date);
        if (attendances.isEmpty()) {
            ConsoleUtil.printInfo("No hay asistencia registrada para esa clase y fecha.");
            return;
        }
        List<String[]> rows = attendances.stream()
                .map(a -> new String[]{a.studentDocument(), a.studentName(), a.status(), show(a.notes())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Documento", "Estudiante", "Estado", "Observaciones"}, rows);
        attendanceController.countByStatus(attendances)
                .forEach((status, count) -> System.out.println("  " + status + ": " + count));
    }

    private void showStudentAttendance() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        LocalDate from = ConsoleUtil.readDate("Fecha inicial");
        LocalDate to = ConsoleUtil.readDate("Fecha final");
        List<Attendance> attendances = attendanceController.studentAttendance(student.getId(), from, to);
        ConsoleUtil.printTitle("Asistencia de " + student.getFullName());
        if (attendances.isEmpty()) {
            ConsoleUtil.printInfo("No hay asistencia registrada en ese rango de fechas.");
            return;
        }
        List<String[]> rows = attendances.stream()
                .map(a -> new String[]{String.valueOf(a.date()), a.subjectName(), a.status()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Fecha", "Materia", "Estado"}, rows);
        attendanceController.countByStatus(attendances)
                .forEach((status, count) -> System.out.println("  " + status + ": " + count));
    }

    private void showAbsenceAlerts() {
        ConsoleUtil.printTitle("Inasistencias preocupantes");
        LocalDate from = ConsoleUtil.readDate("Fecha inicial");
        LocalDate to = ConsoleUtil.readDate("Fecha final");
        int max = ConsoleUtil.readInt("Máximo de días con ausencia permitidos por semana (por ejemplo 3): ", 0, 5);
        List<AbsenceAlert> alerts = attendanceController.absenceAlerts(from, to, max);
        if (alerts.isEmpty()) {
            ConsoleUtil.printSuccess("Ningún estudiante superó " + max + " días con ausencia en una semana.");
            return;
        }
        List<String[]> rows = alerts.stream()
                .map(a -> new String[]{String.valueOf(a.weekStart()), a.studentDocument(), a.studentName(),
                        String.valueOf(a.absentDays())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Semana (lunes)", "Documento", "Estudiante", "Días ausente"}, rows);
        System.out.println("Se cuentan las ausencias sin justificar (Ausente). Las excusas médicas no suman.");
    }
}
