package com.stego.common;

public interface ImageService {
    byte[] embed(byte[] image, String message, String password) throws Exception;
}
