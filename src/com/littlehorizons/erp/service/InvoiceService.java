package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.util.Formats;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Builds the electronic invoice of a payment as a text file: school data, customer (the guardian who
 * paid), detail, totals, simulated CUFE and simulated QR code.
 */
public class InvoiceService {

    /** Folder of the project where the invoices are saved. */
    public static final Path INVOICE_FOLDER = Path.of("facturas");

    private static final int LINE_WIDTH = 74;
    private static final int DESCRIPTION_WIDTH = 36;
    private static final int CUFE_LINE_LENGTH = 48;
    private static final int QUANTITY = 1;
    private static final int QR_SIZE = 21;
    private static final int FINDER_SIZE = 7;
    private static final String DARK_MODULE = "██";
    private static final String LIGHT_MODULE = "  ";
    private static final String NEW_LINE = System.lineSeparator();

    /**
     * Simulated CUFE: SHA-384 hash (the algorithm the DIAN uses for the real CUFE) of the invoice
     * number + the NIT + the issue date, as the statement suggests.
     */
    public String calculateCufe(Invoice invoice) {
        String data = invoice.getNumber() + SchoolInfo.NIT_DIGITS + invoice.getIssueDate();
        return HexFormat.of().formatHex(hash("SHA-384", data));
    }

    /** @return the absolute path of the file, for example ...\facturas\factura_000123.txt */
    public Path write(Invoice invoice) throws IOException {
        Files.createDirectories(INVOICE_FOLDER);
        Path file = INVOICE_FOLDER.resolve(invoice.getFileName());
        Files.writeString(file, buildText(invoice), StandardCharsets.UTF_8);
        return file.toAbsolutePath();
    }

    String buildText(Invoice invoice) {
        Payment payment = invoice.getPayment();
        Guardian customer = payment.getPayer();
        BigDecimal subtotal = payment.getAmount();
        // The concepts have no VAT field: the statement asks for it only "si aplica".
        BigDecimal vat = BigDecimal.ZERO;
        BigDecimal total = subtotal.add(vat);

        List<String> lines = new ArrayList<>();
        lines.add("=".repeat(LINE_WIDTH));
        lines.add(center("FACTURA ELECTRÓNICA DE VENTA"));
        lines.add("=".repeat(LINE_WIDTH));
        lines.add(SchoolInfo.LEGAL_NAME);
        lines.add("NIT: " + SchoolInfo.NIT);
        lines.add("Dirección: " + SchoolInfo.ADDRESS);
        lines.add("Teléfono: " + SchoolInfo.PHONE + "    Correo: " + SchoolInfo.EMAIL);
        lines.add(SchoolInfo.DIAN_RESOLUTION);
        lines.add(SchoolInfo.AUTHORIZED_RANGE);
        lines.add("-".repeat(LINE_WIDTH));
        lines.add(String.format("Factura No.: %-30s Fecha de emisión: %s", invoice.getNumber(),
                Formats.date(invoice.getIssueDate())));
        lines.add("-".repeat(LINE_WIDTH));
        lines.add("CLIENTE (acudiente responsable del pago)");
        lines.add("Nombre: " + customer.getFullName());
        lines.add("Documento: " + customer.getDocument());
        lines.add("Dirección: " + Formats.text(customer.getAddress()));
        lines.add("-".repeat(LINE_WIDTH));
        lines.add(String.format("%-" + DESCRIPTION_WIDTH + "s %5s %15s %15s", "Descripción", "Cant.",
                "Valor unitario", "Subtotal"));
        lines.add(String.format("%-" + DESCRIPTION_WIDTH + "s %5d %15s %15s", fit(payment.getConcept().getName()),
                QUANTITY, Formats.money(payment.getAmount()), Formats.money(subtotal)));
        if (payment.getNotes() != null && !payment.getNotes().isBlank()) {
            lines.add("  " + payment.getNotes());
        }
        lines.add("  Estudiante: " + payment.getStudent().getFullName() + " (" + payment.getStudent().getDocument() + ")");
        lines.add("-".repeat(LINE_WIDTH));
        lines.add(totalLine("Subtotal:", subtotal));
        lines.add(totalLine("IVA (no aplica):", vat));
        lines.add(totalLine("TOTAL:", total));
        lines.add("-".repeat(LINE_WIDTH));
        lines.add("Forma de pago: " + payment.getMethod().getDatabaseValue() + "    Recibo: "
                + payment.getReceiptNumber());
        lines.add("Fecha de pago: " + Formats.date(payment.getPaymentDate()));
        lines.add("-".repeat(LINE_WIDTH));
        lines.add("CUFE (simulado):");
        for (int start = 0; start < invoice.getCufe().length(); start += CUFE_LINE_LENGTH) {
            lines.add("  " + invoice.getCufe().substring(start,
                    Math.min(start + CUFE_LINE_LENGTH, invoice.getCufe().length())));
        }
        lines.add("");
        lines.add("Código QR (simulado):");
        List<String> qrContent = qrContent(invoice, vat, total);
        lines.addAll(qrDrawing(String.join("|", qrContent)));
        lines.add("Contenido del código QR:");
        qrContent.forEach(item -> lines.add("  " + item));
        lines.add("=".repeat(LINE_WIDTH));
        return String.join(NEW_LINE, lines) + NEW_LINE;
    }

