package com.stego.common.exception;

public class CapacityExceededException extends Exception {
    private static final long serialVersionUID = 1L;
    public CapacityExceededException(String message) { super(message); }
    public CapacityExceededException(String message, Throwable cause) { super(message, cause); }
}
