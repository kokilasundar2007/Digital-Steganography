package com.stego.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

import java.io.IOException;

/** Sets UTF-8 encoding for every request and response. */
@WebFilter("/*")
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");
        chain.doFilter(req, res);
    }
}