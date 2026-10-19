package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.controller.TeachingController;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.util.ConsoleUtil;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for the academic load of professors: assigned subjects and weekly schedules.
 */
public class TeachingView extends BaseView {

    private final TeachingController teachingController;
    private final ProfessorController professorController;
    private final SubjectSelector subjectSelector;

    public TeachingView(TeachingController teachingController, ProfessorController professorController,
                        SubjectSelector subjectSelector) {
        this.teachingController = teachingController;
        this.professorController = professorController;
        this.subjectSelector = subjectSelector;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Carga académica y horarios");
            System.out.println("1. Asignar materia a un profesor");
            System.out.println("2. Ver las materias de un profesor");
            System.out.println("3. Quitar una materia de un profesor");
            System.out.println("4. Agregar un horario de clase");
            System.out.println("5. Ver el horario semanal de un profesor");
            System.out.println("6. Quitar un horario de clase");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 6);
            switch (option) {
                case 1 -> runAndPause(this::assignSubject);
                case 2 -> runAndPause(this::showLoad);
                case 3 -> runAndPause(this::removeAssignment);
                case 4 -> runAndPause(this::addSchedule);
                case 5 -> runAndPause(this::showTimetable);
                case 6 -> runAndPause(this::removeSchedule);
                default -> back = true;
            }
        }
    }

    /** Prints the subjects a professor teaches in one year (also used by the professor detail screen). */
    public void printLoadOf(int professorId, int academicYear) {
        List<TeachingAssignment> assignments = teachingController.assignmentsOf(professorId, academicYear);
        System.out.println("\nMaterias que dicta en " + academicYear + ":");
        if (assignments.isEmpty()) {
            System.out.println("  (ninguna)");
            return;
        }
        List<String[]> rows = assignments.stream()
                .map(a -> new String[]{String.valueOf(a.id()), a.subjectCode(), a.subjectName(), a.gradeName(),
                        a.section()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"ID asignación", "Código", "Materia", "Grado", "Sección"}, rows);
    }

    // ------------------------------------------------------------------ actions

    private void assignSubject() {
        Professor professor = readProfessor();
        Subject subject = subjectSelector.choose(true);
        int year = readAcademicYear();
        String section = ConsoleUtil.readRequiredText("Sección (por ejemplo A): ", 10);
        teachingController.assign(professor.getId(), subject.getId(), year, section);
        ConsoleUtil.printSuccess(professor.getFullName() + " ahora dicta " + subject.getName() + " en " + year + ".");
    }

    private void showLoad() {
        Professor professor = readProfessor();
        ConsoleUtil.printInfo("Profesor: " + professor.getFullName());
        printLoadOf(professor.getId(), readAcademicYear());
    }

    private void removeAssignment() {
        Professor professor = readProfessor();
        int year = readAcademicYear();
        printLoadOf(professor.getId(), year);
        int assignmentId = ConsoleUtil.readInt("ID de la asignación a quitar: ", 1, Integer.MAX_VALUE);
        if (ConsoleUtil.confirm("¿Quitar la asignación? También se borrarán sus horarios")) {
            teachingController.removeAssignment(professor.getId(), assignmentId);
            ConsoleUtil.printSuccess("Asignación eliminada.");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    private void addSchedule() {
        Professor professor = readProfessor();
        int year = readAcademicYear();
        printLoadOf(professor.getId(), year);
        int assignmentId = ConsoleUtil.readInt("ID de la asignación: ", 1, Integer.MAX_VALUE);
        String day = ConsoleUtil.readChoice("Día de la semana", ClassSchedule.DAYS);
        LocalTime start = ConsoleUtil.readTime("Hora de inicio");
        LocalTime end = ConsoleUtil.readTime("Hora de fin");
        String room = ConsoleUtil.readOptionalText("Aula", 30);
        teachingController.addSchedule(professor.getId(), assignmentId, day, start, end, room);
        ConsoleUtil.printSuccess("Horario agregado: " + day + " de " + start + " a " + end + ".");
    }

    private void showTimetable() {
        Professor professor = readProfessor();
        int year = readAcademicYear();
        ConsoleUtil.printInfo("Horario de " + professor.getFullName() + " - " + year);
        printTimetable(professor.getId(), year);
    }

    private void removeSchedule() {
        Professor professor = readProfessor();
        int year = readAcademicYear();
        if (!printTimetable(professor.getId(), year)) {
            return;
        }
        int scheduleId = ConsoleUtil.readInt("ID del horario a quitar: ", 1, Integer.MAX_VALUE);
        if (ConsoleUtil.confirm("¿Quitar ese horario?")) {
            teachingController.removeSchedule(professor.getId(), year, scheduleId);
            ConsoleUtil.printSuccess("Horario eliminado.");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    // ------------------------------------------------------------------ helpers

    private Professor readProfessor() {
        return professorController.getByDocument(ConsoleUtil.readDocument("Documento del profesor: "));
    }

    /** @return true if there was something to print */
    private boolean printTimetable(int professorId, int year) {
        List<ClassSchedule> timetable = teachingController.timetableOf(professorId, year);
        if (timetable.isEmpty()) {
            ConsoleUtil.printInfo("El profesor no tiene horarios registrados en " + year + ".");
            return false;
        }
        List<String[]> rows = timetable.stream()
                .map(s -> new String[]{String.valueOf(s.id()), s.day(), s.startTime().toString(),
                        s.endTime().toString(), s.subjectName(), s.gradeName(), s.section(), show(s.room())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"ID", "Día", "Inicio", "Fin", "Materia", "Grado", "Sección", "Aula"}, rows);
        return true;
    }
}
