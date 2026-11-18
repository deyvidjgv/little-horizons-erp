package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Row of the recursos table: a book, a lab device, sports equipment... that can be lent. */
public class Resource {

    public static final int CODE_MAX_LENGTH = 50;
    public static final int NAME_MAX_LENGTH = 150;
    public static final int LOCATION_MAX_LENGTH = 100;
    /** descripcion is a TEXT column; the application keeps it short to fit on screen. */
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    private int id;
    private String code;
    private String name;
    private ResourceCategory category;
    private String description;
    private LocalDate acquisitionDate;
    private String location;
    private ResourceStatus status;

    /** @return for example "LIB-001 Cien años de soledad" */
    public String getDisplayName() {
        return code + " " + name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceCategory getCategory() {
        return category;
    }

    public void setCategory(ResourceCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(LocalDate acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }
}
