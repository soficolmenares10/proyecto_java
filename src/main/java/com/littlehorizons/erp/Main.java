package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.AcademicRecordController;
import com.littlehorizons.erp.controller.AttendanceController;
import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.controller.SubjectEnrollmentController;
import com.littlehorizons.erp.controller.TeachingController;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.JdbcAcademicRecordDao;
import com.littlehorizons.erp.dao.JdbcAssignmentDao;
import com.littlehorizons.erp.dao.JdbcAttendanceDao;
import com.littlehorizons.erp.dao.JdbcEnrollmentDao;
import com.littlehorizons.erp.dao.JdbcGuardianDao;
import com.littlehorizons.erp.dao.JdbcProfessorDao;
import com.littlehorizons.erp.dao.JdbcScheduleDao;
import com.littlehorizons.erp.dao.JdbcStudentDao;
import com.littlehorizons.erp.dao.JdbcSubjectDao;
import com.littlehorizons.erp.dao.JdbcSubjectEnrollmentDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.ScheduleDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleUtil;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.view.AcademicMenuView;
import com.littlehorizons.erp.view.AcademicRecordView;
import com.littlehorizons.erp.view.AttendanceView;
import com.littlehorizons.erp.view.EnrollmentView;
import com.littlehorizons.erp.view.GuardianView;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.ProfessorView;
import com.littlehorizons.erp.view.StudentView;
import com.littlehorizons.erp.view.SubjectEnrollmentView;
import com.littlehorizons.erp.view.SubjectSelector;
import com.littlehorizons.erp.view.SubjectView;
import com.littlehorizons.erp.view.TeachingView;

/**
 * Application entry point.
 *
 * It checks the database connection and then builds the objects of every layer
 * (DAO -> controller -> view) and starts the main menu.
 * Each object receives the ones it needs in its constructor (dependency injection).
 */
public class Main {

    public static void main(String[] args) {
        try {
            if (!DatabaseConnection.getInstance().testConnection()) {
                ConsoleUtil.printError("No se pudo conectar con MySQL. Revise database.properties y "
                        + "confirme que MySQL esté encendido. Detalles en error_log.txt");
                return;
            }
        } catch (IllegalStateException e) {
            AppLogger.error("Configuration error", e);
            ConsoleUtil.printError("No se pudo leer database.properties. Detalles en error_log.txt");
            return;
        }

        // ---- Data access layer
        StudentDao studentDao = new JdbcStudentDao();
        GuardianDao guardianDao = new JdbcGuardianDao();
        EnrollmentDao enrollmentDao = new JdbcEnrollmentDao();
        ProfessorDao professorDao = new JdbcProfessorDao();
        SubjectDao subjectDao = new JdbcSubjectDao();
        AssignmentDao assignmentDao = new JdbcAssignmentDao();
        ScheduleDao scheduleDao = new JdbcScheduleDao();
        SubjectEnrollmentDao subjectEnrollmentDao = new JdbcSubjectEnrollmentDao();
        AcademicRecordDao academicRecordDao = new JdbcAcademicRecordDao();
        AttendanceDao attendanceDao = new JdbcAttendanceDao();

        // ---- Business layer
        StudentController studentController = new StudentController(studentDao, guardianDao);
        GuardianController guardianController = new GuardianController(guardianDao, studentDao);
        EnrollmentController enrollmentController = new EnrollmentController(enrollmentDao, studentDao);
        ProfessorController professorController = new ProfessorController(professorDao);
        SubjectController subjectController = new SubjectController(subjectDao, enrollmentDao);
        TeachingController teachingController =
                new TeachingController(assignmentDao, scheduleDao, professorDao, subjectDao);
        SubjectEnrollmentController subjectEnrollmentController =
                new SubjectEnrollmentController(subjectEnrollmentDao, studentDao, enrollmentDao, subjectDao);
        AcademicRecordController academicRecordController = new AcademicRecordController(academicRecordDao,
                subjectEnrollmentDao, assignmentDao, professorDao, studentDao, subjectDao);
        AttendanceController attendanceController = new AttendanceController(attendanceDao,
                subjectEnrollmentDao, assignmentDao, subjectDao, professorDao);

        // ---- Presentation layer
        SubjectSelector subjectSelector = new SubjectSelector(subjectController, enrollmentController);

        GuardianView guardianView = new GuardianView(guardianController, studentController);
        EnrollmentView enrollmentView = new EnrollmentView(enrollmentController, studentController);
        StudentView studentView = new StudentView(studentController, enrollmentController,
                guardianView, enrollmentView);

        TeachingView teachingView = new TeachingView(teachingController, professorController, subjectSelector);
        ProfessorView professorView = new ProfessorView(professorController, teachingView);

        SubjectView subjectView = new SubjectView(subjectController, subjectSelector);
        SubjectEnrollmentView subjectEnrollmentView = new SubjectEnrollmentView(subjectEnrollmentController,
                studentController, subjectSelector);
        AcademicRecordView academicRecordView = new AcademicRecordView(academicRecordController,
                studentController, professorController, subjectSelector);
        AttendanceView attendanceView = new AttendanceView(attendanceController, studentController,
                professorController, subjectSelector);
        AcademicMenuView academicMenuView = new AcademicMenuView(subjectView, subjectEnrollmentView,
                academicRecordView, attendanceView);

        new MainMenuView(studentView, academicMenuView, professorView).run();
    }
}
