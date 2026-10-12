package lodge.model;

import java.math.BigDecimal;

/** MODEL: the lodge's price list. */
public enum RoomType {
    STANDARD("Standard", "350.00"),
    DELUXE("Deluxe", "550.00"),
    FAMILY("Family", "750.00");

    private final String label;
    private final BigDecimal pricePerNight;

    RoomType(String label, String pricePerNight) {
        this.label = label;
        this.pricePerNight = new BigDecimal(pricePerNight);
    }

    public BigDecimal pricePerNight() {
        return pricePerNight;
    }

    @Override
    public String toString() {
        return label;
    }
}
