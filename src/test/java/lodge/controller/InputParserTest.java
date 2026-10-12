package lodge.controller;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InputParserTest {

    @Test
    void nights() {
        assertEquals(Optional.of(0), InputParser.nights(""));
        assertEquals(Optional.of(5), InputParser.nights(" 5 "));
        assertEquals(Optional.of(365), InputParser.nights("365"));
        assertEquals(Optional.empty(), InputParser.nights("366"));
        assertEquals(Optional.empty(), InputParser.nights("abc"));
        assertEquals(Optional.empty(), InputParser.nights("-1"));
        assertEquals(Optional.empty(), InputParser.nights("2.5"));
        assertEquals(Optional.empty(), InputParser.nights("99999999999"));
    }

    @Test
    void money() {
        assertEquals(Optional.of(new BigDecimal("0.00")), InputParser.money(""));
        assertEquals(Optional.of(new BigDecimal("1000.00")), InputParser.money("1,000"));
        assertEquals(Optional.of(new BigDecimal("150.50")), InputParser.money("150.5"));
        assertEquals(Optional.empty(), InputParser.money("12.345"));
        assertEquals(Optional.empty(), InputParser.money("-5"));
        assertEquals(Optional.empty(), InputParser.money("1e3"));
        assertEquals(Optional.empty(), InputParser.money("K100"));
        assertEquals(Optional.empty(), InputParser.money("10000001"));
    }
}
