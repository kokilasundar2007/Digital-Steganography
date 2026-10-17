package com.stego.web.util;

import com.stego.common.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class SessionManager {

    private SessionManager() {}

    /** Saves the user in a fresh session (stops session-fixation attacks). */
    public static void login(HttpServletRequest req, User user) {
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();
        req.getSession(true).setAttribute("user", user);
    }

    /** Returns the logged-in user, or null if nobody is logged in. */
    public static User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    public static boolean isLoggedIn(HttpServletRequest req) {
        return currentUser(req) != null;
    }

    /** Ends the session. */
    public static void logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
    }

    /** Temporary store for generated stego images, so they can be downloaded by id. */
    @SuppressWarnings("unchecked")
    public static Map<String, byte[]> imageCache(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        Map<String, byte[]> cache = (Map<String, byte[]>) session.getAttribute("stegoCache");
        if (cache == null) {
            cache = new LinkedHashMap<>();
            session.setAttribute("stegoCache", cache);
        }
        return cache;
    }

    /** Stores an image and returns its id. Keeps only the latest 5 to save memory. */
    public static String cacheImage(HttpServletRequest req, byte[] data) {
        Map<String, byte[]> cache = imageCache(req);
        if (cache.size() >= 5) {
            cache.remove(cache.keySet().iterator().next());
        }
        String id = UUID.randomUUID().toString();
        cache.put(id, data);
        return id;
    }
}