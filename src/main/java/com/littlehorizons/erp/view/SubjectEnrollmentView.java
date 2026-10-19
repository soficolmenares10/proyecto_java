package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.SubjectEnrollmentController;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.util.ConsoleUtil;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for the enrollment of students in subjects, period by period.
 */
public class SubjectEnrollmentView extends BaseView {

    private final SubjectEnrollmentController controller;
    private final StudentController studentController;
    private final SubjectSelector subjectSelector;

    public SubjectEnrollmentView(SubjectEnrollmentController controller, StudentController studentController,
                                 SubjectSelector subjectSelector) {
        this.controller = controller;
        this.studentController = studentController;
        this.subjectSelector = subjectSelector;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Inscripción en materias");
            System.out.println("1. Inscribir estudiante en todas las materias de su grado");
            System.out.println("2. Inscribir estudiante en una materia");
            System.out.println("3. Ver las materias de un estudiante");
            System.out.println("4. Ver los estudiantes inscritos en una materia");
            System.out.println("5. Retirar estudiante de una materia");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 5);
            switch (option) {
                case 1 -> runAndPause(this::enrollInGradeSubjects);
                case 2 -> runAndPause(this::enrollInSubject);
                case 3 -> runAndPause(this::showStudentSubjects);
                case 4 -> runAndPause(this::showSubjectStudents);
                case 5 -> runAndPause(this::withdraw);
                default -> back = true;
            }
        }
    }

    private void enrollInGradeSubjects() {
        Student student = readStudent();
        int year = readAcademicYear();
        String period = readPeriod(false);
        int added = controller.enrollInGradeSubjects(student.getId(), year, period);
        ConsoleUtil.printSuccess("Inscripción completada: " + added + " materias para " + student.getFullName()
                + " (período " + period + " de " + year + ").");
    }

    private void enrollInSubject() {
        Student student = readStudent();
        Subject subject = subjectSelector.choose(true);
        int year = readAcademicYear();
        String period = readPeriod(false);
        controller.enrollInSubject(student.getId(), subject.getId(), year, period);
        ConsoleUtil.printSuccess("Inscripción registrada en " + subject.getName() + " (período " + period + ").");
    }

    private void showStudentSubjects() {
        Student student = readStudent();
        int year = readAcademicYear();
        List<SubjectEnrollment> enrollments = controller.listByStudent(student.getId(), year);
        ConsoleUtil.printInfo("Estudiante: " + student.getFullName());
        if (enrollments.isEmpty()) {
            ConsoleUtil.printInfo("No tiene inscripciones en " + year + ".");
            return;
        }
        List<String[]> rows = enrollments.stream()
                .map(e -> new String[]{e.period(), e.subjectName(), show(e.section()), e.status()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Período", "Materia", "Sección", "Estado"}, rows);
    }

    private void showSubjectStudents() {
        Subject subject = subjectSelector.choose(false);
        int year = readAcademicYear();
        String period = readPeriod(false);
        List<SubjectEnrollment> enrollments = controller.listBySubject(subject.getId(), year, period);
        ConsoleUtil.printTitle(subject.getName() + " - período " + period + " de " + year);
        if (enrollments.isEmpty()) {
            ConsoleUtil.printInfo("No hay estudiantes inscritos.");
            return;
        }
        List<String[]> rows = enrollments.stream()
                .map(e -> new String[]{e.studentDocument(), e.studentName(), show(e.section()), e.status()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Documento", "Estudiante", "Sección", "Estado"}, rows);
        System.out.println("Total: " + enrollments.size() + " estudiantes");
    }

    private void withdraw() {
        Student student = readStudent();
        Subject subject = subjectSelector.choose(false);
        int year = readAcademicYear();
        String period = readPeriod(false);
        if (ConsoleUtil.confirm("¿Retirar a " + student.getFullName() + " de " + subject.getName() + "?")) {
            controller.withdraw(student.getId(), subject.getId(), year, period);
            ConsoleUtil.printSuccess("Inscripción retirada.");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    private Student readStudent() {
        return studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
    }
}
