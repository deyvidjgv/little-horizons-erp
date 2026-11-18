package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.ResourceDAO;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Inventory of the resources that can be lent: unique code, valid data, and the Prestado status is
 * only set by the loans (a loaned resource is not edited by hand).
 */
public class ResourceController {

    public static final String CODE_RULE = "El código lleva letras mayúsculas, números y guiones (ejemplo: LIB-001).";

    private static final Pattern CODE = Pattern.compile("[A-Z0-9-]{2,50}");

    private final ResourceDAO resourceDAO;

    public ResourceController(ResourceDAO resourceDAO) {
        this.resourceDAO = resourceDAO;
    }

    public List<ResourceCategory> listCategories() throws DataAccessException {
        return resourceDAO.findCategories();
    }

    /** @param categoryId 0 to list every category */
    public List<Resource> list(int categoryId) throws DataAccessException {
        return resourceDAO.findAll().stream()
                .filter(resource -> categoryId == 0 || resource.getCategory().getId() == categoryId)
                .toList();
    }

    public Optional<Resource> findByCode(String code) throws DataAccessException {
        return resourceDAO.findByCode(normalizeCode(code));
    }

    public static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValidCode(String code) {
        return code != null && CODE.matcher(code).matches();
    }

    /** A new resource starts Disponible. */
    public OperationResult register(Resource resource) throws DataAccessException {
        resource.setCode(normalizeCode(resource.getCode()));
        resource.setStatus(ResourceStatus.AVAILABLE);
        String rejection = validate(resource);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        if (resourceDAO.findByCode(resource.getCode()).isPresent()) {
            return OperationResult.error("Ya existe un recurso con el código " + resource.getCode() + ".");
        }
        resource.setId(resourceDAO.create(resource));
        return OperationResult.ok("Recurso registrado: " + resource.getDisplayName() + ".");
    }

    public OperationResult update(Resource resource) throws DataAccessException {
        resource.setCode(normalizeCode(resource.getCode()));
        String rejection = validate(resource);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        Optional<Resource> current = resourceDAO.findById(resource.getId());
        if (current.isEmpty()) {
            return OperationResult.error("No existe el recurso.");
        }
        boolean wasLoaned = current.get().getStatus() == ResourceStatus.LOANED;
        boolean isLoaned = resource.getStatus() == ResourceStatus.LOANED;
        if (wasLoaned != isLoaned) {
            return OperationResult.error(wasLoaned
                    ? "El recurso está prestado: su estado cambia al registrar la devolución."
                    : "El estado Prestado solo se asigna al registrar un préstamo.");
        }
        Optional<Resource> sameCode = resourceDAO.findByCode(resource.getCode());
        if (sameCode.isPresent() && sameCode.get().getId() != resource.getId()) {
            return OperationResult.error("Ya existe otro recurso con el código " + resource.getCode() + ".");
        }
        return resourceDAO.update(resource)
                ? OperationResult.ok("Recurso actualizado: " + resource.getDisplayName() + ".")
                : OperationResult.error("No existe el recurso.");
    }

    /** A resource that was ever lent is not deleted (its loans would be lost): it is marked Dado de Baja. */
    public OperationResult delete(int resourceId) throws DataAccessException {
        Optional<Resource> resource = resourceDAO.findById(resourceId);
        if (resource.isEmpty()) {
            return OperationResult.error("No existe el recurso.");
        }
        if (resourceDAO.hasLoans(resourceId)) {
            return OperationResult.error("No se puede eliminar " + resource.get().getDisplayName() + ": tiene "
                    + "préstamos registrados. Cambie su estado a Dado de Baja con la opción Actualizar.");
        }
        return resourceDAO.delete(resourceId)
                ? OperationResult.ok("Recurso eliminado: " + resource.get().getDisplayName() + ".")
                : OperationResult.error("No existe el recurso.");
    }

    private static String validate(Resource resource) {
        if (!isValidCode(resource.getCode())) {
            return CODE_RULE;
        }
        String name = resource.getName();
        if (name == null || name.isBlank() || name.length() > Resource.NAME_MAX_LENGTH) {
            return "El nombre es obligatorio (máximo " + Resource.NAME_MAX_LENGTH + " caracteres).";
        }
        if (resource.getCategory() == null || resource.getStatus() == null) {
            return "La categoría y el estado son obligatorios.";
        }
        String description = resource.getDescription();
        if (description != null && description.length() > Resource.DESCRIPTION_MAX_LENGTH) {
            return "La descripción admite máximo " + Resource.DESCRIPTION_MAX_LENGTH + " caracteres.";
        }
        String location = resource.getLocation();
        if (location != null && location.length() > Resource.LOCATION_MAX_LENGTH) {
            return "La ubicación admite máximo " + Resource.LOCATION_MAX_LENGTH + " caracteres.";
        }
        LocalDate acquired = resource.getAcquisitionDate();
        if (acquired != null && acquired.isAfter(LocalDate.now())) {
            return "La fecha de adquisición no puede ser futura.";
        }
        return null;
    }
}
