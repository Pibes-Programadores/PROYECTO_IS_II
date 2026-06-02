package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

import static spark.Spark.*; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).

// Importaciones necesarias para la aplicación Spark
import com.fasterxml.jackson.databind.ObjectMapper; // Utilidad para serializar/deserializar objetos Java a/desde JSON.
// Importaciones de clases del proyecto
import com.is1.proyecto.models.Anuncio;
import com.is1.proyecto.models.Nota;
import com.is1.proyecto.models.AulaAsignacion;
import com.is1.proyecto.config.DBConfigSingleton; // Clase Singleton para la configuración de la base de datos.
import com.is1.proyecto.models.SecretariaAcademica;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.PlanEstudio;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.MateriaPeriodo;
import com.is1.proyecto.models.Correlatividad;
import com.is1.proyecto.models.DocenteMateria;
import java.util.ArrayList;
import java.util.List;

// Importaciones específicas para ActiveJDBC (ORM para la base de datos)
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.models.User; // Modelo de ActiveJDBC que representa la tabla 'users'.
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.is1.proyecto.models.DocenteCarrera;
// Importaciones estándar de Java
import java.util.HashMap; // Para crear mapas de datos (modelos para las plantillas).
import java.util.Map; // Interfaz Map, utilizada para Map.of() o HashMap.

