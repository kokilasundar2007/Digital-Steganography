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

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        User user = SessionManager.currentUser(req);
        ServiceFactory services = (ServiceFactory) getServletContext().getAttribute("services");

        // numbers for the 3 stat cards
        req.setAttribute("totalEmbeddings", services.history().countByType(user.getId(), "EMBED"));
        req.setAttribute("totalExtractions", services.history().countByType(user.getId(), "EXTRACT"));
        req.setAttribute("embeddingsThisWeek", services.history().countByTypeSince(user.getId(), "EMBED", 7));
        req.setAttribute("extractionsThisWeek", services.history().countByTypeSince(user.getId(), "EXTRACT", 7));
        req.setAttribute("todayOps", services.history().countSince(user.getId(), 1));

        FlashMessage.expose(req);
        req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req, res);
    }
}