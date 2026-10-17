package com.stego.common.exception;

/** Checked exception. The message is safe to show to the user. */
public class TamperedDataException extends Exception {
    private static final long serialVersionUID = 1L;

    public TamperedDataException(String message) {
        super(message);
    }

    public TamperedDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
