package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

import static spark.Spark.*; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).

// Importaciones necesarias para la aplicación Spark
import com.fasterxml.jackson.databind.ObjectMapper; // Utilidad para serializar/deserializar objetos Java a/desde JSON.
// Importaciones de clases del proyecto
import com.is1.proyecto.config.DBConfigSingleton; // Clase Singleton para la configuración de la base de datos.
import com.is1.proyecto.models.SecretariaAcademica;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.PlanEstudio;
// Importaciones específicas para ActiveJDBC (ORM para la base de datos)
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.models.User; // Modelo de ActiveJDBC que representa la tabla 'users'.
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
// Importaciones estándar de Java
import java.util.HashMap; // Para crear mapas de datos (modelos para las plantillas).
import java.util.Map; // Interfaz Map, utilizada para Map.of() o HashMap.
import org.javalite.activejdbc.Base; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.
import org.mindrot.jbcrypt.BCrypt; // Utilidad para hashear y verificar contraseñas de forma segura.
// Importaciones de Spark para renderizado de plantillas
import spark.ModelAndView; // Representa un modelo de datos y el nombre de la vista a renderizar.
import spark.template.mustache.MustacheTemplateEngine; // Motor de plantillas Mustache para Spark.

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
        get(
            "/dashboard",
            (req, res) -> {
                Map<String, Object> model = new HashMap<>();

                // 1. Leemos de la sesión usando TUS nombres
                String currentUsername = req
                    .session()
                    .attribute("currentUserUsername");
                String userRole = req.session().attribute("userRole");
                Boolean loggedIn = req.session().attribute("loggedIn");

                // 2. Verificación de seguridad
                if (currentUsername == null || loggedIn == null || !loggedIn) {
                    res.redirect(
                        "/login?error=Debes iniciar sesión para acceder."
                    );
                    return null;
                }

                // 3. Pasamos los datos a la vista (Mustache)
                model.put("username", currentUsername);

                // Creamos "banderas" (true/false) para que el HTML decida qué mostrar
                model.put(
                    "isAdmin",
                    "ADMIN".equals(userRole) || "SECRETARIA".equals(userRole)
                );
                model.put("isDocente", "DOCENTE".equals(userRole));

                return new ModelAndView(model, "dashboard.mustache");
            },
            new MustacheTemplateEngine()
        );

        // GET: Ruta para MOSTRAR el formulario de carga de docente
        get(
            "/teacher/new",
            (req, res) -> {
                // Constructor de model
                Map<String, Object> model = new HashMap<>();

                // REVISAMOS SI HAY MENSAJES EN LA URL
                // Si vienes redirigido de un éxito, la URL será: /teacher/new?message=...
                String successMessage = req.queryParams("message");

                // Si vienes redirigido de un fallo, la URL será: /teacher/new?error=...
                String errorMessage = req.queryParams("error");

                // Si existen, los metemos en la cajita (modelo)
                if (successMessage != null && !successMessage.isEmpty()) {
                    model.put("successMessage", successMessage);
                }

                if (errorMessage != null && !errorMessage.isEmpty()) {
                    model.put("errorMessage", errorMessage);
                }

                // Renderizamos la vista.
                return new ModelAndView(model, "teacher_from.mustache");
            },
            new MustacheTemplateEngine()
        );

        // Importar el modelo Teacher al inicio del archivo si no está:
        // import com.is1.proyecto.models.Teacher;

        // GET: Ruta para cerrar la sesión del usuario.
        get("/logout", (req, res) -> {
            // Invalida completamente la sesión del usuario.
            // Esto elimina todos los atributos guardados en la sesión y la marca como inválida.
            // La cookie JSESSIONID en el navegador también será gestionada para invalidarse.
            req.session().invalidate();

            System.out.println("DEBUG: Sesión cerrada. Redirigiendo a /login.");

            // Redirige al usuario a la página de login con un mensaje de éxito.
            res.redirect("/");

            return null; // Importante retornar null después de una redirección.
        });

        // GET: Muestra el formulario de inicio de sesión (login).
        // Nota: Esta ruta debería ser capaz de leer también mensajes de error/éxito de los query params
        // si se la usa como destino de redirecciones. (Tu código de /user/create ya lo hace, aplicar similar).
        get(
            "/",
            (req, res) -> {
                Map<String, Object> model = new HashMap<>();
                String errorMessage = req.queryParams("error");
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    model.put("errorMessage", errorMessage);
                }
                String successMessage = req.queryParams("message");
                if (successMessage != null && !successMessage.isEmpty()) {
                    model.put("successMessage", successMessage);
                }
                return new ModelAndView(model, "login.mustache");
            },
            new MustacheTemplateEngine()
        ); // Especifica el motor de plantillas para esta ruta.

        // GET: Ruta de alias para el formulario de creación de cuenta.
        // En una aplicación real, probablemente querrías unificar con '/user/create' para evitar duplicidad.
        // POST: Maneja el envío del formulario de creación de nueva cuenta.
        // ==========================================
        // GESTIÓN DE ESTUDIANTES
        // ==========================================
        get(
            "/student/new",
            (req, res) -> {
                String userRole = req.session().attribute("userRole");
                if (
                    userRole == null ||
                    (!userRole.equals("ADMIN") &&
                        !userRole.equals("SECRETARIA"))
                ) {
                    res.redirect("/dashboard");
                    return null;
                }

                Map<String, Object> model = new HashMap<>();
                String successMessage = req.queryParams("message");
                String errorMessage = req.queryParams("error");
                if (successMessage != null) model.put(
                    "successMessage",
                    successMessage
                );
                if (errorMessage != null) model.put(
                    "errorMessage",
                    errorMessage
                );

                return new ModelAndView(model, "student_form.mustache");
            },
            new MustacheTemplateEngine()
        );

        post("/student/new", (req, res) -> {
            String userRole = req.session().attribute("userRole");
            if (
                userRole == null ||
                (!userRole.equals("ADMIN") && !userRole.equals("SECRETARIA"))
            ) {
                res.status(403);
                return "Acceso denegado.";
            }

            // 1. Capturamos todos los datos (incluyendo los específicos de Student)
            String name = req.queryParams("name");
            String lastName = req.queryParams("lastname");
            String dni = req.queryParams("dni");
            String email = req.queryParams("email");
            String legajo = req.queryParams("legajo");
            String tipoEstudiante = req.queryParams("tipo_estudiante");

            if (
                name == null ||
                lastName == null ||
                dni == null ||
                email == null ||
                legajo == null ||
                tipoEstudiante == null
            ) {
                String errorMsg = URLEncoder.encode(
                    "Todos los campos (incluyendo legajo) son obligatorios.",
                    StandardCharsets.UTF_8.toString()
                );
                res.redirect("/student/new?error=" + errorMsg);
                return "";
            }

            try {
                // 2. Guardamos el Usuario base
                User u = new User();
                u.set("nombre", name, "apellido", lastName, "dni", dni);
                u.set("nombre_usuario", email);
                u.set("nivel_acceso", "ESTUDIANTE");
                u.set("password", BCrypt.hashpw("1234", BCrypt.gensalt()));
                u.saveIt();

                // 3. Guardamos el Estudiante hijo
                Student s = new Student();
                s.set("usuario_id", u.getId()); // Fundamental para que la FK funcione
                s.set("legajo", legajo);
                s.set("tipo_estudiante", tipoEstudiante);
                s.saveIt();

                String successMsg = URLEncoder.encode(
                    "Estudiante registrado exitosamente con clave 1234.",
                    StandardCharsets.UTF_8.toString()
                );
                res.redirect("/student/new?message=" + successMsg);
                return "";
            } catch (Exception e) {
                e.printStackTrace();
                res.redirect(
                    "/student/new?error=" +
                        URLEncoder.encode(
                            "Error: El email, DNI o Legajo ya existe.",
                            StandardCharsets.UTF_8.toString()
                        )
                );
                return "";
            }
        });

        // ==========================================
        // GESTIÓN DE SECRETARIOS
        // ==========================================
        get(
            "/secretariaAcademica/new",
            (req, res) -> {
                String userRole = req.session().attribute("userRole");
                if (
                    userRole == null ||
                    (!userRole.equals("ADMIN") &&
                        !userRole.equals("SECRETARIA"))
                ) {
                    res.redirect("/dashboard");
                    return null;
                }

                Map<String, Object> model = new HashMap<>();
                String successMessage = req.queryParams("message");
                String errorMessage = req.queryParams("error");
                if (successMessage != null) model.put(
                    "successMessage",
                    successMessage
                );
                if (errorMessage != null) model.put(
                    "errorMessage",
                    errorMessage
                );

                return new ModelAndView(model, "secretariaAcademica_form.mustache");
            },
            new MustacheTemplateEngine()
        );

        post("/secretariaAcademica/new", (req, res) -> {
            String userRole = req.session().attribute("userRole");
            if (
                userRole == null ||
                (!userRole.equals("ADMIN") && !userRole.equals("SECRETARIA"))
            ) {
                res.status(403);
                return "Acceso denegado.";
            }

            // 1. Capturamos los datos
            String name = req.queryParams("name");
            String lastName = req.queryParams("lastname");
            String dni = req.queryParams("dni");
            String email = req.queryParams("email");
            String oficina = req.queryParams("oficina");
            String interno = req.queryParams("interno");

            if (
                name == null || lastName == null || dni == null || email == null
            ) {
                String errorMsg = URLEncoder.encode(
                    "Nombre, apellido, DNI y email son obligatorios.",
                    StandardCharsets.UTF_8.toString()
                );
                res.redirect("/secretariaAcademica/new?error=" + errorMsg);
                return "";
            }

            try {
                // 2. Guardamos el Usuario base
                User u = new User();
                u.set("nombre", name, "apellido", lastName, "dni", dni);
                u.set("nombre_usuario", email);
                u.set("nivel_acceso", "SECRETARIA");
                u.set("password", BCrypt.hashpw("1234", BCrypt.gensalt()));
                u.saveIt();

                // 3. Guardamos el Secretario hijo
                SecretariaAcademica sec = new SecretariaAcademica();
                sec.set("usuario_id", u.getId());
                sec.set("oficina", oficina); // Puede ser null si no lo completan
                sec.set("interno", interno); // Puede ser null si no lo completan
                sec.saveIt();

                String successMsg = URLEncoder.encode(
                    "Secretaría registrada exitosamente con clave 1234.",
                    StandardCharsets.UTF_8.toString()
                );
                res.redirect("/secretariaAcademica/new?message=" + successMsg);
                return "";
            } catch (Exception e) {
                e.printStackTrace();
                res.redirect(
                    "/secretariaAcademica/new?error=" +
                        URLEncoder.encode(
                            "Error: El email o DNI ya existe.",
                            StandardCharsets.UTF_8.toString()
                        )
                );
                return "";
            }
        });

        // ==========================================
        // GESTIÓN DE CARRERAS Y PLANES DE ESTUDIO
        // ==========================================
        get("/carrera/new", (req, res) -> {
            String userRole = req.session().attribute("userRole");
            if (userRole == null || (!userRole.equals("ADMIN") && !userRole.equals("SECRETARIA"))) {
                res.redirect("/dashboard");
                return null;
            }
            Map<String, Object> model = new HashMap<>();
            String successMessage = req.queryParams("message");
            String errorMessage = req.queryParams("error");
            if (successMessage != null) model.put("successMessage", successMessage);
            if (errorMessage != null) model.put("errorMessage", errorMessage);
            
            return new ModelAndView(model, "carrera_form.mustache");
        }, new MustacheTemplateEngine());

        post("/carrera/new", (req, res) -> {
            String userRole = req.session().attribute("userRole");
            if (userRole == null || (!userRole.equals("ADMIN") && !userRole.equals("SECRETARIA"))) {
                res.status(403);
                return "Acceso denegado.";
            }

            // Captura de datos del frontend
            String nombre = req.queryParams("nombre");
            String duracionAnios = req.queryParams("duracion_anios");
            String tituloOtorgado = req.queryParams("titulo_otorgado");
            String anioResolucion = req.queryParams("anio_resolucion");
            String estado = req.queryParams("estado");

            // Validación rigurosa de nulidad y vacíos
            if (nombre == null || duracionAnios == null || tituloOtorgado == null || anioResolucion == null || estado == null ||
                nombre.isBlank() || duracionAnios.isBlank() || tituloOtorgado.isBlank() || anioResolucion.isBlank() || estado.isBlank()) {
                String errorMsg = URLEncoder.encode("Todos los campos obligatorios deben completarse.", StandardCharsets.UTF_8.toString());
                res.redirect("/carrera/new?error=" + errorMsg);
                return "";
            }

            try {
                // Abrimos transacción para asegurar que no se cree una Carrera sin su Plan correspondiente
                Base.openTransaction();

                // Instanciar y persistir la Carrera
                Carrera carrera = new Carrera();
                carrera.set("nombre", nombre);
                carrera.set("duracion_anios", Integer.parseInt(duracionAnios));
                carrera.set("titulo_otorgado", tituloOtorgado);
                carrera.saveIt();

                // Instanciar y persistir el Plan_Estudio usando el ID generado automáticamente
                PlanEstudio plan = new PlanEstudio();
                plan.set("carrera_id", carrera.getId());
                plan.set("anio_resolucion", Integer.parseInt(anioResolucion));
                plan.set("estado", estado);
                plan.saveIt();

                // Confirmamos los cambios en la BD
                Base.commitTransaction();

                String successMsg = URLEncoder.encode("Carrera '" + nombre + "' y Plan inicial registrados con éxito.", StandardCharsets.UTF_8.toString());
                res.redirect("/carrera/new?message=" + successMsg);
                return "";

            } catch (Exception e) {
                // Si algo falla (ej: violación de restricción UNIQUE en el nombre de carrera), cancelamos todo
                Base.rollbackTransaction();
                e.printStackTrace();
                String errorMsg = URLEncoder.encode("Error al procesar el alta. Verifique si el nombre de la carrera ya existe.", StandardCharsets.UTF_8.toString());
                res.redirect("/carrera/new?error=" + errorMsg);
                return "";
            }
        });

        /*
        // POST: Creación de Docente (Relación con la tabla 'users')
        post("/teacher/new", (req, res) -> {
            // 1. Capturamos los datos que vienen del formulario
            String name = req.queryParams("teacher_name");
            String lastName = req.queryParams("teacher_lastname");
            String dniStr = req.queryParams("teacher_dni");
            String titulo = req.queryParams("titulo");

            if (name == null || lastName == null || dniStr == null) {
                res.redirect("/teacher/new?error=Los campos Nombre, Apellido y DNI son obligatorios.");
                return "";
            }

            try {
             // 3. CREAMOS EL USUARIO BASE
                User u = new User();
                u.set("nombre", name);
                u.set("apellido", lastName);
                u.set("dni", dniStr);
                u.set("email", name.toLowerCase() + "." + lastName.toLowerCase() + "@unrc.edu.ar");
                u.set("nivel_acceso", "DOCENTE");
                u.set("password", "1234");
                u.saveIt();

                // 4. CREAMOS EL DOCENTE
                Teacher t = new Teacher();
                t.set("usuario_id", u.getId());
                t.set("cuil", "20-" + dniStr + "-9");

                // Si el formulario HTML no tiene input para "titulo", ponemos uno por defecto
                t.set("titulo", (titulo != null && !titulo.isEmpty()) ? titulo : "Docente");
                t.saveIt();

                res.redirect("/teacher/new?message=Profesor " + name + " " + lastName + " registrado correctamente.");
                return "";

            } catch (Exception e) {
                System.err.println("Error al crear docente: " + e.getMessage());
                e.printStackTrace();
                res.redirect("/teacher/new?error=Error interno al guardar el profesor.");
                return "";
            }
        });
        */

        post("/teacher/new", (req, res) -> {
            String name = req.queryParams("teacher_name");
            String lastName = req.queryParams("teacher_lastname");
            String dni = req.queryParams("teacher_dni");
            String address = req.queryParams("teacher_address");
            String phone = req.queryParams("teacher_phone");
            String legajo = req.queryParams("legajo_docente");
            String cuil = req.queryParams("teacher_cuil");
            String email = req.queryParams("teacher_email");
            String especialidad = req.queryParams("especialidad");

            if (name == null || lastName == null || dni == null) {
                res.redirect(
                    "/teacher/new?error=Los campos Nombre, Apellido y DNI son obligatorios."
                );
                return "";
            }

            try {
                User u = new User();
                u.set("nombre", name, "apellido", lastName, "dni", dni);
                u.set("nombre_usuario", email);
                u.set("nivel_acceso", "DOCENTE");

                // ENCRIPTAMOS LA CLAVE POR DEFECTO
                String defaultPassHashed = BCrypt.hashpw(
                    "1234",
                    BCrypt.gensalt()
                );
                u.set("password", defaultPassHashed);

                u.saveIt();

                Teacher t = new Teacher();
                t.set("usuario_id", u.getId());
                t.set("legajo_docente", legajo, "cuil", cuil);
                t.set("email", email, "especialidad", especialidad);
                t.saveIt();

                // Cuando la creacion es exitosa
                String mensajeExito =
                    "Docente " + name + " registrado con éxito.";
                String urlCodificada = URLEncoder.encode(
                    mensajeExito,
                    StandardCharsets.UTF_8.toString()
                );
                res.redirect("/teacher/new?message=" + urlCodificada);
                return "";
            } catch (Exception e) {
                e.printStackTrace();
                String mensajeError =
                    "Error interno: Verifique que el legajo, CUIL o Email no estén duplicados.";
                String errorCodificado = URLEncoder.encode(
                    mensajeError,
                    StandardCharsets.UTF_8.toString()
                );
                res.redirect("/teacher/new?error=" + errorCodificado);
                return "";
            }
        });

        // POST: Maneja el envío del formulario de inicio de sesión.
        post(
            "/login",
            (req, res) -> {
                Map<String, Object> model = new HashMap<>();
                String username = req.queryParams("username");
                String plainTextPassword = req.queryParams("password");

                // 1. Validaciones de entrada
                if (
                    username == null ||
                    username.isEmpty() ||
                    plainTextPassword == null ||
                    plainTextPassword.isEmpty()
                ) {
                    res.status(400);
                    model.put(
                        "errorMessage",
                        "El nombre de usuario y la contraseña son requeridos."
                    );
                    return new ModelAndView(model, "login.mustache");
                }

                // 2. Búsqueda en DB
                User ac = User.findFirst("nombre_usuario = ?", username);

                // 3. Verificación de usuario y contraseña (BCrypt)
                if (
                    ac != null &&
                    BCrypt.checkpw(plainTextPassword, ac.getString("password"))
                ) {
                    // --- Gestión de Sesión ---
                    req.session(true).attribute(
                        "currentUserUsername",
                        username
                    );
                    req.session().attribute("userId", ac.getId());
                    req.session().attribute("loggedIn", true);

                    // Guardamos el rol para los permisos que definimos en el Dashboard
                    req.session().attribute("userRole", ac.get("nivel_acceso"));

                    System.out.println(
                        "DEBUG: Login exitoso para: " + username
                    );

                    // PATRÓN PRG: Redirigimos al dashboard (GET) en lugar de renderizarlo aquí
                    res.redirect("/dashboard");
                    return null;
                } else {
                    // Fallo de autenticación
                    res.status(401);
                    System.out.println(
                        "DEBUG: Intento de login fallido para: " + username
                    );
                    model.put(
                        "errorMessage",
                        "Usuario o contraseña incorrectos."
                    );
                    return new ModelAndView(model, "login.mustache");
                }
            },
            new MustacheTemplateEngine()
        );

        // POST: Endpoint para añadir usuarios (API que devuelve JSON, no HTML).
        // Advertencia: Esta ruta tiene un propósito diferente a las de formulario HTML.
        post("/add_users", (req, res) -> {
            res.type("application/json"); // Establece el tipo de contenido de la respuesta a JSON.

            // Obtiene los parámetros 'name' y 'password' de la solicitud.
            String name = req.queryParams("name");
            String password = req.queryParams("password");

            // --- Validaciones básicas ---
            if (
                name == null ||
                name.isEmpty() ||
                password == null ||
                password.isEmpty()
            ) {
                res.status(400); // Bad Request.
                return objectMapper.writeValueAsString(
                    Map.of("error", "Nombre y contraseña son requeridos.")
                );
            }

            try {
                // --- Creación y guardado del usuario usando el modelo ActiveJDBC ---
                User newUser = new User(); // Crea una nueva instancia de tu modelo User.
                // ¡ADVERTENCIA DE SEGURIDAD CRÍTICA!
                // En una aplicación real, las contraseñas DEBEN ser hasheadas (ej. con BCrypt)
                // ANTES de guardarse en la base de datos, NUNCA en texto plano.
                // (Nota: El código original tenía la contraseña en texto plano aquí.
                // Se recomienda usar `BCrypt.hashpw(password, BCrypt.gensalt())` como en la ruta '/user/new').
                newUser.set("nombre", name); // Asigna el nombre al campo 'name'.
                newUser.set("password", password); // Asigna la contraseña al campo 'password'.
                newUser.saveIt(); // Guarda el nuevo usuario en la tabla 'users'.

                res.status(201); // Created.
                // Devuelve una respuesta JSON con el mensaje y el ID del nuevo usuario.
                return objectMapper.writeValueAsString(
                    Map.of(
                        "message",
                        "Usuario '" + name + "' registrado con éxito.",
                        "id",
                        newUser.getId()
                    )
                );
            } catch (Exception e) {
                // Si ocurre cualquier error durante la operación de DB, se captura aquí.
                System.err.println(
                    "Error al registrar usuario: " + e.getMessage()
                );
                e.printStackTrace(); // Imprime el stack trace para depuración.
                res.status(500); // Internal Server Error.
                return objectMapper.writeValueAsString(
                    Map.of(
                        "error",
                        "Error interno al registrar usuario: " + e.getMessage()
                    )
                );
            }
        });
    } // Fin del método main
} // Fin de la clase App