import com.mysql.cj.exceptions.StreamingNotifiable;
import org.javalite.activejdbc.Base; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.
import org.javalite.activejdbc.Model;
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

                String errorMessage = req.queryParams("error");
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    model.put("errorMessage", errorMessage);
                }
                String successMessage = req.queryParams("message");
                if (successMessage != null && !successMessage.isEmpty()) {
                    model.put("successMessage", successMessage);
                }

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
        // modifiqué para que ande bien lo de asignar docentes
        get(
                "/teacher/new",
                (req, res) -> {
                    Map<String, Object> model = new HashMap<>();

                    String successMessage = req.queryParams("message");
                    String errorMessage = req.queryParams("error");

                    if (successMessage != null && !successMessage.isEmpty()) {
                        model.put("successMessage", successMessage);
                    }
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        model.put("errorMessage", errorMessage);
                    }

                    List<Map> carreras = Base.findAll("SELECT id, nombre FROM Carrera ORDER BY nombre ASC");
                    model.put("carreras", carreras);

                    return new ModelAndView(model, "teacher_from.mustache");
                },
                new MustacheTemplateEngine()
        );

        get(
                "/teacher/assign-materia",
                (req, res) -> {
                    String userRole = req.session().attribute("userRole");
                    if (userRole == null || (!userRole.equals("SECRETARIA") && !userRole.equals("ADMIN"))) {
                        String errorMessage = URLEncoder.encode(
                                "Acceso denegado. Solo SECRETARIA puede asignar materias.",
                                StandardCharsets.UTF_8.toString()
                        );
                        res.redirect("/dashboard?error=" + errorMessage);
                        return null;
                    }

                    Map<String, Object> model = new HashMap<>();

                    String successMessage = req.queryParams("message");
                    String errorMessage   = req.queryParams("error");
                    if (successMessage != null && !successMessage.isEmpty()) {
                        model.put("successMessage", successMessage);
                    }
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        model.put("errorMessage", errorMessage);
                    }

                    // Solo enviamos los Planes de Estudio vigentes.
                    List<Map> planesVigentes = Base.findAll(
                            "SELECT p.id AS id, c.id AS carrera_id, " +
                                    "CONCAT(c.nombre, ' — Plan ', p.anio_resolucion) AS descripcion " +
                                    "FROM Plan_Estudio p " +
                                    "JOIN Carrera c ON p.carrera_id = c.id " +
                                    "WHERE p.estado = 'VIGENTE' " +
                                    "ORDER BY c.nombre ASC, p.anio_resolucion DESC"
                    );

                    // ya no docentes ni materias.
                    model.put("planes", planesVigentes);

                    return new ModelAndView(model, "assign_materia_form.mustache");
                },
                new MustacheTemplateEngine()
        );

        // GET /api/materias-por-plan?plan_id=X
        // Devuelve en JSON las materias pertenecientes al plan indicado.
        get("/api/materias-por-plan", (req, res) -> {
            res.type("application/json");

            String planIdParam = req.queryParams("plan_id");
            if (planIdParam == null || planIdParam.isBlank()) {
                res.status(400);
                return objectMapper.writeValueAsString(
                        Map.of("error", "Se requiere el parámetro plan_id.")
                );
            }

            try {
                int planId = Integer.parseInt(planIdParam.trim());

                List<Map> materias = Base.findAll(
                        "SELECT codigo AS id, nombre " +
                                "FROM Materia " +
                                "WHERE plan_estudio_id = ? " +
                                "ORDER BY anio_cursada ASC, nombre ASC",
                        planId
                );

                // Convertimos a List<Map<String,Object>> para serialización limpia con Jackson
                List<Map<String, Object>> resultado = new ArrayList<>();
                for (Map m : materias) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id",     m.get("id"));
                    item.put("nombre", m.get("nombre"));
                    resultado.add(item);
                }

                return objectMapper.writeValueAsString(resultado);

            } catch (NumberFormatException e) {
                res.status(400);
                return objectMapper.writeValueAsString(
                        Map.of("error", "plan_id debe ser un número entero.")
                );
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return objectMapper.writeValueAsString(
                        Map.of("error", "Error interno al obtener materias.")
                );
            }
        });

        // GET /api/docentes-search?carrera_id=X&query=Y
        // Busca docentes asociados a la carrera indicada cuyo nombre, apellido
        // o legajo contengan la cadena `query`. Devuelve JSON.
        get("/api/docentes-search", (req, res) -> {
            res.type("application/json");

            String carreraIdParam = req.queryParams("carrera_id");
            String query          = req.queryParams("query");

            if (carreraIdParam == null || carreraIdParam.isBlank()) {
                res.status(400);
                return objectMapper.writeValueAsString(
                        Map.of("error", "Se requiere el parámetro carrera_id.")
                );
            }
            if (query == null) query = "";
            String like = "%" + query.trim() + "%";

            try {
                int carreraId = Integer.parseInt(carreraIdParam.trim());

                // Buscamos docentes vinculados a la carrera cuyo nombre, apellido
                // o legajo coincidan con el término de búsqueda (LIKE, case-insensitive).
                List<Map> rows = Base.findAll(
                        "SELECT t.usuario_id AS id, " +
                                "       t.legajo_docente AS legajo, " +
                                "       u.nombre, " +
                                "       u.apellido " +
                                "FROM teacher t " +
                                "JOIN users u           ON u.id = t.usuario_id " +
                                "JOIN Docente_Carrera dc ON dc.teacher_id = t.usuario_id " +
                                "WHERE dc.carrera_id = ? " +
                                "  AND (u.nombre LIKE ? OR u.apellido LIKE ? OR t.legajo_docente LIKE ?) " +
                                "ORDER BY u.apellido ASC, u.nombre ASC " +
                                "LIMIT 20",
                        carreraId, like, like, like
                );

                List<Map<String, Object>> resultado = new ArrayList<>();
                for (Map row : rows) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id",     row.get("id"));
                    // Formato: LEGAJO - APELLIDO, Nombre
                    String label = row.get("legajo") + " — " +
                            row.get("apellido") + ", " +
                            row.get("nombre");
                    item.put("label", label);
                    resultado.add(item);
                }

                return objectMapper.writeValueAsString(resultado);

            } catch (NumberFormatException e) {
                res.status(400);
                return objectMapper.writeValueAsString(
                        Map.of("error", "carrera_id debe ser un número entero.")
                );
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return objectMapper.writeValueAsString(
                        Map.of("error", "Error interno en la búsqueda de docentes.")
                );
            }
        });










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

        // ==========================================
        // GESTIÓN DE MATERIAS Y CORRELATIVIDADES
        // ==========================================
        get("/materia/new", (req, res) -> {
            String userRole = req.session().attribute("userRole");
            if (userRole == null || (!userRole.equals("ADMIN") && !userRole.equals("SECRETARIA"))) {
                res.redirect("/dashboard");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            
            // Consulta SQL limpia para armar el selector dinámico de Carreras con sus respectivos Planes Vigentes
            List<Map> planesDropdown = Base.findAll(
                "SELECT p.id as id, CONCAT(c.nombre, ' (Plan Resol: ', p.anio_resolucion, ')') as descripcion " +
                "FROM Plan_Estudio p JOIN Carrera c ON p.carrera_id = c.id WHERE p.estado = 'VIGENTE' ORDER BY c.nombre ASC"
            );
            
            // Traemos las materias cargadas para poblar el mapa de correlatividades recursivo
            List<Map> materiasExistentes = Base.findAll("SELECT codigo, nombre FROM Materia ORDER BY codigo ASC");

            model.put("planes", planesDropdown);
            model.put("materiasExistentes", materiasExistentes);

            String successMessage = req.queryParams("message");
            String errorMessage = req.queryParams("error");
            if (successMessage != null) model.put("successMessage", successMessage);
            if (errorMessage != null) model.put("errorMessage", errorMessage);
            
            return new ModelAndView(model, "materia_form.mustache");
        }, new MustacheTemplateEngine());

        post("/materia/new", (req, res) -> {
            String userRole = req.session().attribute("userRole");
            if (userRole == null || (!userRole.equals("ADMIN") && !userRole.equals("SECRETARIA"))) {
                res.status(403);
                return "Acceso denegado.";
            }

            // Captura de parámetros
            String codigoStr = req.queryParams("codigo"); // Viene como texto desde el HTML
            String planEstudioId = req.queryParams("plan_estudio_id");
            String nombre = req.queryParams("nombre");
            String anioCursada = req.queryParams("anio_cursada");
            String cargaHorariaTotal = req.queryParams("carga_horaria_total");
            String tipoCuatrimestre = req.queryParams("tipo_cuatrimestre");
            
            
            String sentidoCorrelatividad = req.queryParams("sentido_correlatividad");
            String condicion = req.queryParams("condicion");

            if (codigoStr == null || planEstudioId == null || nombre == null || anioCursada == null || tipoCuatrimestre == null ||
                codigoStr.isBlank() || planEstudioId.isBlank() || nombre.isBlank() || anioCursada.isBlank()) {
                String errorMsg = URLEncoder.encode("Error: Complete todos los campos obligatorios.", StandardCharsets.UTF_8.toString());
                res.redirect("/materia/new?error=" + errorMsg);
                return "";
            }

            try {
                // Parseamos los números clave
                int codigoNumerico = Integer.parseInt(codigoStr.trim());

                if (codigoNumerico < 0) {
                    throw new IllegalArgumentException("El código de la materia no puede ser un número negativo.");
                }

                int anioPropuestoMateria = Integer.parseInt(anioCursada.trim());
                
                Base.openTransaction();

                // Validaciones de regla de negocio
                PlanEstudio plan = PlanEstudio.findById(planEstudioId);
                if (plan == null) throw new IllegalArgumentException("El Plan de Estudio seleccionado no existe.");
                
                Carrera carrera = Carrera.findById(plan.get("carrera_id"));
                int maxAniosCarrera = carrera.getInteger("duracion_anios");

                if (anioPropuestoMateria > maxAniosCarrera) {
                    throw new IllegalArgumentException("No se puede asignar a " + anioPropuestoMateria + "° año. La carrera '" + carrera.get("nombre") + "' dura " + maxAniosCarrera + " años.");
                }

                // Tarea A: Guardar Materia con PK numérica
                Materia m = new Materia();
                m.set("codigo", codigoNumerico); // Se inserta como Integer
                m.set("plan_estudio_id", Integer.parseInt(planEstudioId));
                m.set("nombre", nombre);
                m.set("anio_cursada", anioPropuestoMateria);
                if (cargaHorariaTotal != null && !cargaHorariaTotal.isBlank()) {
                    m.set("carga_horaria_total", Integer.parseInt(cargaHorariaTotal));
                }
                m.saveIt();

                // Tarea B: Guardar Periodo
                int anioActualDinamico = java.time.Year.now().getValue();
                MateriaPeriodo mp = new MateriaPeriodo();
                mp.set("materia_codigo", codigoNumerico); // Relación numérica
                mp.set("anio_academico", anioActualDinamico);
                mp.set("tipo_cuatrimestre", tipoCuatrimestre);
                mp.saveIt();

                // Tarea C: Correlatividad
                // 1. En lugar de queryParams (String), usamos queryParamsValues (Array de Strings)
                String[] correlativas = req.queryParamsValues("materia_correlativa_codigo");
                String[] tiposRequisito = req.queryParamsValues("tipo_requisito");
                String[] condiciones = req.queryParamsValues("condicion");
                String[] sentidos = req.queryParamsValues("sentido_correlatividad");

                // 2. Si llegaron datos, los recorremos uno por uno con un bucle FOR
                if (correlativas != null) {
                    for (int i = 0; i < correlativas.length; i++) {
                        
                        // Si en esta fila dejaron "-- Ninguna --", la saltamos y seguimos con la siguiente
                        if (correlativas[i].equals("none")) continue; 

                        // Extraemos los datos específicos de la fila actual del bucle
                        int seleccionadaCodigo = Integer.parseInt(correlativas[i].trim());
                        String tipoReq = tiposRequisito[i];
                        String condicionActual = condiciones[i];
                        String sentidoCorr = sentidos[i];

                        if (codigoNumerico == seleccionadaCodigo) {
                            throw new IllegalArgumentException("Una asignatura no puede ser correlativa de sí misma.");
                        }

                        Materia materiaExistente = Materia.findFirst("codigo = ?", seleccionadaCodigo);
                        MateriaPeriodo periodoExistente = MateriaPeriodo.findFirst("materia_codigo = ?", seleccionadaCodigo);

                        if (materiaExistente == null || periodoExistente == null) {
                            throw new IllegalArgumentException("La materia correlativa " + seleccionadaCodigo + " no es válida.");
                        }

                        int anioExistente = materiaExistente.getInteger("anio_cursada");
                        String cuatExistente = periodoExistente.getString("tipo_cuatrimestre");

                        int anioRequisito, anioObjetivo;
                        String cuatRequisito, cuatObjetivo;

                        if ("REQUIERE".equals(sentidoCorr)) {
                            anioRequisito = anioExistente;
                            cuatRequisito = cuatExistente;
                            anioObjetivo = anioPropuestoMateria;
                            cuatObjetivo = tipoCuatrimestre;
                        } else {
                            anioRequisito = anioPropuestoMateria;
                            cuatRequisito = tipoCuatrimestre;
                            anioObjetivo = anioExistente;
                            cuatObjetivo = cuatExistente;
                        }

                        java.util.function.Function<String, Integer> pesoCuatrimestre = (c) -> {
                            switch (c) {
                                case "PRIMER_CUATRIMESTRE": return 1;
                                case "ANUAL": return 1;
                                case "SEGUNDO_CUATRIMESTRE": return 2;
                                case "VERANO": return 3;
                                default: return 0;
                            }
                        };

                        int pesoReq = pesoCuatrimestre.apply(cuatRequisito);
                        int pesoObj = pesoCuatrimestre.apply(cuatObjetivo);

                        if (anioRequisito > anioObjetivo) {
                            throw new IllegalArgumentException("Inconsistencia Temporal: El requisito pertenece a un año superior.");
                        } else if (anioRequisito == anioObjetivo && pesoReq >= pesoObj) {
                            throw new IllegalArgumentException("Inconsistencia Temporal: El requisito se dicta en paralelo o posterior en el mismo año.");
                        }

                        // 3. Insertamos en la Base de Datos con los 4 parámetros (incluyendo tipoReq)
                        if ("REQUIERE".equals(sentidoCorr)) {
                            Base.exec(
                                "INSERT INTO Correlatividad (materia_codigo, materia_correlativa_codigo, condicion, tipo_requisito) VALUES (?, ?, ?, ?)",
                                codigoNumerico, seleccionadaCodigo, condicionActual, tipoReq
                            );
                        } else if ("ES_REQUISITO".equals(sentidoCorr)) {
                            Base.exec(
                                "INSERT INTO Correlatividad (materia_codigo, materia_correlativa_codigo, condicion, tipo_requisito) VALUES (?, ?, ?, ?)",
                                seleccionadaCodigo, codigoNumerico, condicionActual, tipoReq
                            );
                        }
                    } 
                }

                Base.commitTransaction();
                String successMsg = URLEncoder.encode("Materia [" + codigoNumerico + "] registrada con éxito.", StandardCharsets.UTF_8.toString());
                res.redirect("/materia/new?message=" + successMsg);
                return "";

            } catch (Exception e) {
                Base.rollbackTransaction();
                e.printStackTrace();
                String errorMsg = URLEncoder.encode("Error: " + e.getMessage(), StandardCharsets.UTF_8.toString());
                res.redirect("/materia/new?error=" + errorMsg);
                return "";
            }
        });

        // ==========================================
        // VISTA DE PLAN DE ESTUDIOS (GRILLA)
        // ==========================================
        get("/carrera/materias", (req, res) -> {
            if (req.session().attribute("userRole") == null) {
                res.redirect("/login");
                return null;
            }

            Map<String, Object> model = new HashMap<>();

            // 1. Cargamos el selector de planes vigentes
            List<Map> planesDropdown = Base.findAll(
                "SELECT p.id as id, CONCAT(c.nombre, ' (Plan Resol: ', p.anio_resolucion, ')') as descripcion " +
                "FROM Plan_Estudio p JOIN Carrera c ON p.carrera_id = c.id WHERE p.estado = 'VIGENTE' ORDER BY c.nombre ASC"
            );
            model.put("planes", planesDropdown);

            // 2. Si se mandó un plan por la URL, buscamos sus materias asignadas
            String planIdParam = req.queryParams("plan_id");
            if (planIdParam != null && !planIdParam.isBlank()) {
                int planId = Integer.parseInt(planIdParam.trim());

                // Buscamos las materias asociadas ordenadas cronológicamente por año y cuatrimestre
                List<Map> materiasRaw = Base.findAll(
                    "SELECT m.codigo, m.nombre, m.anio_cursada, mp.tipo_cuatrimestre " +
                    "FROM Materia m " +
                    "JOIN Materia_Periodo mp ON m.codigo = mp.materia_codigo " +
                    "WHERE m.plan_estudio_id = ? " +
                    "ORDER BY m.anio_cursada ASC, " +
                    "CASE mp.tipo_cuatrimestre " +
                    "  WHEN 'PRIMER_CUATRIMESTRE' THEN 1 " +
                    "  WHEN 'ANUAL' THEN 2 " +
                    "  WHEN 'SEGUNDO_CUATRIMESTRE' THEN 3 " +
                    "  WHEN 'VERANO' THEN 4 " +
                    "  ELSE 5 " +
                    "END ASC, m.nombre ASC",
                    planId
                );

                List<Map<String, Object>> materiasProcesadas = new java.util.ArrayList<>();

                for (Map mat : materiasRaw) {
                    Map<String, Object> mDto = new HashMap<>(mat);
                    int mCodigo = (int) mat.get("codigo");

                    // Formateamos visualmente el año y cuatrimestre para la primera columna
                    String cuatRaw = (String) mat.get("tipo_cuatrimestre");
                    String cuatVista = cuatRaw;
                    if ("PRIMER_CUATRIMESTRE".equals(cuatRaw)) cuatVista = "I Cuat.";
                    else if ("SEGUNDO_CUATRIMESTRE".equals(cuatRaw)) cuatVista = "II Cuat.";
                    else if ("ANUAL".equals(cuatRaw)) cuatVista = "Anual";
                    else if ("VERANO".equals(cuatRaw)) cuatVista = "Verano";

                    mDto.put("periodo_vista", mat.get("anio_cursada") + "° Año - " + cuatVista);

                    // Buscamos las correlatividades de esta asignatura concreta (AHORA INCLUYE TIPO REQUISITO)
                    List<Map> corrs = Base.findAll(
                        "SELECT materia_correlativa_codigo, condicion, tipo_requisito FROM Correlatividad WHERE materia_codigo = ?",
                        mCodigo
                    );

                    StringBuilder aprobadasC = new StringBuilder(); // Para CURSAR (Aprobada)
                    StringBuilder regularesC = new StringBuilder(); // Para CURSAR (Regular)
                    StringBuilder aprobadasR = new StringBuilder(); // Para RENDIR (Aprobada)

                    for (Map c : corrs) {
                        int corrCod = (int) c.get("materia_correlativa_codigo");
                        String cond = (String) c.get("condicion");
                        String tipoReq = (String) c.get("tipo_requisito"); // Capturamos la nueva columna

                        if ("RENDIR".equals(tipoReq)) {
                            if (aprobadasR.length() > 0) aprobadasR.append(", ");
                            aprobadasR.append(corrCod);
                        } else {
                            // Si es para CURSAR, evaluamos la condición
                            if ("APROBADA".equals(cond)) {
                                if (aprobadasC.length() > 0) aprobadasC.append(", ");
                                aprobadasC.append(corrCod);
                            } else {
                                if (regularesC.length() > 0) regularesC.append(", ");
                                regularesC.append(corrCod);
                            }
                        }
                    }

                    // Guardamos las 3 variables separadas para que Mustache arme las columnas
                    mDto.put("correlativas_aprobadas", aprobadasC.length() > 0 ? aprobadasC.toString() : "-");
                    mDto.put("correlativas_regulares", regularesC.length() > 0 ? regularesC.toString() : "-");
                    mDto.put("correlativas_rendir", aprobadasR.length() > 0 ? aprobadasR.toString() : "-");

                    materiasProcesadas.add(mDto);
                }

                model.put("materias", materiasProcesadas);
                model.put("mostrarTabla", !materiasProcesadas.isEmpty());
                model.put("sinMaterias", materiasProcesadas.isEmpty());
            }

            return new ModelAndView(model, "materias_list.mustache");
        }, new MustacheTemplateEngine());

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
            String carreraId = req.queryParams("carrera_id");

            if (name == null || lastName == null || dni == null || carreraId == null || carreraId.isBlank()) {
                res.redirect("/teacher/new?error=Los campos Nombre, Apellido, DNI y Carrera son obligatorios.");
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

                DocenteCarrera dc = new DocenteCarrera();
                dc.set("teacher_id", u.getId());
                dc.set("carrera_id", Integer.parseInt(carreraId));
                dc.saveIt();

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

        post("/teacher/assign-materia", (req, res) -> {
            String userRole = req.session().attribute("userRole");
            if (userRole == null || (!userRole.equals("SECRETARIA") && !userRole.equals("ADMIN"))) {
                String errorMessage = URLEncoder.encode(
                    "Acceso denegado. Solo SECRETARIA puede asignar materias.",
                    StandardCharsets.UTF_8.toString()
                );
                res.redirect("/dashboard?error=" + errorMessage);
                return "";
            }

            String teacherIdParam = req.queryParams("teacher_id");
            String materiaIdParam = req.queryParams("materia_id");

            if (
                teacherIdParam == null ||
                teacherIdParam.isEmpty() ||
                materiaIdParam == null ||
                materiaIdParam.isEmpty()
            ) {
                String errorMessage = URLEncoder.encode(
                    "Debes seleccionar un docente y una materia.",
                    StandardCharsets.UTF_8.toString()
                );
                res.redirect("/teacher/assign-materia?error=" + errorMessage);
                return "";
            }

            try {
                int teacherId = Integer.parseInt(teacherIdParam);
                int materiaId = Integer.parseInt(materiaIdParam);

                Teacher teacher = (Teacher) Teacher.findFirst("usuario_id = ?", teacherId);
                Materia materia = (Materia) Materia.findFirst("codigo = ?", materiaId);

                if (teacher == null || materia == null) {
                    String errorMessage = URLEncoder.encode(
                        "Docente o materia no válida.",
                        StandardCharsets.UTF_8.toString()
                    );
                    res.redirect("/teacher/assign-materia?error=" + errorMessage);
                    return "";
                }

                DocenteMateria existing = DocenteMateria.findFirst(
                    "teacher_id = ? AND materia_id = ?",
                    teacherId,
                    materiaId
                );

                if (existing != null) {
                    String errorMessage = URLEncoder.encode(
                        "Esta asignación ya existe.",
                        StandardCharsets.UTF_8.toString()
                    );
                    res.redirect("/teacher/assign-materia?error=" + errorMessage);
                    return "";
                }

                DocenteMateria nuevo = new DocenteMateria();
                nuevo.set("teacher_id", teacherId, "materia_id", materiaId);
                nuevo.saveIt();

                String successMessage = URLEncoder.encode(
                    "Materia asignada correctamente al docente.",
                    StandardCharsets.UTF_8.toString()
                );
                res.redirect("/teacher/assign-materia?message=" + successMessage);
                return "";
            } catch (NumberFormatException e) {
                String errorMessage = URLEncoder.encode(
                    "Los identificadores de docente y materia deben ser numéricos.",
                    StandardCharsets.UTF_8.toString()
                );
                res.redirect("/teacher/assign-materia?error=" + errorMessage);
                return "";
            } catch (Exception e) {
                e.printStackTrace();
                String errorMessage = URLEncoder.encode(
                    "Error interno al asignar la materia. Intente de nuevo.",
                    StandardCharsets.UTF_8.toString()
                );
                res.redirect("/teacher/assign-materia?error=" + errorMessage);
                return "";
            }
        });

        // ==========================================
// PANEL DEL DOCENTE — IS-19
// ==========================================

// Helper de autorización reutilizable
// (definido como lambda local al inicio de main, antes del primer get/post)

// GET /docente/materias — lista las materias asignadas al docente logueado
get("/docente/materias", (req, res) -> {
    String userRole = req.session().attribute("userRole");
    if (userRole == null || !userRole.equals("DOCENTE")) {
        res.redirect("/dashboard?error=" + URLEncoder.encode(
            "Acceso denegado. Solo docentes pueden acceder a esta sección.",
            StandardCharsets.UTF_8.toString()));
        return null;
    }

    Map<String, Object> model = new HashMap<>();

    String successMessage = req.queryParams("message");
    String errorMessage   = req.queryParams("error");
    if (successMessage != null && !successMessage.isEmpty()) model.put("successMessage", successMessage);
    if (errorMessage   != null && !errorMessage.isEmpty())   model.put("errorMessage",   errorMessage);

    // Obtenemos el Teacher a partir del userId guardado en sesión
    Integer userId = req.session().attribute("userId");
    Teacher teacher = Teacher.findFirst("usuario_id = ?", userId);

    if (teacher == null) {
        res.redirect("/dashboard?error=" + URLEncoder.encode(
            "No se encontró un perfil docente para tu usuario.",
            StandardCharsets.UTF_8.toString()));
        return null;
    }

    int teacherId = teacher.getInteger("usuario_id");

    // Buscamos las materias asignadas vía Docente_Materia
    List<Map> materiasRaw = Base.findAll(
        "SELECT m.codigo, m.nombre, m.anio_cursada " +
        "FROM Materia m " +
        "JOIN Docente_Materia dm ON m.codigo = dm.materia_id " +
        "WHERE dm.teacher_id = ? " +
        "ORDER BY m.anio_cursada ASC, m.nombre ASC",
        teacherId
    );

    model.put("materias",    materiasRaw);
    model.put("sinMaterias", materiasRaw.isEmpty());

    return new ModelAndView(model, "docente_materias.mustache");
}, new MustacheTemplateEngine());


// GET /docente/materia/:materiaId — panel de acciones de una materia concreta
get("/docente/materia/:materiaId", (req, res) -> {
    String userRole = req.session().attribute("userRole");
    if (userRole == null || !userRole.equals("DOCENTE")) {
        res.redirect("/dashboard?error=" + URLEncoder.encode(
            "Acceso denegado.", StandardCharsets.UTF_8.toString()));
        return null;
    }

    Integer userId  = req.session().attribute("userId");
    Teacher teacher = Teacher.findFirst("usuario_id = ?", userId);
    if (teacher == null) { res.redirect("/dashboard"); return null; }

    int teacherId  = teacher.getInteger("usuario_id");
    int materiaId;
    try {
        materiaId = Integer.parseInt(req.params("materiaId"));
    } catch (NumberFormatException e) {
        res.redirect("/docente/materias?error=" + URLEncoder.encode(
            "Materia inválida.", StandardCharsets.UTF_8.toString()));
        return null;
    }

    // Verificamos que la materia le pertenezca al docente
    DocenteMateria asignacion = DocenteMateria.findFirst(
        "teacher_id = ? AND materia_id = ?", teacherId, materiaId);
    if (asignacion == null) {
        res.redirect("/docente/materias?error=" + URLEncoder.encode(
            "No tenés acceso a esa materia.", StandardCharsets.UTF_8.toString()));
        return null;
    }

    Materia materia = Materia.findFirst("codigo = ?", materiaId);
    if (materia == null) { res.redirect("/docente/materias"); return null; }

    // Periodo vigente de la materia
    MateriaPeriodo periodo = MateriaPeriodo.findFirst("materia_codigo = ?", materiaId);
    String periodoLabel = "";
    if (periodo != null) {
        String raw = periodo.getString("tipo_cuatrimestre");
        if      ("PRIMER_CUATRIMESTRE".equals(raw))  periodoLabel = "I Cuatrimestre";
        else if ("SEGUNDO_CUATRIMESTRE".equals(raw))  periodoLabel = "II Cuatrimestre";
        else if ("ANUAL".equals(raw))                 periodoLabel = "Anual";
        else if ("VERANO".equals(raw))                periodoLabel = "Verano";
    }

    // Alumnos para el selector de notas (todos los students)
    List<Map<String, Object>> alumnosOptions = new ArrayList<>();
    for (Model studentRecord : Student.findAll()) {
        Student student = (Student) studentRecord;
        User   user     = student.getUser();
        String label    = user != null
            ? user.getString("apellido") + ", " + user.getString("nombre") +
              " — " + student.getLegajo()
            : "Alumno #" + student.getInteger("usuario_id");
        Map<String, Object> opt = new HashMap<>();
        opt.put("id",    student.getInteger("usuario_id"));
        opt.put("label", label);
        alumnosOptions.add(opt);
    }

    Map<String, Object> model = new HashMap<>();
    model.put("codigoMateria", materiaId);
    model.put("nombreMateria", materia.getString("nombre"));
    model.put("anioMateria",   materia.getInteger("anio_cursada"));
    model.put("periodoMateria", periodoLabel);
    model.put("alumnos",       alumnosOptions);

    String successMessage = req.queryParams("message");
    String errorMessage   = req.queryParams("error");
    if (successMessage != null && !successMessage.isEmpty()) model.put("successMessage", successMessage);
    if (errorMessage   != null && !errorMessage.isEmpty())   model.put("errorMessage",   errorMessage);

    return new ModelAndView(model, "docente_panel_materia.mustache");
}, new MustacheTemplateEngine());


// POST /docente/materia/:materiaId/anuncio — persiste un anuncio
post("/docente/materia/:materiaId/anuncio", (req, res) -> {
    String userRole = req.session().attribute("userRole");
    if (userRole == null || !userRole.equals("DOCENTE")) {
        res.redirect("/dashboard?error=" + URLEncoder.encode(
            "Acceso denegado.", StandardCharsets.UTF_8.toString()));
        return "";
    }

    Integer userId  = req.session().attribute("userId");
    Teacher teacher = Teacher.findFirst("usuario_id = ?", userId);
    int teacherId   = teacher.getInteger("usuario_id");
    int materiaId   = Integer.parseInt(req.params("materiaId"));

    // Verificar pertenencia
    if (DocenteMateria.findFirst("teacher_id = ? AND materia_id = ?", teacherId, materiaId) == null) {
        res.redirect("/docente/materias?error=" + URLEncoder.encode(
            "No tenés acceso a esa materia.", StandardCharsets.UTF_8.toString()));
        return "";
    }

    // Obtener el MateriaPeriodo vigente
    MateriaPeriodo mp = MateriaPeriodo.findFirst("materia_codigo = ?", materiaId);
    if (mp == null) {
        res.redirect("/docente/materia/" + materiaId + "?error=" + URLEncoder.encode(
            "No existe un período activo para esta materia.", StandardCharsets.UTF_8.toString()));
        return "";
    }

    String tipo        = req.queryParams("tipo");
    String titulo      = req.queryParams("titulo");
    String contenido   = req.queryParams("contenido");
    String fechaExamen = req.queryParams("fecha_examen");

    try {
        Anuncio anuncio = new Anuncio();
        anuncio.set("materia_periodo_id", mp.getId());
        anuncio.set("teacher_id",         teacherId);
        anuncio.set("tipo",               tipo);
        anuncio.set("titulo",             titulo);
        anuncio.set("contenido",          contenido);
        if ("EXAMEN".equals(tipo) && fechaExamen != null && !fechaExamen.isBlank()) {
            anuncio.set("fecha_examen", java.sql.Date.valueOf(fechaExamen));
        }
        anuncio.saveIt();

        res.redirect("/docente/materia/" + materiaId + "?message=" + URLEncoder.encode(
            "Anuncio publicado correctamente.", StandardCharsets.UTF_8.toString()));
    } catch (Exception e) {
        e.printStackTrace();
        res.redirect("/docente/materia/" + materiaId + "?error=" + URLEncoder.encode(
            "Error al publicar el anuncio: " + e.getMessage(), StandardCharsets.UTF_8.toString()));
    }
    return "";
});


// POST /docente/materia/:materiaId/nota — persiste una nota
post("/docente/materia/:materiaId/nota", (req, res) -> {
    String userRole = req.session().attribute("userRole");
    if (userRole == null || !userRole.equals("DOCENTE")) {
        res.redirect("/dashboard?error=" + URLEncoder.encode(
            "Acceso denegado.", StandardCharsets.UTF_8.toString()));
        return "";
    }

    Integer userId  = req.session().attribute("userId");
    Teacher teacher = Teacher.findFirst("usuario_id = ?", userId);
    int teacherId   = teacher.getInteger("usuario_id");
    int materiaId   = Integer.parseInt(req.params("materiaId"));

    if (DocenteMateria.findFirst("teacher_id = ? AND materia_id = ?", teacherId, materiaId) == null) {
        res.redirect("/docente/materias?error=" + URLEncoder.encode(
            "No tenés acceso a esa materia.", StandardCharsets.UTF_8.toString()));
        return "";
    }

    MateriaPeriodo mp = MateriaPeriodo.findFirst("materia_codigo = ?", materiaId);
    if (mp == null) {
        res.redirect("/docente/materia/" + materiaId + "?error=" + URLEncoder.encode(
            "No existe un período activo para esta materia.", StandardCharsets.UTF_8.toString()));
        return "";
    }

    String studentIdParam = req.queryParams("student_id");
    String valorParam     = req.queryParams("valor");

    if (studentIdParam == null || studentIdParam.isEmpty() || valorParam == null || valorParam.isEmpty()) {
        res.redirect("/docente/materia/" + materiaId + "?error=" + URLEncoder.encode(
            "Debés seleccionar un alumno e ingresar una nota.", StandardCharsets.UTF_8.toString()));
        return "";
    }

    try {
        int    studentId = Integer.parseInt(studentIdParam);
        double valor     = Double.parseDouble(valorParam);

        if (valor < 0 || valor > 10) throw new IllegalArgumentException("La nota debe estar entre 0 y 10.");

        Nota nota = new Nota();
        nota.set("materia_periodo_id", mp.getId());
        nota.set("student_id",         studentId);
        nota.set("teacher_id",         teacherId);
        nota.set("valor",              valor);
        nota.saveIt();

        res.redirect("/docente/materia/" + materiaId + "?message=" + URLEncoder.encode(
            "Nota registrada correctamente.", StandardCharsets.UTF_8.toString()));
    } catch (Exception e) {
        e.printStackTrace();
        res.redirect("/docente/materia/" + materiaId + "?error=" + URLEncoder.encode(
            "Error al registrar la nota: " + e.getMessage(), StandardCharsets.UTF_8.toString()));
    }
    return "";
});


// POST /docente/materia/:materiaId/aula — persiste asignación de aula
post("/docente/materia/:materiaId/aula", (req, res) -> {
    String userRole = req.session().attribute("userRole");
    if (userRole == null || !userRole.equals("DOCENTE")) {
        res.redirect("/dashboard?error=" + URLEncoder.encode(
            "Acceso denegado.", StandardCharsets.UTF_8.toString()));
        return "";
    }

    Integer userId  = req.session().attribute("userId");
    Teacher teacher = Teacher.findFirst("usuario_id = ?", userId);
    int teacherId   = teacher.getInteger("usuario_id");
    int materiaId   = Integer.parseInt(req.params("materiaId"));

    if (DocenteMateria.findFirst("teacher_id = ? AND materia_id = ?", teacherId, materiaId) == null) {
        res.redirect("/docente/materias?error=" + URLEncoder.encode(
            "No tenés acceso a esa materia.", StandardCharsets.UTF_8.toString()));
        return "";
    }

    MateriaPeriodo mp = MateriaPeriodo.findFirst("materia_codigo = ?", materiaId);
    if (mp == null) {
        res.redirect("/docente/materia/" + materiaId + "?error=" + URLEncoder.encode(
            "No existe un período activo para esta materia.", StandardCharsets.UTF_8.toString()));
        return "";
    }

    String aula = req.queryParams("aula");
    if (aula == null || aula.isBlank()) {
        res.redirect("/docente/materia/" + materiaId + "?error=" + URLEncoder.encode(
            "Debés ingresar el nombre o número del aula.", StandardCharsets.UTF_8.toString()));
        return "";
    }

    try {
        AulaAsignacion asig = new AulaAsignacion();
        asig.set("materia_periodo_id", mp.getId());
        asig.set("teacher_id",         teacherId);
        asig.set("aula",               aula);
        asig.saveIt();

        res.redirect("/docente/materia/" + materiaId + "?message=" + URLEncoder.encode(
            "Aula asignada correctamente.", StandardCharsets.UTF_8.toString()));
    } catch (Exception e) {
        e.printStackTrace();
        res.redirect("/docente/materia/" + materiaId + "?error=" + URLEncoder.encode(
            "Error al asignar el aula: " + e.getMessage(), StandardCharsets.UTF_8.toString()));
    }
    return "";
});


// GET /docente/materia/:materiaId/contenido — stub "Próximamente"
get("/docente/materia/:materiaId/contenido", (req, res) -> {
    String userRole = req.session().attribute("userRole");
    if (userRole == null || !userRole.equals("DOCENTE")) {
        res.redirect("/dashboard?error=" + URLEncoder.encode(
            "Acceso denegado.", StandardCharsets.UTF_8.toString()));
        return null;
    }
    return new ModelAndView(new HashMap<>(), "contenido_proximamente.mustache");
}, new MustacheTemplateEngine());



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
    } 
} 