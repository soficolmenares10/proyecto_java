package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.EventSummary;
import com.littlehorizons.erp.model.InventoryRow;
import com.littlehorizons.erp.model.ParticipationSummary;
import com.littlehorizons.erp.model.StudentAverage;
import com.littlehorizons.erp.model.SubjectPerformance;
import java.math.BigDecimal;
import java.util.List;

/** Read-only queries that group data from several tables, used by the reports. */
public interface ReportDao {

    /** The average of every student with grades in the year (periods I to IV). */
    List<StudentAverage> studentAverages(int academicYear);

    /** How the students are doing in each subject taught by a professor. */
    List<SubjectPerformance> subjectPerformanceOfProfessor(int professorId, int academicYear);

    /** Students of a professor (in the subjects he or she teaches) with an average below the limit. */
    List<StudentAverage> lowStudentsOfProfessor(int professorId, int academicYear, BigDecimal limit);

    /** Active clubs, events and podium places of every active student. */
    List<ParticipationSummary> participationByStudent();

    List<EventSummary> eventSummaries(int year);

    List<InventoryRow> inventory();
}
