package lodge.model;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MODEL: owns the list of saved bills and keeps them on disk (one tab-separated
 * line per bill). Views listen for changes instead of keeping their own copy.
 */
public final class BillRepository {

    private static final String HEADER = "#lodge-billing-lab v1";
    private static final int FIELD_COUNT = 13;

    /** Summary numbers for the footer. */
    public record Totals(int count, BigDecimal billed, BigDecimal collected, BigDecimal outstanding) {
    }

    private final Path file;
    private final List<Bill> bills = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private int nextNumber = 1;

    public BillRepository(Path file) {
        this.file = file.toAbsolutePath();
    }

    public static Path defaultFile() {
        return Path.of(System.getProperty("user.home"), ".lodge-billing-lab", "bills.tsv");
    }

    public Path file() {
        return file;
    }

    // ---------- change notification ----------

    public void addChangeListener(Runnable r) {
        listeners.add(r);
    }

    public void removeChangeListener(Runnable r) {
        listeners.remove(r);
    }

    private void fireChanged() {
        for (Runnable r : new ArrayList<>(listeners)) {
            r.run();
        }
    }

    // ---------- reading ----------

    public List<Bill> all() {
        return Collections.unmodifiableList(bills);
    }

    public Bill get(int index) {
        return bills.get(index);
    }

    public int size() {
        return bills.size();
    }

    /** The id the next saved bill will get, e.g. B004. */
    public String nextId() {
        return String.format("B%03d", nextNumber);
    }

    public Totals totals() {
        BigDecimal billed = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        for (Bill b : bills) {
            billed = billed.add(b.total());
            collected = collected.add(b.paid().min(b.total()));
            outstanding = outstanding.add(b.balance().max(BigDecimal.ZERO));
        }
        return new Totals(bills.size(), billed, collected, outstanding);
    }

    // ---------- changing ----------

    public void add(Bill bill) throws IOException {
        int previousNext = nextNumber;
        bills.add(bill);
        nextNumber = Math.max(nextNumber, numberOf(bill.id()) + 1);
        try {
            save();
        } catch (IOException e) {
            bills.remove(bills.size() - 1);
            nextNumber = previousNext;
            throw e;
        }
        fireChanged();
    }

    public Bill remove(int index) throws IOException {
        Bill removed = bills.remove(index);
        try {
            save();
        } catch (IOException e) {
            bills.add(index, removed);
            throw e;
        }
        fireChanged();
        return removed;
    }

    // ---------- disk ----------

    /**
     * Loads bills from disk. Returns how many unreadable lines were skipped.
     * If anything is wrong, the original file is copied to a .backup file first
     * so a later save can never destroy the only copy.
     */
    public int load() throws IOException {
        bills.clear();
        nextNumber = 1;
        int skipped = 0;
        int highest = 0;
        if (Files.exists(file)) {
            List<String> lines;
            try {
                lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                backupCorruptFile();
                throw e;
            }
            for (String line : lines) {
                if (line.isBlank()) continue;
                if (line.startsWith("#")) {
                    if (line.startsWith("#next\t")) {
                        try {
                            nextNumber = Math.max(nextNumber, Integer.parseInt(line.substring(6).trim()));
                        } catch (NumberFormatException ignored) {
                            // the highest id seen below still protects us
                        }
                    }
                    continue;
                }
                try {
                    Bill b = fromLine(line);
                    bills.add(b);
                    highest = Math.max(highest, numberOf(b.id()));
                } catch (RuntimeException e) {
                    skipped++;
                }
            }
            if (skipped > 0) {
                backupCorruptFile();
            }
        }
        nextNumber = Math.max(nextNumber, highest + 1);
        fireChanged();
        return skipped;
    }

    private void backupCorruptFile() {
        try {
            Path backup = file.resolveSibling(file.getFileName() + ".backup");
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // best effort
        }
    }

    private void save() throws IOException {
        Files.createDirectories(file.getParent());
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.add("#next\t" + nextNumber);
        for (Bill b : bills) {
            lines.add(toLine(b));
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ---------- line format ----------

    private static int numberOf(String id) {
        try {
            return Integer.parseInt(id.substring(1));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static String toLine(Bill b) {
        return String.join("\t",
                escape(b.id()), escape(b.guest()), b.room().name(), Integer.toString(b.nights()),
                b.extras().toPlainString(), b.paid().toPlainString(), escape(b.notes()),
                b.roomCharge().toPlainString(), b.discount().toPlainString(),
                b.total().toPlainString(), b.balance().toPlainString(),
                b.createdAt().toString(), escape(b.servedBy()));
    }

    private static Bill fromLine(String line) {
        String[] f = line.split("\t", -1);
        if (f.length != FIELD_COUNT) {
            throw new IllegalArgumentException("wrong number of fields");
        }
        return new Bill(unescape(f[0]), unescape(f[1]), RoomType.valueOf(f[2]), Integer.parseInt(f[3]),
                new BigDecimal(f[4]), new BigDecimal(f[5]), unescape(f[6]),
                new BigDecimal(f[7]), new BigDecimal(f[8]), new BigDecimal(f[9]), new BigDecimal(f[10]),
                LocalDateTime.parse(f[11]), unescape(f[12]));
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\t' -> sb.append("\\t");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 't' -> sb.append('\t');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    default -> sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
