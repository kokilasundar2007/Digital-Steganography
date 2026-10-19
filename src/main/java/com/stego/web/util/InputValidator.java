package com.stego.web.util;

import jakarta.servlet.http.Part;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Simple input checks used by the servlets.
 * All methods are static and null-safe.
 */
public final class InputValidator {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME =
            Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024; // matches @MultipartConfig

    private InputValidator() {
    }

    /** True if the text is null, empty or only spaces. */
    public static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Basic email format check. */
    public static boolean validEmail(String email) {
        return !blank(email) && EMAIL.matcher(email.trim()).matches();
    }

    /** 3-30 letters, digits or underscore. */
    public static boolean validUsername(String username) {
        return username != null && USERNAME.matcher(username.trim()).matches();
    }

    /** At least 6 characters (spaces are allowed and kept as typed). */
    public static boolean validPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    /**
     * Checks the uploaded image.
     * @return an error message, or null if the file is fine.
     */
    public static String checkImage(Part image) {
        if (image == null || image.getSize() <= 0) {
            return "Please choose an image file.";
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            return "Image is too large (maximum 10 MB).";
        }
        String name = fileName(image).toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".png") || name.endsWith(".bmp"))) {
            return "Only PNG or BMP images are supported.";
        }
        return null;
    }

    /** The uploaded file's name without any folder path. */
    public static String fileName(Part part) {
        String name = part == null ? null : part.getSubmittedFileName();
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        // some browsers send the full path, keep only the last piece
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return slash >= 0 ? name.substring(slash + 1) : name;
    }
}