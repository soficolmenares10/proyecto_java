package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.BusinessException;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ConsoleUtil;
import com.littlehorizons.erp.util.Validator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for guardians (acudientes).
 */
public class GuardianView extends BaseView {

    private static final String[] GUARDIAN_HEADERS =
            {"Documento", "Nombre completo", "Parentesco", "Teléfono", "Resp. financiero"};

    private final GuardianController guardianController;
    private final StudentController studentController;

    public GuardianView(GuardianController guardianController, StudentController studentController) {
        this.guardianController = guardianController;
        this.studentController = studentController;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Gestión de Acudientes");
            System.out.println("1. Registrar acudiente");
            System.out.println("2. Listar acudientes");
            System.out.println("3. Ver acudiente y sus estudiantes");
            System.out.println("4. Actualizar acudiente");
            System.out.println("5. Asociar acudiente a un estudiante");
            System.out.println("6. Quitar acudiente de un estudiante");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 6);
            switch (option) {
                case 1 -> runAndPause(this::registerGuardian);
                case 2 -> runAndPause(this::listGuardians);
                case 3 -> runAndPause(this::showGuardian);
                case 4 -> runAndPause(this::updateGuardian);
                case 5 -> runAndPause(this::linkGuardian);
                case 6 -> runAndPause(this::unlinkGuardian);
                default -> back = true;
            }
        }
    }

    /**
     * Lets the user pick an existing guardian or register a new one.
     *
     * @return the guardian id, or null if the user cancelled or something failed
     */
    public Integer selectOrCreateGuardian() {
        ConsoleUtil.printTitle("Acudiente del estudiante");
        System.out.println("1. Usar un acudiente que ya existe");
        System.out.println("2. Registrar un acudiente nuevo");
        System.out.println("0. Cancelar");
        int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 2);
        if (option == 1) {
            return callSafely(() -> {
                Guardian guardian = guardianController.getByDocument(
                        ConsoleUtil.readDocument("Documento del acudiente: "));
                ConsoleUtil.printInfo("Acudiente seleccionado: " + guardian.getFullName()
                        + " (" + guardian.getRelationship() + ")");
                return guardian.getId();
            });
        }
        if (option == 2) {
            return callSafely(this::registerGuardian);
        }
        return null;
    }

    /** Prints the guardians of one student (used by the student detail screen). */
    public void printGuardiansOf(int studentId) {
        List<Guardian> guardians = guardianController.getGuardiansOfStudent(studentId);
        System.out.println("\nAcudientes:");
        if (guardians.isEmpty()) {
            System.out.println("  (sin acudientes)");
            return;
        }
        String[] headers = {"Documento", "Nombre completo", "Parentesco", "Teléfono", "Contacto principal"};
        List<String[]> rows = guardians.stream()
                .map(g -> new String[]{g.getDocumentType() + " " + g.getDocumentNumber(), g.getFullName(),
                        g.getRelationship(), g.getPrimaryPhone(), g.isPrimaryForStudent() ? "Sí" : "No"})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(headers, rows);
    }

    // ------------------------------------------------------------------ actions

    private int registerGuardian() {
        ConsoleUtil.printTitle("Registrar acudiente");
        Guardian guardian = new Guardian();
        guardian.setDocumentType(ConsoleUtil.readChoice("Tipo de documento", Guardian.DOCUMENT_TYPES));
        guardian.setDocumentNumber(ConsoleUtil.readDocument("Número de documento: "));
        if (guardianController.isDocumentTaken(guardian.getDocumentNumber())) {
            throw new BusinessException("Ya existe un acudiente con ese documento. "
                    + "Use la opción de acudiente existente.");
        }
        guardian.setFirstName(ConsoleUtil.readRequiredText("Primer nombre: ", 100));
        guardian.setMiddleName(ConsoleUtil.readOptionalText("Segundo nombre", 100));
        guardian.setLastName(ConsoleUtil.readRequiredText("Primer apellido: ", 100));
        guardian.setSecondLastName(ConsoleUtil.readOptionalText("Segundo apellido", 100));
        guardian.setRelationship(ConsoleUtil.readChoice("Parentesco", Guardian.RELATIONSHIPS));
        guardian.setPrimaryPhone(ConsoleUtil.readPhone("Teléfono principal: "));
        guardian.setSecondaryPhone(ConsoleUtil.readOptionalPhone("Teléfono secundario"));
        guardian.setEmail(ConsoleUtil.readOptionalEmail("Correo electrónico"));
        guardian.setAddress(ConsoleUtil.readOptionalText("Dirección", 255));
        guardian.setOccupation(ConsoleUtil.readOptionalText("Ocupación", 100));
        guardian.setFinancialResponsible(ConsoleUtil.confirm("¿Es el responsable de los pagos?"));

        int id = guardianController.register(guardian);
        ConsoleUtil.printSuccess("Acudiente registrado con ID " + id + ": " + guardian.getFullName());
        return id;
    }

    private void listGuardians() {
        ConsoleUtil.printTitle("Lista de acudientes");
        List<Guardian> guardians = guardianController.listAll();
        if (guardians.isEmpty()) {
            ConsoleUtil.printInfo("No hay acudientes registrados.");
            return;
        }
        List<String[]> rows = guardians.stream()
                .map(g -> new String[]{g.getDocumentType() + " " + g.getDocumentNumber(), g.getFullName(),
                        g.getRelationship(), g.getPrimaryPhone(), g.isFinancialResponsible() ? "Sí" : "No"})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(GUARDIAN_HEADERS, rows);
        System.out.println("Total: " + guardians.size() + " acudientes");
    }

    private void showGuardian() {
        Guardian guardian = guardianController.getByDocument(ConsoleUtil.readDocument("Documento del acudiente: "));
        ConsoleUtil.printTitle("Acudiente");
        printLine("Documento", guardian.getDocumentType() + " " + guardian.getDocumentNumber());
        printLine("Nombre", guardian.getFullName());
        printLine("Parentesco", guardian.getRelationship());
        printLine("Teléfono principal", guardian.getPrimaryPhone());
        printLine("Teléfono secundario", guardian.getSecondaryPhone());
        printLine("Correo", guardian.getEmail());
        printLine("Dirección", guardian.getAddress());
        printLine("Ocupación", guardian.getOccupation());
        printLine("Responsable de pagos", guardian.isFinancialResponsible() ? "Sí" : "No");

        List<Student> students = guardianController.getStudentsOfGuardian(guardian.getId());
        System.out.println("\nEstudiantes a su cargo:");
        if (students.isEmpty()) {
            System.out.println("  (ninguno)");
            return;
        }
        List<String[]> rows = students.stream()
                .map(s -> new String[]{s.getDocumentType() + " " + s.getDocumentNumber(), s.getFullName(),
                        s.getStatus()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Documento", "Estudiante", "Estado"}, rows);
    }

    private void updateGuardian() {
        Guardian guardian = guardianController.getByDocument(ConsoleUtil.readDocument("Documento del acudiente: "));
        ConsoleUtil.printInfo("Escriba el nuevo valor o presione ENTER para conservar el actual.");
        guardian.setDocumentType(ConsoleUtil.readChoiceKeepingCurrent("Tipo de documento",
                Guardian.DOCUMENT_TYPES, guardian.getDocumentType()));
        guardian.setDocumentNumber(ConsoleUtil.readKeepingCurrent("Número de documento",
                guardian.getDocumentNumber(), true, Validator::isValidDocument,
                "Documento inválido. Use de 5 a 20 letras o números."));
        guardian.setFirstName(ConsoleUtil.readTextKeepingCurrent("Primer nombre", guardian.getFirstName(), 100, true));
        guardian.setMiddleName(ConsoleUtil.readTextKeepingCurrent("Segundo nombre", guardian.getMiddleName(), 100, false));
        guardian.setLastName(ConsoleUtil.readTextKeepingCurrent("Primer apellido", guardian.getLastName(), 100, true));
        guardian.setSecondLastName(ConsoleUtil.readTextKeepingCurrent("Segundo apellido",
                guardian.getSecondLastName(), 100, false));
        guardian.setRelationship(ConsoleUtil.readChoiceKeepingCurrent("Parentesco",
                Guardian.RELATIONSHIPS, guardian.getRelationship()));
        guardian.setPrimaryPhone(ConsoleUtil.readKeepingCurrent("Teléfono principal", guardian.getPrimaryPhone(),
                true, Validator::isValidPhone, "Teléfono inválido. Use de 7 a 20 dígitos."));
        guardian.setSecondaryPhone(ConsoleUtil.readKeepingCurrent("Teléfono secundario",
                guardian.getSecondaryPhone(), false, Validator::isValidPhone,
                "Teléfono inválido. Use de 7 a 20 dígitos."));
        guardian.setEmail(ConsoleUtil.readKeepingCurrent("Correo", guardian.getEmail(), false,
                Validator::isValidEmail, "Correo inválido. Ejemplo: nombre@ejemplo.com"));
        guardian.setAddress(ConsoleUtil.readTextKeepingCurrent("Dirección", guardian.getAddress(), 255, false));
        guardian.setOccupation(ConsoleUtil.readTextKeepingCurrent("Ocupación", guardian.getOccupation(), 100, false));

        guardianController.update(guardian);
        ConsoleUtil.printSuccess("Acudiente actualizado correctamente.");
    }

    private void linkGuardian() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        Guardian guardian = guardianController.getByDocument(ConsoleUtil.readDocument("Documento del acudiente: "));
        ConsoleUtil.printInfo(guardian.getFullName() + " -> " + student.getFullName());
        boolean primary = ConsoleUtil.confirm("¿Será el contacto principal del estudiante?");
        boolean emergency = ConsoleUtil.confirm("¿Es contacto de emergencia?");
        guardianController.link(student.getId(), guardian.getId(), primary, emergency);
        ConsoleUtil.printSuccess("Acudiente asociado correctamente.");
    }

    private void unlinkGuardian() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        printGuardiansOf(student.getId());
        Guardian guardian = guardianController.getByDocument(
                ConsoleUtil.readDocument("Documento del acudiente a quitar: "));
        if (ConsoleUtil.confirm("¿Quitar a " + guardian.getFullName() + " de " + student.getFullName() + "?")) {
            guardianController.unlink(student.getId(), guardian.getId());
            ConsoleUtil.printSuccess("Acudiente quitado correctamente.");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }
}
