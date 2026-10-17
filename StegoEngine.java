package com.stego.common;

import com.stego.common.exception.CapacityExceededException;
import com.stego.common.exception.TamperedDataException;
import java.awt.image.BufferedImage;

/** Implemented by M2 (LsbStegoEngine). Internal: used by ImageServiceImpl and ExtractionServiceImpl. */
public interface StegoEngine {

    /** How many bytes of data the image can hide. */
    long capacityInBytes(BufferedImage image);

    /** Hides the bytes (the engine stores the length itself). Returns a new image. */
    BufferedImage embed(BufferedImage cover, byte[] data) throws CapacityExceededException;

    /** Reads back exactly the bytes that were embedded. */
    byte[] extract(BufferedImage stego) throws TamperedDataException;
}
