package com.is1.proyecto;

import com.is1.proyecto.controllers.*;
 // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

import static spark.Spark.*; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).

// Importaciones necesarias para la aplicación Spark
import com.fasterxml.jackson.databind.ObjectMapper; // Utilidad para serializar/deserializar objetos Java a/desde JSON.
import com.is1.proyecto.config.DBConfigSingleton; // Clase Singleton para la configuración de la base de datos.
// Importaciones de clases del proyecto
// Importaciones específicas para ActiveJDBC (ORM para la base de datos)
// Importaciones estándar de Java
// Importaciones de Spark para renderizado de plantillas
// Importaciones para ISSUE #28
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;




/**
 * Clase principal de la aplicación Spark.
 * Configura las rutas, filtros y el inicio del servidor web.
 */
public class App {

    // Instancia estática y final de ObjectMapper para la serialización/deserialización JSON.
    // Se inicializa una sola vez para ser reutilizada en toda la aplicación.
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Método principal que se ejecuta al iniciar la aplicación.
     * Aquí se configuran todas las rutas y filtros de Spark.
     */
    public static void main(String[] args) {
        port(8080); // Configura el puerto en el que la aplicación Spark escuchará las peticiones (por defecto es 4567).

        // Obtener la instancia única del singleton de configuración de la base de datos.
        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();


        String STATIC_DIR  = System.getProperty("user.dir") + "/public";
        String UPLOAD_DIR  = STATIC_DIR + "/img/uploads";
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(UPLOAD_DIR));
        } catch (java.io.IOException e) {
            System.err.println("No se pudo crear la carpeta de imágenes: " + e.getMessage());
        }
        staticFiles.externalLocation(STATIC_DIR);

        // --- Filtro 'before' para gestionar la conexión a la base de datos ---
        // Este filtro se ejecuta antes de cada solicitud HTTP.
        before((req, res) -> {
            try {
                dbConfig.openConnection(); // Usamos el método encapsulado del Singleton
            } catch (Exception e) {
                System.err.println(
                    "Error al abrir conexión: " + e.getMessage()
                );
                halt(500, "{\"error\": \"Error interno del servidor DB\"}");
            }
        });

        // --- Filtro 'afterAfter' para cerrar la conexión a la base de datos ---
        // Este filtro se ejecuta después de que cada solicitud HTTP ha sido procesada.
        afterAfter((req, res) -> {
            try {
                dbConfig.closeConnection(); // Usamos el método encapsulado
            } catch (Exception e) {
                System.err.println(
                    "Error al cerrar conexión: " + e.getMessage()
                );
            }
        });


        // --- Rutas GET para renderizar formularios y páginas HTML ---

        // GET: Muestra el formulario de creación de cuenta.
        // Soporta la visualización de mensajes de éxito o error pasados como query parameters.

        // GET: Ruta para mostrar el dashboard (panel de control) del usuario.
        // Requiere que el usuario esté autenticado.
        
        // ── Registro de rutas por dominio ──────────────────────────
        AuthController.register(objectMapper);
        DashboardController.register();
        TeacherController.register();
        ApiController.register(objectMapper);
        StudentController.register();
        AulaVirtualController.register();
        SecretariaController.register();
        CarreraController.register();
        MateriaController.register();
        AnuncioController.register();
        NotaController.register();
        ExamenController.register(objectMapper);
        PerfilController.register();
        ConfiguracionController.register();


        // Actualizar filtro de inscripción a exámenes para excluir PROMOCION y APROBADO
        // (el GET /estudiante/inscripcion/examen ya filtra correctamente: solo REGULAR o LIBRE pasan)
    }
}
