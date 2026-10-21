package com.littlehorizons.erp.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * School data and invoicing settings, read from invoice.properties
 * (src/main/resources). They appear in the header of every invoice.
 */
public final class InvoiceSettings {

    private static final String CONFIG_FILE = "invoice.properties";

    private final Properties properties = new Properties();

    public InvoiceSettings() {
        try (InputStream input = InvoiceSettings.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            } else {
                AppLogger.warning(CONFIG_FILE + " was not found; default invoice settings are used");
            }
        } catch (IOException e) {
            AppLogger.error("Could not read " + CONFIG_FILE, e);
        }
    }

    public String schoolName() {
        return get("school.name", "Colegio Internacional Little Horizons S.A.S.");
    }

    public String schoolNit() {
        return get("school.nit", "900123456-7");
    }

    public String schoolAddress() {
        return get("school.address", "Carrera 15 # 100-20");
    }

    public String schoolPhone() {
        return get("school.phone", "(601) 555 0100");
    }

    public String schoolEmail() {
        return get("school.email", "facturacion@littlehorizons.edu.co");
    }

    public String invoicePrefix() {
        return get("invoice.prefix", "FE");
    }

    public long rangeFrom() {
        return Long.parseLong(get("resolution.range.from", "1"));
    }

    public long rangeTo() {
        return Long.parseLong(get("resolution.range.to", "100000"));
    }

    /** Text printed on the invoice about the (simulated) DIAN authorization. */
    public String resolutionText() {
        return "Resolución DIAN No. " + get("resolution.number", "18760000001")
                + " del " + get("resolution.date", "2026-01-15")
                + " - Prefijo " + invoicePrefix() + " del " + rangeFrom() + " al " + rangeTo()
                + " (resolución simulada)";
    }

    /** Folder where the PDF files are saved (relative to the folder where the program runs). */
    public Path outputFolder() {
        return Paths.get(get("output.folder", "invoices"));
    }

    private String get(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }
}
