package com.stego.common;

/** Implemented by M3 (ExtractionServiceImpl). Used by M1. */
public interface ExtractionService {

    /**
     * @return the hidden message
     * @throws com.stego.common.exception.TamperedDataException wrong password or modified image
     */
    String extract(byte[] image, String password) throws Exception;
}
