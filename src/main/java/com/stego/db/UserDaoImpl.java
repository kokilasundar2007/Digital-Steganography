package com.stego.db;

import com.stego.common.UserDao;
import com.stego.common.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class UserDaoImpl implements UserDao {

    @Override
    public User create(User user) {
        String sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setId(keys.getInt(1));
            }
            return user;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create user", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findOne("SELECT * FROM users WHERE username = ?", username);
    }

    @Override
    public Optional<User> findById(int id) {
        return findOne("SELECT * FROM users WHERE id = ?", id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return exists("SELECT 1 FROM users WHERE username = ?", username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return exists("SELECT 1 FROM users WHERE email = ?", email);
    }

    // ---- helpers ----

    private Optional<User> findOne(String sql, Object param) {
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("User lookup failed", e);
        }
    }

    private boolean exists(String sql, String param) {
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("User existence check failed", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        return u;
    }
}
