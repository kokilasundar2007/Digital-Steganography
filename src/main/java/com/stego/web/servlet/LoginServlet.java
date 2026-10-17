package com.stego.web.servlet;

import com.stego.app.ServiceFactory;
import com.stego.common.exception.AuthException;
import com.stego.common.model.User;
import com.stego.web.util.FlashMessage;
import com.stego.web.util.InputValidator;
import com.stego.web.util.SessionManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/login.jsp";

    /** Show the login page. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        // already logged in, go to dashboard
        if (SessionManager.isLoggedIn(req)) {
            res.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }

        FlashMessage.expose(req);
        req.getRequestDispatcher(VIEW).forward(req, res);
    }

    /** Check username and password. */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (InputValidator.blank(username) || InputValidator.blank(password)) {
            FlashMessage.now(req, "Username and password are required.", "error");
            req.getRequestDispatcher(VIEW).forward(req, res);
            return;
        }

        try {
            ServiceFactory services = (ServiceFactory) getServletContext().getAttribute("services");
            User user = services.auth().login(username.trim(), password);

            SessionManager.login(req, user);
            res.sendRedirect(req.getContextPath() + "/dashboard");

        } catch (AuthException e) {
            FlashMessage.now(req, "Invalid username or password.", "error");
            req.getRequestDispatcher(VIEW).forward(req, res);
        }
    }
}