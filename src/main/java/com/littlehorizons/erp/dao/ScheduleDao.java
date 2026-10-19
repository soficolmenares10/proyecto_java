package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClassSchedule;
import java.util.List;

/** Data access operations for class schedules (day and hours of each assignment). */
public interface ScheduleDao {

    /** @return the generated schedule id */
    int insert(ClassSchedule schedule);

    List<ClassSchedule> findByProfessorAndYear(int professorId, int academicYear);

    /** Schedules of one group (same grade and section) in one year. */
    List<ClassSchedule> findByGroupAndYear(int gradeId, String section, int academicYear);

    boolean delete(int scheduleId);
}
