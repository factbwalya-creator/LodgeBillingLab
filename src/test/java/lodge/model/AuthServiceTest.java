package lodge.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthServiceTest {

    private static AuthService auth;

    @BeforeAll
    static void setUp() {
        auth = new AuthService();
    }

    @Test
    void correctLoginWorksAndUsernameIsNotCaseSensitive() {
        assertEquals(Optional.of("admin"), auth.authenticate("admin", "lodge123".toCharArray()));
        assertEquals(Optional.of("reception"), auth.authenticate("  Reception ", "welcome1".toCharArray()));
    }

    @Test
    void wrongPasswordUnknownUserAndEmptyInputAreRejected() {
        assertEquals(Optional.empty(), auth.authenticate("admin", "wrong".toCharArray()));
        assertEquals(Optional.empty(), auth.authenticate("nobody", "lodge123".toCharArray()));
        assertEquals(Optional.empty(), auth.authenticate("admin", new char[0]));
        assertEquals(Optional.empty(), auth.authenticate("", "lodge123".toCharArray()));
        assertEquals(Optional.empty(), auth.authenticate(null, "lodge123".toCharArray()));
    }

    @Test
    void passwordIsCaseSensitive() {
        assertEquals(Optional.empty(), auth.authenticate("admin", "LODGE123".toCharArray()));
    }
}
