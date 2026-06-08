USE proyecto_is_ii;

INSERT INTO Carrera (id, nombre, duracion_anios, titulo_otorgado) VALUES
(1, 'Analista en Ciencias de la Computación',  3, 'Analista en Ciencias de la Computación'),
(2, 'Ingeniería Agronómica', 5, 'Ingeniero/a agrónomo/a');

INSERT INTO Plan_Estudio (id, carrera_id, anio_resolucion, estado) VALUES
(1, 1, 2018, 'VIGENTE'),
(2, 2, 2023, 'VIGENTE'),
(3, 1, 2010, 'OBSOLETO');


-- DOCENTES
INSERT INTO users (id, dni, nombre, apellido, direccion, telefono, nombre_usuario, password, nivel_acceso) VALUES
(10, '20111000', 'Roberto', 'Pérez',    'Banda Norte',  '3584100001', 'rperez',    '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'DOCENTE'),
(11, '21222000', 'Silvia',  'Romero',   'Centro',       '3584100002', 'sromero',   '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'DOCENTE'),
(12, '22333000', 'Gustavo', 'Ferreyra', 'Alberdi',      '3584100003', 'gferreyra', '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'DOCENTE');

-- SECRETARÍA
INSERT INTO users (id, dni, nombre, apellido, direccion, telefono, nombre_usuario, password, nivel_acceso) VALUES
(20, '30444000', 'Laura',   'Medina',   'Riverside',   '3584200001', 'lmedina',   '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'SECRETARIA');

-- ESTUDIANTES
INSERT INTO users (id, dni, nombre, apellido, direccion, telefono, nombre_usuario, password, nivel_acceso) VALUES
(30, '40111001', 'Valentina', 'López',    'Banda Norte',  '3584300001', 'vlopez',    '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'ESTUDIANTE'),
(31, '41222002', 'Matías',   'González', 'Centro',       '3584300002', 'mgonzalez', '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'ESTUDIANTE'),
(32, '42333003', 'Camila',   'Torres',   'Alberdi',      '3584300003', 'ctorres',   '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'ESTUDIANTE'),
(33, '43444004', 'Luca',    'Díaz',     'Villa Dalcar',   '3584300004', 'ldiaz',     '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'ESTUDIANTE'),
(34, '44555005', 'Joaquin',  'Álvarez',  'Delicias',    '3584300005', 'jalvarez',  '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'ESTUDIANTE'),
(35, '45666006', 'Manuel',  'Sánchez',  'Centro',      '3584300006', 'msanchez',  '$2a$10$tzGyrad6vMs9/BPymyxxv.JdZ8KEaDipWPuj1UqE1U6KuzRbDciy6', 'ESTUDIANTE');


INSERT INTO teacher (usuario_id, legajo_docente, cuil, email, especialidad) VALUES
(10, 'D-3001', '20-20111000-3', 'rperez@agro.unrc.edu.ar',    'Ganadería'),
(11, 'D-3002', '27-21222000-4', 'sromero@exa.unrc.edu.ar',   'Ingeniería de Software y Metodologías'),
(12, 'D-3003', '20-22333000-5', 'gferreyra@exa.unrc.edu.ar', 'Sistemas Operativos y Redes');

INSERT INTO secretariaAcademica (usuario_id, oficina, interno) VALUES
(20, 'Pabellón 1 - Piso 2', '312');

INSERT INTO student (usuario_id, legajo, tipo_estudiante, plan_estudio_id) VALUES
(30, 'E-2001', 'REGULAR',     1),
(31, 'E-2002', 'REGULAR',     1),
(32, 'E-2003', 'REGULAR',     1),
(33, 'E-2004', 'REGULAR',     2),
(34, 'E-2005', 'VOCACIONAL',  2),
(35, 'E-2006', 'INTERCAMBIO', 2);   

-- Compu
INSERT INTO Materia (codigo, plan_estudio_id, nombre, anio_cursada, carga_horaria_total) VALUES
(101, 1, 'Matemática Discreta',            1, 120),
(102, 1, 'Introducción a la Algorítmica',  1, 120),
(111, 1, 'Estructuras de Datos',           2, 120),
(112, 1, 'Bases de Datos',                 2, 120),
(121, 1, 'Ingeniería de Software I',       3, 100);

-- Agro
INSERT INTO Materia (codigo, plan_estudio_id, nombre, anio_cursada, carga_horaria_total) VALUES
(501, 2, 'Ganadería I',            1, 120),
(502, 2, 'Gestión de Campos', 1, 100),
(511, 2, 'Agroquímicos II', 2, 160);


INSERT INTO Correlatividad (materia_codigo, materia_correlativa_codigo, condicion, tipo_requisito) VALUES
-- Compu: para cursar
(111, 101, 'APROBADA', 'CURSAR'),
(111, 102, 'REGULAR',  'CURSAR'),
(112, 101, 'REGULAR',  'CURSAR'),
(121, 111, 'APROBADA', 'CURSAR'),
(121, 112, 'REGULAR',  'CURSAR'),
-- Compu: para rendir final
(111, 101, 'APROBADA', 'RENDIR'),
(112, 101, 'APROBADA', 'RENDIR'),
(121, 112, 'APROBADA', 'RENDIR'),
-- Agro: para cursar
(511, 501, 'REGULAR',  'CURSAR'),
(511, 502, 'REGULAR',  'CURSAR'),
-- Agro: para rendir final
(511, 501, 'APROBADA', 'RENDIR');


INSERT INTO Docente_Materia (teacher_id, materia_id) VALUES
(11, 101),
(11, 112),
(12, 102),
(12, 111),
(11, 121),
(10, 501),
(10, 502),
(10, 511);

INSERT INTO Docente_Carrera (teacher_id, carrera_id) VALUES
(11, 1),
(12, 1),
(10, 2);


INSERT INTO Materia_Periodo (id, materia_codigo, anio_academico, tipo_cuatrimestre) VALUES
-- 1er cuatrimestre 2026
(1,  101, 2026, 'PRIMER_CUATRIMESTRE'),   -- Mat Dis
(2,  102, 2026, 'PRIMER_CUATRIMESTRE'),   -- Intro Algor
(3,  111, 2026, 'PRIMER_CUATRIMESTRE'),   -- Estructuras
(4,  121, 2026, 'PRIMER_CUATRIMESTRE'),   -- Ing Soft
(5,  501, 2026, 'PRIMER_CUATRIMESTRE'),   -- Gan I
(6,  502, 2026, 'PRIMER_CUATRIMESTRE'),   -- Gestión
-- 2do cuatrimestre 2026
(7,  112, 2026, 'SEGUNDO_CUATRIMESTRE'),  -- Bases de Datos
(8,  511, 2026, 'SEGUNDO_CUATRIMESTRE');  -- Agroquímicos II


INSERT INTO Aula (id, numero_nombre, capacidad, estado_aula) VALUES
(1, 'Aula 10 - Pabellón 1',      60,  'DISPONIBLE'),
(2, 'Aula 11 - Pabellón 1',      60,  'DISPONIBLE'),
(3, 'Laboratorio 101',   40,  'DISPONIBLE'),
(4, 'Laboratorio 110',   40,  'DISPONIBLE'),
(5, 'Aula Magna',                250,  'OCUPADA'),
(6, 'Aula 3 - Pabellón 2',       40,  'EN_MANTENIMIENTO');


INSERT INTO SolicitudAula (aula_id, materia_periodo_id, dia_semana, horario_inicio, horario_fin, estado_solicitud) VALUES
(3, 3, 'LUNES',    '08:00:00', '10:00:00', 'APROBADA'),   -- Lab1, Estructuras
(1, 1, 'MARTES',   '10:00:00', '12:00:00', 'APROBADA'),   -- Aula 10, Mat. Discreta
(2, 4, 'MIERCOLES','14:00:00', '17:00:00', 'PENDIENTE'),  -- Ing. Software (para aprobar en demo)
(4, 7, 'JUEVES',   '09:00:00', '12:00:00', 'PENDIENTE'),  -- Bases de Datos (para aprobar en demo)
(1, 8, 'VIERNES',  '16:00:00', '18:00:00', 'RECHAZADA'),  -- Agroquímicos II (rechazada)
(2, 5, 'LUNES',    '14:00:00', '16:00:00', 'APROBADA');   -- Ganadería I


INSERT INTO Aula_Asignacion (materia_periodo_id, teacher_id, aula) VALUES
(3, 12, 'Laboratorio 1 - Exactas'),
(1, 11, 'Aula 10 - Pabellón 1'),
(5, 10, 'Aula 11 - Pabellón 1');


INSERT INTO Anuncio (id, materia_periodo_id, teacher_id, tipo, titulo, contenido, fecha_examen) VALUES
-- Matemática Discreta
(1, 1, 11, 'GENERAL', 'Apunte Unidad 1 disponible',
   'El material de grafos ya está en el campus virtual. Leer antes del jueves.', NULL),
(2, 1, 11, 'EXAMEN',  'Primer Parcial — Matemática Discreta',
   'Temas: grafos, relaciones de equivalencia y combinatoria. Escrito con libro cerrado.', '2026-04-25'),
(3, 1, 11, 'EXAMEN',  'Segundo Parcial — Matemática Discreta',
   'Temas: álgebra booleana, lógica proposicional y cardinalidad.', '2026-06-13'),

-- Estructuras de Datos
(4, 3, 12, 'GENERAL', 'Entrega TP Árboles AVL',
   'La entrega es el viernes 17/5 antes de las 23:59 por el repositorio del grupo.', NULL),
(5, 3, 12, 'EXAMEN',  'Primer Parcial — Estructuras de Datos',
   'Temas: listas enlazadas, pilas, colas y árboles binarios.', '2026-04-30'),

-- Ingeniería de Software I
(6, 4, 11, 'GENERAL', 'Formación de grupos para el proyecto integrador',
   'Los grupos de 4 integrantes deben registrarse antes del viernes en el aula virtual.', NULL),
(7, 4, 11, 'EXAMEN',  'Parcial de Ingeniería de Software',
   'Temas: Scrum, casos de uso, diagramas de clase y estimación por Puntos de Función.', '2026-05-07'),

-- Ganadería I
(8, 5, 10, 'GENERAL', 'Bienvenida a Ganadería I',
   'Se adjunta el programa de la materia y el cronograma de trabajos prácticos de campo.', NULL),
(9, 5, 10, 'EXAMEN',  'Primer Parcial — Ganadería I',
   'Temas: razas bovinas, sistemas de producción extensivos e índices reproductivos.', '2026-05-12'),

-- Bases de Datos (2do cuatrimestre)
(10, 7, 11, 'GENERAL', 'Inicio del 2do cuatrimestre — Bases de Datos',
   'La cursada comienza el 4 de agosto. Revisar horarios actualizados en el campus.', NULL);

-- Valentina compu
INSERT INTO Estado_Academico (usuario_id, materia_codigo, estado) VALUES
(30, 101, 'APROBADO'),
(30, 102, 'APROBADO'),
(30, 111, 'APROBADO'),
(30, 112, 'REGULAR'),
(30, 121, 'INSCRIPTO');

-- Matias compu
INSERT INTO Estado_Academico (usuario_id, materia_codigo, estado) VALUES
(31, 101, 'APROBADO'),
(31, 102, 'REGULAR'),
(31, 111, 'REGULAR'),
(31, 112, 'INSCRIPTO');

-- Camila compu
INSERT INTO Estado_Academico (usuario_id, materia_codigo, estado) VALUES
(32, 101, 'INSCRIPTO'),
(32, 102, 'REPROBADO');   -- para demo de estado REPROBADO

-- Luca agro
INSERT INTO Estado_Academico (usuario_id, materia_codigo, estado) VALUES
(33, 501, 'APROBADO'),
(33, 502, 'REGULAR'),
(33, 511, 'INSCRIPTO');

-- Joaquín agro vocacional
INSERT INTO Estado_Academico (usuario_id, materia_codigo, estado) VALUES
(34, 501, 'REGULAR'),
(34, 502, 'LIBRE');        -- para demo de restricción al rendir final

-- Manuel agro intercambio
INSERT INTO Estado_Academico (usuario_id, materia_codigo, estado) VALUES
(35, 501, 'PROMOCION'),
(35, 502, 'APROBADO');


INSERT INTO Nota (materia_periodo_id, student_id, teacher_id, valor, instancia) VALUES
-- Mat. Discreta — parciales
(1, 30, 11, 8.00, 'PARCIAL'),
(1, 31, 11, 6.50, 'PARCIAL'),
(1, 32, 11, 3.00, 'PARCIAL'),   -- desaprobó
-- Mat. Discreta — cierre de cursada
(1, 30, 11, 7.50, 'CURSADA'),
(1, 31, 11, 6.00, 'CURSADA'),
-- Mat. Discreta — finales ya rendidos
(1, 30, 11, 8.00, 'FINAL'),

-- Estructuras de Datos — parciales
(3, 30, 12, 9.00, 'PARCIAL'),
(3, 31, 12, 5.50, 'PARCIAL'),
-- Estructuras de Datos — cierre de cursada
(3, 30, 12, 8.50, 'CURSADA'),

-- Ganadería I — parciales
(5, 33, 10, 7.00, 'PARCIAL'),
(5, 34, 10, 6.00, 'PARCIAL'),
(5, 35, 10, 9.50, 'PARCIAL'),
-- Ganadería I — cierre de cursada
(5, 33, 10, 7.00, 'CURSADA'),
(5, 35, 10, 9.00, 'CURSADA'),
-- Ganadería I — final rendido
(5, 33, 10, 7.50, 'FINAL');


INSERT INTO Inscripcion_Parcial (usuario_id, anuncio_id) VALUES
-- Primer parcial Mat. Discreta (anuncio 2)
(30, 2),
(31, 2),
(32, 2),
-- Segundo parcial Mat. Discreta (anuncio 3)
(30, 3),
(31, 3),
-- Primer parcial Estructuras (anuncio 5)
(30, 5),
(31, 5),
-- Parcial Ing. Software (anuncio 7)
(30, 7),
-- Primer parcial Ganadería I (anuncio 9)
(33, 9),
(34, 9),
(35, 9);


INSERT INTO mesas_examen (id, materia_codigo, fecha) VALUES
(1, 101, '2026-07-14'),   -- Mat. Discreta — turno julio
(2, 102, '2026-07-16'),   -- Intro Algorítmica — turno julio
(3, 111, '2026-07-21'),   -- Estructuras de Datos — turno julio
(4, 101, '2026-12-09'),   -- Mat. Discreta — turno diciembre
(5, 112, '2026-12-15'),   -- Bases de Datos — turno diciembre
(6, 501, '2026-07-18'),   -- Ganadería I — turno julio
(7, 502, '2026-07-23');   -- Gestión de Campos — turno julio


INSERT INTO inscripciones_examen (usuario_id, mesa_id) VALUES
(31, 1),   -- Matías  (REGULAR en 101) → Mat. Discreta julio
(31, 3),   -- Matías  (REGULAR en 111) → Estructuras julio
(31, 4),   -- Matías  → Mat. Discreta diciembre
(34, 6),   -- Joaquín (REGULAR en 501) → Ganadería julio
(33, 7);   -- Luca    (REGULAR en 502) → Gestión de Campos julio

-- Contraseña para todos: admin
INSERT INTO gestorSistema (usuario_id, area_responsabilidad) VALUES
(1, 'Administración general del sistema');