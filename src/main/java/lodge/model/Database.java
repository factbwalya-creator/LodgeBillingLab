package lodge.model;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MODEL: the single place that talks to SQLite.
 * All SQL lives here — nothing else in the app writes queries.
 */
public final class Database {

    private final Path file;
    private final String url;

    public Database(Path file) throws SQLException, IOException {
        this.file = file.toAbsolutePath();
        Files.createDirectories(this.file.getParent());
        this.url = "jdbc:sqlite:" + this.file;
        init();
    }

    public static Path defaultFile() {
        return Path.of(System.getProperty("user.home"), ".lodge-billing-lab", "lodge.db");
    }

    public Path file() {
        return file;
    }

    private Connection open() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    private void init() throws SQLException {
        try (Connection c = open(); Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT NOT NULL UNIQUE,
                    display_name  TEXT NOT NULL,
                    role          TEXT NOT NULL,
                    salt          BLOB NOT NULL,
                    hash          BLOB NOT NULL,
                    active        INTEGER NOT NULL DEFAULT 1,
                    created_at    TEXT NOT NULL
                )
            """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    key    TEXT PRIMARY KEY,
                    value  BLOB NOT NULL
                )
            """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS bills (
                    id            TEXT PRIMARY KEY,
                    guest         TEXT NOT NULL,
                    room          TEXT NOT NULL,
                    nights        INTEGER NOT NULL,
                    extras        TEXT NOT NULL,
                    paid          TEXT NOT NULL,
                    notes         TEXT NOT NULL DEFAULT '',
                    room_charge   TEXT NOT NULL,
                    discount      TEXT NOT NULL,
                    total         TEXT NOT NULL,
                    balance       TEXT NOT NULL,
                    created_at    TEXT NOT NULL,
                    served_by     TEXT NOT NULL
                )
            """);
            s.execute("CREATE INDEX IF NOT EXISTS idx_bills_guest ON bills(guest)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_bills_created ON bills(created_at)");
        }
    }

    // =================================================================
    //  Users
    // =================================================================

    public int countUsers() throws SQLException {
        try (Connection c = open();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM users")) {
            return r.next() ? r.getInt(1) : 0;
        }
    }

    public Optional<User> findUserByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, display_name, role, salt, hash, active " +
                "FROM users WHERE username = ?";
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return Optional.empty();
                return Optional.of(new User(
                        r.getInt("id"),
                        r.getString("username"),
                        r.getString("display_name"),
                        User.Role.valueOf(r.getString("role")),
                        r.getBytes("salt"),
                        r.getBytes("hash"),
                        r.getInt("active") == 1));
            }
        }
    }

    public User insertUser(String username, String displayName, User.Role role,
                           byte[] salt, byte[] hash) throws SQLException {
        String sql = """
            INSERT INTO users (username, display_name, role, salt, hash, active, created_at)
            VALUES (?, ?, ?, ?, ?, 1, ?)
        """;
        try (Connection c = open();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, username);
            p.setString(2, displayName);
            p.setString(3, role.name());
            p.setBytes(4, salt);
            p.setBytes(5, hash);
            p.setString(6, LocalDateTime.now().toString());
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : -1;
                return new User(id, username, displayName, role, salt, hash, true);
            }
        }
    }

    public List<User> listUsers() throws SQLException {
        String sql = "SELECT id, username, display_name, role, salt, hash, active " +
                "FROM users ORDER BY username";
        List<User> out = new ArrayList<>();
        try (Connection c = open();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                out.add(new User(
                        r.getInt("id"),
                        r.getString("username"),
                        r.getString("display_name"),
                        User.Role.valueOf(r.getString("role")),
                        r.getBytes("salt"),
                        r.getBytes("hash"),
                        r.getInt("active") == 1));
            }
        }
        return out;
    }

    public void setUserActive(int userId, boolean active) throws SQLException {
        try (Connection c = open();
             PreparedStatement p = c.prepareStatement("UPDATE users SET active = ? WHERE id = ?")) {
            p.setInt(1, active ? 1 : 0);
            p.setInt(2, userId);
            p.executeUpdate();
        }
    }

    public void updateUserPassword(int userId, byte[] salt, byte[] hash) throws SQLException {
        try (Connection c = open();
             PreparedStatement p = c.prepareStatement(
                     "UPDATE users SET salt = ?, hash = ? WHERE id = ?")) {
            p.setBytes(1, salt);
            p.setBytes(2, hash);
            p.setInt(3, userId);
            p.executeUpdate();
        }
    }

    // =================================================================
    //  Officiating code (settings table)
    // =================================================================

    public byte[] getOfficiatingSalt() throws SQLException {
        return getSettingBlob("officiating_salt");
    }

    public byte[] getOfficiatingHash() throws SQLException {
        return getSettingBlob("officiating_hash");
    }

    public void saveOfficiatingCode(byte[] salt, byte[] hash) throws SQLException {
        putSettingBlob("officiating_salt", salt);
        putSettingBlob("officiating_hash", hash);
    }

    private byte[] getSettingBlob(String key) throws SQLException {
        try (Connection c = open();
             PreparedStatement p = c.prepareStatement("SELECT value FROM settings WHERE key = ?")) {
            p.setString(1, key);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getBytes(1) : null;
            }
        }
    }

    private void putSettingBlob(String key, byte[] value) throws SQLException {
        try (Connection c = open();
             PreparedStatement p = c.prepareStatement(
                     "INSERT INTO settings(key, value) VALUES(?, ?) " +
                             "ON CONFLICT(key) DO UPDATE SET value = excluded.value")) {
            p.setString(1, key);
            p.setBytes(2, value);
            p.executeUpdate();
        }
    }

    // =================================================================
    //  Bills
    // =================================================================

    public List<Bill> loadBills() throws SQLException {
        List<Bill> out = new ArrayList<>();
        String sql = "SELECT * FROM bills ORDER BY created_at";
        try (Connection c = open();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                out.add(new Bill(
                        r.getString("id"),
                        r.getString("guest"),
                        RoomType.valueOf(r.getString("room")),
                        r.getInt("nights"),
                        new BigDecimal(r.getString("extras")),
                        new BigDecimal(r.getString("paid")),
                        r.getString("notes"),
                        new BigDecimal(r.getString("room_charge")),
                        new BigDecimal(r.getString("discount")),
                        new BigDecimal(r.getString("total")),
                        new BigDecimal(r.getString("balance")),
                        LocalDateTime.parse(r.getString("created_at")),
                        r.getString("served_by")));
            }
        }
        return out;
    }

    public void insertBill(Bill b) throws SQLException {
        String sql = """
            INSERT INTO bills (id, guest, room, nights, extras, paid, notes,
                               room_charge, discount, total, balance, created_at, served_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, b.id());
            p.setString(2, b.guest());
            p.setString(3, b.room().name());
            p.setInt(4, b.nights());
            p.setString(5, b.extras().toPlainString());
            p.setString(6, b.paid().toPlainString());
            p.setString(7, b.notes());
            p.setString(8, b.roomCharge().toPlainString());
            p.setString(9, b.discount().toPlainString());
            p.setString(10, b.total().toPlainString());
            p.setString(11, b.balance().toPlainString());
            p.setString(12, b.createdAt().toString());
            p.setString(13, b.servedBy());
            p.executeUpdate();
        }
    }

    public void deleteBill(String id) throws SQLException {
        try (Connection c = open();
             PreparedStatement p = c.prepareStatement("DELETE FROM bills WHERE id = ?")) {
            p.setString(1, id);
            p.executeUpdate();
        }
    }
}