package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.BusinessException;
import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.util.ConsoleUtil;
import com.littlehorizons.erp.util.Validator;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for professor management.
 */
public class ProfessorView extends BaseView {

    private final ProfessorController professorController;
    private final TeachingView teachingView;

    public ProfessorView(ProfessorController professorController, TeachingView teachingView) {
        this.professorController = professorController;
        this.teachingView = teachingView;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Gestión de Profesores");
            System.out.println("1. Registrar profesor");
            System.out.println("2. Listar profesores");
            System.out.println("3. Buscar profesor (nombre, documento o especialización)");
            System.out.println("4. Ver detalle de un profesor");
            System.out.println("5. Actualizar profesor");
            System.out.println("6. Cambiar estado (Activo, Inactivo, Licencia)");
            System.out.println("7. Carga académica y horarios");
            System.out.println("0. Volver al menú principal");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 7);
            switch (option) {
                case 1 -> runAndPause(this::registerProfessor);
                case 2 -> runAndPause(this::listProfessors);
                case 3 -> runAndPause(this::searchProfessors);
                case 4 -> runAndPause(this::showProfessor);
                case 5 -> runAndPause(this::updateProfessor);
                case 6 -> runAndPause(this::changeStatus);
                case 7 -> teachingView.run();
                default -> back = true;
            }
        }
    }

    private void registerProfessor() {
        ConsoleUtil.printTitle("Registrar profesor");
        Professor professor = new Professor();
        professor.setDocumentType(ConsoleUtil.readChoice("Tipo de documento", Professor.DOCUMENT_TYPES));
        professor.setDocumentNumber(ConsoleUtil.readDocument("Número de documento: "));
        if (professorController.isDocumentTaken(professor.getDocumentNumber())) {
            throw new BusinessException("Ya existe un profesor con ese documento.");
        }
        professor.setFirstName(ConsoleUtil.readRequiredText("Primer nombre: ", 100));
        professor.setMiddleName(ConsoleUtil.readOptionalText("Segundo nombre", 100));
        professor.setLastName(ConsoleUtil.readRequiredText("Primer apellido: ", 100));
        professor.setSecondLastName(ConsoleUtil.readOptionalText("Segundo apellido", 100));
        professor.setEmail(ConsoleUtil.readEmail("Correo electrónico: "));
        professor.setPhone(ConsoleUtil.readOptionalPhone("Teléfono"));
        professor.setSpecialization(ConsoleUtil.readOptionalText("Especialización (por ejemplo Matemáticas)", 100));
        professor.setHireDate(ConsoleUtil.readDate("Fecha de contratación"));

        int id = professorController.register(professor);
        ConsoleUtil.printSuccess("Profesor registrado con ID " + id + ": " + professor.getFullName());
    }

    private void listProfessors() {
        ConsoleUtil.printTitle("Lista de profesores");
        List<Professor> professors = professorController.listAll();
        if (professors.isEmpty()) {
            ConsoleUtil.printInfo("No hay profesores registrados.");
            return;
        }
        printTable(professors);
        System.out.println("Total: " + professors.size() + " profesores");
    }

    private void searchProfessors() {
        String text = ConsoleUtil.readRequiredText("Texto a buscar: ", 100);
        List<Professor> professors = professorController.search(text);
        if (professors.isEmpty()) {
            ConsoleUtil.printInfo("No se encontraron profesores con ese texto.");
            return;
        }
        printTable(professors);
        System.out.println("Encontrados: " + professors.size());
    }

    private void showProfessor() {
        Professor professor = professorController.getByDocument(ConsoleUtil.readDocument("Documento del profesor: "));
        ConsoleUtil.printTitle("Detalle del profesor");
        printLine("ID", String.valueOf(professor.getId()));
        printLine("Documento", professor.getDocumentType() + " " + professor.getDocumentNumber());
        printLine("Nombre completo", professor.getFullName());
        printLine("Correo", professor.getEmail());
        printLine("Teléfono", professor.getPhone());
        printLine("Especialización", professor.getSpecialization());
        printLine("Fecha de contratación", String.valueOf(professor.getHireDate()));
        printLine("Estado", professor.getStatus());
        teachingView.printLoadOf(professor.getId(), LocalDate.now().getYear());
    }

    private void updateProfessor() {
        Professor professor = professorController.getByDocument(ConsoleUtil.readDocument("Documento del profesor: "));
        ConsoleUtil.printInfo("Escriba el nuevo valor o presione ENTER para conservar el actual.");
        professor.setDocumentType(ConsoleUtil.readChoiceKeepingCurrent("Tipo de documento",
                Professor.DOCUMENT_TYPES, professor.getDocumentType()));
        professor.setDocumentNumber(ConsoleUtil.readKeepingCurrent("Número de documento",
                professor.getDocumentNumber(), true, Validator::isValidDocument,
                "Documento inválido. Use de 5 a 20 letras o números."));
        professor.setFirstName(ConsoleUtil.readTextKeepingCurrent("Primer nombre", professor.getFirstName(), 100, true));
        professor.setMiddleName(ConsoleUtil.readTextKeepingCurrent("Segundo nombre", professor.getMiddleName(), 100, false));
        professor.setLastName(ConsoleUtil.readTextKeepingCurrent("Primer apellido", professor.getLastName(), 100, true));
        professor.setSecondLastName(ConsoleUtil.readTextKeepingCurrent("Segundo apellido",
                professor.getSecondLastName(), 100, false));
        professor.setEmail(ConsoleUtil.readKeepingCurrent("Correo", professor.getEmail(), true,
                Validator::isValidEmail, "Correo inválido. Ejemplo: nombre@ejemplo.com"));
        professor.setPhone(ConsoleUtil.readKeepingCurrent("Teléfono", professor.getPhone(), false,
                Validator::isValidPhone, "Teléfono inválido. Use de 7 a 20 dígitos."));
        professor.setSpecialization(ConsoleUtil.readTextKeepingCurrent("Especialización",
                professor.getSpecialization(), 100, false));
        professor.setHireDate(ConsoleUtil.readDateKeepingCurrent("Fecha de contratación", professor.getHireDate()));

        professorController.update(professor);
        ConsoleUtil.printSuccess("Profesor actualizado correctamente.");
    }

    private void changeStatus() {
        Professor professor = professorController.getByDocument(ConsoleUtil.readDocument("Documento del profesor: "));
        ConsoleUtil.printInfo(professor.getFullName() + " - estado actual: " + professor.getStatus());
        String newStatus = ConsoleUtil.readChoice("Nuevo estado", Professor.STATUSES);
        if (ConsoleUtil.confirm("¿Cambiar el estado a " + newStatus + "?")) {
            professorController.changeStatus(professor.getId(), newStatus);
            ConsoleUtil.printSuccess("Estado actualizado a " + newStatus + ".");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    private void printTable(List<Professor> professors) {
        List<String[]> rows = professors.stream()
                .map(p -> new String[]{String.valueOf(p.getId()), p.getDocumentType() + " " + p.getDocumentNumber(),
                        p.getFullName(), show(p.getSpecialization()), p.getStatus()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"ID", "Documento", "Nombre completo", "Especialización", "Estado"}, rows);
    }
}
