package lodge.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** MODEL: one saved bill. Immutable data only, no logic. */
public record Bill(String id, String guest, RoomType room, int nights,
                   BigDecimal extras, BigDecimal paid, String notes,
                   BigDecimal roomCharge, BigDecimal discount, BigDecimal total, BigDecimal balance,
                   LocalDateTime createdAt, String servedBy) {

    public Bill {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(guest, "guest");
        Objects.requireNonNull(room, "room");
        Objects.requireNonNull(createdAt, "createdAt");
        notes = notes == null ? "" : notes;
        servedBy = servedBy == null ? "" : servedBy;
    }
}
