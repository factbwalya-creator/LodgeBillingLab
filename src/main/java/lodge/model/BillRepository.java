package lodge.model;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MODEL: owns the in-memory list of bills and keeps SQLite in sync through {@link Database}.
 * Views listen for changes instead of keeping their own copy.
 */
public final class BillRepository {

    /** Summary numbers for the footer. */
    public record Totals(int count, BigDecimal billed, BigDecimal collected, BigDecimal outstanding) { }

    private final Database db;
    private final List<Bill> cache = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private int nextNumber = 1;

    public BillRepository(Database db) {
        this.db = db;
    }

    public Database database() {
        return db;
    }

    // ---------- change notification ----------

    public void addChangeListener(Runnable r) { listeners.add(r); }
    public void removeChangeListener(Runnable r) { listeners.remove(r); }
    private void fireChanged() { for (Runnable r : new ArrayList<>(listeners)) r.run(); }

    // ---------- reading ----------

    public List<Bill> all() { return Collections.unmodifiableList(cache); }
    public Bill get(int index) { return cache.get(index); }
    public int size() { return cache.size(); }
    public String nextId() { return String.format("B%03d", nextNumber); }

    public Totals totals() {
        BigDecimal billed = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        for (Bill b : cache) {
            billed = billed.add(b.total());
            collected = collected.add(b.paid().min(b.total()));
            outstanding = outstanding.add(b.balance().max(BigDecimal.ZERO));
        }
        return new Totals(cache.size(), billed, collected, outstanding);
    }

    // ---------- changing ----------

    public int load() throws SQLException {
        cache.clear();
        cache.addAll(db.loadBills());
        nextNumber = cache.stream().mapToInt(b -> parseNum(b.id())).max().orElse(0) + 1;
        fireChanged();
        return 0;
    }

    public void add(Bill bill) throws SQLException {
        db.insertBill(bill);
        cache.add(bill);
        nextNumber = Math.max(nextNumber, parseNum(bill.id()) + 1);
        fireChanged();
    }

    public Bill remove(int index) throws SQLException {
        Bill removed = cache.remove(index);
        try {
            db.deleteBill(removed.id());
        } catch (SQLException e) {
            cache.add(index, removed);
            throw e;
        }
        fireChanged();
        return removed;
    }

    private static int parseNum(String id) {
        try { return Integer.parseInt(id.substring(1)); }
        catch (RuntimeException e) { return 0; }
    }
}