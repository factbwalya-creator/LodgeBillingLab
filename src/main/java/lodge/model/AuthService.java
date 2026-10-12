package lodge.model;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * MODEL: checks usernames and passwords.
 * Passwords are never stored. Each one is kept as a salted PBKDF2 hash, which is
 * deliberately slow so guessing is expensive. Practice accounts:
 * admin / lodge123 and reception / welcome1. A real system would keep these in a database.
 */
public final class AuthService {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final byte[] DUMMY_SALT = new byte[16];

    private record Credential(byte[] salt, byte[] hash) {
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Credential> accounts = new HashMap<>();

    public AuthService() {
        addAccount("admin", "lodge123");
        addAccount("reception", "welcome1");
    }

    private void addAccount(String username, String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        accounts.put(username, new Credential(salt, derive(password.toCharArray(), salt)));
    }

    /** Returns the canonical username if the login is correct, otherwise empty. */
    public Optional<String> authenticate(String username, char[] password) {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            return Optional.empty();
        }
        String key = username.trim().toLowerCase(Locale.ROOT);
        Credential c = accounts.get(key);
        // Hash even for unknown users so the response time does not reveal which names exist.
        byte[] attempt = derive(password, c != null ? c.salt() : DUMMY_SALT);
        boolean ok = c != null && MessageDigest.isEqual(attempt, c.hash());
        return ok ? Optional.of(key) : Optional.empty();
    }

    private static byte[] derive(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }
}
