package com.stego.web.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** One-time message shown on the next page (replaces AlertUtil). */
public final class FlashMessage {

    private FlashMessage() {}

    /** Green success message, shown after a redirect. */
    public static void success(HttpServletRequest req, String message) {
        set(req, message, "success");
    }

    /** Red error message, shown after a redirect. */
    public static void error(HttpServletRequest req, String message) {
        set(req, message, "error");
    }

    private static void set(HttpServletRequest req, String message, String type) {
        HttpSession session = req.getSession(true);
        session.setAttribute("flash", message);
        session.setAttribute("flashType", type);
    }

    /** Moves the message from the session to the request, so the JSP shows it only once. */
    public static void expose(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("flash") == null) return;

        req.setAttribute("flash", session.getAttribute("flash"));
        req.setAttribute("flashType", session.getAttribute("flashType"));
        session.removeAttribute("flash");
        session.removeAttribute("flashType");
    }

    /** Message for the current request only (when the servlet forwards without redirecting). */
    public static void now(HttpServletRequest req, String message, String type) {
        req.setAttribute("flash", message);
        req.setAttribute("flashType", type);
    }
}