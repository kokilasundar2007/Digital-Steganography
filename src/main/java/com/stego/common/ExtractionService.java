package com.stego.common;

public interface ExtractionService {
    String extract(byte[] image, String password) throws Exception;
}
