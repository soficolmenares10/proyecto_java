package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleUtil;

/**
 * Menu of the academic module. Each option opens one of its screens.
 */
public class AcademicMenuView extends BaseView {

    private final SubjectView subjectView;
    private final SubjectEnrollmentView subjectEnrollmentView;
    private final AcademicRecordView academicRecordView;
    private final AttendanceView attendanceView;

    public AcademicMenuView(SubjectView subjectView, SubjectEnrollmentView subjectEnrollmentView,
                            AcademicRecordView academicRecordView, AttendanceView attendanceView) {
        this.subjectView = subjectView;
        this.subjectEnrollmentView = subjectEnrollmentView;
        this.academicRecordView = academicRecordView;
        this.attendanceView = attendanceView;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Gestión Académica");
            System.out.println("1. Catálogo de materias");
            System.out.println("2. Inscripción en materias");
            System.out.println("3. Calificaciones y promedios");
            System.out.println("4. Control de asistencia");
            System.out.println("0. Volver al menú principal");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 4);
            switch (option) {
                case 1 -> subjectView.run();
                case 2 -> subjectEnrollmentView.run();
                case 3 -> academicRecordView.run();
                case 4 -> attendanceView.run();
                default -> back = true;
            }
        }
    }
}
