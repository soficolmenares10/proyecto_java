package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.BusinessException;
import com.littlehorizons.erp.controller.ChargeController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.ChargeGenerationResult;
import com.littlehorizons.erp.model.DebtorSummary;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PendingCharge;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ConsoleUtil;
import com.littlehorizons.erp.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for the charges billed to students and for the debtors (morosos) report.
 */
public class ChargeView extends BaseView {

    private final ChargeController chargeController;
    private final StudentController studentController;
    private final ConceptView conceptView;

    public ChargeView(ChargeController chargeController, StudentController studentController,
                      ConceptView conceptView) {
        this.chargeController = chargeController;
        this.studentController = studentController;
        this.conceptView = conceptView;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Cobros y morosos");
            System.out.println("1. Generar los cobros de un mes (todos los estudiantes matriculados)");
            System.out.println("2. Crear un cobro individual");
            System.out.println("3. Ver los cobros pendientes de un estudiante");
            System.out.println("4. Reporte de morosos (más de un mes sin pagar)");
            System.out.println("5. Eliminar un cobro pendiente");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 5);
            switch (option) {
                case 1 -> runAndPause(this::generateMonthly);
                case 2 -> runAndPause(this::createCharge);
                case 3 -> runAndPause(this::showPending);
                case 4 -> runAndPause(this::showDebtors);
                case 5 -> runAndPause(this::deleteCharge);
                default -> back = true;
            }
        }
    }

    private void generateMonthly() {
        PaymentConcept concept = conceptView.chooseConcept(true, true);
        int year = readAcademicYear();
        int month = ConsoleUtil.readInt("Mes (1 a 12): ", 1, 12);
        if (!ConsoleUtil.confirm("¿Generar el cobro de " + concept.getName() + " del mes " + month + "/" + year
                + " para todos los estudiantes matriculados?")) {
            ConsoleUtil.printInfo("Operación cancelada.");
            return;
        }
        ChargeGenerationResult result = chargeController.generateMonthly(concept.getId(), year, month);
        ConsoleUtil.printSuccess("Cobros creados: " + result.created() + ". Ya existían: " + result.alreadyExisted() + ".");
    }

    private void createCharge() {
        Student student = readStudent();
        PaymentConcept concept = conceptView.chooseConcept(true, false);
        int year = readAcademicYear();
        String description = ConsoleUtil.readRequiredText("Descripción del cobro (por ejemplo Uniforme 2026): ", 60);
        LocalDate dueDate = ConsoleUtil.readDate("Fecha de vencimiento");
        int id = chargeController.create(student.getId(), concept.getId(), year, description, dueDate);
        ConsoleUtil.printSuccess("Cobro creado con ID " + id + " para " + student.getFullName() + ".");
    }

    private void showPending() {
        Student student = readStudent();
        ConsoleUtil.printInfo("Estudiante: " + student.getFullName());
        List<PendingCharge> pending = chargeController.pendingOf(student.getId());
        printPending(pending);
    }

    private void showDebtors() {
        ConsoleUtil.printTitle("Reporte de morosos");
        List<DebtorSummary> debtors = chargeController.debtors();
        if (debtors.isEmpty()) {
            ConsoleUtil.printSuccess("Ningún estudiante tiene cobros vencidos hace más de un mes.");
            return;
        }
        List<String[]> rows = debtors.stream()
                .map(d -> new String[]{d.studentDocument(), d.studentName(), String.valueOf(d.overdueCharges()),
                        Money.format(d.totalOwed()), String.valueOf(d.oldestDueDate()), String.valueOf(d.daysOverdue())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Documento", "Estudiante", "Cobros vencidos", "Total adeudado",
                "Vencido desde", "Días de mora"}, rows);
        BigDecimal total = debtors.stream().map(DebtorSummary::totalOwed).reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println(debtors.size() + " estudiantes en mora. Total adeudado: " + Money.format(total));
    }

    private void deleteCharge() {
        Student student = readStudent();
        List<PendingCharge> pending = chargeController.pendingOf(student.getId());
        if (pending.isEmpty()) {
            throw new BusinessException("El estudiante no tiene cobros pendientes.");
        }
        printPending(pending);
        int id = ConsoleUtil.readInt("ID del cobro a eliminar: ", 1, Integer.MAX_VALUE);
        boolean listed = pending.stream().anyMatch(charge -> charge.id() == id);
        if (!listed) {
            throw new BusinessException("Ese ID no está en la lista de cobros pendientes del estudiante.");
        }
        if (ConsoleUtil.confirm("¿Eliminar ese cobro?")) {
            chargeController.deletePending(id);
            ConsoleUtil.printSuccess("Cobro eliminado.");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    private void printPending(List<PendingCharge> pending) {
        if (pending.isEmpty()) {
            ConsoleUtil.printInfo("No tiene cobros pendientes.");
            return;
        }
        LocalDate today = LocalDate.now();
        List<String[]> rows = pending.stream()
                .map(c -> new String[]{String.valueOf(c.id()), c.description(), String.valueOf(c.dueDate()),
                        Money.format(c.expectedAmount()),
                        String.valueOf(Math.max(0, ChronoUnit.DAYS.between(c.dueDate(), today)))})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"ID", "Cobro", "Vence", "Valor", "Días de mora"}, rows);
        BigDecimal total = pending.stream().map(PendingCharge::expectedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("Total pendiente: " + Money.format(total));
    }

    private Student readStudent() {
        return studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
    }
}
