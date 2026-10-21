package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ConsoleUtil;

/**
 * Menu of the financial module.
 */
public class FinancialMenuView extends BaseView {

    private final ConceptView conceptView;
    private final PaymentView paymentView;
    private final ChargeView chargeView;
    private final InvoiceView invoiceView;

    public FinancialMenuView(ConceptView conceptView, PaymentView paymentView, ChargeView chargeView,
                             InvoiceView invoiceView) {
        this.conceptView = conceptView;
        this.paymentView = paymentView;
        this.chargeView = chargeView;
        this.invoiceView = invoiceView;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Gestión Financiera");
            System.out.println("1. Conceptos de pago y valores personalizados");
            System.out.println("2. Pagos");
            System.out.println("3. Cobros y morosos");
            System.out.println("4. Facturas electrónicas");
            System.out.println("0. Volver al menú principal");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 4);
            switch (option) {
                case 1 -> conceptView.run();
                case 2 -> paymentView.run();
                case 3 -> chargeView.run();
                case 4 -> invoiceView.run();
                default -> back = true;
            }
        }
    }
}
