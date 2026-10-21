package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.LoanController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceLoan;
import com.littlehorizons.erp.util.ConsoleUtil;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for the inventory of loanable resources.
 */
public class ResourceView extends BaseView {

    private final ResourceController resourceController;
    private final LoanController loanController;

    public ResourceView(ResourceController resourceController, LoanController loanController) {
        this.resourceController = resourceController;
        this.loanController = loanController;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Inventario de recursos");
            System.out.println("1. Registrar recurso");
            System.out.println("2. Listar recursos (se puede filtrar por estado)");
            System.out.println("3. Buscar recurso (código, nombre o ubicación)");
            System.out.println("4. Ver detalle de un recurso e historial de préstamos");
            System.out.println("5. Actualizar recurso");
            System.out.println("6. Cambiar estado (mantenimiento, baja o disponible)");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 6);
            switch (option) {
                case 1 -> runAndPause(this::registerResource);
                case 2 -> runAndPause(this::listResources);
                case 3 -> runAndPause(this::searchResources);
                case 4 -> runAndPause(this::showResource);
                case 5 -> runAndPause(this::updateResource);
                case 6 -> runAndPause(this::changeStatus);
                default -> back = true;
            }
        }
    }

    private void registerResource() {
        ConsoleUtil.printTitle("Registrar recurso");
        Resource resource = new Resource();
        resource.setCode(ConsoleUtil.readRequiredText("Código del recurso (por ejemplo LIB-041): ", 50));
        resource.setName(ConsoleUtil.readRequiredText("Nombre del recurso: ", 150));
        resource.setCategoryId(chooseCategory().id());
        resource.setDescription(ConsoleUtil.readOptionalText("Descripción", 1000));
        if (ConsoleUtil.confirm("¿Conoce la fecha de adquisición?")) {
            resource.setAcquisitionDate(ConsoleUtil.readDate("Fecha de adquisición"));
        }
        resource.setLocation(ConsoleUtil.readOptionalText("Ubicación física (por ejemplo Biblioteca - Estante A)", 100));
        int id = resourceController.register(resource);
        ConsoleUtil.printSuccess("Recurso registrado con ID " + id + ": " + resource.getCode() + " - " + resource.getName());
    }

    private void listResources() {
        String status = ConsoleUtil.readOptionalChoice("Filtrar por estado", Resource.STATUSES);
        printResources(resourceController.list(status));
    }

    private void searchResources() {
        String text = ConsoleUtil.readRequiredText("Texto a buscar: ", 100);
        printResources(resourceController.search(text));
    }

    private void showResource() {
        Resource resource = readResource();
        ConsoleUtil.printTitle(resource.getCode() + " - " + resource.getName());
        printLine("Categoría", resource.getCategoryName());
        printLine("Descripción", resource.getDescription());
        printLine("Fecha de adquisición", resource.getAcquisitionDate() == null ? null : String.valueOf(resource.getAcquisitionDate()));
        printLine("Ubicación", resource.getLocation());
        printLine("Estado", resource.getStatus());

        List<ResourceLoan> history = loanController.historyOfResource(resource.getId());
        System.out.println("\nHistorial de préstamos:");
        if (history.isEmpty()) {
            System.out.println("  (sin préstamos)");
            return;
        }
        List<String[]> rows = history.stream()
                .map(l -> new String[]{l.borrowerName(), l.borrowerType().getLabel(), String.valueOf(l.loanDate()),
                        String.valueOf(l.dueDate()), l.returnDate() == null ? "-" : String.valueOf(l.returnDate()),
                        l.status()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Prestatario", "Tipo", "Préstamo", "Vence", "Devuelto", "Estado"}, rows);
    }

    private void updateResource() {
        Resource resource = readResource();
        ConsoleUtil.printInfo("Escriba el nuevo valor o presione ENTER para conservar el actual.");
        resource.setName(ConsoleUtil.readTextKeepingCurrent("Nombre", resource.getName(), 150, true));
        List<String> categoryNames = resourceController.categories().stream()
                .map(ResourceCategory::name)
                .collect(Collectors.toList());
        String category = ConsoleUtil.readChoiceKeepingCurrent("Categoría", categoryNames, resource.getCategoryName());
        resource.setCategoryId(resourceController.categories().get(categoryNames.indexOf(category)).id());
        resource.setDescription(ConsoleUtil.readTextKeepingCurrent("Descripción", resource.getDescription(), 1000, false));
        if (resource.getAcquisitionDate() != null) {
            resource.setAcquisitionDate(ConsoleUtil.readDateKeepingCurrent("Fecha de adquisición",
                    resource.getAcquisitionDate()));
        }
        resource.setLocation(ConsoleUtil.readTextKeepingCurrent("Ubicación", resource.getLocation(), 100, false));
        resourceController.update(resource);
        ConsoleUtil.printSuccess("Recurso actualizado correctamente.");
    }

    private void changeStatus() {
        Resource resource = readResource();
        ConsoleUtil.printInfo(resource.getName() + " - estado actual: " + resource.getStatus());
        String newStatus = ConsoleUtil.readChoice("Nuevo estado", Resource.MANUAL_STATUSES);
        if (ConsoleUtil.confirm("¿Cambiar el estado a " + newStatus + "?")) {
            resourceController.changeStatus(resource.getId(), newStatus);
            ConsoleUtil.printSuccess("Estado actualizado a " + newStatus + ".");
        } else {
            ConsoleUtil.printInfo("Operación cancelada.");
        }
    }

    private Resource readResource() {
        return resourceController.getByCode(ConsoleUtil.readRequiredText("Código del recurso (por ejemplo LIB-001): ", 50));
    }

    private ResourceCategory chooseCategory() {
        List<ResourceCategory> categories = resourceController.categories();
        List<String> names = categories.stream().map(ResourceCategory::name).collect(Collectors.toList());
        String chosen = ConsoleUtil.readChoice("Categoría", names);
        return categories.get(names.indexOf(chosen));
    }

    private void printResources(List<Resource> resources) {
        if (resources.isEmpty()) {
            ConsoleUtil.printInfo("No hay recursos con esos criterios.");
            return;
        }
        List<String[]> rows = resources.stream()
                .map(r -> new String[]{r.getCode(), r.getName(), r.getCategoryName(), show(r.getLocation()), r.getStatus()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Código", "Recurso", "Categoría", "Ubicación", "Estado"}, rows);
        System.out.println("Total: " + resources.size() + " recursos");
    }
}
