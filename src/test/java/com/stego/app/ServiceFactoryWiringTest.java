package com.stego.app;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Checks that ServiceFactory can build every service by reflection, exactly as Tomcat will at startup.
 * It needs no running database: DbConnection only reads db.properties and loads the JDBC driver class.
 *
 * <p>It fails when a class is missing, a constructor does not match what ServiceFactory passes,
 * or db.properties is not on the classpath (it must live in src/main/resources).
 */
public class ServiceFactoryWiringTest {

    @Test
    public void create_buildsEveryService() {
        ServiceFactory factory = ServiceFactory.create();
        assertNotNull(factory.auth());
        assertNotNull(factory.images());
        assertNotNull(factory.extraction());
        assertNotNull(factory.history());
    }
}