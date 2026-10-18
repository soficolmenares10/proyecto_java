package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.BusinessException;
import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ConsoleUtil;
import com.littlehorizons.erp.util.Validator;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Console screens for student management.
 */
public class StudentView extends BaseView {

    private final StudentController studentController;
    private final EnrollmentController enrollmentController;
    private final GuardianView guardianView;
    private final EnrollmentView enrollmentView;

    public StudentView(StudentController studentController, EnrollmentController enrollmentController,
                       GuardianView guardianView, EnrollmentView enrollmentView) {
        this.studentController = studentController;
        this.enrollmentController = enrollmentController;
        this.guardianView = guardianView;
        this.enrollmentView = enrollmentView;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Gestión de Estudiantes");
            System.out.println("1. Registrar estudiante");
            System.out.println("2. Listar estudiantes");
            System.out.println("3. Buscar estudiante (nombre o documento)");
            System.out.println("4. Ver detalle de un estudiante");
            System.out.println("5. Actualizar estudiante");
            System.out.println("6. Suspender o reactivar estudiante");
            System.out.println("7. Gestión de acudientes");
            System.out.println("8. Matrículas");
            System.out.println("0. Volver al menú principal");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 8);
            switch (option) {
                case 1 -> runAndPause(this::registerStudent);
                case 2 -> runAndPause(this::listStudents);
                case 3 -> runAndPause(this::searchStudents);
                case 4 -> runAndPause(this::showStudent);
                case 5 -> runAndPause(this::updateStudent);
                case 6 -> runAndPause(this::changeStatus);
                case 7 -> guardianView.run();
                case 8 -> enrollmentView.run();
                default -> back = true;
            }
        }
    }

    // ------------------------------------------------------------------ actions

    private void registerStudent() {
        ConsoleUtil.printTitle("Registrar estudiante");
        Student student = new Student();
        student.setDocumentType(ConsoleUtil.readChoice("Tipo de documento", Student.DOCUMENT_TYPES));
        student.setDocumentNumber(ConsoleUtil.readDocument("Número de documento: "));
        if (studentController.isDocumentTaken(student.getDocumentNumber())) {
            throw new BusinessException("Ya existe un estudiante con ese documento.");
        }
        student.setFirstName(ConsoleUtil.readRequiredText("Primer nombre: ", 100));
        student.setMiddleName(ConsoleUtil.readOptionalText("Segundo nombre", 100));
        student.setLastName(ConsoleUtil.readRequiredText("Primer apellido: ", 100));
        student.setSecondLastName(ConsoleUtil.readOptionalText("Segundo apellido", 100));
        student.setBirthDate(ConsoleUtil.readDate("Fecha de nacimiento"));
        student.setGender(ConsoleUtil.readChoice("Género", Student.GENDERS));
        student.setAddress(ConsoleUtil.readOptionalText("Dirección", 255));
        student.setPhone(ConsoleUtil.readOptionalPhone("Teléfono"));
        student.setEmail(ConsoleUtil.readOptionalEmail("Correo electrónico"));
        student.setBloodType(ConsoleUtil.readOptionalChoice("Tipo de sangre", Student.BLOOD_TYPES));
        student.setAllergies(ConsoleUtil.readOptionalText("Alergias", 500));
        student.setMedicalConditions(ConsoleUtil.readOptionalText("Condiciones médicas", 500));
        student.setAdmissionDate(ConsoleUtil.readDate("Fecha de ingreso al colegio"));

        System.out.println("\nTodo estudiante necesita al menos un acudiente.");
        Integer guardianId = guardianView.selectOrCreateGuardian();
        if (guardianId == null) {
            ConsoleUtil.printInfo("Registro cancelado: el estudiante necesita un acudiente.");
            return;
        }

        int id = studentController.register(student, guardianId);
        ConsoleUtil.printSuccess("Estudiante registrado con ID " + id + ": " + student.getFullName());
        enrollmentView.offerEnrollment(id);
    }

    private void listStudents() {
        ConsoleUtil.printTitle("Lista de estudiantes");
        List<Student> students = studentController.listAll();
        if (students.isEmpty()) {
            ConsoleUtil.printInfo("No hay estudiantes registrados.");
            return;
        }
        printStudentTable(students);
        System.out.println("Total: " + students.size() + " estudiantes");
        studentController.countByStatus()
                .forEach((status, count) -> System.out.println("  " + status + ": " + count));
    }

    private void searchStudents() {
        String text = ConsoleUtil.readRequiredText("Nombre, apellido o documento a buscar: ", 100);
        List<Student> students = studentController.search(text);
        if (students.isEmpty()) {
            ConsoleUtil.printInfo("No se encontraron estudiantes con ese texto.");
            return;
        }
        printStudentTable(students);
        System.out.println("Encontrados: " + students.size());
    }

    private void showStudent() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        ConsoleUtil.printTitle("Detalle del estudiante");
        printLine("ID", String.valueOf(student.getId()));
        printLine("Documento", student.getDocumentType() + " " + student.getDocumentNumber());
        printLine("Nombre completo", student.getFullName());
        printLine("Fecha de nacimiento", student.getBirthDate() + " (" + student.getAge() + " años)");
        printLine("Género", student.getGender());
        printLine("Dirección", student.getAddress());
        printLine("Teléfono", student.getPhone());
        printLine("Correo", student.getEmail());
        printLine("Tipo de sangre", student.getBloodType());
        printLine("Alergias", student.getAllergies());
        printLine("Condiciones médicas", student.getMedicalConditions());
        printLine("Fecha de ingreso", String.valueOf(student.getAdmissionDate()));
        printLine("Estado", student.getStatus());
        guardianView.printGuardiansOf(student.getId());
        enrollmentView.printHistoryOf(student.getId());
    }

    private void updateStudent() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        ConsoleUtil.printInfo("Escriba el nuevo valor o presione ENTER para conservar el actual.");
        student.setDocumentType(ConsoleUtil.readChoiceKeepingCurrent("Tipo de documento",
                Student.DOCUMENT_TYPES, student.getDocumentType()));
        student.setDocumentNumber(ConsoleUtil.readKeepingCurrent("Número de documento",
                student.getDocumentNumber(), true, Validator::isValidDocument,
                "Documento inválido. Use de 5 a 20 letras o números."));
        student.setFirstName(ConsoleUtil.readTextKeepingCurrent("Primer nombre", student.getFirstName(), 100, true));
        student.setMiddleName(ConsoleUtil.readTextKeepingCurrent("Segundo nombre", student.getMiddleName(), 100, false));
        student.setLastName(ConsoleUtil.readTextKeepingCurrent("Primer apellido", student.getLastName(), 100, true));
        student.setSecondLastName(ConsoleUtil.readTextKeepingCurrent("Segundo apellido",
                student.getSecondLastName(), 100, false));
        student.setBirthDate(ConsoleUtil.readDateKeepingCurrent("Fecha de nacimiento", student.getBirthDate()));
        student.setGender(ConsoleUtil.readChoiceKeepingCurrent("Género", Student.GENDERS, student.getGender()));
        student.setAddress(ConsoleUtil.readTextKeepingCurrent("Dirección", student.getAddress(), 255, false));
        student.setPhone(ConsoleUtil.readKeepingCurrent("Teléfono", student.getPhone(), false,
                Validator::isValidPhone, "Teléfono inválido. Use de 7 a 20 dígitos."));
        student.setEmail(ConsoleUtil.readKeepingCurrent("Correo", student.getEmail(), false,
                Validator::isValidEmail, "Correo inválido. Ejemplo: nombre@ejemplo.com"));
        student.setBloodType(ConsoleUtil.readChoiceKeepingCurrent("Tipo de sangre",
                Student.BLOOD_TYPES, student.getBloodType()));
        student.setAllergies(ConsoleUtil.readTextKeepingCurrent("Alergias", student.getAllergies(), 500, false));
        student.setMedicalConditions(ConsoleUtil.readTextKeepingCurrent("Condiciones médicas",
                student.getMedicalConditions(), 500, false));
        student.setAdmissionDate(ConsoleUtil.readDateKeepingCurrent("Fecha de ingreso", student.getAdmissionDate()));

        studentController.update(student);
        ConsoleUtil.printSuccess("Estudiante actualizado correctamente.");
    }

    private void changeStatus() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        ConsoleUtil.printInfo(student.getFullName() + " - estado actual: " + student.getStatus());
        String newStatus = ConsoleUtil.readChoice("Nuevo estado", List.of("Activo", "Suspendido"));
        if (ConsoleUtil.confirm("¿Cambiar el estado a " + newStatus + "?")) {
            studentController.changeStatus(student.getId(), newStatus);
            ConsoleUtil.printSuccess("Estado actualizado a " + newStatus + ".");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    // ------------------------------------------------------------------ helpers

    private void printStudentTable(List<Student> students) {
        Map<Integer, String> gradeByStudent =
                enrollmentController.currentGradeByStudent(LocalDate.now().getYear());
        List<String[]> rows = students.stream()
                .map(s -> new String[]{String.valueOf(s.getId()),
                        s.getDocumentType() + " " + s.getDocumentNumber(),
                        s.getFullName(),
                        String.valueOf(s.getAge()),
                        gradeByStudent.getOrDefault(s.getId(), "Sin matrícula"),
                        s.getStatus()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"ID", "Documento", "Nombre completo", "Edad", "Grado actual", "Estado"},
                rows);
    }
}
