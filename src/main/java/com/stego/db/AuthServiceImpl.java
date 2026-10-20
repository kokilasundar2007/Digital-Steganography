package com.stego.db;

import com.stego.common.AuthService;
import com.stego.common.UserDao;
import com.stego.common.exception.AuthException;
import com.stego.common.model.User;

import java.util.regex.Pattern;

public class AuthServiceImpl implements AuthService {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,30}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD = 8;
    private static final String GENERIC_FAILURE = "Invalid username or password.";

    private final UserDao userDao;
    private final PasswordHasher hasher;
    private final LoginAttemptTracker tracker;
    private final String dummyHash; // so unknown usernames cost the same time as known ones

    public AuthServiceImpl(UserDao userDao, PasswordHasher hasher, LoginAttemptTracker tracker) {
        this.userDao = userDao;
        this.hasher = hasher;
        this.tracker = tracker;
        this.dummyHash = hasher.hash("dummy-password".toCharArray());
    }

    @Override
    public User register(String username, String email, String password) throws AuthException {
        username = username == null ? "" : username.trim();
        email = email == null ? "" : email.trim();

        if (!USERNAME.matcher(username).matches())
            throw new AuthException("Username must be 3-30 characters: letters, digits or underscore.");
        if (!EMAIL.matcher(email).matches())
            throw new AuthException("Please enter a valid email address.");
        if (password == null || password.length() < MIN_PASSWORD)
            throw new AuthException("Password must be at least " + MIN_PASSWORD + " characters.");
        if (userDao.existsByUsername(username))
            throw new AuthException("That username is already taken.");
        if (userDao.existsByEmail(email))
            throw new AuthException("That email is already registered.");

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(hasher.hash(password.toCharArray()));
        return userDao.create(user);
    }

    @Override
    public User login(String username, String password) throws AuthException {
        if (username == null || password == null || username.isBlank() || password.isEmpty())
            throw new AuthException(GENERIC_FAILURE);

        username = username.trim();
        if (tracker.isLocked(username)) {
            throw new AuthException("Too many failed attempts. Try again in "
                    + tracker.remainingLockMinutes(username) + " minute(s).");
        }

        User user = userDao.findByUsername(username).orElse(null);
        boolean ok = hasher.verify(password.toCharArray(), user != null ? user.getPasswordHash() : dummyHash);

        if (user == null || !ok) {
            tracker.recordFailure(username);
            throw new AuthException(GENERIC_FAILURE);   // same message: no user enumeration
        }
        tracker.recordSuccess(username);
        return user;
    }
}
