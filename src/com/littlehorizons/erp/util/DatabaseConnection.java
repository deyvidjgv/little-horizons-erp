package com.littlehorizons.erp.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Singleton that keeps the connection settings of erp_little_horizons.
 * The settings are read only once from db.properties. Each call to getConnection() opens a new
 * connection, and whoever asks for it closes it with try-with-resources, so no connection stays
 * open after an error.
 */
public final class DatabaseConnection {

    private static final String CONFIG_FILE = "db.properties";
    private static final String URL_KEY = "db.url";
    private static final String USER_KEY = "db.user";
    private static final String PASSWORD_KEY = "db.password";
    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection(Properties settings) {
        this.url = settings.getProperty(URL_KEY);
        this.user = settings.getProperty(USER_KEY);
        this.password = settings.getProperty(PASSWORD_KEY);
    }

    public static synchronized DatabaseConnection getInstance() throws SQLException {
        if (instance == null) {
            instance = new DatabaseConnection(loadSettings());
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Opens and closes a test connection, to warn at startup if the database does not answer. */
    public void testConnection() throws SQLException {
        try (Connection connection = getConnection()) {
            if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                throw new SQLException("La base de datos no responde.");
            }
        }
    }

    private static Properties loadSettings() throws SQLException {
        Properties settings = new Properties();
        try (FileReader reader = new FileReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            settings.load(reader);
        } catch (FileNotFoundException e) {
            throw new SQLException("No existe " + CONFIG_FILE
                    + ". Copie db.properties.example como db.properties y escriba sus credenciales.", e);
        } catch (IOException e) {
            throw new SQLException("No se pudo leer " + CONFIG_FILE + ": " + e.getMessage(), e);
        }
        Optional<String> missingKey = Stream.of(URL_KEY, USER_KEY, PASSWORD_KEY)
                .filter(key -> settings.getProperty(key) == null)
                .findFirst();
        if (missingKey.isPresent()) {
            throw new SQLException("Falta la línea '" + missingKey.get() + "=' en " + CONFIG_FILE + ".");
        }
        return settings;
    }
}
