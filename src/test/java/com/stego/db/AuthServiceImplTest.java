package com.stego.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.stego.common.UserDao;
import com.stego.common.exception.AuthException;
import com.stego.common.model.User;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/** Unit tests: the database is replaced by an in-memory fake, so MySQL is not needed. */
public class AuthServiceImplTest {

    private static class FakeUserDao implements UserDao {
        final Map<String, User> byUsername = new HashMap<>();
        boolean failCreateWithDuplicate = false;
        private int nextId = 1;

        @Override
        public User findByUsername(String username) {
            return byUsername.get(username);
        }

        @Override
        public boolean existsByUsername(String username) {
            return byUsername.containsKey(username);
        }

        @Override
        public boolean existsByEmail(String email) {
            return byUsername.values().stream().anyMatch(u -> u.getEmail().equals(email));
        }

        @Override
        public int create(User user) {
            if (failCreateWithDuplicate) {
                throw new IllegalStateException("dup", new SQLIntegrityConstraintViolationException("dup"));
            }
            int id = nextId++;
            byUsername.put(user.getUsername(),
                    new User(id, user.getUsername(), user.getFullName(), user.getEmail(), user.getPasswordHash()));
            return id;
        }
    }

    private final FakeUserDao dao = new FakeUserDao();
    private final AuthServiceImpl auth = new AuthServiceImpl(dao, new PasswordHasher(), new LoginAttemptTracker());

    private void registerAlice() throws AuthException {
        auth.register("Alice Doe", "alice@example.com", "alice_1", "correct-horse");
    }

    private void assertRejected(String fullName, String email, String username, String password) {
        try {
            auth.register(fullName, email, username, password);
            fail("expected AuthException");
        } catch (AuthException expected) {
            assertTrue(dao.byUsername.isEmpty());
        }
    }

    @Test
    public void register_storesAHash_notThePassword() throws Exception {
        registerAlice();
        User stored = dao.findByUsername("alice_1");
        assertNotNull(stored);
        assertEquals("Alice Doe", stored.getFullName());
        assertFalse(stored.getPasswordHash().contains("correct-horse"));
        assertTrue(stored.getPasswordHash().startsWith("600000:"));
    }

    @Test
    public void register_rejectsShortPassword() {
        assertRejected("Alice Doe", "alice@example.com", "alice_1", "short");
    }

    @Test
    public void register_rejectsBadUsername() {
        assertRejected("Alice Doe", "alice@example.com", "a b", "correct-horse");
    }

    @Test
    public void register_rejectsBadEmail() {
        assertRejected("Alice Doe", "not-an-email", "alice_1", "correct-horse");
    }

    @Test
    public void register_rejectsBlankFullName() {
        assertRejected("   ", "alice@example.com", "alice_1", "correct-horse");
    }

    @Test
    public void register_rejectsDuplicateUsernameAndEmail() throws Exception {
        registerAlice();
        try {
            auth.register("Other", "other@example.com", "alice_1", "correct-horse");
            fail("expected AuthException");
        } catch (AuthException expected) {
            // username taken
        }
        try {
            auth.register("Other", "alice@example.com", "other_1", "correct-horse");
            fail("expected AuthException");
        } catch (AuthException expected) {
            // email taken
        }
    }

    @Test
    public void register_turnsDatabaseDuplicateIntoAuthException() {
        dao.failCreateWithDuplicate = true;
        try {
            auth.register("Alice Doe", "alice@example.com", "alice_1", "correct-horse");
            fail("expected AuthException");
        } catch (AuthException expected) {
            // correct: no raw database exception reaches the servlet
        }
    }

    @Test
    public void login_succeeds_andDoesNotKeepTheHash() throws Exception {
        registerAlice();
        User user = auth.login("alice_1", "correct-horse");
        assertEquals("alice_1", user.getUsername());
        assertEquals("Alice Doe", user.getFullName());
        assertEquals("", user.getPasswordHash());
    }

    @Test
    public void login_wrongPasswordAndUnknownUser_giveTheSameMessage() throws Exception {
        registerAlice();
        String wrongPassword = null;
        String unknownUser = null;
        try {
            auth.login("alice_1", "wrong-password");
        } catch (AuthException e) {
            wrongPassword = e.getMessage();
        }
        try {
            auth.login("nobody", "wrong-password");
        } catch (AuthException e) {
            unknownUser = e.getMessage();
        }
        assertNotNull(wrongPassword);
        assertEquals(wrongPassword, unknownUser);
    }

    @Test
    public void login_locksTheAccountAfterFiveFailures() throws Exception {
        registerAlice();
        for (int i = 0; i < 5; i++) {
            try {
                auth.login("alice_1", "wrong-password");
            } catch (AuthException expected) {
                // counted as a failure
            }
        }
        try {
            auth.login("alice_1", "correct-horse");   // even the right password is refused while locked
            fail("expected AuthException");
        } catch (AuthException e) {
            assertTrue(e.getMessage().startsWith("Too many failed attempts"));
        }
    }
}