    /** The same fields a DIAN QR code has: number, date, NITs, values and CUFE. */
    private static List<String> qrContent(Invoice invoice, BigDecimal vat, BigDecimal total) {
        Payment payment = invoice.getPayment();
        return List.of(
                "NumFac: " + invoice.getNumber(),
                "FecFac: " + invoice.getIssueDate(),
                "NitFac: " + SchoolInfo.NIT_DIGITS,
                "DocAdq: " + payment.getPayer().getDocumentNumber(),
                "ValFac: " + qrValue(payment.getAmount()),
                "ValIva: " + qrValue(vat),
                "ValTolFac: " + qrValue(total),
                "CUFE: " + invoice.getCufe());
    }

    /**
     * Draws a 21 x 21 pattern that looks like a QR code: the three corner squares of every QR and, in the
     * rest, modules taken from the hash of the content. It is a simulation: it cannot be scanned.
     */
    private static List<String> qrDrawing(String content) {
        byte[] bits = hash("SHA-256", content);
        List<String> rows = new ArrayList<>();
        int bitIndex = 0;
        for (int row = 0; row < QR_SIZE; row++) {
            StringBuilder line = new StringBuilder("  ");
            for (int column = 0; column < QR_SIZE; column++) {
                boolean dark;
                if (isFinderArea(row, column)) {
                    dark = isFinderDark(row % (QR_SIZE - FINDER_SIZE), column % (QR_SIZE - FINDER_SIZE));
                } else {
                    int bit = bitIndex++ % (bits.length * Byte.SIZE);
                    dark = (bits[bit / Byte.SIZE] >> (bit % Byte.SIZE) & 1) == 1;
                }
                line.append(dark ? DARK_MODULE : LIGHT_MODULE);
            }
            rows.add(line.toString());
        }
        return rows;
    }

    /** The finder squares (with their white border) are in the top-left, top-right and bottom-left corners. */
    private static boolean isFinderArea(int row, int column) {
        int limit = FINDER_SIZE + 1;
        boolean top = row < limit;
        boolean left = column < limit;
        boolean right = column >= QR_SIZE - limit;
        boolean bottom = row >= QR_SIZE - limit;
        return (top && left) || (top && right) || (bottom && left);
    }

    /** Inside a finder area: dark outer ring, light ring, dark 3 x 3 center, light border. */
    private static boolean isFinderDark(int row, int column) {
        if (row >= FINDER_SIZE || column >= FINDER_SIZE) {
            return false;
        }
        boolean outerRing = row == 0 || row == FINDER_SIZE - 1 || column == 0 || column == FINDER_SIZE - 1;
        boolean center = row >= 2 && row <= 4 && column >= 2 && column <= 4;
        return outerRing || center;
    }

    /** Values of the QR always with two decimals, like the DECIMAL(10,2) columns. */
    private static String qrValue(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String totalLine(String label, BigDecimal value) {
        return String.format("%" + (LINE_WIDTH - 16) + "s %15s", label, Formats.money(value));
    }

    private static String center(String text) {
        int padding = Math.max(0, (LINE_WIDTH - text.length()) / 2);
        return " ".repeat(padding) + text;
    }

    private static String fit(String text) {
        return text.length() <= DESCRIPTION_WIDTH ? text : text.substring(0, DESCRIPTION_WIDTH - 3) + "...";
    }

    private static byte[] hash(String algorithm, String data) {
        try {
            return MessageDigest.getInstance(algorithm).digest(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // Every JVM must include SHA-256 and SHA-384.
            throw new IllegalStateException("La JVM no tiene el algoritmo " + algorithm, e);
        }
    }
}
