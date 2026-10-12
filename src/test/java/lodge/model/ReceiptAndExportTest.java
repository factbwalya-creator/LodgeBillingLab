package lodge.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptAndExportTest {

    @TempDir
    Path dir;

    private static Bill bill(String guest, int nights, String paid, String notes) {
        var r = BillingCalculator.calculate(RoomType.DELUXE, nights, new BigDecimal("200"), new BigDecimal(paid));
        return new Bill("B003", guest, RoomType.DELUXE, nights, r.extras(), r.paid(), notes,
                r.roomCharge(), r.discount(), r.total(), r.balance(),
                LocalDateTime.of(2026, 10, 11, 20, 46), "admin");
    }

    @Test
    void receiptShowsBalanceAndNotes() {
        String text = ReceiptFormatter.format(bill("John Phiri", 5, "1000", "Late checkout"));
        assertTrue(text.contains("John Phiri"));
        assertTrue(text.contains("5 nights @ K 550.00"));
        assertTrue(text.contains("K 2,812.50"));
        assertTrue(text.contains("BALANCE:"));
        assertTrue(text.contains("Notes: Late checkout"));
        assertTrue(text.contains("Served by: admin"));
    }

    @Test
    void receiptSaysChangeDueWhenOverpaidAndOmitsEmptyNotes() {
        String text = ReceiptFormatter.format(bill("Mary Banda", 1, "1000", ""));
        assertTrue(text.contains("CHANGE DUE:"));
        assertFalse(text.contains("BALANCE:"));
        assertFalse(text.contains("Notes:"));
        assertTrue(text.contains("1 night @"));
    }

    @Test
    void csvQuotesCommasAndNeutralisesFormulas() throws IOException {
        Path out = dir.resolve("out.csv");
        BillCsvExporter.write(List.of(bill("=SUM(A1)", 5, "1000", "He said \"hi\", twice")), out);
        String csv = Files.readString(out);
        assertTrue(csv.startsWith("﻿Bill,Date,Guest"));
        assertTrue(csv.contains("'=SUM(A1)"));
        assertTrue(csv.contains("\"He said \"\"hi\"\", twice\""));
        assertTrue(csv.contains("2812.50"));
    }
}
