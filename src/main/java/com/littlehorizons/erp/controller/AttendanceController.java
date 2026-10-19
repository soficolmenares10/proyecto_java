package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.model.AbsenceAlert;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for the daily attendance and for the absence report.
 */
public class AttendanceController {

    private final AttendanceDao attendanceDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final AssignmentDao assignmentDao;
    private final SubjectDao subjectDao;
    private final ProfessorDao professorDao;

    public AttendanceController(AttendanceDao attendanceDao, SubjectEnrollmentDao subjectEnrollmentDao,
                                AssignmentDao assignmentDao, SubjectDao subjectDao, ProfessorDao professorDao) {
        this.attendanceDao = attendanceDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.assignmentDao = assignmentDao;
        this.subjectDao = subjectDao;
        this.professorDao = professorDao;
    }

    /** The students enrolled in a subject during the year of the given date. */
    public List<SubjectEnrollment> studentsOfClass(int subjectId, LocalDate date) {
        return subjectEnrollmentDao.findDistinctStudentsBySubject(subjectId, date.getYear());
    }

    /**
     * Registers the attendance of a whole class. Every enrolled student is marked "Presente"
     * except the ones listed in {@code exceptions} (student id -> Ausente, Tardanza or Excusa Médica).
     *
     * @return how many students were saved with each status
     */
    public Map<String, Long> registerClass(int subjectId, int professorId, LocalDate date,
                                           Map<Integer, String> exceptions) {
        Subject subject = subjectDao.findById(subjectId)
                .orElseThrow(() -> new BusinessException("La materia no existe."));
        if (professorDao.findById(professorId).isEmpty()) {
            throw new BusinessException("El profesor no existe.");
        }
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha no puede ser posterior a hoy.");
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BusinessException("No hay clases los sábados ni los domingos.");
        }
        if (!assignmentDao.isAssigned(professorId, subjectId, date.getYear())) {
            throw new BusinessException("El profesor no dicta " + subject.getName() + " en " + date.getYear() + ".");
        }
        List<SubjectEnrollment> students = studentsOfClass(subjectId, date);
        if (students.isEmpty()) {
            throw new BusinessException("No hay estudiantes inscritos en " + subject.getName() + " en "
                    + date.getYear() + ".");
        }
        Set<Integer> enrolledIds = students.stream().map(SubjectEnrollment::studentId).collect(Collectors.toSet());
        for (Map.Entry<Integer, String> exception : exceptions.entrySet()) {
            if (!enrolledIds.contains(exception.getKey())) {
                throw new BusinessException("Un estudiante de la lista de novedades no está inscrito en la materia.");
            }
            Rules.requireOneOf(exception.getValue(), Attendance.EXCEPTIONS, "El estado de asistencia");
        }

        List<Attendance> toSave = new ArrayList<>();
        for (SubjectEnrollment student : students) {
            String status = exceptions.getOrDefault(student.studentId(), Attendance.PRESENT);
            toSave.add(new Attendance(0, student.studentId(), null, null, subjectId, null, date, status,
                    null, professorId));
        }
        attendanceDao.saveAll(toSave);
        return countByStatus(toSave);
    }

    public List<Attendance> classAttendance(int subjectId, LocalDate date) {
        return attendanceDao.findBySubjectAndDate(subjectId, date);
    }

    public List<Attendance> studentAttendance(int studentId, LocalDate from, LocalDate to) {
        checkRange(from, to);
        return attendanceDao.findByStudent(studentId, from, to);
    }

    /** How many records there are of each status (in the order Presente, Ausente, Tardanza, Excusa Médica). */
    public Map<String, Long> countByStatus(List<Attendance> attendances) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : Attendance.STATUSES) {
            counts.put(status, attendances.stream().filter(a -> a.status().equals(status)).count());
        }
        return counts;
    }

    /**
     * Students that were absent ("Ausente") on MORE than {@code maxAbsentDays} different days
     * in the same week (Monday to Sunday). Excused absences do not count.
     */
    public List<AbsenceAlert> absenceAlerts(LocalDate from, LocalDate to, int maxAbsentDays) {
        checkRange(from, to);
        if (maxAbsentDays < 0) {
            throw new BusinessException("El máximo de ausencias no puede ser negativo.");
        }
        Map<String, List<Attendance>> byStudentAndWeek = attendanceDao
                .findByStatusBetween(Attendance.ABSENT, from, to).stream()
                .collect(Collectors.groupingBy(a -> a.studentId() + "|" + weekStart(a.date())));

        return byStudentAndWeek.values().stream()
                .map(list -> new AbsenceAlert(list.get(0).studentId(), list.get(0).studentName(),
                        list.get(0).studentDocument(), weekStart(list.get(0).date()),
                        list.stream().map(Attendance::date).distinct().count()))
                .filter(alert -> alert.absentDays() > maxAbsentDays)
                .sorted(Comparator.comparingLong(AbsenceAlert::absentDays).reversed()
                        .thenComparing(AbsenceAlert::studentName))
                .collect(Collectors.toList());
    }

    private static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private void checkRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("La fecha inicial debe ser anterior o igual a la fecha final.");
        }
    }
}
