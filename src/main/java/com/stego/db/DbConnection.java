package com.stego.db;
 
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
 
/**
 * Loads db.properties once and hands out JDBC connections. Callers must close them. (M4)
 *
 * <p>ServiceFactory creates ONE instance with {@code new DbConnection()} and passes it to the DAOs,
 * so this class needs a public no-argument constructor.
 *
 * <p>db.properties must be on the classpath: put it in {@code src/main/resources/}.
 * Keys: db.url, db.username, db.password and (optional) db.driver.
 *
 * <p>Passwords must never be committed. The committed file keeps a placeholder; at run time the
 * real password is taken from the environment variable STEGO_DB_PASSWORD when it is set.
 */
public final class DbConnection {
 
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String PASSWORD_ENV = "STEGO_DB_PASSWORD";
 
    private final String url;
    private final String username;
    private final String password;
 
    public DbConnection() {
        Properties props = new Properties();
        try (InputStream in = DbConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "db.properties not found on the classpath (it must be in src/main/resources)");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read db.properties", e);
        }
 
        this.url = required(props, "db.url");
        this.username = required(props, "db.username");
 
        String fromEnvironment = System.getenv(PASSWORD_ENV);
        this.password = (fromEnvironment != null && !fromEnvironment.isEmpty())
                ? fromEnvironment
                : props.getProperty("db.password", "");
 
        String driver = props.getProperty("db.driver", DEFAULT_DRIVER);
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC driver not found: " + driver, e);
        }
    }
 
    /** Opens a new connection. The caller must close it (use try-with-resources). */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
 
    private static String required(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing '" + key + "' in db.properties");
        }
        return value.trim();
    }
}