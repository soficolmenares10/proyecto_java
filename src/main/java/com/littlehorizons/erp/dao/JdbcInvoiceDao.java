package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceLine;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of InvoiceDao (tables "facturas" and "detalle_factura"). */
public class JdbcInvoiceDao implements InvoiceDao {

    private static final String SELECT =
            "SELECT f.id_factura, f.numero_factura, f.id_pago, p.numero_recibo, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) "
            + "AS nombre_estudiante, f.fecha_emision, f.subtotal, f.valor_iva, f.total, f.cufe, "
            + "f.nombre_archivo_pdf, f.ruta_archivo "
            + "FROM facturas f "
            + "JOIN pagos p ON p.id_pago = f.id_pago "
            + "JOIN estudiantes e ON e.id_estudiante = p.id_estudiante ";

    @Override
    public long nextSequence() {
        int last = JdbcSupport.queryCount(
                "SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(numero_factura, '-', -1) AS UNSIGNED)), 0) FROM facturas",
                statement -> { });
        return last + 1L;
    }

    @Override
    public int insert(Invoice invoice, List<InvoiceLine> lines) {
        String sql = "INSERT INTO facturas (numero_factura, id_pago, fecha_emision, subtotal, valor_iva, total, "
                + "cufe, nombre_archivo_pdf, ruta_archivo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            int invoiceId = 0;
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, invoice.number());
                statement.setInt(2, invoice.paymentId());
                JdbcSupport.setDate(statement, 3, invoice.issueDate());
                statement.setBigDecimal(4, invoice.subtotal());
                statement.setBigDecimal(5, invoice.vat());
                statement.setBigDecimal(6, invoice.total());
                statement.setString(7, invoice.cufe());
                statement.setString(8, invoice.pdfFileName());
                statement.setString(9, invoice.pdfPath());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        invoiceId = keys.getInt(1);
                    }
                }
            }
            try (PreparedStatement lineStatement = connection.prepareStatement(
                    "INSERT INTO detalle_factura (id_factura, descripcion, cantidad, valor_unitario, subtotal) "
                    + "VALUES (?, ?, ?, ?, ?)")) {
                for (InvoiceLine line : lines) {
                    lineStatement.setInt(1, invoiceId);
                    lineStatement.setString(2, line.description());
                    lineStatement.setInt(3, line.quantity());
                    lineStatement.setBigDecimal(4, line.unitPrice());
                    lineStatement.setBigDecimal(5, line.subtotal());
                    lineStatement.executeUpdate();
                }
            }
            return invoiceId;
        });
    }

    @Override
    public Optional<Invoice> findById(int id) {
        return JdbcSupport.queryList(SELECT + "WHERE f.id_factura = ?",
                statement -> statement.setInt(1, id), this::map).stream().findFirst();
    }

    @Override
    public Optional<Invoice> findByNumber(String number) {
        return JdbcSupport.queryList(SELECT + "WHERE f.numero_factura = ?",
                statement -> statement.setString(1, number), this::map).stream().findFirst();
    }

    @Override
    public Optional<Invoice> findByPaymentId(int paymentId) {
        return JdbcSupport.queryList(SELECT + "WHERE f.id_pago = ?",
                statement -> statement.setInt(1, paymentId), this::map).stream().findFirst();
    }

    @Override
    public List<InvoiceLine> findLines(int invoiceId) {
        return JdbcSupport.queryList("SELECT descripcion, cantidad, valor_unitario, subtotal FROM detalle_factura "
                + "WHERE id_factura = ? ORDER BY id_detalle", statement -> statement.setInt(1, invoiceId),
                rs -> new InvoiceLine(rs.getString("descripcion"), rs.getInt("cantidad"),
                        rs.getBigDecimal("valor_unitario"), rs.getBigDecimal("subtotal")));
    }

    @Override
    public List<Invoice> findAll() {
        return JdbcSupport.queryList(SELECT + "ORDER BY f.id_factura DESC", statement -> { }, this::map);
    }

    private Invoice map(ResultSet rs) throws SQLException {
        return new Invoice(rs.getInt("id_factura"), rs.getString("numero_factura"), rs.getInt("id_pago"),
                rs.getString("numero_recibo"), rs.getString("nombre_estudiante"),
                JdbcSupport.toLocalDate(rs.getDate("fecha_emision")), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("valor_iva"), rs.getBigDecimal("total"), rs.getString("cufe"),
                rs.getString("nombre_archivo_pdf"), rs.getString("ruta_archivo"));
    }
}
