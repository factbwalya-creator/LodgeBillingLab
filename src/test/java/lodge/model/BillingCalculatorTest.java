package lodge.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillingCalculatorTest {

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    @Test
    void noDiscountUnderFiveNights() {
        var r = BillingCalculator.calculate(RoomType.STANDARD, 4, bd("100"), bd("0"));
        assertEquals(bd("1400.00"), r.roomCharge());
        assertEquals(bd("0.00"), r.discount());
        assertEquals(bd("1500.00"), r.total());
    }

    @Test
    void fivePercentDiscountFromFiveNightsOnRoomChargeOnly() {
        // The example from the design: Deluxe, 5 nights, K200 extras, K1000 paid.
        var r = BillingCalculator.calculate(RoomType.DELUXE, 5, bd("200"), bd("1000"));
        assertEquals(bd("2750.00"), r.roomCharge());
        assertEquals(bd("137.50"), r.discount());
        assertEquals(bd("2812.50"), r.total());
        assertEquals(bd("1812.50"), r.balance());
    }

    @Test
    void overpaymentGivesNegativeBalanceMeaningChangeDue() {
        var r = BillingCalculator.calculate(RoomType.STANDARD, 1, bd("0"), bd("400"));
        assertEquals(bd("-50.00"), r.balance());
    }

    @Test
    void noFloatingPointDrift() {
        var r = BillingCalculator.calculate(RoomType.FAMILY, 7, bd("0.10"), bd("0.20"));
        assertEquals(bd("4987.60"), r.total());
        assertEquals(bd("4987.40"), r.balance());
    }

    @Test
    void moneyAndDiscountLabelFormatting() {
        assertEquals("K 1,234.50", BillingCalculator.money(bd("1234.5")));
        assertEquals("5%", BillingCalculator.discountLabel());
    }
}
