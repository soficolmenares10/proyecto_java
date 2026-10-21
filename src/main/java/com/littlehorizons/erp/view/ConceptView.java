package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.BusinessException;
import com.littlehorizons.erp.controller.ConceptController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentRate;
import com.littlehorizons.erp.util.ConsoleUtil;
import com.littlehorizons.erp.util.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for the payment concept catalog and for custom amounts per student.
 */
public class ConceptView extends BaseView {

    private final ConceptController conceptController;
    private final StudentController studentController;

    public ConceptView(ConceptController conceptController, StudentController studentController) {
        this.conceptController = conceptController;
        this.studentController = studentController;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Conceptos de pago");
            System.out.println("1. Registrar concepto");
            System.out.println("2. Listar conceptos");
            System.out.println("3. Actualizar concepto");
            System.out.println("4. Activar o desactivar concepto");
            System.out.println("5. Asignar un valor personalizado a un estudiante (hermanos, becas)");
            System.out.println("6. Ver los valores personalizados de un estudiante");
            System.out.println("7. Quitar un valor personalizado");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 7);
            switch (option) {
                case 1 -> runAndPause(this::registerConcept);
                case 2 -> runAndPause(this::listConcepts);
                case 3 -> runAndPause(this::updateConcept);
                case 4 -> runAndPause(this::toggleActive);
                case 5 -> runAndPause(this::setRate);
                case 6 -> runAndPause(this::showRates);
                case 7 -> runAndPause(this::removeRate);
                default -> back = true;
            }
        }
    }

    /** Lets the user pick a concept from the list (used also by other screens). */
    public PaymentConcept chooseConcept(boolean activeOnly, boolean recurringOnly) {
        List<PaymentConcept> concepts = conceptController.listAll().stream()
                .filter(c -> !activeOnly || c.isActive())
                .filter(c -> !recurringOnly || c.isRecurring())
                .collect(Collectors.toList());
        if (concepts.isEmpty()) {
            throw new BusinessException("No hay conceptos disponibles.");
        }
        List<String> labels = concepts.stream()
                .map(c -> c.getCode() + " - " + c.getName() + " (" + Money.format(c.getBaseAmount()) + ")"
                        + (c.isActive() ? "" : " [inactivo]"))
                .collect(Collectors.toList());
        String chosen = ConsoleUtil.readChoice("Concepto", labels);
        return concepts.get(labels.indexOf(chosen));
    }

    private void registerConcept() {
        ConsoleUtil.printTitle("Registrar concepto de pago");
        PaymentConcept concept = new PaymentConcept();
        concept.setCode(ConsoleUtil.readRequiredText("Código del concepto (por ejemplo LIBROS): ", 20));
        concept.setName(ConsoleUtil.readRequiredText("Nombre del concepto: ", 100));
        concept.setDescription(ConsoleUtil.readOptionalText("Descripción", 1000));
        concept.setBaseAmount(ConsoleUtil.readPositiveAmount("Valor base en pesos (por ejemplo 350000): "));
        concept.setRecurring(ConsoleUtil.confirm("¿Es un cobro recurrente (por ejemplo mensual)?"));
        concept.setVatPercent(BigDecimal.valueOf(ConsoleUtil.readInt(
                "Porcentaje de IVA incluido en el valor (0 si no aplica, máximo 100): ", 0, 100)));
        int id = conceptController.register(concept);
        ConsoleUtil.printSuccess("Concepto registrado con ID " + id + ": " + concept.getCode() + " - " + concept.getName());
    }

    private void listConcepts() {
        ConsoleUtil.printTitle("Catálogo de conceptos");
        List<PaymentConcept> concepts = conceptController.listAll();
        if (concepts.isEmpty()) {
            ConsoleUtil.printInfo("No hay conceptos registrados.");
            return;
        }
        List<String[]> rows = concepts.stream()
                .map(c -> new String[]{c.getCode(), c.getName(), Money.format(c.getBaseAmount()),
                        c.isRecurring() ? "Sí" : "No", c.getVatPercent().stripTrailingZeros().toPlainString() + "%",
                        c.isActive() ? "Activo" : "Inactivo"})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Código", "Concepto", "Valor base", "Recurrente", "IVA", "Estado"}, rows);
    }

    private void updateConcept() {
        PaymentConcept concept = chooseConcept(false, false);
        ConsoleUtil.printInfo("Escriba el nuevo valor o presione ENTER para conservar el actual.");
        concept.setName(ConsoleUtil.readTextKeepingCurrent("Nombre", concept.getName(), 100, true));
        concept.setDescription(ConsoleUtil.readTextKeepingCurrent("Descripción", concept.getDescription(), 1000, false));
        String amount = ConsoleUtil.readKeepingCurrent("Valor base", concept.getBaseAmount().toPlainString(), true,
                text -> text.matches("\\d{1,8}(\\.\\d{1,2})?"), "Escriba un valor numérico, por ejemplo 350000.");
        concept.setBaseAmount(new BigDecimal(amount));
        concept.setRecurring(ConsoleUtil.confirm("¿Es recurrente? (actualmente " + (concept.isRecurring() ? "sí" : "no") + ")"));
        String vat = ConsoleUtil.readKeepingCurrent("IVA (%)", concept.getVatPercent().stripTrailingZeros().toPlainString(),
                true, text -> text.matches("\\d{1,3}(\\.\\d{1,2})?"), "Escriba un porcentaje, por ejemplo 19.");
        concept.setVatPercent(new BigDecimal(vat));
        conceptController.update(concept);
        ConsoleUtil.printSuccess("Concepto actualizado correctamente.");
    }

    private void toggleActive() {
        PaymentConcept concept = chooseConcept(false, false);
        boolean newState = !concept.isActive();
        if (ConsoleUtil.confirm("¿Desea " + (newState ? "activar" : "desactivar") + " el concepto " + concept.getName() + "?")) {
            conceptController.setActive(concept.getId(), newState);
            ConsoleUtil.printSuccess("Concepto " + (newState ? "activado" : "desactivado") + ".");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    private void setRate() {
        Student student = readStudent();
        PaymentConcept concept = chooseConcept(true, false);
        int year = readAcademicYear();
        ConsoleUtil.printInfo("Valor actual para el estudiante: "
                + Money.format(conceptController.effectiveAmount(student.getId(), concept.getId(), year)));
        BigDecimal amount = ConsoleUtil.readPositiveAmount("Nuevo valor personalizado en pesos: ");
        String reason = ConsoleUtil.readRequiredText("Motivo (por ejemplo Beca parcial 30%): ", 150);
        conceptController.setStudentRate(student.getId(), concept.getId(), year, amount, reason);
        ConsoleUtil.printSuccess("Valor personalizado guardado: " + Money.format(amount) + " para "
                + student.getFullName() + " en " + concept.getName() + " (" + year + ").");
    }

    private void showRates() {
        Student student = readStudent();
        if (!printRates(student)) {
            ConsoleUtil.printInfo("El estudiante no tiene valores personalizados.");
        }
    }

    private void removeRate() {
        Student student = readStudent();
        if (!printRates(student)) {
            ConsoleUtil.printInfo("El estudiante no tiene valores personalizados.");
            return;
        }
        int rateId = ConsoleUtil.readInt("ID del valor personalizado a quitar: ", 1, Integer.MAX_VALUE);
        if (ConsoleUtil.confirm("¿Quitar ese valor personalizado? Se volverá a cobrar el valor base")) {
            conceptController.removeStudentRate(student.getId(), rateId);
            ConsoleUtil.printSuccess("Valor personalizado desactivado.");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    /** @return true if the student has custom values */
    private boolean printRates(Student student) {
        List<StudentRate> rates = conceptController.ratesOfStudent(student.getId());
        ConsoleUtil.printInfo("Estudiante: " + student.getFullName());
        if (rates.isEmpty()) {
            return false;
        }
        List<String[]> rows = rates.stream()
                .map(r -> new String[]{String.valueOf(r.id()), String.valueOf(r.academicYear()), r.conceptName(),
                        Money.format(r.amount()), r.reason(), r.active() ? "Activo" : "Inactivo"})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"ID", "Año", "Concepto", "Valor", "Motivo", "Estado"}, rows);
        return true;
    }

    private Student readStudent() {
        return studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
    }
}
