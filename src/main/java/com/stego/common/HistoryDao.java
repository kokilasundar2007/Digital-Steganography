package com.stego.common;

import com.stego.common.model.HistoryRecord;
import java.util.List;

public interface HistoryDao {
    void record(int userId, String type, String filename, boolean success);
    List<HistoryRecord> find(int userId, String q, String type, String date, int page, int size);
    int count(int userId, String q, String type, String date);
    int countByType(int userId, String type);
    int countByTypeSince(int userId, String type, int days);
    int countSince(int userId, int days);
}
