package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.ScheduleDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.util.Validator;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic for the academic load of professors: which subjects they teach
 * and in which days and hours, avoiding schedule conflicts.
 */
public class TeachingController {

    private static final LocalTime OPENING_TIME = LocalTime.of(6, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(18, 0);

    private final AssignmentDao assignmentDao;
    private final ScheduleDao scheduleDao;
    private final ProfessorDao professorDao;
    private final SubjectDao subjectDao;

    public TeachingController(AssignmentDao assignmentDao, ScheduleDao scheduleDao,
                              ProfessorDao professorDao, SubjectDao subjectDao) {
        this.assignmentDao = assignmentDao;
        this.scheduleDao = scheduleDao;
        this.professorDao = professorDao;
        this.subjectDao = subjectDao;
    }

    // -------------------------------------------------------------- assignments

    /** @return the new assignment id */
    public int assign(int professorId, int subjectId, int academicYear, String section) {
        Professor professor = professorDao.findById(professorId)
                .orElseThrow(() -> new BusinessException("El profesor no existe."));
        if (!"Activo".equals(professor.getStatus())) {
            throw new BusinessException("Solo se pueden asignar materias a profesores activos. Estado actual: "
                    + professor.getStatus() + ".");
        }
        Subject subject = subjectDao.findById(subjectId)
                .orElseThrow(() -> new BusinessException("La materia no existe."));
        if (!subject.isActive()) {
            throw new BusinessException("La materia " + subject.getName() + " está inactiva.");
        }
        Rules.requireAcademicYear(academicYear);
        Rules.requireText(section, "La sección", 10);
        String cleanSection = section.trim().toUpperCase();
        if (assignmentDao.exists(professorId, subjectId, academicYear, cleanSection)) {
            throw new BusinessException("Ese profesor ya dicta esa materia en la sección "
                    + cleanSection + " en " + academicYear + ".");
        }
        return assignmentDao.insert(professorId, subjectId, academicYear, cleanSection);
    }

    public List<TeachingAssignment> assignmentsOf(int professorId, int academicYear) {
        return assignmentDao.findByProfessorAndYear(professorId, academicYear);
    }

    /** Removes an assignment of the given professor (its schedules are removed with it). */
    public void removeAssignment(int professorId, int assignmentId) {
        TeachingAssignment assignment = assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new BusinessException("La asignación no existe."));
        if (assignment.professorId() != professorId) {
            throw new BusinessException("Esa asignación no pertenece a este profesor.");
        }
        assignmentDao.delete(assignmentId);
    }

    // ---------------------------------------------------------------- schedules

    /**
     * Adds a weekly class hour to an assignment after checking that:
     * the hours are valid, the professor is free, and the group (grade and section) is free.
     *
     * @return the new schedule id
     */
    public int addSchedule(int professorId, int assignmentId, String day, LocalTime start, LocalTime end,
                           String room) {
        TeachingAssignment assignment = assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new BusinessException("La asignación no existe."));
        if (assignment.professorId() != professorId) {
            throw new BusinessException("Esa asignación no pertenece a este profesor.");
        }
        Rules.requireOneOf(day, ClassSchedule.DAYS, "El día");
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BusinessException("La hora de inicio debe ser anterior a la hora de fin.");
        }
        if (start.isBefore(OPENING_TIME) || end.isAfter(CLOSING_TIME)) {
            throw new BusinessException("El horario debe estar entre " + OPENING_TIME + " y " + CLOSING_TIME + ".");
        }
        Rules.limitText(room, "El aula", 30);

        int year = assignment.academicYear();
        Optional<ClassSchedule> professorConflict = scheduleDao.findByProfessorAndYear(professorId, year).stream()
                .filter(other -> other.overlapsWith(day, start, end))
                .findFirst();
        if (professorConflict.isPresent()) {
            ClassSchedule other = professorConflict.get();
            throw new BusinessException("Conflicto: el profesor ya dicta " + other.subjectName() + " ("
                    + other.gradeName() + ") el " + other.day() + " de " + other.startTime() + " a "
                    + other.endTime() + ".");
        }
        Optional<ClassSchedule> groupConflict = scheduleDao
                .findByGroupAndYear(assignment.gradeId(), assignment.section(), year).stream()
                .filter(other -> other.overlapsWith(day, start, end))
                .findFirst();
        if (groupConflict.isPresent()) {
            ClassSchedule other = groupConflict.get();
            throw new BusinessException("Conflicto: el grupo " + assignment.gradeName() + " "
                    + assignment.section() + " ya tiene " + other.subjectName() + " el " + other.day()
                    + " de " + other.startTime() + " a " + other.endTime() + ".");
        }
        return scheduleDao.insert(new ClassSchedule(0, assignmentId, professorId, day, start, end,
                Validator.isNotBlank(room) ? room.trim() : null, year, assignment.section(),
                assignment.subjectName(), assignment.gradeId(), assignment.gradeName()));
    }

    /** The weekly timetable of a professor, ordered by day and start time. */
    public List<ClassSchedule> timetableOf(int professorId, int academicYear) {
        return scheduleDao.findByProfessorAndYear(professorId, academicYear).stream()
                .sorted(Comparator.comparingInt((ClassSchedule s) -> ClassSchedule.DAYS.indexOf(s.day()))
                        .thenComparing(ClassSchedule::startTime))
                .collect(Collectors.toList());
    }

    /** Removes a schedule that belongs to the given professor. */
    public void removeSchedule(int professorId, int academicYear, int scheduleId) {
        boolean belongsToProfessor = scheduleDao.findByProfessorAndYear(professorId, academicYear).stream()
                .anyMatch(schedule -> schedule.id() == scheduleId);
        if (!belongsToProfessor) {
            throw new BusinessException("Ese horario no existe o no pertenece a este profesor en " + academicYear + ".");
        }
        scheduleDao.delete(scheduleId);
    }
}
