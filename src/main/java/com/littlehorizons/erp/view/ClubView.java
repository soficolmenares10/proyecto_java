package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.BusinessException;
import com.littlehorizons.erp.controller.ClubController;
import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ConsoleUtil;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for clubs and their members.
 */
public class ClubView extends BaseView {

    private final ClubController clubController;
    private final ProfessorController professorController;
    private final StudentController studentController;

    public ClubView(ClubController clubController, ProfessorController professorController,
                    StudentController studentController) {
        this.clubController = clubController;
        this.professorController = professorController;
        this.studentController = studentController;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Clubes");
            System.out.println("1. Registrar club");
            System.out.println("2. Listar clubes y cupos");
            System.out.println("3. Ver detalle de un club y sus miembros");
            System.out.println("4. Actualizar club");
            System.out.println("5. Activar o desactivar club");
            System.out.println("6. Inscribir estudiante en un club");
            System.out.println("7. Retirar estudiante de un club");
            System.out.println("8. Ver los clubes de un estudiante");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 8);
            switch (option) {
                case 1 -> runAndPause(this::registerClub);
                case 2 -> runAndPause(this::listClubs);
                case 3 -> runAndPause(this::showClub);
                case 4 -> runAndPause(this::updateClub);
                case 5 -> runAndPause(this::toggleActive);
                case 6 -> runAndPause(this::enrollStudent);
                case 7 -> runAndPause(this::withdrawStudent);
                case 8 -> runAndPause(this::showStudentClubs);
                default -> back = true;
            }
        }
    }

    // ------------------------------------------------------------------ actions

    private void registerClub() {
        ConsoleUtil.printTitle("Registrar club");
        Club club = new Club();
        club.setName(ConsoleUtil.readRequiredText("Nombre del club: ", 100));
        club.setDescription(ConsoleUtil.readOptionalText("Descripción", 1000));
        Professor supervisor = professorController.getByDocument(
                ConsoleUtil.readDocument("Documento del profesor encargado: "));
        ConsoleUtil.printInfo("Encargado: " + supervisor.getFullName());
        club.setSupervisorId(supervisor.getId());
        club.setCapacity(ConsoleUtil.readInt("Cupo máximo de estudiantes (1 a 200): ", 1, 200));
        club.setMeetingSchedule(ConsoleUtil.readOptionalText("Horario de reunión (por ejemplo Martes 3-5 PM)", 100));

        int id = clubController.register(club);
        ConsoleUtil.printSuccess("Club registrado con ID " + id + ": " + club.getName());
    }

    private void listClubs() {
        ConsoleUtil.printTitle("Clubes extracurriculares");
        List<Club> clubs = clubController.listAll();
        if (clubs.isEmpty()) {
            ConsoleUtil.printInfo("No hay clubes registrados.");
            return;
        }
        List<String[]> rows = clubs.stream()
                .map(c -> new String[]{String.valueOf(c.getId()), c.getName(), show(c.getSupervisorName()),
                        c.getActiveMembers() + "/" + c.getCapacity(), String.valueOf(c.getAvailableSpots()),
                        show(c.getMeetingSchedule()), c.isActive() ? "Activo" : "Inactivo"})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"ID", "Club", "Encargado", "Miembros/Cupo", "Cupos libres",
                "Horario", "Estado"}, rows);
        System.out.println("Total: " + clubs.size() + " clubes");
    }

    private void showClub() {
        Club club = chooseClub(false);
        ConsoleUtil.printTitle(club.getName());
        printLine("Descripción", club.getDescription());
        printLine("Encargado", club.getSupervisorName());
        printLine("Horario de reunión", club.getMeetingSchedule());
        printLine("Cupo", club.getActiveMembers() + " de " + club.getCapacity()
                + " (" + club.getAvailableSpots() + " libres)");
        printLine("Estado", club.isActive() ? "Activo" : "Inactivo");

        List<ClubMembership> members = clubController.members(club.getId(), true);
        System.out.println("\nMiembros activos:");
        if (members.isEmpty()) {
            System.out.println("  (ninguno)");
            return;
        }
        List<String[]> rows = members.stream()
                .map(m -> new String[]{m.studentDocument(), m.studentName(), String.valueOf(m.joinDate())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Documento", "Estudiante", "Fecha de ingreso"}, rows);
    }

    private void updateClub() {
        Club club = chooseClub(false);
        ConsoleUtil.printInfo("Escriba el nuevo valor o presione ENTER para conservar el actual.");
        club.setName(ConsoleUtil.readTextKeepingCurrent("Nombre", club.getName(), 100, true));
        club.setDescription(ConsoleUtil.readTextKeepingCurrent("Descripción", club.getDescription(), 1000, false));
        club.setMeetingSchedule(ConsoleUtil.readTextKeepingCurrent("Horario de reunión",
                club.getMeetingSchedule(), 100, false));
        String capacity = ConsoleUtil.readKeepingCurrent("Cupo máximo", String.valueOf(club.getCapacity()), true,
                text -> text.matches("\\d{1,3}"), "Escriba un número entero entre 1 y 200.");
        club.setCapacity(Integer.parseInt(capacity));
        System.out.println("Encargado actual: " + show(club.getSupervisorName()));
        String supervisorDocument = ConsoleUtil.readOptionalDocument(
                "Documento del nuevo encargado (ENTER para conservar): ");
        if (supervisorDocument != null) {
            club.setSupervisorId(professorController.getByDocument(supervisorDocument).getId());
        }
        clubController.update(club);
        ConsoleUtil.printSuccess("Club actualizado correctamente.");
    }

    private void toggleActive() {
        Club club = chooseClub(false);
        boolean newState = !club.isActive();
        if (ConsoleUtil.confirm("¿Desea " + (newState ? "activar" : "desactivar") + " el club " + club.getName() + "?")) {
            clubController.setActive(club.getId(), newState);
            ConsoleUtil.printSuccess("Club " + (newState ? "activado" : "desactivado") + ".");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    private void enrollStudent() {
        Student student = readStudent();
        Club club = chooseClub(true);
        clubController.enroll(student.getId(), club.getId());
        ConsoleUtil.printSuccess(student.getFullName() + " ahora es miembro de " + club.getName() + ".");
    }

    private void withdrawStudent() {
        Student student = readStudent();
        List<ClubMembership> active = clubController.clubsOfStudent(student.getId()).stream()
                .filter(m -> ClubMembership.ACTIVE.equals(m.status()))
                .collect(Collectors.toList());
        if (active.isEmpty()) {
            throw new BusinessException(student.getFullName() + " no es miembro activo de ningún club.");
        }
        List<String> labels = active.stream().map(ClubMembership::clubName).collect(Collectors.toList());
        String chosen = ConsoleUtil.readChoice("Club del que se retira", labels);
        ClubMembership membership = active.get(labels.indexOf(chosen));
        String reason = ConsoleUtil.readOptionalText("Motivo del retiro", 1000);
        if (ConsoleUtil.confirm("¿Retirar a " + student.getFullName() + " de " + membership.clubName() + "?")) {
            clubController.withdraw(student.getId(), membership.clubId(), reason);
            ConsoleUtil.printSuccess("Estudiante retirado del club.");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    private void showStudentClubs() {
        Student student = readStudent();
        List<ClubMembership> memberships = clubController.clubsOfStudent(student.getId());
        ConsoleUtil.printInfo("Estudiante: " + student.getFullName());
        if (memberships.isEmpty()) {
            ConsoleUtil.printInfo("No ha participado en ningún club.");
            return;
        }
        List<String[]> rows = memberships.stream()
                .map(m -> new String[]{m.clubName(), String.valueOf(m.joinDate()), m.status(),
                        m.withdrawalDate() == null ? "-" : String.valueOf(m.withdrawalDate()),
                        show(m.withdrawalReason())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Club", "Ingreso", "Estado", "Retiro", "Motivo"}, rows);
    }

    // ------------------------------------------------------------------ helpers

    private Student readStudent() {
        return studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
    }

    private Club chooseClub(boolean activeOnly) {
        List<Club> clubs = clubController.listAll().stream()
                .filter(club -> !activeOnly || club.isActive())
                .collect(Collectors.toList());
        if (clubs.isEmpty()) {
            throw new BusinessException("No hay clubes disponibles.");
        }
        List<String> labels = clubs.stream()
                .map(c -> c.getName() + " (" + c.getActiveMembers() + "/" + c.getCapacity() + ")"
                        + (c.isActive() ? "" : " [inactivo]"))
                .collect(Collectors.toList());
        String chosen = ConsoleUtil.readChoice("Club", labels);
        return clubs.get(labels.indexOf(chosen));
    }
}
