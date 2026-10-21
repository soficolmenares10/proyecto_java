package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.AcademicRecordController;
import com.littlehorizons.erp.controller.AttendanceController;
import com.littlehorizons.erp.controller.ChargeController;
import com.littlehorizons.erp.controller.ClubController;
import com.littlehorizons.erp.controller.ConceptController;
import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.EventController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.InvoiceController;
import com.littlehorizons.erp.controller.LoanController;
import com.littlehorizons.erp.controller.PaymentController;
import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.controller.SubjectEnrollmentController;
import com.littlehorizons.erp.controller.TeachingController;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.ChargeDao;
import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ConceptDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.JdbcAcademicRecordDao;
import com.littlehorizons.erp.dao.JdbcAssignmentDao;
import com.littlehorizons.erp.dao.JdbcAttendanceDao;
import com.littlehorizons.erp.dao.JdbcChargeDao;
import com.littlehorizons.erp.dao.JdbcClubDao;
import com.littlehorizons.erp.dao.JdbcConceptDao;
import com.littlehorizons.erp.dao.JdbcEnrollmentDao;
import com.littlehorizons.erp.dao.JdbcEventDao;
import com.littlehorizons.erp.dao.JdbcGuardianDao;
import com.littlehorizons.erp.dao.JdbcInvoiceDao;
import com.littlehorizons.erp.dao.JdbcLoanDao;
import com.littlehorizons.erp.dao.JdbcMembershipDao;
import com.littlehorizons.erp.dao.JdbcParticipationDao;
import com.littlehorizons.erp.dao.JdbcPaymentDao;
import com.littlehorizons.erp.dao.JdbcProfessorDao;
import com.littlehorizons.erp.dao.JdbcReportDao;
import com.littlehorizons.erp.dao.JdbcResourceDao;
import com.littlehorizons.erp.dao.JdbcScheduleDao;
import com.littlehorizons.erp.dao.JdbcStudentDao;
import com.littlehorizons.erp.dao.JdbcStudentRateDao;
import com.littlehorizons.erp.dao.JdbcSubjectDao;
import com.littlehorizons.erp.dao.JdbcSubjectEnrollmentDao;
import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.MembershipDao;
import com.littlehorizons.erp.dao.ParticipationDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.ScheduleDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentRateDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.service.InvoiceDocumentGenerator;
import com.littlehorizons.erp.service.PdfInvoiceGenerator;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleUtil;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.InvoiceSettings;
import com.littlehorizons.erp.view.AcademicMenuView;
import com.littlehorizons.erp.view.AcademicRecordView;
import com.littlehorizons.erp.view.AttendanceView;
import com.littlehorizons.erp.view.ChargeView;
import com.littlehorizons.erp.view.ClubView;
import com.littlehorizons.erp.view.ConceptView;
import com.littlehorizons.erp.view.EnrollmentView;
import com.littlehorizons.erp.view.EventView;
import com.littlehorizons.erp.view.ExtracurricularMenuView;
import com.littlehorizons.erp.view.FinancialMenuView;
import com.littlehorizons.erp.view.GuardianView;
import com.littlehorizons.erp.view.InvoiceView;
import com.littlehorizons.erp.view.LoanView;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.PaymentView;
import com.littlehorizons.erp.view.ProfessorView;
import com.littlehorizons.erp.view.ReportView;
import com.littlehorizons.erp.view.ResourceMenuView;
import com.littlehorizons.erp.view.ResourceView;
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
        // The PDF library draws the QR code as an image; no screen is needed for that.
        System.setProperty("java.awt.headless", "true");

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
        buildApplication().run();
    }

    /** Creates every DAO, controller and view, connecting them, and returns the main menu. */
    private static MainMenuView buildApplication() {
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
        ClubDao clubDao = new JdbcClubDao();
        MembershipDao membershipDao = new JdbcMembershipDao();
        EventDao eventDao = new JdbcEventDao();
        ParticipationDao participationDao = new JdbcParticipationDao();
        ConceptDao conceptDao = new JdbcConceptDao();
        StudentRateDao studentRateDao = new JdbcStudentRateDao();
        PaymentDao paymentDao = new JdbcPaymentDao();
        ChargeDao chargeDao = new JdbcChargeDao();
        InvoiceDao invoiceDao = new JdbcInvoiceDao();
        ResourceDao resourceDao = new JdbcResourceDao();
        LoanDao loanDao = new JdbcLoanDao();
        ReportDao reportDao = new JdbcReportDao();

        // ---- Services
        InvoiceSettings invoiceSettings = new InvoiceSettings();
        InvoiceDocumentGenerator documentGenerator = new PdfInvoiceGenerator();

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
        ClubController clubController = new ClubController(clubDao, membershipDao, professorDao, studentDao);
        EventController eventController = new EventController(eventDao, participationDao, professorDao, studentDao);
        ConceptController conceptController = new ConceptController(conceptDao, studentRateDao, studentDao);
        PaymentController paymentController =
                new PaymentController(paymentDao, chargeDao, studentDao, guardianDao, conceptController);
        ChargeController chargeController =
                new ChargeController(chargeDao, studentDao, enrollmentDao, conceptController);
        InvoiceController invoiceController = new InvoiceController(invoiceDao, paymentDao, guardianDao,
                studentDao, conceptDao, documentGenerator, invoiceSettings);
        ResourceController resourceController = new ResourceController(resourceDao);
        LoanController loanController = new LoanController(loanDao, resourceDao, studentDao, professorDao);
        ReportController reportController = new ReportController(reportDao, studentDao, professorDao, clubDao,
                enrollmentDao, paymentDao, chargeController, loanController);

        // ---- Presentation layer: students
        SubjectSelector subjectSelector = new SubjectSelector(subjectController, enrollmentController);
        GuardianView guardianView = new GuardianView(guardianController, studentController);
        EnrollmentView enrollmentView = new EnrollmentView(enrollmentController, studentController);
        StudentView studentView = new StudentView(studentController, enrollmentController,
                guardianView, enrollmentView);

        // ---- Presentation layer: professors and academic module
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

        // ---- Presentation layer: extracurricular activities
        ClubView clubView = new ClubView(clubController, professorController, studentController);
        EventView eventView = new EventView(eventController, professorController, studentController);
        ExtracurricularMenuView extracurricularMenuView = new ExtracurricularMenuView(clubView, eventView);

        // ---- Presentation layer: financial module
        ConceptView conceptView = new ConceptView(conceptController, studentController);
        PaymentView paymentView = new PaymentView(paymentController, studentController, guardianController,
                chargeController, conceptController, invoiceController, conceptView);
        ChargeView chargeView = new ChargeView(chargeController, studentController, conceptView);
        InvoiceView invoiceView = new InvoiceView(invoiceController, paymentController);
        FinancialMenuView financialMenuView = new FinancialMenuView(conceptView, paymentView, chargeView, invoiceView);

        // ---- Presentation layer: resources and reports
        ResourceView resourceView = new ResourceView(resourceController, loanController);
        LoanView loanView = new LoanView(loanController, resourceController, studentController, professorController);
        ResourceMenuView resourceMenuView = new ResourceMenuView(resourceView, loanView);
        ReportView reportView = new ReportView(reportController, professorController, loanController);

        return new MainMenuView(studentView, academicMenuView, professorView, extracurricularMenuView,
                financialMenuView, resourceMenuView, reportView);
    }
}
