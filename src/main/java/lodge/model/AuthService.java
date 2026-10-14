package lodge.model;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * MODEL: authenticates users, creates accounts, and manages the officiating code.
 * Accounts live in the SQLite database (table: users).
 * The officiating code is required to create any new account.
 */
public final class AuthService {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int MIN_PASSWORD = 6;
    private static final byte[] DUMMY_SALT = new byte[16];

    private final Database db;
    private final SecureRandom random = new SecureRandom();

    public AuthService(Database db) throws SQLException {
        this.db = db;
        seedDefaultAccountsIfEmpty();
    }

    /** On first run only, create the two practice accounts. */
    private void seedDefaultAccountsIfEmpty() throws SQLException {
        if (db.countUsers() > 0) return;
        createUser("admin", "Administrator", "lodge123".toCharArray(), User.Role.ADMIN);
        createUser("reception", "Receptionist", "welcome1".toCharArray(), User.Role.RECEPTION);
    }

    // =================================================================
    //  Login
    // =================================================================

    public Optional<User> authenticate(String username, char[] password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            return Optional.empty();
        }
        String key = username.trim().toLowerCase(Locale.ROOT);
        User u = db.findUserByUsername(key).orElse(null);
        // Hash even for unknown users so timing does not reveal which names exist.
        byte[] attempt = derive(password, u != null ? u.salt() : DUMMY_SALT);
        boolean ok = u != null && u.active() && MessageDigest.isEqual(attempt, u.hash());
        return ok ? Optional.of(u) : Optional.empty();
    }

    // =================================================================
    //  Officiating code
    // =================================================================

    public boolean hasOfficiatingCode() throws SQLException {
        return db.getOfficiatingHash() != null;
    }

    public boolean officiatingCodeMatches(char[] code) throws SQLException {
        byte[] salt = db.getOfficiatingSalt();
        byte[] stored = db.getOfficiatingHash();
        if (salt == null || stored == null || code == null) return false;
        byte[] attempt = derive(code, salt);
        return MessageDigest.isEqual(attempt, stored);
    }

    public void setOfficiatingCode(char[] code) throws SQLException {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        db.saveOfficiatingCode(salt, derive(code, salt));
    }

    // =================================================================
    //  Account creation and admin actions
    // =================================================================

    public User createUser(String username, String displayName, char[] password, User.Role role)
            throws SQLException {
        String key = username.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) throw new IllegalArgumentException("Username is required.");
        if (displayName == null || displayName.isBlank())
            throw new IllegalArgumentException("Display name is required.");
        if (password == null || password.length < MIN_PASSWORD)
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD + " characters.");
        if (db.findUserByUsername(key).isPresent())
            throw new IllegalArgumentException("That username is already taken.");

        byte[] salt = new byte[16];
        random.nextBytes(salt);
        byte[] hash = derive(password, salt);
        return db.insertUser(key, displayName.trim(), role, salt, hash);
    }

    public void setUserActive(int userId, boolean active) throws SQLException {
        db.setUserActive(userId, active);
    }

    public void resetPassword(int userId, char[] newPassword) throws SQLException {
        if (newPassword == null || newPassword.length < MIN_PASSWORD)
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD + " characters.");
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        byte[] hash = derive(newPassword, salt);
        db.updateUserPassword(userId, salt, hash);
    }

    // =================================================================

    private static byte[] derive(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
            Arrays.fill(password, '\0');
        }
    }
}