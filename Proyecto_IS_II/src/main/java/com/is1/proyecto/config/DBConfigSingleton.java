// Archivo: com/is1/proyecto/config/DBConfigSingleton.java
package com.is1.proyecto.config;

import org.javalite.activejdbc.Base;

public final class DBConfigSingleton {

    private static DBConfigSingleton instance;

    private final String dbUrl;
    private final String user;
    private final String pass;
    private final String driver;

    private DBConfigSingleton() {
        this.driver = "com.mysql.cj.jdbc.Driver"; // Nuevo driver de MySQL

        // Leemos variables de entorno (ideal para prod), si no existen, usamos valores por defecto (dev)
        String envUrl = System.getenv("DB_URL");
        this.dbUrl = (envUrl != null) ? envUrl : "jdbc:mysql://localhost:3306/proyecto_is_ii?useSSL=false&serverTimezone=UTC";

        String envUser = System.getenv("DB_USER");
        this.user = (envUser != null) ? envUser : "dev";

        String envPass = System.getenv("DB_PASS");
        this.pass = (envPass != null) ? envPass : ""; // Contraseña por defecto de XAMPP local
    }

    public static synchronized DBConfigSingleton getInstance() {
        if (instance == null) {
            instance = new DBConfigSingleton();
        }
        return instance;
    }

    public void openConnection() {
        if (!Base.hasConnection()) {
            Base.open(this.driver, this.dbUrl, this.user, this.pass);
        }
    }

    public void closeConnection() {
        if (Base.hasConnection()) {
            Base.close();
        }
    }

    // Getters
    public String getDbUrl() { return dbUrl; }
    public String getUser() { return user; }
    public String getPass() { return pass; }
    public String getDriver() { return driver; }
}