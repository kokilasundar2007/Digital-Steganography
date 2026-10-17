package com.stego.common;

import com.stego.common.exception.CapacityExceededException;
import com.stego.common.exception.TamperedDataException;
import java.awt.image.BufferedImage;

public interface StegoEngine {
    long capacityInBytes(BufferedImage image);
    BufferedImage embed(BufferedImage cover, byte[] data) throws CapacityExceededException;
    byte[] extract(BufferedImage stego) throws TamperedDataException;
}
