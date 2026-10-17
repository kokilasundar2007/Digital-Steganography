package com.stego.web.servlet;

import com.stego.app.ServiceFactory;
import com.stego.common.exception.AuthException;
import com.stego.web.util.FlashMessage;
import com.stego.web.util.InputValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/register.jsp";

    /** Show the registration page. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        req.getRequestDispatcher(VIEW).forward(req, res);
    }

    /** Validate the form and create the account. */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");

        String error = null;

        if (InputValidator.blank(fullName)) {
            error = "Full name is required.";
        } else if (!InputValidator.validEmail(email)) {
            error = "Enter a valid email address.";
        } else if (!InputValidator.validUsername(username)) {
            error = "Username must be 3-30 letters, digits or underscore.";
        } else if (!InputValidator.validPassword(password)) {
            error = "Password must be at least 6 characters.";
        } else if (!password.equals(confirm)) {
            error = "Passwords do not match.";
        }

        if (error == null) {
            try {
                ServiceFactory services = (ServiceFactory) getServletContext().getAttribute("services");
                services.auth().register(fullName.trim(), email.trim(), username.trim(), password);

                // success: go to login page with a green message
                FlashMessage.success(req, "Account created. Please login.");
                res.sendRedirect(req.getContextPath() + "/login");
                return;

            } catch (AuthException e) {
                error = e.getMessage();   // for example: username already exists
            }
        }

        FlashMessage.now(req, error, "error");
        req.getRequestDispatcher(VIEW).forward(req, res);
    }
}