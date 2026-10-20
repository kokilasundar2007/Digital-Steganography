package com.stego.db;

import com.stego.common.AuthService;
import com.stego.common.UserDao;
import com.stego.common.exception.AuthException;
import com.stego.common.model.User;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Pattern;

/** Registration and login. (M4) Implements the interface defined by M5. */
public class AuthServiceImpl implements AuthService {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,30}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_FULL_NAME = 100;
    private static final int MAX_EMAIL = 254;
    private static final String GENERIC_FAILURE = "Invalid username or password.";

    private final UserDao userDao;
    private final PasswordHasher hasher;
    private final LoginAttemptTracker tracker;
    private final String dummyHash; // so unknown usernames cost the same time as known ones

    /** ServiceFactory calls this with (userDao, hasher, tracker). */
    public AuthServiceImpl(UserDao userDao, PasswordHasher hasher, LoginAttemptTracker tracker) {
        this.userDao = userDao;
        this.hasher = hasher;
        this.tracker = tracker;
        this.dummyHash = hasher.hash("dummy-password");
    }

    @Override
    public void register(String fullName, String email, String username, String password)
            throws AuthException {
        fullName = fullName == null ? "" : fullName.trim();
        email = email == null ? "" : email.trim();
        username = username == null ? "" : username.trim();

        if (fullName.isEmpty() || fullName.length() > MAX_FULL_NAME)
            throw new AuthException("Please enter your full name (up to " + MAX_FULL_NAME + " characters).");
        if (email.length() > MAX_EMAIL || !EMAIL.matcher(email).matches())
            throw new AuthException("Please enter a valid email address.");
        if (!USERNAME.matcher(username).matches())
            throw new AuthException("Username must be 3-30 characters: letters, digits or underscore.");
        if (password == null || password.length() < MIN_PASSWORD)
            throw new AuthException("Password must be at least " + MIN_PASSWORD + " characters.");
        if (userDao.existsByUsername(username))
            throw new AuthException("That username is already taken.");
        if (userDao.existsByEmail(email))
            throw new AuthException("That email is already registered.");

        // id 0 = "not saved yet"; the database assigns the real id
        User user = new User(0, username, fullName, email, hasher.hash(password));
        try {
            userDao.create(user);
        } catch (IllegalStateException e) {
            // two people registering the same name at the same moment: the database unique key decides
            if (e.getCause() instanceof SQLIntegrityConstraintViolationException) {
                throw new AuthException("That username or email is already registered.");
            }
            throw e;
        }
    }

    @Override
    public User login(String username, String password) throws AuthException {
        if (username == null || password == null || username.trim().isEmpty() || password.isEmpty())
            throw new AuthException(GENERIC_FAILURE);

        username = username.trim();
        if (tracker.isLocked(username)) {
            throw new AuthException("Too many failed attempts. Try again in "
                    + tracker.remainingLockMinutes(username) + " minute(s).");
        }

        User user = userDao.findByUsername(username);
        boolean ok = hasher.verify(password, user != null ? user.getPasswordHash() : dummyHash);

        if (user == null || !ok) {
            tracker.recordFailure(username);
            throw new AuthException(GENERIC_FAILURE);   // same message: no user enumeration
        }
        tracker.recordSuccess(username);

        // The password hash is not needed after login, so it is not kept in the session.
        return new User(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(), "");
    }
}