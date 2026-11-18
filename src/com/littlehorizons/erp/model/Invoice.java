package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Row of the facturas table: the electronic invoice of one payment. */
public class Invoice {

    private static final String NUMBER_FORMAT = "%06d";
    private static final String FILE_PREFIX = "factura_";
    /** The project generates the invoices as text files. */
    private static final String FILE_EXTENSION = ".txt";

    private int id;
    private String number;
    private Payment payment;
    private LocalDate issueDate;
    private String cufe;
    private String fileName;
    private String filePath;

    /** @return the consecutive number with six digits, for example 000123 */
    public static String formatNumber(int consecutive) {
        return String.format(NUMBER_FORMAT, consecutive);
    }

    /** @return a unique file name, for example factura_000123.txt */
    public static String fileNameFor(String number) {
        return FILE_PREFIX + number + FILE_EXTENSION;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public String getCufe() {
        return cufe;
    }

    public void setCufe(String cufe) {
        this.cufe = cufe;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
