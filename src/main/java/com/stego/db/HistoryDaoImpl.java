package com.stego.db;

import com.stego.common.HistoryDao;
import com.stego.common.model.HistoryRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class HistoryDaoImpl implements HistoryDao {

    @Override
    public void save(HistoryRecord r) {
        String sql = "INSERT INTO history (user_id, operation, file_name, payload_size, psnr, success) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getUserId());
            ps.setString(2, r.getOperation());          // "EMBED" or "EXTRACT"
            ps.setString(3, r.getFileName());
            ps.setInt(4, r.getPayloadSize());
            if (r.getPsnr() == null) ps.setNull(5, Types.DOUBLE);
            else ps.setDouble(5, r.getPsnr());
            ps.setBoolean(6, r.isSuccess());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) r.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save history record", e);
        }
    }

    @Override
    public List<HistoryRecord> findByUserId(int userId) {
        String sql = "SELECT * FROM history WHERE user_id = ? ORDER BY created_at DESC, id DESC";
        List<HistoryRecord> list = new ArrayList<>();
        try (Connection c = DbConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load history", e);
        }
    }

    private HistoryRecord map(ResultSet rs) throws SQLException {
        HistoryRecord r = new HistoryRecord();
        r.setId(rs.getLong("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setOperation(rs.getString("operation"));
        r.setFileName(rs.getString("file_name"));
        r.setPayloadSize(rs.getInt("payload_size"));
        double psnr = rs.getDouble("psnr");
        r.setPsnr(rs.wasNull() ? null : psnr);
        r.setSuccess(rs.getBoolean("success"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        return r;
    }
}
