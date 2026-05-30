DROP TABLE IF EXISTS sesion;
DROP TABLE IF EXISTS gestorSistema;
DROP TABLE IF EXISTS secretariaAcademica;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS Docente_Materia;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS SolicitudAula;
DROP TABLE IF EXISTS Aula;
DROP TABLE IF EXISTS Materia_Periodo;
DROP TABLE IF EXISTS Correlatividad;
DROP TABLE IF EXISTS Materia;
DROP TABLE IF EXISTS Plan_Estudio;
DROP TABLE IF EXISTS Carrera;


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
    legajo_docente VARCHAR(50) NOT NULL UNIQUE,
    cuil VARCHAR(20) NOT NULL UNIQUE,
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

CREATE TABLE Carrera (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL UNIQUE,
    duracion_anios INT NOT NULL,
    titulo_otorgado VARCHAR(150) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE Plan_Estudio (
    id INT AUTO_INCREMENT PRIMARY KEY,
    carrera_id INT NOT NULL,
    anio_resolucion INT NOT NULL,
    estado ENUM('VIGENTE', 'OBSOLETO') DEFAULT 'VIGENTE',
    CONSTRAINT fk_plan_carrera FOREIGN KEY (carrera_id) REFERENCES Carrera(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Materia (
    codigo INT PRIMARY KEY, 
    plan_estudio_id INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    anio_cursada INT NOT NULL,
    carga_horaria_total INT,
    CONSTRAINT fk_materia_plan FOREIGN KEY (plan_estudio_id) REFERENCES Plan_Estudio(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Docente_Materia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    teacher_id INT NOT NULL,
    materia_id INT NOT NULL,
    CONSTRAINT fk_docente_materia_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(usuario_id) ON DELETE CASCADE,
    CONSTRAINT fk_docente_materia_materia FOREIGN KEY (materia_id) REFERENCES Materia(codigo) ON DELETE CASCADE,
    UNIQUE KEY unique_teacher_materia (teacher_id, materia_id)
) ENGINE=InnoDB;

CREATE TABLE Correlatividad (
    materia_codigo INT NOT NULL,
    materia_correlativa_codigo INT NOT NULL,
    condicion ENUM('REGULAR', 'APROBADA') NOT NULL DEFAULT 'APROBADA',
    tipo_requisito ENUM('CURSAR', 'RENDIR') NOT NULL DEFAULT 'CURSAR', -- <- Columna agregada
    PRIMARY KEY (materia_codigo, materia_correlativa_codigo, tipo_requisito), -- <- PK actualizada
    CONSTRAINT fk_corr_materia FOREIGN KEY (materia_codigo) REFERENCES Materia(codigo) ON DELETE CASCADE,
    CONSTRAINT fk_corr_requisito FOREIGN KEY (materia_correlativa_codigo) REFERENCES Materia(codigo) ON DELETE CASCADE,
    CONSTRAINT chk_no_auto_correlativa CHECK (materia_codigo != materia_correlativa_codigo)
) ENGINE=InnoDB;

CREATE TABLE Materia_Periodo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    materia_codigo INT NOT NULL,         
    anio_academico INT NOT NULL,
    tipo_cuatrimestre ENUM('PRIMER_CUATRIMESTRE', 'SEGUNDO_CUATRIMESTRE', 'ANUAL', 'VERANO') NOT NULL,
    CONSTRAINT fk_periodo_materia FOREIGN KEY (materia_codigo) REFERENCES Materia(codigo) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Aula (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero_nombre VARCHAR(50) NOT NULL UNIQUE,
    capacidad INT NOT NULL,
    estado_aula ENUM('DISPONIBLE', 'OCUPADA', 'EN_MANTENIMIENTO', 'CLAUSURADA') DEFAULT 'DISPONIBLE'
) ENGINE=InnoDB;

CREATE TABLE SolicitudAula (
    id INT AUTO_INCREMENT PRIMARY KEY,
    aula_id INT NOT NULL,
    materia_periodo_id INT NOT NULL,
    dia_semana ENUM('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO') NOT NULL,
    horario_inicio TIME NOT NULL,
    horario_fin TIME NOT NULL,
    estado_solicitud ENUM('PENDIENTE', 'APROBADA', 'RECHAZADA') DEFAULT 'PENDIENTE',
    CONSTRAINT fk_solicitud_aula FOREIGN KEY (aula_id) REFERENCES Aula(id) ON DELETE CASCADE,
    CONSTRAINT fk_solicitud_materia FOREIGN KEY (materia_periodo_id) REFERENCES Materia_Periodo(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- SCRIPT DE INICIALIZACIÓN (SEED)
-- Creamos el superusuario por defecto para todo el equipo
INSERT IGNORE INTO users (dni, nombre, apellido, nombre_usuario, password, nivel_acceso) 
VALUES (
    '00000000', 
    'Administrador', 
    'Sistema', 
    'admin', 
    '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', -- Esta es la clave 'admin' hasheada (reemplazala por el hash real que tengas en tu BD)
    'ADMIN'
);