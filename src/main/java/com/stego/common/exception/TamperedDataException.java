package com.stego.common.exception;

public class TamperedDataException extends Exception {
    private static final long serialVersionUID = 1L;
    public TamperedDataException(String message) { super(message); }
    public TamperedDataException(String message, Throwable cause) { super(message, cause); }
}
