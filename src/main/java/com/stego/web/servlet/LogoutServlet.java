package com.stego.web.servlet;

import com.stego.web.util.FlashMessage;
import com.stego.web.util.SessionManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        SessionManager.logout(req);                       // ends the session
        FlashMessage.success(req, "You have been logged out.");   // starts a new session for the message
        res.sendRedirect(req.getContextPath() + "/login");
    }
}