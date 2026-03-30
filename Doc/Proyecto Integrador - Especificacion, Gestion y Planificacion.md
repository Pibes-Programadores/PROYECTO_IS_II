
# 1. Requirements Describir su proyecto:

## A) Problemas que se quiere resolver

+ Implementar *Equivalencias* entre las materias, para que no cualquier alumno pueda anotarse a una determinada materia.
+ Migrar la DB a MySQL/PostgreSQL.

## B) Usuarios del Sistema

* Estudiantes.
* Profesores.
* Secretaría Académica.
* Gestores de Sistema.


## C) Funcionalidades Principales

### Funcionalidades Generales
- Registrar nuevos usuarios en la plataforma:
	- Alumnos.
	- Docente.
	- Secretaría Académica.
- Registrar Carreras.
- Registrar materias.
- Capturar fallos en la plataforma y generar una interfaz para redirreccionar usuarios.

### Funcionalidades Alumnos 
* Inscribirse materias.
* Inscribirse a exámenes.

### Funcionalidades Secretaria Académica
* Anotar alumnos.
* Asignar docentes a materias.

### Funcionalidades Docentes
-  Cargar notas.
-  Subir avisos, materiales.
-  Reservar aulas.


## D) Restricciones Técnicas

- Según el *nivel/rol* del usuario (Alumno/Docente ,etc), determinar sus funcionalidades. 


## E) Tamaño Equipo

* 5 integrantes.


## F) Tecnologías Elegidas

- **Lenguaje de Desarrollo**: Java.
- **Gestor de Proyecto**: Maven.
- **Visualizador/API**: Spark.
- **Motor Base de Datos**: sqlite3/MySQL.
- **Motores de IA**: Claude, Gemini, Copilot, etc.


## G) Plazo Estimado

* 4 meses.


## H) Cambios de Alcance Ocurridos

Aumentar las funcionalidades otorgadas por el sistema, incorporando también nuevos roles para los usuarios.


## I) Problemas Encontrados

- Migración de **DB** a un nuevo motor.
- Pobre manejo de excepciones.
- Implementación de nuevas funcionalidades. 


## H) Formas de Organización del Equipo

* Metodología Scrum.
	* Breves reuniones informativas para actualizar y documentar el progreso del proyecto.
	* Distribución modular de tareas/funcionalidades en módulos.