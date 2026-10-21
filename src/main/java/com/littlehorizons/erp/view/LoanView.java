package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.LoanController;
import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.LoanReturn;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceLoan;
import com.littlehorizons.erp.util.ConsoleUtil;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for lending and returning resources.
 */
public class LoanView extends BaseView {

    private final LoanController loanController;
    private final ResourceController resourceController;
    private final StudentController studentController;
    private final ProfessorController professorController;

    public LoanView(LoanController loanController, ResourceController resourceController,
                    StudentController studentController, ProfessorController professorController) {
        this.loanController = loanController;
        this.resourceController = resourceController;
        this.studentController = studentController;
        this.professorController = professorController;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Préstamos y devoluciones");
            System.out.println("1. Registrar un préstamo");
            System.out.println("2. Registrar una devolución");
            System.out.println("3. Ver los préstamos abiertos");
            System.out.println("4. Ver los préstamos vencidos");
            System.out.println("5. Historial de préstamos de un estudiante o profesor");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 5);
            switch (option) {
                case 1 -> runAndPause(this::lendResource);
                case 2 -> runAndPause(this::returnResource);
                case 3 -> runAndPause(this::showOpenLoans);
                case 4 -> runAndPause(this::showOverdueLoans);
                case 5 -> runAndPause(this::showBorrowerHistory);
                default -> back = true;
            }
        }
    }

    private void lendResource() {
        ConsoleUtil.printTitle("Registrar préstamo");
        BorrowerType type = chooseBorrowerType();
        int borrowerId = readBorrowerId(type);
        Resource resource = readResource();
        ConsoleUtil.printInfo("Recurso: " + resource.getName() + " (" + resource.getStatus() + ")");
        int days = ConsoleUtil.readInt("Días de préstamo (1 a 30): ", 1, 30);
        String notes = ConsoleUtil.readOptionalText("Observaciones", 1000);
        loanController.lend(resource.getId(), type, borrowerId, days, notes);
        ConsoleUtil.printSuccess("Préstamo registrado. Fecha de devolución: " + LocalDate.now().plusDays(days) + ".");
    }

    private void returnResource() {
        Resource resource = readResource();
        LoanReturn result = loanController.giveBack(resource.getId());
        ConsoleUtil.printSuccess("Devolución registrada: " + resource.getName() + " (de " + result.loan().borrowerName() + ").");
        if (result.daysLate() > 0) {
            ConsoleUtil.printInfo("Se devolvió con " + result.daysLate() + " días de retraso.");
        }
    }

    private void showOpenLoans() {
        ConsoleUtil.printTitle("Préstamos abiertos");
        printLoans(loanController.openLoans(), false);
    }

    private void showOverdueLoans() {
        ConsoleUtil.printTitle("Préstamos vencidos");
        printLoans(loanController.overdueLoans(), true);
    }

    private void showBorrowerHistory() {
        BorrowerType type = chooseBorrowerType();
        int borrowerId = readBorrowerId(type);
        List<ResourceLoan> history = loanController.historyOfBorrower(type, borrowerId);
        if (history.isEmpty()) {
            ConsoleUtil.printInfo("No tiene préstamos registrados.");
            return;
        }
        List<String[]> rows = history.stream()
                .map(l -> new String[]{l.resourceCode(), l.resourceName(), String.valueOf(l.loanDate()),
                        String.valueOf(l.dueDate()), l.returnDate() == null ? "-" : String.valueOf(l.returnDate()),
                        l.status()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Código", "Recurso", "Préstamo", "Vence", "Devuelto", "Estado"}, rows);
    }

    // ------------------------------------------------------------------ helpers

    private BorrowerType chooseBorrowerType() {
        List<String> labels = Arrays.stream(BorrowerType.values()).map(BorrowerType::getLabel).collect(Collectors.toList());
        String chosen = ConsoleUtil.readChoice("¿Quién pide el recurso?", labels);
        return BorrowerType.values()[labels.indexOf(chosen)];
    }

    /** Asks for a document and returns the id of the student or professor. */
    private int readBorrowerId(BorrowerType type) {
        String document = ConsoleUtil.readDocument("Documento del " + type.getLabel().toLowerCase() + ": ");
        if (type == BorrowerType.STUDENT) {
            var student = studentController.getByDocument(document);
            ConsoleUtil.printInfo("Estudiante: " + student.getFullName());
            return student.getId();
        }
        var professor = professorController.getByDocument(document);
        ConsoleUtil.printInfo("Profesor: " + professor.getFullName());
        return professor.getId();
    }

    private Resource readResource() {
        return resourceController.getByCode(ConsoleUtil.readRequiredText("Código del recurso (por ejemplo LIB-001): ", 50));
    }

    private void printLoans(List<ResourceLoan> loans, boolean withDaysLate) {
        if (loans.isEmpty()) {
            ConsoleUtil.printSuccess("No hay préstamos en esta lista.");
            return;
        }
        LocalDate today = LocalDate.now();
        List<String[]> rows = loans.stream()
                .map(l -> withDaysLate
                        ? new String[]{l.resourceCode(), l.resourceName(), l.borrowerName(), l.borrowerType().getLabel(),
                                String.valueOf(l.dueDate()),
                                String.valueOf(Math.max(0, ChronoUnit.DAYS.between(l.dueDate(), today)))}
                        : new String[]{l.resourceCode(), l.resourceName(), l.borrowerName(), l.borrowerType().getLabel(),
                                String.valueOf(l.dueDate()), l.status()})
                .collect(Collectors.toList());
        String[] headers = withDaysLate
                ? new String[]{"Código", "Recurso", "Prestatario", "Tipo", "Debía devolver", "Días de retraso"}
                : new String[]{"Código", "Recurso", "Prestatario", "Tipo", "Vence", "Estado"};
        ConsoleUtil.printTable(headers, rows);
        System.out.println("Total: " + loans.size() + " préstamos");
    }
}
