package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleUtil;

/**
 * Menu of the resource module (inventory and loans).
 */
public class ResourceMenuView extends BaseView {

    private final ResourceView resourceView;
    private final LoanView loanView;

    public ResourceMenuView(ResourceView resourceView, LoanView loanView) {
        this.resourceView = resourceView;
        this.loanView = loanView;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Gestión de Recursos");
            System.out.println("1. Inventario de recursos");
            System.out.println("2. Préstamos y devoluciones");
            System.out.println("0. Volver al menú principal");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 2);
            switch (option) {
                case 1 -> resourceView.run();
                case 2 -> loanView.run();
                default -> back = true;
            }
        }
    }
}
