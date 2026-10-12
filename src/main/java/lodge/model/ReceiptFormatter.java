package lodge.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static lodge.model.BillingCalculator.money;

/** MODEL: turns a saved bill into receipt text (also used for print and save-to-file). */
public final class ReceiptFormatter {

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    private ReceiptFormatter() {
    }

    public static String format(Bill b) {
        // The price per night is worked out from the saved bill, so an old receipt
        // stays correct even if the price list changes later.
        BigDecimal perNight = b.roomCharge().divide(BigDecimal.valueOf(Math.max(1, b.nights())), 2, RoundingMode.HALF_UP);

        StringBuilder sb = new StringBuilder();
        sb.append("===== LODGE RECEIPT =====\n");
        sb.append("Bill:     ").append(b.id()).append('\n');
        sb.append("Date:     ").append(b.createdAt().format(WHEN)).append('\n');
        sb.append("Guest:    ").append(b.guest()).append('\n');
        sb.append("Room:     ").append(b.room()).append(" (").append(b.nights())
                .append(b.nights() == 1 ? " night" : " nights").append(" @ ").append(money(perNight)).append(")\n\n");
        sb.append(row("Room charge:", money(b.roomCharge())));
        sb.append(row("Discount:", "-" + money(b.discount())));
        sb.append(row("Extras:", money(b.extras())));
        sb.append("-".repeat(29)).append('\n');
        sb.append(row("TOTAL:", money(b.total())));
        sb.append(row("Paid:", money(b.paid())));
        if (b.balance().signum() < 0) {
            sb.append(row("CHANGE DUE:", money(b.balance().negate())));
        } else {
            sb.append(row("BALANCE:", money(b.balance())));
        }
        if (!b.notes().isEmpty()) {
            sb.append("\nNotes: ").append(b.notes()).append('\n');
        }
        sb.append("\nServed by: ").append(b.servedBy()).append('\n');
        return sb.toString();
    }

    private static String row(String label, String value) {
        return String.format("%-14s%15s\n", label, value);
    }
}
