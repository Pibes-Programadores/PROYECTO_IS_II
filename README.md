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

## Solución del problema de conexión a MySQL

Si la aplicación muestra un error de conexión como:

- `Failed to connect to JDBC URL: jdbc:mysql://localhost:3306/proyecto_is_ii...`
- `Access denied for user 'dev'@'localhost'`
- `Unknown database 'proyecto_is_ii'`

seguí estos pasos:

1. Verifiqué que el servidor MySQL de XAMPP estaba activo.
   - En la captura de XAMPP se veía MySQL iniciado en el puerto `3307`.
   - Con PowerShell confirmé que el servicio `mysqld` escuchaba en `3306` y `3307`.

2. Revisé la configuración de la aplicación en `src/main/java/com/is1/proyecto/config/DBConfigSingleton.java`.
   - Allí se define la URL de conexión por defecto.
   - Cambié la URL de `3306` a `3307`:
     ```java
     this.dbUrl = "jdbc:mysql://localhost:3307/proyecto_is_ii?useSSL=false&serverTimezone=UTC";
     ```

3. Probé la conexión directa al servidor MySQL de XAMPP.
   - El puerto `3307` estaba disponible, pero aún faltaba la base de datos.
   - El puerto `3307` respondió correctamente cuando consulté `SHOW DATABASES`.

4. Creé la base de datos `proyecto_is_ii` en el servidor correcto y cargué el esquema.
   - Usé el archivo `src/main/resources/scheme.sql`.
   - También aseguré que el usuario `dev@localhost` existiera con contraseña vacía.

5. Verifiqué que el usuario `dev` pudiera conectarse con la base recién creada:
   - `jdbc:mysql://localhost:3307/proyecto_is_ii`
   - usuario: `dev`
   - contraseña: `""` (vacía)

6. Después de esto, la aplicación ya pudo conectarse correctamente a MySQL.

### Notas extra

- Si querés usar variables de entorno, podés definir:
  - `DB_URL`
  - `DB_USER`
  - `DB_PASS`

- El archivo `DBConfigSingleton.java` también contiene el driver:
  - `com.mysql.cj.jdbc.Driver`

- Para evitar errores de ActiveJDBC, siempre compilá con:
  ```bash
  mvn clean compile process-classes exec:java
  ```

   Una vez iniciada la aplicación, abrí tu navegador e ingresá a:
   ```
   http://localhost:8080
   ```

---

##  Estructura del proyecto

```
Proyecto_IS_II
├── db
│   ├── dev.db
│   └── prod.db
├── pom.xml
├── src
│   └── main
│      ├── java
│      │   └── com
│      │       └── is1
│      │           └── proyecto
│      │               ├── App.java
│      │               ├── config
│      │               │   └── DBConfigSingleton.java
│      │               └── models
│      │                   ├── GestorSistema.java
│      │                   ├── SecretariaAcademica.java
│      │                   ├── Sesion.java
│      │                   ├── Student.java
│      │                   ├── Teacher.java
│      │                   └── User.java
│      └── resources
│          ├── scheme.sql
│          └── templates
│              ├── dashboard.mustache
│              ├── error.mustache
│              ├── hello.mustache
│              ├── login.mustache
│              ├── teacher_from.mustache
│              └── user_form.mustache
│   
└── target
    ├── classes
    │   ├── activejdbc_models.properties
    │   ├── com
    │   │   └── is1
    │   │       └── proyecto
    │   │           ├── App.class
    │   │           ├── config
    │   │           │   └── DBConfigSingleton.class
    │   │           └── models
    │   │               ├── GestorSistema.class
    │   │               ├── SecretariaAcademica.class
    │   │               ├── Sesion.class
    │   │               ├── Student.class
    │   │               ├── Teacher.class
    │   │               └── User.class
    │   ├── scheme.sql
    │   └── templates
    │       ├── dashboard.mustache
    │       ├── error.mustache
    │       ├── hello.mustache
    │       ├── login.mustache
    │       ├── teacher_from.mustache
    │       └── user_form.mustache
    ├── generated-sources
    │   └── annotations
    └── maven-status
        └── maven-compiler-plugin
            └── compile
                └── default-compile
                    ├── createdFiles.lst
                    └── inputFiles.lst
```

---

User: admin1
Pass: admin1
