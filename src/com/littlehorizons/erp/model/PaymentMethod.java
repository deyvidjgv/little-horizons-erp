package com.littlehorizons.erp.model;

/** Values of the pagos.metodo_pago column. */
public enum PaymentMethod implements DatabaseValue {
    CASH("Efectivo"),
    TRANSFER("Transferencia"),
    DEBIT_CARD("Tarjeta Débito"),
    CREDIT_CARD("Tarjeta Crédito"),
    CHECK("Cheque");

    private final String databaseValue;

    PaymentMethod(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
