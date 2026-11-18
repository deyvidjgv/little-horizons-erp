package com.littlehorizons.erp.service;

/**
 * Data of the school that the invoice shows. The statement gives the legal name; the NIT, the
 * contact data and the DIAN resolution are simulated, as the statement allows for the invoice.
 */
public final class SchoolInfo {

    public static final String LEGAL_NAME = "Colegio Internacional Little Horizons S.A.S.";
    public static final String NIT = "901.456.789-3";
    /** The NIT without dots or hyphen, as it goes into the CUFE. */
    public static final String NIT_DIGITS = "9014567893";
    public static final String ADDRESS = "Avenida 0 # 12-45, Barrio Caobos, Cúcuta, Norte de Santander";
    public static final String PHONE = "607 5712345";
    public static final String EMAIL = "facturacion@littlehorizons.edu.co";
    public static final String DIAN_RESOLUTION = "Resolución de habilitación DIAN No. 18764012345678 "
            + "del 15/01/2026 (simulada)";
    public static final String AUTHORIZED_RANGE = "Numeración autorizada: 000001 al 999999";

    private SchoolInfo() {
    }
}
