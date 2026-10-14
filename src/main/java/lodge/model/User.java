package lodge.model;

/** MODEL: one account. Password is never stored — only its salt + PBKDF2 hash. */
public record User(int id, String username, String displayName, Role role,
                   byte[] salt, byte[] hash, boolean active) {

    public enum Role { ADMIN, RECEPTION }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}