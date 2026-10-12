package lodge.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BillRepositoryTest {

    @TempDir
    Path dir;

    private static Bill bill(String id, String guest, String notes) {
        var r = BillingCalculator.calculate(RoomType.DELUXE, 5, new BigDecimal("200"), new BigDecimal("1000"));
        return new Bill(id, guest, RoomType.DELUXE, 5, r.extras(), r.paid(), notes,
                r.roomCharge(), r.discount(), r.total(), r.balance(),
                LocalDateTime.of(2026, 10, 11, 20, 46), "admin");
    }

    @Test
    void billsSurviveRestartIncludingAwkwardText() throws IOException {
        Path file = dir.resolve("bills.tsv");
        BillRepository a = new BillRepository(file);
        Bill tricky = bill("B001", "Tab\tName \\ Back", "line one\nline two\r\nend");
        a.add(tricky);
        a.add(bill("B002", "Mary Banda", ""));

        BillRepository b = new BillRepository(file);
        assertEquals(0, b.load());
        assertEquals(2, b.size());
        assertEquals(tricky, b.get(0));
        assertEquals("B003", b.nextId());
    }

    @Test
    void idsAreNotReusedAfterDeleteAndRestart() throws IOException {
        Path file = dir.resolve("bills.tsv");
        BillRepository a = new BillRepository(file);
        a.add(bill("B001", "A", ""));
        a.add(bill("B002", "B", ""));
        a.remove(1);

        BillRepository b = new BillRepository(file);
        b.load();
        assertEquals(1, b.size());
        assertEquals("B003", b.nextId());
    }

    @Test
    void badLinesAreSkippedAndFileIsBackedUp() throws IOException {
        Path file = dir.resolve("bills.tsv");
        BillRepository a = new BillRepository(file);
        a.add(bill("B001", "Good", ""));
        Files.writeString(file, Files.readString(file) + "this is not a bill\n");

        BillRepository b = new BillRepository(file);
        assertEquals(1, b.load());
        assertEquals(1, b.size());
        assertTrue(Files.exists(dir.resolve("bills.tsv.backup")));
    }

    @Test
    void missingFileMeansEmptyList() throws IOException {
        BillRepository r = new BillRepository(dir.resolve("none.tsv"));
        assertEquals(0, r.load());
        assertEquals(0, r.size());
        assertEquals("B001", r.nextId());
    }

    @Test
    void failedSaveRollsBackTheChange() throws IOException {
        // A directory where the file should be makes every save fail.
        Path file = dir.resolve("blocked");
        Files.createDirectories(file.resolve("child"));
        BillRepository r = new BillRepository(file);
        assertThrows(IOException.class, () -> r.add(bill("B001", "X", "")));
        assertEquals(0, r.size());
        assertEquals("B001", r.nextId());
    }

    @Test
    void totalsAddUpAndIgnoreOverpaymentInOutstanding() throws IOException {
        BillRepository r = new BillRepository(dir.resolve("bills.tsv"));
        r.add(bill("B001", "Owes", "")); // total 2812.50, paid 1000, balance 1812.50
        var over = BillingCalculator.calculate(RoomType.STANDARD, 1, BigDecimal.ZERO, new BigDecimal("400"));
        r.add(new Bill("B002", "Overpaid", RoomType.STANDARD, 1, over.extras(), over.paid(), "",
                over.roomCharge(), over.discount(), over.total(), over.balance(),
                LocalDateTime.of(2026, 10, 11, 21, 0), "admin"));

        var t = r.totals();
        assertEquals(2, t.count());
        assertEquals(new BigDecimal("3162.50"), t.billed());
        assertEquals(new BigDecimal("1350.00"), t.collected()); // 1000 + min(400, 350)
        assertEquals(new BigDecimal("1812.50"), t.outstanding());
    }

    @Test
    void listenersAreToldAboutChanges() throws IOException {
        BillRepository r = new BillRepository(dir.resolve("bills.tsv"));
        int[] calls = {0};
        Runnable l = () -> calls[0]++;
        r.addChangeListener(l);
        r.add(bill("B001", "A", ""));
        r.remove(0);
        assertEquals(2, calls[0]);
        r.removeChangeListener(l);
        r.add(bill("B002", "B", ""));
        assertEquals(2, calls[0]);
    }
}
