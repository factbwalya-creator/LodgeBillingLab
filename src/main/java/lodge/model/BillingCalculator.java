package lodge.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** MODEL: the business rules. No Swing code, so it can be tested without a window. */
public final class BillingCalculator {

    public static final int DISCOUNT_MIN_NIGHTS = 5;
    public static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.05");

    private BillingCalculator() {
    }

    /** Result of a calculation. All amounts have exactly 2 decimal places. */
    public record Result(BigDecimal roomCharge, BigDecimal discount, BigDecimal extras,
                         BigDecimal total, BigDecimal paid, BigDecimal balance) {
    }

    public static Result calculate(RoomType room, int nights, BigDecimal extras, BigDecimal paid) {
        BigDecimal roomCharge = scale(room.pricePerNight().multiply(BigDecimal.valueOf(nights)));
        BigDecimal discount = nights >= DISCOUNT_MIN_NIGHTS
                ? scale(roomCharge.multiply(DISCOUNT_RATE))
                : scale(BigDecimal.ZERO);
        BigDecimal ex = scale(extras);
        BigDecimal pd = scale(paid);
        BigDecimal total = roomCharge.subtract(discount).add(ex); // extras: no discount, no tax
        BigDecimal balance = total.subtract(pd);                  // negative = change due
        return new Result(roomCharge, discount, ex, total, pd, balance);
    }

    /** "5%" - built from the constant so the screen text can never disagree with the rule. */
    public static String discountLabel() {
        return DISCOUNT_RATE.movePointRight(2).stripTrailingZeros().toPlainString() + "%";
    }

    public static String money(BigDecimal amount) {
        return String.format(Locale.US, "K %,.2f", amount);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
