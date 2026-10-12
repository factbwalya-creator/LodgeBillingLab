package lodge.controller;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * CONTROLLER helper: turns what the user typed into numbers.
 * An empty Optional means "this is not a valid entry". Empty text counts as zero.
 */
final class InputParser {

    static final int MAX_NIGHTS = 365;
    static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000");

    private static final Pattern WHOLE = Pattern.compile("\\d{1,4}");
    private static final Pattern MONEY = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private InputParser() {
    }

    static Optional<Integer> nights(String text) {
        String t = text.trim();
        if (t.isEmpty()) return Optional.of(0);
        if (!WHOLE.matcher(t).matches()) return Optional.empty();
        int v = Integer.parseInt(t);
        return v <= MAX_NIGHTS ? Optional.of(v) : Optional.empty();
    }

    /** Accepts 1000, 1,000 and 1000.50, but not negatives, letters, or more than 2 decimals. */
    static Optional<BigDecimal> money(String text) {
        String t = text.trim().replace(",", "");
        if (t.isEmpty()) return Optional.of(new BigDecimal("0.00"));
        if (!MONEY.matcher(t).matches()) return Optional.empty();
        BigDecimal v = new BigDecimal(t).setScale(2);
        return v.compareTo(MAX_AMOUNT) <= 0 ? Optional.of(v) : Optional.empty();
    }
}
