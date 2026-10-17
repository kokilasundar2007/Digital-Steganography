package com.stego.common;

import com.stego.common.model.HistoryRecord;
import java.util.List;

/**
 * Implemented by M4 (HistoryDaoImpl). Used by M1 (all six methods).
 * No checked exceptions: implementations wrap SQLException in a RuntimeException.
 *
 * type  : exactly "EMBED" or "EXTRACT"
 * q, type, date : null or empty means "no filter"
 * date  : format yyyy-MM-dd
 * page  : starts at 1
 */
public interface HistoryDao {

    void record(int userId, String type, String filename, boolean success);

    /** Newest records first. */
    List<HistoryRecord> find(int userId, String q, String type, String date, int page, int size);

    int count(int userId, String q, String type, String date);

    int countByType(int userId, String type);

    int countByTypeSince(int userId, String type, int days);

    int countSince(int userId, int days);
}
