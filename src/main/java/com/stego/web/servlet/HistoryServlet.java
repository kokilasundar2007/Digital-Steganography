package com.stego.web.servlet;

import com.stego.app.ServiceFactory;
import com.stego.common.model.User;
import com.stego.web.util.FlashMessage;
import com.stego.web.util.SessionManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/history")
public class HistoryServlet extends HttpServlet {

    private static final int PAGE_SIZE = 5;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        User user = SessionManager.currentUser(req);
        ServiceFactory services = (ServiceFactory) getServletContext().getAttribute("services");

        // filters from the search bar
        String q = req.getParameter("q");
        String type = req.getParameter("type");
        String date = req.getParameter("date");

        // page number (default 1)
        int page = 1;
        try {
            page = Integer.parseInt(req.getParameter("page"));
        } catch (NumberFormatException e) {
            page = 1;
        }

        // total records and total pages
        int total = services.history().count(user.getId(), q, type, date);
        int totalPages = (int) Math.ceil(total / (double) PAGE_SIZE);
        if (totalPages < 1) {
            totalPages = 1;
        }
        if (page < 1) {
            page = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        // send data to history.jsp
        req.setAttribute("records", services.history().find(user.getId(), q, type, date, page, PAGE_SIZE));
        req.setAttribute("currentPage", page);
        req.setAttribute("pageSize", PAGE_SIZE);
        req.setAttribute("totalPages", totalPages);

        FlashMessage.expose(req);
        req.getRequestDispatcher("/WEB-INF/views/history.jsp").forward(req, res);
    }
}