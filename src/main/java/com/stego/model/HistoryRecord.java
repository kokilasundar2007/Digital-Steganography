package com.stego.common.model;

public class HistoryRecord {
    private final int id;
    private final int userId;
    private final String operationType;
    private final String filename;
    private final String createdAt;
    private final boolean success;

    public HistoryRecord(int id, int userId, String operationType,
                         String filename, String createdAt, boolean success) {
        this.id = id;
        this.userId = userId;
        this.operationType = operationType;
        this.filename = filename;
        this.createdAt = createdAt;
        this.success = success;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getOperationType() { return operationType; }
    public String getFilename() { return filename; }
    public Object getCreatedAt() { return createdAt; }
    public boolean isSuccess() { return success; }
}
