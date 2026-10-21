package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.LoanController;
import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.CollectionSummary;
import com.littlehorizons.erp.model.DebtorSummary;
import com.littlehorizons.erp.model.EventSummary;
import com.littlehorizons.erp.model.GeneralSummary;
import com.littlehorizons.erp.model.GradeAverage;
import com.littlehorizons.erp.model.InventoryRow;
import com.littlehorizons.erp.model.ParticipationSummary;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceLoan;
import com.littlehorizons.erp.model.StudentAverage;
import com.littlehorizons.erp.model.SubjectPerformance;
import com.littlehorizons.erp.util.ConsoleUtil;
import com.littlehorizons.erp.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Console screens for the reports and statistics.
 */
public class ReportView extends BaseView {

    private final ReportController reportController;
    private final ProfessorController professorController;
    private final LoanController loanController;

    public ReportView(ReportController reportController, ProfessorController professorController,
                      LoanController loanController) {
        this.reportController = reportController;
        this.professorController = professorController;
        this.loanController = loanController;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Reportes y Estadísticas");
            System.out.println("1. Resumen general del colegio");
            System.out.println("2. Estudiantes que necesitan apoyo académico");
            System.out.println("3. Promedio por grado");
            System.out.println("4. Rendimiento de los estudiantes de un profesor");
            System.out.println("5. Participación extracurricular");
            System.out.println("6. Recaudos por rango de fechas");
            System.out.println("7. Morosos (más de un mes sin pagar)");
            System.out.println("8. Préstamos vencidos de recursos");
            System.out.println("9. Inventario de recursos por estado");
            System.out.println("0. Volver al menú principal");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 9);
            switch (option) {
                case 1 -> runAndPause(this::showGeneralSummary);
                case 2 -> runAndPause(this::showStudentsNeedingSupport);
                case 3 -> runAndPause(this::showAverageByGrade);
                case 4 -> runAndPause(this::showProfessorPerformance);
                case 5 -> runAndPause(this::showParticipation);
                case 6 -> runAndPause(this::showCollections);
                case 7 -> runAndPause(this::showDebtors);
                case 8 -> runAndPause(this::showOverdueLoans);
                case 9 -> runAndPause(this::showInventory);
                default -> back = true;
            }
        }
    }

    private void showGeneralSummary() {
        GeneralSummary summary = reportController.generalSummary();
        ConsoleUtil.printTitle("Resumen general - " + LocalDate.now());
        System.out.println("Estudiantes por estado:");
        summary.studentsByStatus().forEach((status, count) -> System.out.println("  " + status + ": " + count));
        printLine("Matriculados este año", String.valueOf(summary.enrollmentsThisYear()));
        printLine("Profesores activos", String.valueOf(summary.activeProfessors()));
        printLine("Clubes activos", String.valueOf(summary.activeClubs()));
        printLine("Recaudado este mes", Money.format(summary.collectedThisMonth()));
        printLine("Estudiantes en mora", String.valueOf(summary.debtors()));
        printLine("Préstamos vencidos", String.valueOf(summary.overdueLoans()));
    }

    private void showStudentsNeedingSupport() {
        int year = readAcademicYear();
        BigDecimal limit = ConsoleUtil.readGrade("Promedio mínimo aceptable (por ejemplo 3.0): ");
        List<StudentAverage> students = reportController.studentsNeedingSupport(year, limit);
        ConsoleUtil.printTitle("Estudiantes con promedio menor a " + limit + " en " + year);
        if (students.isEmpty()) {
            ConsoleUtil.printSuccess("Ningún estudiante está por debajo de ese promedio.");
            return;
        }
        printStudentAverages(students);
    }

    private void showAverageByGrade() {
        int year = readAcademicYear();
        List<GradeAverage> averages = reportController.averageByGrade(year);
        ConsoleUtil.printTitle("Promedio por grado - " + year);
        if (averages.isEmpty()) {
            ConsoleUtil.printInfo("No hay notas registradas en ese año.");
            return;
        }
        List<String[]> rows = averages.stream()
                .map(a -> new String[]{a.gradeName(), String.valueOf(a.students()), a.average().toPlainString()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Grado", "Estudiantes con notas", "Promedio"}, rows);
    }

    private void showProfessorPerformance() {
        Professor professor = professorController.getByDocument(ConsoleUtil.readDocument("Documento del profesor: "));
        int year = readAcademicYear();
        BigDecimal limit = ConsoleUtil.readGrade("Promedio mínimo aceptable (por ejemplo 3.0): ");
        ConsoleUtil.printTitle("Rendimiento de los estudiantes de " + professor.getFullName() + " - " + year);

        List<SubjectPerformance> subjects = reportController.professorPerformance(professor.getId(), year);
        if (subjects.isEmpty()) {
            ConsoleUtil.printInfo("El profesor no dicta materias en " + year + ".");
            return;
        }
        List<String[]> rows = subjects.stream()
                .map(s -> new String[]{s.subjectName(), s.gradeName(), String.valueOf(s.students()),
                        s.average() == null ? "-" : s.average().toPlainString(), String.valueOf(s.lowGrades())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Materia", "Grado", "Estudiantes", "Promedio", "Notas bajo 3.0"}, rows);

        List<StudentAverage> low = reportController.professorLowStudents(professor.getId(), year, limit);
        System.out.println("\nEstudiantes que necesitan apoyo adicional (promedio menor a " + limit + "):");
        if (low.isEmpty()) {
            System.out.println("  (ninguno)");
        } else {
            printStudentAverages(low);
        }
    }

    private void showParticipation() {
        List<String> options = List.of("Estudiantes más activos", "Estudiantes sin actividades extracurriculares",
                "Participación por evento", "Ocupación de los clubes");
        String chosen = ConsoleUtil.readChoice("Reporte de participación", options);
        switch (options.indexOf(chosen)) {
            case 0 -> printMostActive();
            case 1 -> printWithoutActivities();
            case 2 -> printEventParticipation();
            default -> printClubOccupancy();
        }
    }

    private void showCollections() {
        LocalDate from = ConsoleUtil.readDate("Fecha inicial");
        LocalDate to = ConsoleUtil.readDate("Fecha final");
        CollectionSummary summary = reportController.collections(from, to);
        ConsoleUtil.printTitle("Recaudos del " + from + " al " + to);
        if (summary.payments() == 0) {
            ConsoleUtil.printInfo("No hay pagos en ese rango de fechas.");
            return;
        }
        System.out.println("Total recaudado: " + Money.format(summary.total()) + " en " + summary.payments() + " pagos");
        System.out.println("\nPor concepto:");
        ConsoleUtil.printTable(new String[]{"Concepto", "Total"}, toRows(summary.byConcept()));
        System.out.println("Por método de pago:");
        ConsoleUtil.printTable(new String[]{"Método", "Total"}, toRows(summary.byMethod()));
    }

    private void showDebtors() {
        List<DebtorSummary> debtors = reportController.debtors();
        ConsoleUtil.printTitle("Morosos (más de un mes sin pagar)");
        if (debtors.isEmpty()) {
            ConsoleUtil.printSuccess("Ningún estudiante tiene cobros vencidos hace más de un mes.");
            return;
        }
        List<String[]> rows = debtors.stream()
                .map(d -> new String[]{d.studentDocument(), d.studentName(), String.valueOf(d.overdueCharges()),
                        Money.format(d.totalOwed()), String.valueOf(d.daysOverdue())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Documento", "Estudiante", "Cobros vencidos", "Total adeudado",
                "Días de mora"}, rows);
        BigDecimal total = debtors.stream().map(DebtorSummary::totalOwed).reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println(debtors.size() + " estudiantes en mora. Total adeudado: " + Money.format(total));
    }

    private void showOverdueLoans() {
        List<ResourceLoan> loans = loanController.overdueLoans();
        ConsoleUtil.printTitle("Préstamos vencidos");
        if (loans.isEmpty()) {
            ConsoleUtil.printSuccess("No hay préstamos vencidos.");
            return;
        }
        LocalDate today = LocalDate.now();
        List<String[]> rows = loans.stream()
                .map(l -> new String[]{l.resourceCode(), l.resourceName(), l.borrowerName(), l.borrowerType().getLabel(),
                        String.valueOf(l.dueDate()), String.valueOf(ChronoUnit.DAYS.between(l.dueDate(), today))})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Código", "Recurso", "Prestatario", "Tipo", "Debía devolver",
                "Días de retraso"}, rows);
    }

    private void showInventory() {
        List<InventoryRow> inventory = reportController.inventory();
        ConsoleUtil.printTitle("Inventario de recursos por estado");
        Map<String, Map<String, Long>> byCategory = new TreeMap<>();
        for (InventoryRow row : inventory) {
            byCategory.computeIfAbsent(row.category(), key -> new TreeMap<>()).put(row.status(), row.count());
        }
        List<String[]> rows = new ArrayList<>();
        long[] totals = new long[Resource.STATUSES.size()];
        byCategory.forEach((category, counts) -> {
            List<String> cells = new ArrayList<>();
            cells.add(category);
            long sum = 0;
            for (int i = 0; i < Resource.STATUSES.size(); i++) {
                long count = counts.getOrDefault(Resource.STATUSES.get(i), 0L);
                cells.add(String.valueOf(count));
                totals[i] += count;
                sum += count;
            }
            cells.add(String.valueOf(sum));
            rows.add(cells.toArray(new String[0]));
        });
        List<String> totalRow = new ArrayList<>();
        totalRow.add("TOTAL");
        long grandTotal = 0;
        for (long total : totals) {
            totalRow.add(String.valueOf(total));
            grandTotal += total;
        }
        totalRow.add(String.valueOf(grandTotal));
        rows.add(totalRow.toArray(new String[0]));

        List<String> headers = new ArrayList<>();
        headers.add("Categoría");
        headers.addAll(Resource.STATUSES);
        headers.add("Total");
        ConsoleUtil.printTable(headers.toArray(new String[0]), rows);
    }

    // ------------------------------------------------------------------ helpers

    private void printStudentAverages(List<StudentAverage> students) {
        List<String[]> rows = students.stream()
                .map(s -> new String[]{s.studentDocument(), s.studentName(), s.gradeName(),
                        s.average().toPlainString(), String.valueOf(s.gradesCount())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Documento", "Estudiante", "Grado", "Promedio", "Notas"}, rows);
        System.out.println("Total: " + students.size() + " estudiantes");
    }

    private void printMostActive() {
        List<ParticipationSummary> list = reportController.mostActiveStudents(10);
        ConsoleUtil.printTitle("Los 10 estudiantes más activos");
        if (list.isEmpty()) {
            ConsoleUtil.printInfo("Todavía no hay participación registrada.");
            return;
        }
        List<String[]> rows = list.stream()
                .map(p -> new String[]{p.studentDocument(), p.studentName(), String.valueOf(p.activeClubs()),
                        String.valueOf(p.events()), String.valueOf(p.podiums())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Documento", "Estudiante", "Clubes activos", "Eventos", "Podios"}, rows);
    }

    private void printWithoutActivities() {
        List<ParticipationSummary> list = reportController.studentsWithoutActivities();
        ConsoleUtil.printTitle("Estudiantes sin actividades extracurriculares");
        if (list.isEmpty()) {
            ConsoleUtil.printSuccess("Todos los estudiantes activos tienen alguna actividad.");
            return;
        }
        List<String[]> rows = list.stream()
                .map(p -> new String[]{p.studentDocument(), p.studentName()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Documento", "Estudiante"}, rows);
        System.out.println("Total: " + list.size() + " estudiantes");
    }

    private void printEventParticipation() {
        int year = readAcademicYear();
        List<EventSummary> events = reportController.eventParticipation(year);
        ConsoleUtil.printTitle("Participación por evento - " + year);
        if (events.isEmpty()) {
            ConsoleUtil.printInfo("No hay eventos en ese año.");
            return;
        }
        List<String[]> rows = events.stream()
                .map(e -> new String[]{String.valueOf(e.date()), e.eventName(), e.type(),
                        e.competition() ? "Sí" : "No", String.valueOf(e.participants())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Fecha", "Evento", "Tipo", "Competencia", "Participantes"}, rows);
    }

    private void printClubOccupancy() {
        List<Club> clubs = reportController.clubOccupancy();
        ConsoleUtil.printTitle("Ocupación de los clubes");
        List<String[]> rows = clubs.stream()
                .map(c -> new String[]{c.getName(), c.getActiveMembers() + "/" + c.getCapacity(),
                        (100 * c.getActiveMembers() / c.getCapacity()) + "%"})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Club", "Miembros/Cupo", "Ocupación"}, rows);
    }

    private List<String[]> toRows(Map<String, BigDecimal> totals) {
        return totals.entrySet().stream()
                .map(entry -> new String[]{entry.getKey(), Money.format(entry.getValue())})
                .collect(Collectors.toList());
    }
}
