package com.stego.db;

import com.stego.common.UserDao;
import com.stego.common.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Objects;

/** Users table access. (M4) Implements the interface defined by M5. */
public class UserDaoImpl implements UserDao {

    private static final String COLUMNS = "id, username, full_name, email, password_hash";

    private final DbConnection db;

    /** ServiceFactory passes the shared DbConnection. */
    public UserDaoImpl(DbConnection db) {
        this.db = Objects.requireNonNull(db, "db");
    }

    /** Returns the user, or null when the username is unknown. */
    @Override
    public User findByUsername(String username) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE username = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("User lookup failed", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return exists("SELECT 1 FROM users WHERE username = ?", username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return exists("SELECT 1 FROM users WHERE email = ?", email);
    }

    /**
     * Inserts the user and returns the new id.
     *
     * @throws IllegalStateException with an SQLIntegrityConstraintViolationException as cause when
     *         the username or email already exists (AuthServiceImpl turns that into an AuthException)
     */
    @Override
    public int create(User user) {
        String sql = "INSERT INTO users (username, full_name, email, password_hash) VALUES (?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new IllegalStateException("Could not read the new user id");
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalStateException("Username or email already exists", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create user", e);
        }
    }

    // ---- helpers ----

    private boolean exists(String sql, String param) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("User existence check failed", e);
        }
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"));
    }
}