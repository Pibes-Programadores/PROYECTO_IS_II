DROP TABLE IF EXISTS sesion;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS secretariaAcademica;
DROP TABLE IF EXISTS gestorSistema;
DROP TABLE IF EXISTS users;

-- Creación de la Tabla Base: Usuario
-- Usamos ENUM para Nivel_Acceso como pide el catálogo.
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dni VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    direccion VARCHAR(255),
    telefono VARCHAR(50),
    nombre_usuario VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nivel_acceso ENUM('ESTUDIANTE', 'DOCENTE', 'SECRETARIA', 'ADMIN') NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;


-- Tabla Estudiante
-- Relación 1:1 con Usuario. El id del estudiante es el mismo id del usuario.
CREATE TABLE student (
    usuario_id INT PRIMARY KEY,
    legajo VARCHAR(20) NOT NULL UNIQUE,
    tipo_estudiante ENUM('REGULAR', 'VOCACIONAL', 'INTERCAMBIO') NOT NULL,
    CONSTRAINT fk_estudiante_usuario 
        FOREIGN KEY (usuario_id) REFERENCES users(id) 
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- Tabla Docente
CREATE TABLE teacher (
    usuario_id INT PRIMARY KEY,
    cuil VARCHAR(20) NOT NULL UNIQUE,
    titulo VARCHAR(100),
    CONSTRAINT fk_docente_usuario 
        FOREIGN KEY (usuario_id) REFERENCES users(id) 
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE teacher (
    usuario_id INT PRIMARY KEY,
    legajo_docente VARCHAR(50) NOT NULL UNIQUE,
    cuil VARCHAR(20) NOT NULL UNIQUE, -- Agregado nuevamente
    email VARCHAR(150) NOT NULL UNIQUE,
    especialidad VARCHAR(150),
    CONSTRAINT fk_teacher_user FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Tabla SecretariaAcademica
CREATE TABLE secretariaAcademica (
    usuario_id INT PRIMARY KEY,
    oficina VARCHAR(50),
    interno VARCHAR(20),
    CONSTRAINT fk_secretaria_usuario 
        FOREIGN KEY (usuario_id) REFERENCES users(id) 
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- Tabla GestorSistema (Administrador IT)
CREATE TABLE gestorSistema (
    usuario_id INT PRIMARY KEY,
    area_responsabilidad VARCHAR(100),
    CONSTRAINT fk_gestor_usuario 
        FOREIGN KEY (usuario_id) REFERENCES users(id) 
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- Tabla Sesion
-- Implementa ON DELETE CASCADE para que si se borra el usuario, se borren sus sesiones.
CREATE TABLE sesion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    fecha_inicio DATETIME NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    CONSTRAINT fk_sesion_usuario 
        FOREIGN KEY (usuario_id) REFERENCES users(id) 
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- SCRIPT DE INICIALIZACIÓN (SEED)
-- Creamos el superusuario por defecto para todo el equipo
INSERT IGNORE INTO users (dni, nombre, apellido, nombre_usuario, password, nivel_acceso) 
VALUES (
    '00000000', 
    'Administrador', 
    'Sistema', 
    'admin', 
    '$2a$10$vI8tmZH.RYv4uV9tf.X8su6U2S0.M.G8Y.199321.123', -- Esta es la clave 'admin' hasheada (reemplazala por el hash real que tengas en tu BD)
    'ADMIN'
);