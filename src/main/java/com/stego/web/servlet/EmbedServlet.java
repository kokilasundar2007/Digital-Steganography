package com.stego.web.servlet;

import com.stego.app.ServiceFactory;
import com.stego.common.exception.CapacityExceededException;
import com.stego.common.model.User;
import com.stego.web.util.FlashMessage;
import com.stego.web.util.InputValidator;
import com.stego.web.util.SessionManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;

@WebServlet("/embed")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 12 * 1024 * 1024)
public class EmbedServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/embed.jsp";

    /** Show the embed page. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        FlashMessage.expose(req);
        req.getRequestDispatcher(VIEW).forward(req, res);
    }

    /** Hide the message inside the image. */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        User user = SessionManager.currentUser(req);
        ServiceFactory services = (ServiceFactory) getServletContext().getAttribute("services");

        Part image = req.getPart("image");
        String message = req.getParameter("message");
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");

        // check the inputs
        String error = InputValidator.checkImage(image);
        if (error == null && InputValidator.blank(message)) {
            error = "Secret message cannot be empty.";
        }
        if (error == null && !InputValidator.validPassword(password)) {
            error = "Password must be at least 6 characters.";
        }
        if (error == null && !password.equals(confirm)) {
            error = "Passwords do not match.";
        }

        // embed
        if (error == null) {
            String fileName = InputValidator.fileName(image);
            try {
                InputStream in = image.getInputStream();
                byte[] stego = services.images().embed(in.readAllBytes(), message, password);
                services.history().record(user.getId(), "EMBED", fileName, true);

                // keep the image in the session so the user can download it
                String id = SessionManager.cacheImage(req, stego);
                req.setAttribute("downloadId", id);
                FlashMessage.now(req, "Message embedded successfully. Download your stego image.", "success");

            } catch (CapacityExceededException e) {
                services.history().record(user.getId(), "EMBED", fileName, false);
                error = "Message is too large for this image. Use a bigger image or a shorter message.";
            } catch (Exception e) {
                services.history().record(user.getId(), "EMBED", fileName, false);
                error = "Embedding failed: " + e.getMessage();
            }
        }

        if (error != null) {
            FlashMessage.now(req, error, "error");
        }
        req.getRequestDispatcher(VIEW).forward(req, res);
    }
}