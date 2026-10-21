package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.util.Validator;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Business logic for the inventory of loanable resources.
 */
public class ResourceController {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9-]{2,50}$");

    private final ResourceDao resourceDao;

    public ResourceController(ResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    /** @return the new resource id */
    public int register(Resource resource) {
        resource.setCode(resource.getCode() == null ? null : resource.getCode().trim().toUpperCase());
        if (resource.getCode() == null || !CODE_PATTERN.matcher(resource.getCode()).matches()) {
            throw new BusinessException("El código debe tener de 2 a 50 letras, números o guiones (ej: LIB-041).");
        }
        if (resourceDao.existsByCode(resource.getCode())) {
            throw new BusinessException("Ya existe un recurso con el código " + resource.getCode() + ".");
        }
        validateEditableFields(resource);
        resource.setStatus(Resource.AVAILABLE);
        return resourceDao.insert(resource);
    }

    public void update(Resource resource) {
        getById(resource.getId());
        validateEditableFields(resource);
        resourceDao.update(resource);
    }

    /**
     * Changes the status by hand: Disponible, En Mantenimiento or Dado de Baja.
     * A loaned resource must be returned first, and a discarded one cannot be used again.
     */
    public void changeStatus(int resourceId, String newStatus) {
        Resource resource = getById(resourceId);
        Rules.requireOneOf(newStatus, Resource.MANUAL_STATUSES, "El estado");
        if (Resource.LOANED.equals(resource.getStatus())) {
            throw new BusinessException("El recurso está prestado. Registre primero su devolución.");
        }
        if (Resource.DISCARDED.equals(resource.getStatus())) {
            throw new BusinessException("Un recurso dado de baja no puede cambiar de estado.");
        }
        if (newStatus.equals(resource.getStatus())) {
            throw new BusinessException("El recurso ya está en estado " + newStatus + ".");
        }
        resourceDao.updateStatus(resourceId, newStatus);
    }

    public Resource getById(int resourceId) {
        return resourceDao.findById(resourceId).orElseThrow(() -> new BusinessException("El recurso no existe."));
    }

    public Resource getByCode(String code) {
        return resourceDao.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException("No existe un recurso con el código " + code.trim() + "."));
    }

    /** Resources filtered by status (null = all statuses). */
    public List<Resource> list(String status) {
        return resourceDao.findAll().stream()
                .filter(resource -> status == null || resource.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    public List<Resource> search(String text) {
        if (!Validator.isNotBlank(text)) {
            throw new BusinessException("Escriba un texto para buscar.");
        }
        return resourceDao.search(text.trim());
    }

    public List<ResourceCategory> categories() {
        return resourceDao.findCategories();
    }

    private void validateEditableFields(Resource resource) {
        Rules.requireText(resource.getName(), "El nombre del recurso", 150);
        Rules.limitText(resource.getDescription(), "La descripción", 1000);
        Rules.limitText(resource.getLocation(), "La ubicación", 100);
        boolean categoryExists = categories().stream().anyMatch(category -> category.id() == resource.getCategoryId());
        if (!categoryExists) {
            throw new BusinessException("La categoría seleccionada no existe.");
        }
        LocalDate acquired = resource.getAcquisitionDate();
        if (acquired != null && (acquired.isAfter(LocalDate.now()) || acquired.getYear() < 1950)) {
            throw new BusinessException("La fecha de adquisición debe estar entre 1950 y hoy.");
        }
    }
}
