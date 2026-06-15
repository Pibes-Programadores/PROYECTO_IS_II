package com.is1.proyecto.controllers;

import static spark.Spark.*; // Importa los métodos estáticos principales de Spark (get, post, before, after, etc.).
import com.fasterxml.jackson.databind.ObjectMapper; // Utilidad para serializar/deserializar objetos Java a/desde JSON.
import com.is1.proyecto.config.DBConfigSingleton; // Clase Singleton para la configuración de la base de datos.
import com.is1.proyecto.models.Anuncio;
import com.is1.proyecto.models.AulaAsignacion;
import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.Correlatividad;
import com.is1.proyecto.models.DocenteCarrera;
import com.is1.proyecto.models.DocenteMateria;
import com.is1.proyecto.models.EstadoAcademico;
import com.is1.proyecto.models.InscripcionExamen;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.MateriaPeriodo;
import com.is1.proyecto.models.MesaExamen;
import com.is1.proyecto.models.Nota;
import com.is1.proyecto.models.PlanEstudio;
import com.is1.proyecto.models.SecretariaAcademica;
import com.is1.proyecto.models.Student;
import com.is1.proyecto.models.Teacher;
import com.is1.proyecto.models.User; // Modelo de ActiveJDBC que representa la tabla 'users'.
import com.mysql.cj.exceptions.StreamingNotifiable;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap; // Para crear mapas de datos (modelos para las plantillas).
import java.util.List;
import java.util.Map; // Interfaz Map, utilizada para Map.of() o HashMap.
import org.javalite.activejdbc.Base; // Clase central de ActiveJDBC para gestionar la conexión a la base de datos.
import org.javalite.activejdbc.Model;
import org.mindrot.jbcrypt.BCrypt; // Utilidad para hashear y verificar contraseñas de forma segura.
import spark.ModelAndView; // Representa un modelo de datos y el nombre de la vista a renderizar.
import spark.template.mustache.MustacheTemplateEngine; // Motor de plantillas Mustache para Spark.
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class TeacherController {
    public static void register() {
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

                        List<Map> carreras = Base.findAll(
                            "SELECT id, nombre FROM Carrera ORDER BY nombre ASC"
                        );
                        model.put("carreras", carreras);

                        return new ModelAndView(model, "teacher_from.mustache");
                    },
                    new MustacheTemplateEngine()
                );

        get(
                    "/teacher/assign-materia",
                    (req, res) -> {
                        String userRole = req.session().attribute("userRole");
                        if (
                            userRole == null ||
                            (!userRole.equals("SECRETARIA") &&
                                !userRole.equals("ADMIN"))
                        ) {
                            String errorMessage = URLEncoder.encode(
                                "Acceso denegado. Solo SECRETARIA puede asignar materias.",
                                StandardCharsets.UTF_8.toString()
                            );
                            res.redirect("/dashboard?error=" + errorMessage);
                            return null;
                        }

                        Map<String, Object> model = new HashMap<>();

                        String successMessage = req.queryParams("message");
                        String errorMessage = req.queryParams("error");
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

                    if (
                        name == null ||
                        lastName == null ||
                        dni == null ||
                        carreraId == null ||
                        carreraId.isBlank()
                    ) {
                        res.redirect(
                            "/teacher/new?error=Los campos Nombre, Apellido, DNI y Carrera son obligatorios."
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
                    if (
                        userRole == null ||
                        (!userRole.equals("SECRETARIA") && !userRole.equals("ADMIN"))
                    ) {
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

                        Teacher teacher = (Teacher) Teacher.findFirst(
                            "usuario_id = ?",
                            teacherId
                        );
                        Materia materia = (Materia) Materia.findFirst(
                            "codigo = ?",
                            materiaId
                        );

                        if (teacher == null || materia == null) {
                            String errorMessage = URLEncoder.encode(
                                "Docente o materia no válida.",
                                StandardCharsets.UTF_8.toString()
                            );
                            res.redirect(
                                "/teacher/assign-materia?error=" + errorMessage
                            );
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
                            res.redirect(
                                "/teacher/assign-materia?error=" + errorMessage
                            );
                            return "";
                        }

                        DocenteMateria nuevo = new DocenteMateria();
                        nuevo.set("teacher_id", teacherId, "materia_id", materiaId);
                        nuevo.saveIt();

                        String successMessage = URLEncoder.encode(
                            "Materia asignada correctamente al docente.",
                            StandardCharsets.UTF_8.toString()
                        );
                        res.redirect(
                            "/teacher/assign-materia?message=" + successMessage
                        );
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

        get(
                    "/docente/materias",
                    (req, res) -> {
                        String userRole = req.session().attribute("userRole");
                        if (userRole == null || !userRole.equals("DOCENTE")) {
                            res.redirect(
                                "/dashboard?error=" +
                                    URLEncoder.encode(
                                        "Acceso denegado. Solo docentes pueden acceder a esta sección.",
                                        StandardCharsets.UTF_8.toString()
                                    )
                            );
                            return null;
                        }

                        Map<String, Object> model = new HashMap<>();

                        String successMessage = req.queryParams("message");
                        String errorMessage = req.queryParams("error");
                        if (
                            successMessage != null && !successMessage.isEmpty()
                        ) model.put("successMessage", successMessage);
                        if (errorMessage != null && !errorMessage.isEmpty()) model.put(
                            "errorMessage",
                            errorMessage
                        );

                        // Obtenemos el Teacher a partir del userId guardado en sesión
                        Integer userId = req.session().attribute("userId");
                        Teacher teacher = Teacher.findFirst("usuario_id = ?", userId);

                        if (teacher == null) {
                            res.redirect(
                                "/dashboard?error=" +
                                    URLEncoder.encode(
                                        "No se encontró un perfil docente para tu usuario.",
                                        StandardCharsets.UTF_8.toString()
                                    )
                            );
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

                        model.put("materias", materiasRaw);
                        model.put("sinMaterias", materiasRaw.isEmpty());

                        return new ModelAndView(model, "docente_materias.mustache");
                    },
                    new MustacheTemplateEngine()
                );

        get(
                    "/docente/materia/:materiaId",
                    (req, res) -> {
                        String userRole = req.session().attribute("userRole");
                        if (userRole == null || !userRole.equals("DOCENTE")) {
                            res.redirect(
                                "/dashboard?error=" +
                                    URLEncoder.encode(
                                        "Acceso denegado.",
                                        StandardCharsets.UTF_8.toString()
                                    )
                            );
                            return null;
                        }

                        Integer userId = req.session().attribute("userId");
                        Teacher teacher = Teacher.findFirst("usuario_id = ?", userId);
                        if (teacher == null) {
                            res.redirect("/dashboard");
                            return null;
                        }

                        int teacherId = teacher.getInteger("usuario_id");
                        int materiaId;
                        try {
                            materiaId = Integer.parseInt(req.params("materiaId"));
                        } catch (NumberFormatException e) {
                            res.redirect(
                                "/docente/materias?error=" +
                                    URLEncoder.encode(
                                        "Materia inválida.",
                                        StandardCharsets.UTF_8.toString()
                                    )
                            );
                            return null;
                        }

                        // Verificamos que la materia le pertenezca al docente
                        DocenteMateria asignacion = DocenteMateria.findFirst(
                            "teacher_id = ? AND materia_id = ?",
                            teacherId,
                            materiaId
                        );
                        if (asignacion == null) {
                            res.redirect(
                                "/docente/materias?error=" +
                                    URLEncoder.encode(
                                        "No tenés acceso a esa materia.",
                                        StandardCharsets.UTF_8.toString()
                                    )
                            );
                            return null;
                        }

                        Materia materia = Materia.findFirst("codigo = ?", materiaId);
                        if (materia == null) {
                            res.redirect("/docente/materias");
                            return null;
                        }

                        // Periodo vigente de la materia
                        MateriaPeriodo periodo = MateriaPeriodo.findFirst(
                            "materia_codigo = ?",
                            materiaId
                        );
                        String periodoLabel = "";
                        if (periodo != null) {
                            String raw = periodo.getString("tipo_cuatrimestre");
                            if ("PRIMER_CUATRIMESTRE".equals(raw)) periodoLabel =
                                "I Cuatrimestre";
                            else if ("SEGUNDO_CUATRIMESTRE".equals(raw)) periodoLabel =
                                "II Cuatrimestre";
                            else if ("ANUAL".equals(raw)) periodoLabel = "Anual";
                            else if ("VERANO".equals(raw)) periodoLabel = "Verano";
                        }

                        // Alumnos para el selector de notas (sólo inscriptos o regulares)
                        List<Map> inscriptosRows = Base.findAll(
                            "SELECT u.id as usuario_id, u.nombre, u.apellido, s.legajo " +
                                "FROM users u " +
                                "JOIN student s ON u.id = s.usuario_id " +
                                "JOIN Estado_Academico ea ON s.usuario_id = ea.usuario_id " +
                                "WHERE ea.materia_codigo = ? AND ea.estado IN ('INSCRIPTO', 'REGULAR')",
                            materiaId
                        );

                        List<Map<String, Object>> alumnosOptions = new ArrayList<>();
                        for (Map row : inscriptosRows) {
                            String label =
                                row.get("apellido") +
                                ", " +
                                row.get("nombre") +
                                " — " +
                                row.get("legajo");
                            Map<String, Object> opt = new HashMap<>();
                            opt.put("id", row.get("usuario_id"));
                            opt.put("label", label);
                            alumnosOptions.add(opt);
                        }

                        Map<String, Object> model = new HashMap<>();
                        model.put("codigoMateria", materiaId);
                        model.put("nombreMateria", materia.getString("nombre"));
                        model.put("anioMateria", materia.getInteger("anio_cursada"));
                        model.put("periodoMateria", periodoLabel);
                        model.put("alumnos", alumnosOptions);
                        model.put("hayAlumnos", !alumnosOptions.isEmpty());

                        List<Map<String, Object>> anuncios = new ArrayList<>();
                        if (periodo != null) {
                            List<Map> anunciosDB = Base.findAll(
                                "SELECT id, tipo, titulo, contenido, fecha_examen FROM Anuncio WHERE materia_periodo_id = ? ORDER BY fecha_creacion DESC",
                                periodo.getId()
                            );
                            for (Map a : anunciosDB) {
                                Map<String, Object> anuncioMap = new HashMap<>();
                                anuncioMap.put("id", a.get("id"));
                                anuncioMap.put("tipo", a.get("tipo"));
                                anuncioMap.put("titulo", a.get("titulo"));
                                anuncioMap.put("contenido", a.get("contenido"));
                                anuncioMap.put("fechaExamen", a.get("fecha_examen"));
                                anuncioMap.put(
                                    "esExamen",
                                    "EXAMEN".equals(a.get("tipo"))
                                );
                                if ("EXAMEN".equals(a.get("tipo"))) {
                                    List<Map> conteoRows = Base.findAll(
                                        "SELECT COUNT(*) AS total FROM Inscripcion_Parcial WHERE anuncio_id = ?",
                                        ((Number) a.get("id")).intValue()
                                    );
                                    int total = conteoRows.isEmpty()
                                        ? 0
                                        : (
                                              (Number) conteoRows.get(0).get("total")
                                          ).intValue();
                                    anuncioMap.put("inscriptosCount", total);
                                }
                                anuncios.add(anuncioMap);
                            }
                        }
                        model.put("anuncios", anuncios);

                        // Anuncios del período con contador de inscriptos a parciales
                        List<Map<String, Object>> anunciosConConteo = new ArrayList<>();
                        if (periodo != null) {
                            List<Map> anunciosDB = Base.findAll(
                                "SELECT id, tipo, titulo, contenido, fecha_examen FROM Anuncio WHERE materia_periodo_id = ? ORDER BY fecha_creacion DESC",
                                periodo.getId()
                            );
                            for (Map a : anunciosDB) {
                                Map<String, Object> anuncioMap = new HashMap<>();
                                anuncioMap.put("id", a.get("id"));
                                anuncioMap.put("tipo", a.get("tipo"));
                                anuncioMap.put("titulo", a.get("titulo"));
                                anuncioMap.put("contenido", a.get("contenido"));
                                anuncioMap.put("fechaExamen", a.get("fecha_examen"));
                                anuncioMap.put(
                                    "esExamen",
                                    "EXAMEN".equals(a.get("tipo"))
                                );
                                if ("EXAMEN".equals(a.get("tipo"))) {
                                    List<Map> conteoRows = Base.findAll(
                                        "SELECT COUNT(*) AS total FROM Inscripcion_Parcial WHERE anuncio_id = ?",
                                        ((Number) a.get("id")).intValue()
                                    );
                                    int total = conteoRows.isEmpty()
                                        ? 0
                                        : (
                                              (Number) conteoRows.get(0).get("total")
                                          ).intValue();
                                    anuncioMap.put("inscriptosCount", total);
                                }
                                anunciosConConteo.add(anuncioMap);
                            }
                        }
                        model.put("anuncios", anunciosConConteo);

                        String successMessage = req.queryParams("message");
                        String errorMessage = req.queryParams("error");
                        if (
                            successMessage != null && !successMessage.isEmpty()
                        ) model.put("successMessage", successMessage);
                        if (errorMessage != null && !errorMessage.isEmpty()) model.put(
                            "errorMessage",
                            errorMessage
                        );

                        return new ModelAndView(
                            model,
                            "docente_panel_materia.mustache"
                        );
                    },
                    new MustacheTemplateEngine()
                );

        post("/docente/materia/:materiaId/aula", (req, res) -> {
                    String userRole = req.session().attribute("userRole");
                    if (userRole == null || !userRole.equals("DOCENTE")) {
                        res.redirect(
                            "/dashboard?error=" +
                                URLEncoder.encode(
                                    "Acceso denegado.",
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                        return "";
                    }

                    Integer userId = req.session().attribute("userId");
                    Teacher teacher = Teacher.findFirst("usuario_id = ?", userId);
                    int teacherId = teacher.getInteger("usuario_id");
                    int materiaId = Integer.parseInt(req.params("materiaId"));

                    if (
                        DocenteMateria.findFirst(
                            "teacher_id = ? AND materia_id = ?",
                            teacherId,
                            materiaId
                        ) == null
                    ) {
                        res.redirect(
                            "/docente/materias?error=" +
                                URLEncoder.encode(
                                    "No tenés acceso a esa materia.",
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                        return "";
                    }

                    MateriaPeriodo mp = MateriaPeriodo.findFirst(
                        "materia_codigo = ?",
                        materiaId
                    );
                    if (mp == null) {
                        res.redirect(
                            "/docente/materia/" +
                                materiaId +
                                "?error=" +
                                URLEncoder.encode(
                                    "No existe un período activo para esta materia.",
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                        return "";
                    }

                    String aula = req.queryParams("aula");
                    if (aula == null || aula.isBlank()) {
                        res.redirect(
                            "/docente/materia/" +
                                materiaId +
                                "?error=" +
                                URLEncoder.encode(
                                    "Debés ingresar el nombre o número del aula.",
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                        return "";
                    }

                    try {
                        AulaAsignacion asig = new AulaAsignacion();
                        asig.set("materia_periodo_id", mp.getId());
                        asig.set("teacher_id", teacherId);
                        asig.set("aula", aula);
                        asig.saveIt();

                        res.redirect(
                            "/docente/materia/" +
                                materiaId +
                                "?message=" +
                                URLEncoder.encode(
                                    "Aula asignada correctamente.",
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        res.redirect(
                            "/docente/materia/" +
                                materiaId +
                                "?error=" +
                                URLEncoder.encode(
                                    "Error al asignar el aula: " + e.getMessage(),
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                    }
                    return "";
                });

        get(
                    "/docente/materia/:materiaId/contenido",
                    (req, res) -> {
                        String userRole = req.session().attribute("userRole");
                        if (userRole == null || !userRole.equals("DOCENTE")) {
                            res.redirect(
                                "/dashboard?error=" +
                                    URLEncoder.encode(
                                        "Acceso denegado.",
                                        StandardCharsets.UTF_8.toString()
                                    )
                            );
                            return null;
                        }
                        return new ModelAndView(
                            new HashMap<>(),
                            "contenido_proximamente.mustache"
                        );
                    },
                    new MustacheTemplateEngine()
                );

        get("/docente/edit/:id", (req, res) -> {
                Boolean loggedIn = req.session().attribute("loggedIn");
                String  userRole = req.session().attribute("userRole");
                if (!Boolean.TRUE.equals(loggedIn)) { res.redirect("/login"); return null; }
                if (!"ADMIN".equals(userRole) && !"SECRETARIA".equals(userRole)) {
                    res.status(403);
                    Map<String, Object> em = new HashMap<>();
                    em.put("errorMessage", "Acceso denegado.");
                    return new ModelAndView(em, "error.mustache");
                }
    
                int docenteId = Integer.parseInt(req.params("id"));
                List<Map> rows = Base.findAll(
                    "SELECT u.id, u.nombre, u.apellido, u.dni, u.direccion, u.telefono, " +
                    "       u.nombre_usuario, t.legajo_docente, t.cuil, t.email, t.especialidad " +
                    "FROM users u JOIN teacher t ON t.usuario_id = u.id " +
                    "WHERE u.id = ? AND u.nivel_acceso = 'DOCENTE'",
                    docenteId
                );
                if (rows.isEmpty()) {
                    res.redirect("/configuracion?error=" + URLEncoder.encode(
                        "Docente no encontrado.", StandardCharsets.UTF_8.toString()));
                    return null;
                }
                Map d = rows.get(0);
                Map<String, Object> model = new HashMap<>();
                model.put("id",             ((Number) d.get("id")).intValue());
                model.put("nombre",         d.get("nombre"));
                model.put("apellido",       d.get("apellido"));
                model.put("dni",            d.get("dni"));
                model.put("direccion",      d.get("direccion") != null ? d.get("direccion") : "");
                model.put("telefono",       d.get("telefono")  != null ? d.get("telefono")  : "");
                model.put("nombre_usuario", d.get("nombre_usuario"));
                model.put("legajo_docente", d.get("legajo_docente"));
                model.put("cuil",           d.get("cuil"));
                model.put("email",          d.get("email"));
                model.put("especialidad",   d.get("especialidad") != null ? d.get("especialidad") : "");
    
                String err = req.queryParams("error");
                if (err != null && !err.isEmpty()) model.put("errorMessage", err);
    
                return new ModelAndView(model, "docente_edit_form.mustache");
            }, new MustacheTemplateEngine());

        post("/docente/edit/:id", (req, res) -> {
                Boolean loggedIn = req.session().attribute("loggedIn");
                String  userRole = req.session().attribute("userRole");
                if (!Boolean.TRUE.equals(loggedIn)) { res.redirect("/login"); return null; }
                if (!"ADMIN".equals(userRole) && !"SECRETARIA".equals(userRole)) {
                    res.status(403); return "Acceso denegado.";
                }
    
                int docenteId = Integer.parseInt(req.params("id"));
    
                String nombre      = req.queryParams("nombre");
                String apellido    = req.queryParams("apellido");
                String dni         = req.queryParams("dni");
                String direccion   = req.queryParams("direccion");
                String telefono    = req.queryParams("telefono");
                String email       = req.queryParams("email");
                String especialidad = req.queryParams("especialidad");
    
                if (nombre == null || nombre.isBlank() ||
                    apellido == null || apellido.isBlank() ||
                    email == null || email.isBlank()) {
                    res.redirect("/docente/edit/" + docenteId + "?error=" + URLEncoder.encode(
                        "Nombre, apellido y email son obligatorios.", StandardCharsets.UTF_8.toString()));
                    return null;
                }
    
                try {
                    Base.openTransaction();
                    Base.exec(
                        "UPDATE users SET nombre = ?, apellido = ?, dni = ?, " +
                        "                 direccion = ?, telefono = ? WHERE id = ?",
                        nombre.trim(), apellido.trim(), dni != null ? dni.trim() : "",
                        direccion, telefono, docenteId
                    );
                    Base.exec(
                        "UPDATE teacher SET email = ?, especialidad = ? WHERE usuario_id = ?",
                        email.trim(), especialidad, docenteId
                    );
                    Base.commitTransaction();
                    res.redirect("/configuracion?message=" + URLEncoder.encode(
                        "Docente actualizado correctamente.", StandardCharsets.UTF_8.toString()) + "#docentes");
                } catch (Exception e) {
                    Base.rollbackTransaction();
                    e.printStackTrace();
                    res.redirect("/docente/edit/" + docenteId + "?error=" + URLEncoder.encode(
                        "Error al guardar: " + e.getMessage(), StandardCharsets.UTF_8.toString()));
                }
                return null;
            });

        post("/docente/delete/:id", (req, res) -> {
                Boolean loggedIn = req.session().attribute("loggedIn");
                String  userRole = req.session().attribute("userRole");
                if (!Boolean.TRUE.equals(loggedIn)) { res.redirect("/login"); return null; }
                if (!"ADMIN".equals(userRole) && !"SECRETARIA".equals(userRole)) {
                    res.status(403); return "Acceso denegado.";
                }
    
                int docenteId  = Integer.parseInt(req.params("id"));
                Object currentUserId = req.session().attribute("userId");
                int    myId     = ((Number) currentUserId).intValue();
    
                if (docenteId == myId) {
                    res.redirect("/configuracion?error=" + URLEncoder.encode(
                        "No podés eliminar tu propio usuario.", StandardCharsets.UTF_8.toString()) + "#docentes");
                    return null;
                }
    
                List<Map> check = Base.findAll(
                    "SELECT id FROM users WHERE id = ? AND nivel_acceso = 'DOCENTE'", docenteId
                );
                if (check.isEmpty()) {
                    res.redirect("/configuracion?error=" + URLEncoder.encode(
                        "Docente no encontrado.", StandardCharsets.UTF_8.toString()) + "#docentes");
                    return null;
                }
    
                try {
                    // ON DELETE CASCADE en teacher, Docente_Materia, Docente_Carrera,
                    // Anuncio, Nota, sesion → todo se limpia automáticamente
                    Base.exec("DELETE FROM users WHERE id = ?", docenteId);
                    res.redirect("/configuracion?message=" + URLEncoder.encode(
                        "Docente eliminado correctamente.", StandardCharsets.UTF_8.toString()) + "#docentes");
                } catch (Exception e) {
                    e.printStackTrace();
                    res.redirect("/configuracion?error=" + URLEncoder.encode(
                        "Error al eliminar: " + e.getMessage(), StandardCharsets.UTF_8.toString()) + "#docentes");
                }
                return null;
            });

    }
}
