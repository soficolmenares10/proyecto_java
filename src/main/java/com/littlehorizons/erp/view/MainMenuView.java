package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleUtil;

/**
 * Main menu of the application. Each option opens the view of one module.
 */
public class MainMenuView extends BaseView {

    private static final int BOX_WIDTH = 46;

    private final StudentView studentView;
    private final AcademicMenuView academicMenuView;
    private final ProfessorView professorView;
    private final ExtracurricularMenuView extracurricularMenuView;

    public MainMenuView(StudentView studentView, AcademicMenuView academicMenuView, ProfessorView professorView,
                        ExtracurricularMenuView extracurricularMenuView) {
        this.studentView = studentView;
        this.academicMenuView = academicMenuView;
        this.professorView = professorView;
        this.extracurricularMenuView = extracurricularMenuView;
    }

    public void run() {
        boolean exit = false;
        while (!exit) {
            printMenu();
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 1, 8);
            switch (option) {
                case 1 -> studentView.run();
                case 2 -> academicMenuView.run();
                case 3 -> professorView.run();
                case 4 -> extracurricularMenuView.run();
                case 5, 6, 7 -> {
                    ConsoleUtil.printInfo("Este módulo se implementará en las próximas fases del proyecto.");
                    ConsoleUtil.pause();
                }
                default -> exit = true;
            }
        }
        System.out.println("\nGracias por usar el ERP Little Horizons. ¡Hasta pronto!");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("╔" + "═".repeat(BOX_WIDTH) + "╗");
        System.out.println(boxLine("   ERP Colegio Little Horizons"));
        System.out.println(boxLine("   Sistema de Gestión Educativa"));
        System.out.println("╚" + "═".repeat(BOX_WIDTH) + "╝");
        System.out.println();
        System.out.println("1. Gestión de Estudiantes");
        System.out.println("2. Gestión Académica");
        System.out.println("3. Gestión de Profesores");
        System.out.println("4. Actividades Extracurriculares");
        System.out.println("5. Gestión Financiera");
        System.out.println("6. Gestión de Recursos");
        System.out.println("7. Reportes y Estadísticas");
        System.out.println("8. Salir");
        System.out.println();
    }

    private String boxLine(String text) {
        return "║" + String.format("%-" + BOX_WIDTH + "s", text) + "║";
    }
}
