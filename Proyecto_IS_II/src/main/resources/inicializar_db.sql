USE proyecto_is_ii;

-- Inserción de Carreras
INSERT INTO Carrera (id, nombre, duracion_anios, titulo_otorgado) VALUES 
(1, 'Analista en Ciencias de la Computación', 3, 'Analista en Ciencias de la Computación'),
(2, 'Licenciatura en Ciencias de la Computación', 5, 'Licenciado en Ciencias de la Computación');

-- Inserción de Planes de Estudio
INSERT INTO Plan_Estudio (id, carrera_id, anio_resolucion, estado) VALUES 
(1, 1, 2018, 'VIGENTE'),
(2, 2, 2018, 'VIGENTE');

-- Inserción de Usuarios (Contraseña por defecto igual a admin)
INSERT INTO users (id, dni, nombre, apellido, direccion, telefono, nombre_usuario, password, nivel_acceso) VALUES
(2, '40111222', 'Lauti', 'Suárez', 'Río Cuarto', '3584111111', 'lsuarez', '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'ESTUDIANTE'),
(3, '41222333', 'Feli', 'García', 'Río Cuarto', '3584222222', 'fgarcia', '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'ESTUDIANTE'),
(4, '42333444', 'Jero', 'Martínez', 'Río Cuarto', '3584333333', 'jmartinez', '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'ESTUDIANTE'),
(5, '30555666', 'Carlos', 'Docente', 'Río Cuarto', '3584555555', 'cdocente', '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'DOCENTE'),
(6, '32666777', 'Ana', 'Profesora', 'Río Cuarto', '3584666666', 'aprofesora', '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'DOCENTE'),
(7, '28777888', 'Marta', 'Secretaria', 'Río Cuarto', '3584777777', 'msecretaria', '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'SECRETARIA');

-- Inserción de Estudiantes
INSERT INTO student (usuario_id, legajo, tipo_estudiante, plan_estudio_id) VALUES
(2, 'A-1001', 'REGULAR', 1),
(3, 'A-1002', 'REGULAR', 1),
(4, 'A-1003', 'VOCACIONAL', 2);

-- Inserción de Docentes
INSERT INTO teacher (usuario_id, legajo_docente, cuil, email, especialidad) VALUES
(5, 'D-2001', '20-30555666-5', 'cdocente@exa.unrc.edu.ar', 'Bases de Datos y Sistemas de Información'),
(6, 'D-2002', '27-32666777-4', 'aprofesora@exa.unrc.edu.ar', 'Matemática Discreta y Algorítmica');

-- Inserción de Secretaría Académica
INSERT INTO secretariaAcademica (usuario_id, oficina, interno) VALUES
(7, 'Pabellón 2', '458');

-- Inserción de Materias
INSERT INTO Materia (codigo, plan_estudio_id, nombre, anio_cursada, carga_horaria_total) VALUES
(101, 1, 'Matemática Discreta', 1, 120),
(102, 1, 'Introducción a la Algorítmica', 1, 120),
(201, 1, 'Sistemas Operativos', 2, 100),
(202, 1, 'Bases de Datos', 2, 120),
(301, 1, 'Ingeniería de Software', 3, 100);

-- Inserción de Relación Docente-Materia
INSERT INTO Docente_Materia (teacher_id, materia_id) VALUES
(6, 101),
(5, 202),
(5, 301);

-- Inserción de Correlatividades
INSERT INTO Correlatividad (materia_codigo, materia_correlativa_codigo, condicion, tipo_requisito) VALUES
(201, 101, 'APROBADA', 'CURSAR'),
(202, 101, 'REGULAR', 'CURSAR'),
(301, 202, 'APROBADA', 'CURSAR');

-- Inserción de Períodos de Materias
INSERT INTO Materia_Periodo (id, materia_codigo, anio_academico, tipo_cuatrimestre) VALUES
(1, 101, 2026, 'PRIMER_CUATRIMESTRE'),
(2, 202, 2026, 'SEGUNDO_CUATRIMESTRE'),
(3, 301, 2026, 'PRIMER_CUATRIMESTRE');

-- Inserción de Relación Docente-Carrera
INSERT INTO Docente_Carrera (teacher_id, carrera_id) VALUES
(5, 1),
(6, 1),
(5, 2);

-- Inserción de Aulas
INSERT INTO Aula (id, numero_nombre, capacidad, estado_aula) VALUES
(1, 'Aula 11 - Pabellón 2', 60, 'DISPONIBLE'),
(2, 'Laboratorio 3 - Exactas', 35, 'DISPONIBLE'),
(3, 'Aula Magna', 250, 'OCUPADA');

-- Inserción de Solicitudes de Aulas
INSERT INTO SolicitudAula (aula_id, materia_periodo_id, dia_semana, horario_inicio, horario_fin, estado_solicitud) VALUES
(2, 2, 'MARTES', '10:00:00', '13:00:00', 'APROBADA'),
(1, 1, 'JUEVES', '14:00:00', '16:00:00', 'PENDIENTE');

-- Inserción de Asignación de Aulas
INSERT INTO Aula_Asignacion (materia_periodo_id, teacher_id, aula) VALUES
(2, 5, 'Laboratorio 3 - Exactas');

-- Inserción de Anuncios
INSERT INTO Anuncio (materia_periodo_id, teacher_id, tipo, titulo, contenido, fecha_examen) VALUES
(1, 6, 'GENERAL', 'Material de lectura inicial', 'El apunte de la unidad 1 ya está disponible.', NULL),
(2, 5, 'EXAMEN', 'Primer Parcial de BD', 'Temas: Diseño Lógico, BCNF y 3NF.', '2026-05-15');

-- Inserción de Notas
INSERT INTO Nota (materia_periodo_id, student_id, teacher_id, valor) VALUES
(1, 2, 6, 8.50),
(1, 3, 6, 6.00);

-- Inserción de Estados Académicos
INSERT INTO Estado_Academico (usuario_id, materia_codigo, estado) VALUES
(2, 101, 'APROBADO'),
(2, 202, 'INSCRIPTO'),
(3, 101, 'REGULAR'),
(4, 101, 'LIBRE');

-- Inscribir en primer parcial DB
INSERT INTO Inscripcion_Parcial (usuario_id, anuncio_id) VALUES
(2, 1),
(3, 1);