package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.InvoiceData;
import java.nio.file.Path;

/**
 * Creates the visual document of an invoice. The controller depends on this interface,
 * so the PDF library can be replaced without touching the business logic.
 */
public interface InvoiceDocumentGenerator {

    /**
     * Creates the document and saves it in the given file.
     *
     * @throws InvoiceGenerationException if the file could not be created
     */
    void generate(InvoiceData data, Path destination);
}
