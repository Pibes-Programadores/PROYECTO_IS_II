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
PROYECTO_IS_II/
├── Proyecto_IS_II/        # Código fuente principal
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/      # Lógica de negocio, rutas y controladores
│   │   │   └── resources/
│   │   │       └── templates/  # Plantillas Mustache (.hbs / .mustache)
│   └── pom.xml            # Configuración de Maven
└── Doc/                   # Documentación del proyecto
```

---

