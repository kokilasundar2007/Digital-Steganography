package com.stego.db;

import com.stego.common.HistoryDao;
import com.stego.common.model.HistoryRecord;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/** History table access. (M4) Implements the interface defined by M5. */
public class HistoryDaoImpl implements HistoryDao {

    private static final Logger LOG = Logger.getLogger(HistoryDaoImpl.class.getName());
    private static final int MAX_FILENAME = 255;
    private static final int MAX_TYPE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DbConnection db;

    /** ServiceFactory passes the shared DbConnection. */
    public HistoryDaoImpl(DbConnection db) {
        this.db = Objects.requireNonNull(db, "db");
    }

    /**
     * Saves one operation. History is a side feature, so a database problem here is logged
     * and swallowed: it must not make a successful embed or extract look like a failure.
     */
    @Override
    public void record(int userId, String type, String filename, boolean success) {
        String sql = "INSERT INTO history (user_id, operation_type, filename, success) VALUES (?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, normalizeType(type));
            ps.setString(3, shorten(filename, MAX_FILENAME));
            ps.setBoolean(4, success);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Could not record a history entry", e);
        }
    }

    /**
     * One page of history, newest first.
     *
     * @param q    text searched in the file name; blank means no filter
     * @param type EMBED or EXTRACT; blank or ALL means no filter
     * @param date yyyy-MM-dd; blank or invalid means no filter
     * @param page 1-based page number
     * @param size rows per page (limited to 100)
     */
    @Override
    public List<HistoryRecord> find(int userId, String q, String type, String date, int page, int size) {
        Filter f = filter(userId, q, type, date);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        long offset = (Math.max((long) page, 1L) - 1L) * pageSize;

        String sql = "SELECT id, user_id, operation_type, filename, success, created_at FROM history"
                + f.where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";
        List<Object> params = new ArrayList<>(f.params);
        params.add(pageSize);
        params.add(offset);

        List<HistoryRecord> list = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load history", e);
        }
    }

    @Override
    public int count(int userId, String q, String type, String date) {
        Filter f = filter(userId, q, type, date);
        return countWhere("SELECT COUNT(*) FROM history" + f.where, f.params);
    }

    @Override
    public int countByType(int userId, String type) {
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(normalizeType(type));
        return countWhere("SELECT COUNT(*) FROM history WHERE user_id = ? AND operation_type = ?", params);
    }

    /** days = 1 means today (since midnight), 7 means the last 7 days including today. */
    @Override
    public int countByTypeSince(int userId, String type, int days) {
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(normalizeType(type));
        params.add(Math.max(days, 1) - 1);
        return countWhere("SELECT COUNT(*) FROM history WHERE user_id = ? AND operation_type = ? "
                + "AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)", params);
    }

    /** days = 1 means today (since midnight), 7 means the last 7 days including today. */
    @Override
    public int countSince(int userId, int days) {
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(Math.max(days, 1) - 1);
        return countWhere("SELECT COUNT(*) FROM history WHERE user_id = ? "
                + "AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)", params);
    }

    // ---- helpers ----

    /** WHERE clause and its parameters. Only fixed SQL text is concatenated; values are always bound. */
    private static final class Filter {
        final String where;
        final List<Object> params;

        Filter(String where, List<Object> params) {
            this.where = where;
            this.params = params;
        }
    }

    private static Filter filter(int userId, String q, String type, String date) {
        StringBuilder where = new StringBuilder(" WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (q != null && !q.trim().isEmpty()) {
            where.append(" AND filename LIKE ? ESCAPE '!'");
            params.add("%" + escapeLike(q.trim()) + "%");
        }
        if (type != null && !type.trim().isEmpty() && !type.trim().equalsIgnoreCase("ALL")) {
            where.append(" AND operation_type = ?");
            params.add(normalizeType(type));
        }
        if (date != null && !date.trim().isEmpty()) {
            try {
                params.add(Date.valueOf(LocalDate.parse(date.trim())));
                where.append(" AND DATE(created_at) = ?");
            } catch (DateTimeParseException e) {
                // invalid date text: ignore the date filter
            }
        }
        return new Filter(where.toString(), params);
    }

    private int countWhere(String sql, List<Object> params) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not count history", e);
        }
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            int index = i + 1;
            if (p instanceof Integer) {
                ps.setInt(index, (Integer) p);
            } else if (p instanceof Long) {
                ps.setLong(index, (Long) p);
            } else if (p instanceof Date) {
                ps.setDate(index, (Date) p);
            } else {
                ps.setString(index, (String) p);
            }
        }
    }

    private static HistoryRecord map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        String when = ts == null ? "" : ts.toLocalDateTime().format(SHOWN);
        return new HistoryRecord(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("operation_type"),
                rs.getString("filename"),
                when,
                rs.getBoolean("success"));
    }

    private static String normalizeType(String type) {
        String t = type == null ? "UNKNOWN" : type.trim().toUpperCase(Locale.ROOT);
        return shorten(t.isEmpty() ? "UNKNOWN" : t, MAX_TYPE);
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    /** Makes %, _ and ! in the search text match literally. */
    private static String escapeLike(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}