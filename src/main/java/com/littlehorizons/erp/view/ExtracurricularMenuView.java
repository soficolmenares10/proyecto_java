package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleUtil;

/**
 * Menu of the extracurricular module (clubs and events).
 */
public class ExtracurricularMenuView extends BaseView {

    private final ClubView clubView;
    private final EventView eventView;

    public ExtracurricularMenuView(ClubView clubView, EventView eventView) {
        this.clubView = clubView;
        this.eventView = eventView;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Actividades Extracurriculares");
            System.out.println("1. Clubes");
            System.out.println("2. Eventos");
            System.out.println("0. Volver al menú principal");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 2);
            switch (option) {
                case 1 -> clubView.run();
                case 2 -> eventView.run();
                default -> back = true;
            }
        }
    }
}
