package com.stego.web.servlet;
import com.stego.app.ServiceFactory;
import com.stego.common.exception.TamperedDataException;
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

@WebServlet("/extract")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 12 * 1024 * 1024)
public class ExtractServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/extract.jsp";

    /** Show the extract page. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        FlashMessage.expose(req);
        req.getRequestDispatcher(VIEW).forward(req, res);
    }

    /** Get the hidden message out of the image. */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        User user = SessionManager.currentUser(req);
        ServiceFactory services = (ServiceFactory) getServletContext().getAttribute("services");

        Part image = req.getPart("image");
        String password = req.getParameter("password");

        // check the inputs
        String error = InputValidator.checkImage(image);
        if (error == null && InputValidator.blank(password)) {
            error = "Decryption password is required.";
        }

        // extract
        if (error == null) {
            String fileName = InputValidator.fileName(image);
            try {
                InputStream in = image.getInputStream();
                String text = services.extraction().extract(in.readAllBytes(), password);
                services.history().record(user.getId(), "EXTRACT", fileName, true);

                req.setAttribute("extracted", text);
                FlashMessage.now(req, "Image processed. Hidden message extracted.", "success");

            } catch (TamperedDataException e) {
                services.history().record(user.getId(), "EXTRACT", fileName, false);
                error = "Invalid password, or the image has been modified.";
            } catch (Exception e) {
                services.history().record(user.getId(), "EXTRACT", fileName, false);
                error = "No hidden message found or extraction failed.";
            }
        }

        if (error != null) {
            FlashMessage.now(req, error, "error");
        }
        req.getRequestDispatcher(VIEW).forward(req, res);
    }
}