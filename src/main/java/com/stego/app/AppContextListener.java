package com.stego.app;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {

    public static final String ATTRIBUTE = "services";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            ServiceFactory factory = ServiceFactory.create();
            sce.getServletContext().setAttribute(ATTRIBUTE, factory);
            sce.getServletContext().log("Stego services started");
        } catch (RuntimeException e) {
            sce.getServletContext().log("Stego services FAILED to start: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().removeAttribute(ATTRIBUTE);
    }
}
