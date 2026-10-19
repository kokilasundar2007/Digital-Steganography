package com.stego.app;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Runs once when Tomcat starts the app. Builds all services and stores them
 * in the ServletContext under the name "services" (every M1 servlet reads it):
 *
 *   ServiceFactory f = (ServiceFactory) getServletContext().getAttribute("services");
 */
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
