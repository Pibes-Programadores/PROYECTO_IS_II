#  Sistema de Gestión Universitaria

Aplicación web para la gestión integral de una universidad, desarrollada como proyecto de Ingeniería de Software II.

---

##  Descripción

Este sistema permite administrar los procesos académicos de una universidad, incluyendo la gestión de alumnos, docentes, materias, inscripciones y demás entidades del ámbito universitario. La aplicación expone sus funcionalidades a través de una interfaz web dinámica.

---

##  Tecnologías utilizadas

| Tecnología | Descripción |
|---|---|
| **Java** | Lenguaje principal de desarrollo |
| **Spark Java** | Framework web liviano para el manejo de rutas y HTTP |
| **Mustache** | Motor de plantillas para la generación de vistas HTML |
| **Maven** | Gestión de dependencias y construcción del proyecto |

---

##  Instalación y configuración

### Requisitos previos

Antes de comenzar, asegurate de tener instalado:

- **Java 11 o superior**
  -  Windows /  macOS /  Linux: Descargarlo desde [https://adoptium.net](https://adoptium.net) y seguir el instalador correspondiente.
  - Verificar la instalación:
    ```bash
    java -version
    ```

- **Maven 3.6 o superior**
  -  **Windows**: Descargar el binario desde [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi), descomprimir y agregar la carpeta `bin` al `PATH` del sistema.
  -  **macOS**: Instalar con Homebrew:
    ```bash
    brew install maven
    ```
  -  **Linux** (Debian/Ubuntu):
    ```bash
    sudo apt update && sudo apt install maven
    ```
  - Verificar la instalación:
    ```bash
    mvn -version
    ```

---

### Pasos para ejecutar el proyecto

1. **Clonar el repositorio**

   ```bash
   git clone https://github.com/Pibes-Programadores/PROYECTO_IS_II.git
   ```

2. **Ingresar al directorio del proyecto**

   -  **Windows** (Command Prompt o PowerShell):
     ```cmd
     cd PROYECTO_IS_II\Proyecto_IS_II
     ```
   -  **macOS** /  **Linux**:
     ```bash
     cd PROYECTO_IS_II/Proyecto_IS_II
     ```

3. **Compilar el proyecto con Maven**

   En todos los sistemas operativos:
   ```bash
   mvn clean install
   ```

4. **Ejecutar la aplicación**

   En todos los sistemas operativos:
   ```bash
   mvn exec:java
   ```

5. **Abrir en el navegador**

   Una vez iniciada la aplicación, abrí tu navegador e ingresá a:
   ```
   http://localhost:8080
   ```

---

##  Estructura del proyecto

```
Proyecto_IS_II
|-- LICENSE
|-- META-INF
|   `-- MANIFEST.MF
|-- Proyecto_IS_II
|-- db
|   |-- dev.db
|   `-- prod.db
|-- dependency-reduced-pom.xml
|-- dummy_test
|-- pom.xml
|-- src
|   |-- main
|   |   |-- java
|   |   |   `-- com
|   |   |       `-- is1
|   |   |           `-- proyecto
|   |   |               |-- App.java
|   |   |               |-- config
|   |   |               |   `-- DBConfigSingleton.java
|   |   |               `-- models
|   |   |                   |-- Anuncio.java
|   |   |                   |-- AulaAsignacion.java
|   |   |                   |-- Carrera.java
|   |   |                   |-- Correlatividad.java
|   |   |                   |-- DocenteCarrera.java
|   |   |                   |-- DocenteMateria.java
|   |   |                   |-- EstadoAcademico.java
|   |   |                   |-- GestorSistema.java
|   |   |                   |-- InscripcionExamen.java
|   |   |                   |-- Materia.java
|   |   |                   |-- MateriaPeriodo.java
|   |   |                   |-- MesaExamen.java
|   |   |                   |-- Nota.java
|   |   |                   |-- PlanEstudio.java
|   |   |                   |-- SecretariaAcademica.java
|   |   |                   |-- Sesion.java
|   |   |                   |-- Student.java
|   |   |                   |-- Teacher.java
|   |   |                   `-- User.java
|   |   `-- resources
|   |       |-- activejdbc_models.properties
|   |       |-- scheme.sql
|   |       `-- templates
|   |           |-- assign_materia_form.mustache
|   |           |-- carrera_form.mustache
|   |           |-- contenido_proximamente.mustache
|   |           |-- dashboard.mustache
|   |           |-- docente_materias.mustache
|   |           |-- docente_panel_materia.mustache
|   |           |-- error.mustache
|   |           |-- hello.mustache
|   |           |-- inscripcion_examenes.mustache
|   |           |-- inscripcion_materias.mustache
|   |           |-- login.mustache
|   |           |-- materia_form.mustache
|   |           |-- materias_list.mustache
|   |           |-- secretariaAcademica_form.mustache
|   |           |-- student_form.mustache
|   |           `-- teacher_from.mustache
|   `-- test
|       `-- java
|           `-- com
|               `-- is1
|                   `-- proyecto
|                       `-- AppTest.java
`-- target
    |-- classes
    |   |-- activejdbc_models.properties
    |   |-- scheme.sql
    |   `-- templates
    |       |-- dashboard.mustache
    |       |-- inscripcion_examenes.mustache
    |       |-- inscripcion_materias.mustache
    |       `-- student_form.mustache
    |-- maven-archiver
    |   `-- pom.properties
    `-- maven-status
        `-- maven-compiler-plugin
            |-- compile
            |   `-- default-compile
            |       |-- createdFiles.lst
            |       `-- inputFiles.lst
            `-- testCompile
                `-- default-testCompile
                    |-- createdFiles.lst
                    `-- inputFiles.lst

```

---

User: admin1
Pass: admin1
