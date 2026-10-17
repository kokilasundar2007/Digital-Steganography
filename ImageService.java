package com.stego.common;

/** Implemented by M2 (ImageServiceImpl). Used by M1. */
public interface ImageService {

    /**
     * @param image    original PNG/BMP bytes
     * @param message  secret text
     * @param password used to encrypt the message
     * @return the stego image as PNG bytes
     * @throws com.stego.common.exception.CapacityExceededException if the message is too big
     */
    byte[] embed(byte[] image, String message, String password) throws Exception;
}
