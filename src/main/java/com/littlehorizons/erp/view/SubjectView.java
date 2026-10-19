package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.ConsoleUtil;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for the subject (materia) catalog.
 */
public class SubjectView extends BaseView {

    private final SubjectController subjectController;
    private final SubjectSelector subjectSelector;

    public SubjectView(SubjectController subjectController, SubjectSelector subjectSelector) {
        this.subjectController = subjectController;
        this.subjectSelector = subjectSelector;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Catálogo de materias");
            System.out.println("1. Listar materias de un grado");
            System.out.println("2. Registrar materia");
            System.out.println("3. Actualizar materia");
            System.out.println("4. Activar o desactivar materia");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 4);
            switch (option) {
                case 1 -> runAndPause(this::listSubjects);
                case 2 -> runAndPause(this::registerSubject);
                case 3 -> runAndPause(this::updateSubject);
                case 4 -> runAndPause(this::toggleActive);
                default -> back = true;
            }
        }
    }

    private void listSubjects() {
        Grade grade = subjectSelector.chooseGrade();
        List<Subject> subjects = subjectController.listByGrade(grade.getId());
        ConsoleUtil.printTitle("Materias de " + grade.getName());
        if (subjects.isEmpty()) {
            ConsoleUtil.printInfo("Ese grado todavía no tiene materias.");
            return;
        }
        List<String[]> rows = subjects.stream()
                .map(s -> new String[]{s.getCode(), s.getName(), String.valueOf(s.getWeeklyHours()),
                        s.isActive() ? "Activa" : "Inactiva"})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Código", "Materia", "Horas/semana", "Estado"}, rows);
    }

    private void registerSubject() {
        ConsoleUtil.printTitle("Registrar materia");
        Subject subject = new Subject();
        subject.setGradeId(subjectSelector.chooseGrade().getId());
        subject.setCode(ConsoleUtil.readRequiredText("Código de la materia (por ejemplo ART-5): ", 20));
        subject.setName(ConsoleUtil.readRequiredText("Nombre de la materia: ", 100));
        subject.setDescription(ConsoleUtil.readOptionalText("Descripción", 1000));
        subject.setWeeklyHours(ConsoleUtil.readInt("Horas semanales (1 a 10): ", 1, 10));
        int id = subjectController.register(subject);
        ConsoleUtil.printSuccess("Materia registrada con ID " + id + ": " + subject.getCode() + " - " + subject.getName());
    }

    private void updateSubject() {
        Subject subject = subjectSelector.choose(false);
        ConsoleUtil.printInfo("Escriba el nuevo valor o presione ENTER para conservar el actual.");
        subject.setName(ConsoleUtil.readTextKeepingCurrent("Nombre", subject.getName(), 100, true));
        subject.setDescription(ConsoleUtil.readTextKeepingCurrent("Descripción", subject.getDescription(), 1000, false));
        String hours = ConsoleUtil.readKeepingCurrent("Horas semanales", String.valueOf(subject.getWeeklyHours()),
                true, text -> text.matches("\\d{1,2}"), "Escriba un número entero entre 1 y 10.");
        subject.setWeeklyHours(Integer.parseInt(hours));
        subjectController.update(subject);
        ConsoleUtil.printSuccess("Materia actualizada correctamente.");
    }

    private void toggleActive() {
        Subject subject = subjectSelector.choose(false);
        boolean newState = !subject.isActive();
        String action = newState ? "activar" : "desactivar";
        if (ConsoleUtil.confirm("¿Desea " + action + " la materia " + subject.getName() + "?")) {
            subjectController.setActive(subject.getId(), newState);
            ConsoleUtil.printSuccess("Materia " + (newState ? "activada" : "desactivada") + ".");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }
}
