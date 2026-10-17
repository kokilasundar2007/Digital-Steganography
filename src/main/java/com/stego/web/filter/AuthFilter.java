package com.stego.web.filter;

import com.stego.web.util.SessionManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Blocks the inside pages if the user is not logged in. */
@WebFilter(urlPatterns = {"/dashboard", "/embed", "/extract", "/history", "/download"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // do not let the browser cache these pages
        res.setHeader("Cache-Control", "no-store");

        if (!SessionManager.isLoggedIn(req)) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        chain.doFilter(request, response);
    }
}