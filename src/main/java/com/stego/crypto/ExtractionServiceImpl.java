package com.stego.crypto;

import com.stego.common.CryptoService;
import com.stego.common.ExtractionService;
import com.stego.common.PayloadCodec;
import com.stego.common.StegoEngine;
import com.stego.common.exception.TamperedDataException;
import com.stego.common.model.Payload;
import com.stego.stego.ImageServiceImpl;

import java.awt.image.BufferedImage;

/**
 * Extraction pipeline. (M3)
 * image -> StegoEngine.extract -> PayloadCodec.decode -> CryptoService.decrypt -> text
 */
public class ExtractionServiceImpl implements ExtractionService {

    private final CryptoService crypto;
    private final PayloadCodec codec;
    private final StegoEngine engine;

    // The parameter order must match ServiceFactory, which calls (crypto, codec, engine).
    public ExtractionServiceImpl(CryptoService crypto, PayloadCodec codec, StegoEngine engine) {
        this.crypto = crypto;
        this.codec = codec;
        this.engine = engine;
    }

    @Override
    public String extract(byte[] image, String password) throws Exception {
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("Image is required");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // M2's reader checks the real format, the file size and the pixel count before decoding.
        BufferedImage bufferedImage = ImageServiceImpl.readValidated(image);

        byte[] framed;
        try {
            framed = engine.extract(bufferedImage);                  // 1. read LSBs
        } catch (RuntimeException e) {                       // garbage length bytes etc.
            throw new TamperedDataException("No valid hidden data found in this image.");
        }

        Payload payload = codec.decode(framed);                      // 2. bytes -> Payload
        return crypto.decrypt(payload, password);                    // 3. Payload -> text
    }
}