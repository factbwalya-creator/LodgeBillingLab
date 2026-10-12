package lodge.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** MODEL: writes all bills to a CSV file that opens cleanly in Excel. */
public final class BillCsvExporter {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private BillCsvExporter() {
    }

    public static void write(List<Bill> bills, Path target) throws IOException {
        StringBuilder sb = new StringBuilder("﻿"); // byte-order mark so Excel reads UTF-8
        sb.append("Bill,Date,Guest,Room,Nights,Room charge,Discount,Extras,Total,Paid,Balance,Served by,Notes\r\n");
        for (Bill b : bills) {
            sb.append(text(b.id())).append(',')
                    .append(b.createdAt().format(WHEN)).append(',')
                    .append(text(b.guest())).append(',')
                    .append(text(b.room().toString())).append(',')
                    .append(b.nights()).append(',')
                    .append(b.roomCharge().toPlainString()).append(',')
                    .append(b.discount().toPlainString()).append(',')
                    .append(b.extras().toPlainString()).append(',')
                    .append(b.total().toPlainString()).append(',')
                    .append(b.paid().toPlainString()).append(',')
                    .append(b.balance().toPlainString()).append(',')
                    .append(text(b.servedBy())).append(',')
                    .append(text(b.notes())).append("\r\n");
        }
        Files.writeString(target, sb.toString(), StandardCharsets.UTF_8);
    }

    /** Quotes a text cell, and defuses spreadsheet formulas such as =CMD(...) typed into a name. */
    private static String text(String value) {
        String s = value;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
