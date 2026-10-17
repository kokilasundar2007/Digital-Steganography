package com.stego.web.servlet;

import com.stego.web.util.SessionManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/download")
public class DownloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {

        // find the image saved in the session by its id
        String id = req.getParameter("id");
        byte[] image = SessionManager.imageCache(req).get(id);

        if (image == null) {
            res.sendError(404, "Image expired. Please embed again.");
            return;
        }

        res.setContentType("image/png");

        // "inline" shows it in the page, otherwise it downloads as a file
        if (req.getParameter("inline") == null) {
            res.setHeader("Content-Disposition", "attachment; filename=\"stego_image.png\"");
        }

        res.setContentLength(image.length);
        res.getOutputStream().write(image);
    }
}