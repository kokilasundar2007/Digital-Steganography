package com.stego.crypto;

import com.stego.common.CryptoService;
import com.stego.common.ExtractionService;
import com.stego.common.PayloadCodec;
import com.stego.common.StegoEngine;
import com.stego.common.exception.TamperedDataException;
import com.stego.common.model.Payload;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

/**
 * Extraction pipeline. (M3)
 * image -> StegoEngine.extract -> PayloadCodec.decode -> CryptoService.decrypt -> text
 */
public class ExtractionServiceImpl implements ExtractionService {

    private final StegoEngine engine;
    private final PayloadCodec codec;
    private final CryptoService crypto;

    public ExtractionServiceImpl(StegoEngine engine, PayloadCodec codec, CryptoService crypto) {
        this.engine = engine;
        this.codec = codec;
        this.crypto = crypto;
    }

    @Override
    public String extract(byte[] image, String password) throws Exception {
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("Image is required");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        BufferedImage bufferedImage = ImageIO.read(
    new ByteArrayInputStream(image)
);

if (bufferedImage == null) {
    throw new IllegalArgumentException("Invalid image format");
}



        byte[] framed;
        try {
            framed = engine.extract(bufferedImage);                  // 1. read LSBs
        } catch (RuntimeException e) {                       // garbage length bytes etc.
            throw new TamperedDataException("No valid hidden data found in this image.");
        }

        Payload payload = codec.decode(framed);              
        return crypto.decrypt(payload,password);
    }
}
