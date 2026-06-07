SET FOREIGN_KEY_CHECKS = 0;
USE proyecto_is_ii;
DROP TABLE IF EXISTS sesion;
DROP TABLE IF EXISTS gestorSistema;
DROP TABLE IF EXISTS secretariaAcademica;
DROP TABLE IF EXISTS Docente_Carrera;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS Docente_Materia;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS SolicitudAula;
DROP TABLE IF EXISTS Aula;
DROP TABLE IF EXISTS Aula_Asignacion;
DROP TABLE IF EXISTS Inscripcion_Parcial;
DROP TABLE IF EXISTS Nota;
DROP TABLE IF EXISTS Anuncio;
DROP TABLE IF EXISTS Materia_Periodo;
DROP TABLE IF EXISTS Correlatividad;
DROP TABLE IF EXISTS Materia;
DROP TABLE IF EXISTS Plan_Estudio;
DROP TABLE IF EXISTS Carrera;
DROP TABLE IF EXISTS inscripciones_examen;
DROP TABLE IF EXISTS mesas_examen;
DROP TABLE IF EXISTS Estado_Academico;
 
-- Creación de la Tabla Base: Usuario
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
 
CREATE TABLE student (
    usuario_id INT PRIMARY KEY,
    legajo VARCHAR(20) NOT NULL UNIQUE,
    tipo_estudiante ENUM('REGULAR', 'VOCACIONAL', 'INTERCAMBIO') NOT NULL,
    plan_estudio_id INT NOT NULL,
    CONSTRAINT fk_estudiante_usuario 
        FOREIGN KEY (usuario_id) REFERENCES users(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_estudiante_plan
        FOREIGN KEY (plan_estudio_id) REFERENCES Plan_Estudio(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;
 
CREATE TABLE teacher (
    usuario_id INT PRIMARY KEY,
    legajo_docente VARCHAR(50) NOT NULL UNIQUE,
    cuil VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    titulo VARCHAR(100),
    especialidad VARCHAR(150),
    CONSTRAINT fk_teacher_user 
        FOREIGN KEY (usuario_id) REFERENCES users(id) 
        ON DELETE CASCADE
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
    tipo_requisito ENUM('CURSAR', 'RENDIR') NOT NULL DEFAULT 'CURSAR',
    PRIMARY KEY (materia_codigo, materia_correlativa_codigo, tipo_requisito),
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
 
CREATE TABLE Docente_Carrera (
    teacher_id INT NOT NULL,
    carrera_id INT NOT NULL,
    PRIMARY KEY (teacher_id, carrera_id),
    CONSTRAINT fk_dc_teacher  FOREIGN KEY (teacher_id)  REFERENCES teacher(usuario_id)  ON DELETE CASCADE,
    CONSTRAINT fk_dc_carrera  FOREIGN KEY (carrera_id)  REFERENCES Carrera(id) ON DELETE CASCADE
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
 
CREATE TABLE Anuncio (
    id INT AUTO_INCREMENT PRIMARY KEY,
    materia_periodo_id INT NOT NULL,
    teacher_id INT NOT NULL,
    tipo ENUM('GENERAL', 'EXAMEN') NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    contenido TEXT NOT NULL,
    fecha_examen DATE NULL,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (materia_periodo_id) REFERENCES Materia_Periodo(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teacher(usuario_id) ON DELETE CASCADE
) ENGINE=InnoDB;
 
CREATE TABLE Nota (
    id INT AUTO_INCREMENT PRIMARY KEY,
    materia_periodo_id INT NOT NULL,
    student_id INT NOT NULL,
    teacher_id INT NOT NULL,
    valor DECIMAL(5,2) NOT NULL,
    fecha_carga DATETIME DEFAULT CURRENT_TIMESTAMP,
    instancia ENUM('PARCIAL', 'CURSADA', 'FINAL') NOT NULL DEFAULT 'CURSADA',
    FOREIGN KEY (materia_periodo_id) REFERENCES Materia_Periodo(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES student(usuario_id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teacher(usuario_id) ON DELETE CASCADE
) ENGINE=InnoDB;
 
CREATE TABLE Aula_Asignacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    materia_periodo_id INT NOT NULL,
    teacher_id INT NOT NULL,
    aula VARCHAR(50) NOT NULL,
    fecha_asignacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (materia_periodo_id) REFERENCES Materia_Periodo(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teacher(usuario_id) ON DELETE CASCADE
) ENGINE=InnoDB;
 
CREATE TABLE IF NOT EXISTS mesas_examen (
    id INT AUTO_INCREMENT PRIMARY KEY,
    materia_codigo INT NOT NULL,
    fecha DATE NOT NULL,
    CONSTRAINT fk_mesa_materia FOREIGN KEY (materia_codigo) REFERENCES Materia(codigo) ON DELETE CASCADE
) ENGINE=InnoDB;
 
CREATE TABLE IF NOT EXISTS inscripciones_examen (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    mesa_id INT NOT NULL,
    fecha_inscripcion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inscripcion_alumno FOREIGN KEY (usuario_id) REFERENCES student(usuario_id) ON DELETE CASCADE,
    CONSTRAINT fk_inscripcion_mesa FOREIGN KEY (mesa_id) REFERENCES mesas_examen(id) ON DELETE CASCADE,
    UNIQUE KEY unique_alumno_mesa (usuario_id, mesa_id)
) ENGINE=InnoDB;
 
CREATE TABLE Estado_Academico (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    materia_codigo INT NOT NULL,
    estado ENUM('INSCRIPTO', 'REGULAR', 'APROBADO', 'REPROBADO', 'LIBRE', 'PROMOCION') NOT NULL,
    fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_estado_estudiante FOREIGN KEY (usuario_id) REFERENCES student(usuario_id) ON DELETE CASCADE,
    CONSTRAINT fk_estado_materia FOREIGN KEY (materia_codigo) REFERENCES Materia(codigo) ON DELETE CASCADE,
    UNIQUE KEY unique_estudiante_materia (usuario_id, materia_codigo)
) ENGINE=InnoDB;
 
CREATE TABLE Inscripcion_Parcial (
    id INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    usuario_id INT NOT NULL,
    anuncio_id INT NOT NULL,
    fecha_inscripcion DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ip_usuario FOREIGN KEY (usuario_id) REFERENCES student(usuario_id) ON DELETE CASCADE,
    CONSTRAINT fk_ip_anuncio FOREIGN KEY (anuncio_id) REFERENCES Anuncio(id) ON DELETE CASCADE,
    UNIQUE KEY unique_estudiante_parcial (usuario_id, anuncio_id)
) ENGINE=InnoDB;
 
INSERT IGNORE INTO users (dni, nombre, apellido, nombre_usuario, password, nivel_acceso) 
VALUES (
    '00000000', 
    'Administrador', 
    'Sistema', 
    'admin', 
    '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6',
    'ADMIN'
);