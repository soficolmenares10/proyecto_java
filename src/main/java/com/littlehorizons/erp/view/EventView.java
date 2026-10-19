package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.BusinessException;
import com.littlehorizons.erp.controller.EventController;
import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.EventParticipation;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ConsoleUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for school events, participants and competition results.
 */
public class EventView extends BaseView {

    private final EventController eventController;
    private final ProfessorController professorController;
    private final StudentController studentController;

    public EventView(EventController eventController, ProfessorController professorController,
                     StudentController studentController) {
        this.eventController = eventController;
        this.professorController = professorController;
        this.studentController = studentController;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Eventos");
            System.out.println("1. Registrar evento");
            System.out.println("2. Calendario de eventos de un año");
            System.out.println("3. Ver detalle de un evento, participantes y podio");
            System.out.println("4. Actualizar evento");
            System.out.println("5. Activar o desactivar evento");
            System.out.println("6. Registrar participante en un evento");
            System.out.println("7. Asignar posición a un competidor");
            System.out.println("8. Quitar participante de un evento");
            System.out.println("9. Historial de participación de un estudiante");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 9);
            switch (option) {
                case 1 -> runAndPause(this::registerEvent);
                case 2 -> runAndPause(this::showCalendar);
                case 3 -> runAndPause(this::showEvent);
                case 4 -> runAndPause(this::updateEvent);
                case 5 -> runAndPause(this::toggleActive);
                case 6 -> runAndPause(this::addParticipant);
                case 7 -> runAndPause(this::setPosition);
                case 8 -> runAndPause(this::removeParticipant);
                case 9 -> runAndPause(this::showStudentHistory);
                default -> back = true;
            }
        }
    }

    // ------------------------------------------------------------------ actions

    private void registerEvent() {
        ConsoleUtil.printTitle("Registrar evento");
        SchoolEvent event = new SchoolEvent();
        event.setName(ConsoleUtil.readRequiredText("Nombre del evento: ", 150));
        event.setDescription(ConsoleUtil.readOptionalText("Descripción", 1000));
        event.setType(ConsoleUtil.readChoice("Tipo de evento", SchoolEvent.TYPES));
        event.setDate(ConsoleUtil.readDate("Fecha del evento"));
        event.setPlace(ConsoleUtil.readOptionalText("Lugar", 150));
        event.setCompetition(ConsoleUtil.confirm("¿El evento tiene competencia (con posiciones)?"));
        String organizerDocument = ConsoleUtil.readOptionalDocument(
                "Documento del profesor organizador (ENTER para omitir): ");
        if (organizerDocument != null) {
            event.setOrganizerId(professorController.getByDocument(organizerDocument).getId());
        }
        int id = eventController.register(event);
        ConsoleUtil.printSuccess("Evento registrado con ID " + id + ": " + event.getName());
    }

    private void showCalendar() {
        int year = readEventYear();
        List<SchoolEvent> events = eventController.listByYear(year);
        ConsoleUtil.printTitle("Calendario de eventos " + year);
        if (events.isEmpty()) {
            ConsoleUtil.printInfo("No hay eventos registrados en " + year + ".");
            return;
        }
        List<String[]> rows = events.stream()
                .map(e -> new String[]{String.valueOf(e.getDate()), e.getName(), e.getType(), show(e.getPlace()),
                        e.isCompetition() ? "Sí" : "No", e.isActive() ? "Activo" : "Inactivo"})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Fecha", "Evento", "Tipo", "Lugar", "Competencia", "Estado"}, rows);
        System.out.println("Total: " + events.size() + " eventos");
    }

    private void showEvent() {
        SchoolEvent event = chooseEvent();
        ConsoleUtil.printTitle(event.getName());
        printLine("Tipo", event.getType());
        printLine("Fecha", String.valueOf(event.getDate()));
        printLine("Lugar", event.getPlace());
        printLine("Descripción", event.getDescription());
        printLine("Organizador", event.getOrganizerName());
        printLine("Competencia", event.isCompetition() ? "Sí" : "No");
        printLine("Estado", event.isActive() ? "Activo" : "Inactivo");

        List<EventParticipation> participants = eventController.participants(event.getId());
        System.out.println("\nParticipantes (" + participants.size() + "):");
        if (participants.isEmpty()) {
            System.out.println("  (ninguno registrado)");
            return;
        }
        List<String[]> rows = participants.stream()
                .map(p -> new String[]{p.studentDocument(), p.studentName(), p.role(), show(p.position())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Documento", "Estudiante", "Rol", "Posición"}, rows);

        if (event.isCompetition()) {
            System.out.println("Podio:");
            for (String position : EventParticipation.PODIUM) {
                String winner = participants.stream()
                        .filter(p -> position.equals(p.position()))
                        .map(EventParticipation::studentName)
                        .findFirst()
                        .orElse("(sin asignar)");
                System.out.println("  " + position + ": " + winner);
            }
        }
    }

    private void updateEvent() {
        SchoolEvent event = chooseEvent();
        ConsoleUtil.printInfo("Escriba el nuevo valor o presione ENTER para conservar el actual.");
        event.setName(ConsoleUtil.readTextKeepingCurrent("Nombre", event.getName(), 150, true));
        event.setDescription(ConsoleUtil.readTextKeepingCurrent("Descripción", event.getDescription(), 1000, false));
        event.setType(ConsoleUtil.readChoiceKeepingCurrent("Tipo", SchoolEvent.TYPES, event.getType()));
        event.setDate(ConsoleUtil.readDateKeepingCurrent("Fecha", event.getDate()));
        event.setPlace(ConsoleUtil.readTextKeepingCurrent("Lugar", event.getPlace(), 150, false));
        System.out.println("Organizador actual: " + show(event.getOrganizerName()));
        String organizerDocument = ConsoleUtil.readOptionalDocument(
                "Documento del nuevo organizador (ENTER para conservar): ");
        if (organizerDocument != null) {
            event.setOrganizerId(professorController.getByDocument(organizerDocument).getId());
        }
        eventController.update(event);
        ConsoleUtil.printSuccess("Evento actualizado correctamente.");
    }

    private void toggleActive() {
        SchoolEvent event = chooseEvent();
        boolean newState = !event.isActive();
        if (ConsoleUtil.confirm("¿Desea " + (newState ? "activar" : "desactivar") + " el evento " + event.getName() + "?")) {
            eventController.setActive(event.getId(), newState);
            ConsoleUtil.printSuccess("Evento " + (newState ? "activado" : "desactivado") + ".");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    private void addParticipant() {
        SchoolEvent event = chooseEvent();
        Student student = readStudent();
        String role = ConsoleUtil.readChoice("Rol del estudiante", event.isCompetition()
                ? EventParticipation.ROLES : EventParticipation.ROLES_WITHOUT_COMPETITION);
        String position = null;
        if (EventParticipation.COMPETITOR.equals(role)) {
            position = ConsoleUtil.readChoice("Posición obtenida", EventParticipation.POSITIONS);
        }
        String notes = ConsoleUtil.readOptionalText("Observaciones", 1000);
        eventController.addParticipant(event.getId(), student.getId(), role, position, notes);
        ConsoleUtil.printSuccess(student.getFullName() + " quedó registrado en " + event.getName() + ".");
    }

    private void setPosition() {
        SchoolEvent event = chooseEvent();
        Student student = readStudent();
        String position = ConsoleUtil.readChoice("Posición obtenida", EventParticipation.POSITIONS);
        eventController.setPosition(event.getId(), student.getId(), position);
        ConsoleUtil.printSuccess("Posición asignada: " + position + " para " + student.getFullName() + ".");
    }

    private void removeParticipant() {
        SchoolEvent event = chooseEvent();
        Student student = readStudent();
        if (ConsoleUtil.confirm("¿Quitar a " + student.getFullName() + " de " + event.getName() + "?")) {
            eventController.removeParticipant(event.getId(), student.getId());
            ConsoleUtil.printSuccess("Participante eliminado del evento.");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    private void showStudentHistory() {
        Student student = readStudent();
        List<EventParticipation> history = eventController.eventsOfStudent(student.getId());
        ConsoleUtil.printInfo("Estudiante: " + student.getFullName());
        if (history.isEmpty()) {
            ConsoleUtil.printInfo("No ha participado en eventos.");
            return;
        }
        List<String[]> rows = history.stream()
                .map(p -> new String[]{String.valueOf(p.eventDate()), p.eventName(), p.role(), show(p.position())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Fecha", "Evento", "Rol", "Posición"}, rows);
        System.out.println("Total: " + history.size() + " participaciones");
    }

    // ------------------------------------------------------------------ helpers

    private Student readStudent() {
        return studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
    }

    private int readEventYear() {
        int currentYear = LocalDate.now().getYear();
        return ConsoleUtil.readInt("Año del evento (por ejemplo " + currentYear + "): ", 2000, currentYear + 5);
    }

    private SchoolEvent chooseEvent() {
        int year = readEventYear();
        List<SchoolEvent> events = eventController.listByYear(year);
        if (events.isEmpty()) {
            throw new BusinessException("No hay eventos registrados en " + year + ".");
        }
        List<String> labels = events.stream()
                .map(e -> e.getDate() + " - " + e.getName() + (e.isActive() ? "" : " [inactivo]"))
                .collect(Collectors.toList());
        String chosen = ConsoleUtil.readChoice("Evento", labels);
        return events.get(labels.indexOf(chosen));
    }
}
