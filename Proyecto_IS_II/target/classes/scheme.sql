-- Elimina las tablas si existen para evitar errores al recrear (orden inverso a las dependencias)
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS users;

-- Crea la tabla 'users' adaptada para MySQL
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY, -- En MySQL se usa AUTO_INCREMENT en lugar de AUTOINCREMENT
    name VARCHAR(255) NOT NULL UNIQUE, -- VARCHAR es el estándar para cadenas en MySQL
    password VARCHAR(255) NOT NULL
);

-- Crea la tabla 'teacher' adaptada para MySQL
CREATE TABLE teacher (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dni VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    lastName VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    phone BIGINT -- BIGINT para evitar que se rompa con números de teléfono largos
);