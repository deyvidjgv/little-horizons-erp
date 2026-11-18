package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** Row of the conceptos_pago table: something the school charges for (matrícula, pensión...). */
public class PaymentConcept {

    public static final int CODE_MAX_LENGTH = 20;
    public static final int NAME_MAX_LENGTH = 100;
    /** descripcion is a TEXT column; the application keeps it short to fit on screen. */
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    private int id;
    private String code;
    private String name;
    private String description;
    private BigDecimal baseAmount;
    /** A recurring concept is charged every month (for example, the pensión). */
    private boolean recurring;
    private boolean active;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
