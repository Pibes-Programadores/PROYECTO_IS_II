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

public class StudentController {
    public static void register() {
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

                        List<Map> planesVigentes = Base.findAll(
                            "SELECT p.id AS id, " +
                                "CONCAT(c.nombre, ' (Plan Resol: ', p.anio_resolucion, ')') AS descripcion " +
                                "FROM Plan_Estudio p " +
                                "JOIN Carrera c ON p.carrera_id = c.id " +
                                "WHERE p.estado = 'VIGENTE' " +
                                "ORDER BY c.nombre ASC"
                        );
                        model.put("planes", planesVigentes);

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
                    String planEstudioId = req.queryParams("plan_estudio_id");

                    if (
                        name == null ||
                        lastName == null ||
                        dni == null ||
                        email == null ||
                        legajo == null ||
                        tipoEstudiante == null ||
                        planEstudioId == null ||
                        planEstudioId.isBlank()
                    ) {
                        String errorMsg = URLEncoder.encode(
                            "Todos los campos (incluyendo legajo) son obligatorios.",
                            StandardCharsets.UTF_8.toString()
                        );
                        res.redirect("/student/new?error=" + errorMsg);
                        return "";
                    }

                    try {
                        Base.openTransaction();

                        // 2. Guardamos el Usuario base
                        User u = new User();
                        u.set("nombre", name, "apellido", lastName, "dni", dni);
                        u.set("nombre_usuario", email);
                        u.set("nivel_acceso", "ESTUDIANTE");
                        u.set("password", BCrypt.hashpw("1234", BCrypt.gensalt()));
                        u.saveIt();

                        // 3. Guardamos el Estudiante hijo con SQL directo para evitar
                        // que ActiveJDBC confunda el INSERT con un UPDATE (ya que @IdName
                        // no es AUTO_INCREMENT y el id se provee explícitamente).
                        Base.exec(
                            "INSERT INTO student (usuario_id, legajo, tipo_estudiante, plan_estudio_id) VALUES (?, ?, ?, ?)",
                            u.getId(),
                            legajo,
                            tipoEstudiante,
                            Integer.parseInt(planEstudioId)
                        );

                        Base.commitTransaction();

                        String successMsg = URLEncoder.encode(
                            "Estudiante registrado exitosamente con clave 1234.",
                            StandardCharsets.UTF_8.toString()
                        );
                        res.redirect("/student/new?message=" + successMsg);
                        return "";
                    } catch (Exception e) {
                        Base.rollbackTransaction();
                        e.printStackTrace();
                        res.redirect(
                            "/student/new?error=" +
                                URLEncoder.encode(
                                    "Error al registrar: " + e.getMessage(),
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                        return "";
                    }
                });

        get(
                    "/estudiante/materias",
                    (req, res) -> {
                        // ── 1. Control de acceso ──────────────────────────────────────────────────
                        String userRole = req.session().attribute("userRole");
                        Object userIdObj = req.session().attribute("userId");

                        if (!"ESTUDIANTE".equals(userRole) || userIdObj == null) {
                            res.redirect(
                                "/login?error=" +
                                    URLEncoder.encode(
                                        "Acceso restringido a estudiantes.",
                                        StandardCharsets.UTF_8.toString()
                                    )
                            );
                            return null;
                        }

                        int userId = ((Number) userIdObj).intValue();

                        Map<String, Object> model = new HashMap<>();

                        // ── 2. Obtener plan_estudio_id y etiqueta del plan ────────────────────────
                        List<Map> studentRows = Base.findAll(
                            "SELECT" +
                                "    s.plan_estudio_id" +
                                "  , c.nombre          AS nombre_carrera" +
                                "  , p.anio_resolucion" +
                                " FROM student s" +
                                " JOIN Plan_Estudio p ON p.id = s.plan_estudio_id" +
                                " JOIN Carrera c      ON c.id = p.carrera_id" +
                                " WHERE s.usuario_id = ?",
                            userId
                        );

                        if (studentRows.isEmpty()) {
                            model.put(
                                "errorMessage",
                                "No se encontraron datos de tu cuenta de estudiante."
                            );
                            return new ModelAndView(
                                model,
                                "historia_academica.mustache"
                            );
                        }

                        Map studentRow = studentRows.get(0);
                        int planEstudioId = (
                            (Number) studentRow.get("plan_estudio_id")
                        ).intValue();
                        String nombrePlan =
                            studentRow.get("nombre_carrera") +
                            " — Plan " +
                            studentRow.get("anio_resolucion");
                        model.put("nombrePlan", nombrePlan);

                        // ── 3. Materias del plan con estado académico del alumno ──────────────────
                        //
                        // El JOIN a Materia_Periodo replica la lógica de GET /carrera/materias:
                        // se toma el primer Materia_Periodo de cada materia (MIN id) para obtener
                        // el cuatrimestre del plan y ordenar correctamente dentro de cada año.
                        //
                        // ORDER BY:
                        //   1° m.anio_cursada ASC         (año curricular)
                        //   2° peso del cuatrimestre ASC  (PRIMER/ANUAL=1, SEGUNDO=2, VERANO=3)
                        //   3° m.nombre ASC               (desempate alfabético)
                        List<Map> rows = Base.findAll(
                            "SELECT" +
                                "    m.codigo" +
                                "  , m.nombre" +
                                "  , m.anio_cursada" +
                                "  , mp_plan.tipo_cuatrimestre" +
                                "  , ea.estado" +
                                "  , (" +
                                "        SELECT n.valor" +
                                "        FROM Nota n" +
                                "        JOIN Materia_Periodo mp ON mp.id = n.materia_periodo_id" +
                                "        WHERE mp.materia_codigo = m.codigo" +
                                "          AND n.student_id      = ?" +
                                "        ORDER BY n.fecha_carga DESC" +
                                "        LIMIT 1" +
                                "    ) AS nota" +
                                " FROM Materia m" +
                                " LEFT JOIN Materia_Periodo mp_plan" +
                                "        ON mp_plan.materia_codigo = m.codigo" +
                                "       AND mp_plan.id = (" +
                                "               SELECT MIN(id)" +
                                "               FROM Materia_Periodo" +
                                "               WHERE materia_codigo = m.codigo" +
                                "           )" +
                                " LEFT JOIN Estado_Academico ea" +
                                "        ON ea.materia_codigo = m.codigo" +
                                "       AND ea.usuario_id     = ?" +
                                " WHERE m.plan_estudio_id = ?" +
                                " ORDER BY m.anio_cursada ASC" +
                                "        , CASE mp_plan.tipo_cuatrimestre" +
                                "              WHEN 'PRIMER_CUATRIMESTRE'  THEN 1" +
                                "              WHEN 'ANUAL'                THEN 1" +
                                "              WHEN 'SEGUNDO_CUATRIMESTRE' THEN 2" +
                                "              WHEN 'VERANO'               THEN 3" +
                                "              ELSE 4" +
                                "          END ASC" +
                                "        , m.nombre ASC",
                            userId,
                            userId,
                            planEstudioId
                        );

                        // ── 4. Transformar filas para Mustache ────────────────────────────────────
                        List<Map<String, Object>> materias = new ArrayList<>();
                        String grupoAnterior = "";

                        for (Map row : rows) {
                            Map<String, Object> item = new HashMap<>();

                            item.put("codigo", row.get("codigo"));
                            item.put("nombre", row.get("nombre"));
                            item.put("anio_cursada", row.get("anio_cursada"));

                            // Estado: "-" si la materia nunca fue cursada (LEFT JOIN → NULL)
                            String estado = (String) row.get("estado");
                            item.put("estado", estado != null ? estado : "-");

                            // Nota: DECIMAL(5,2) llega como BigDecimal. Sin nota → "-".
                            Object notaVal = row.get("nota");
                            if (notaVal != null) {
                                java.math.BigDecimal bd = new java.math.BigDecimal(
                                    notaVal.toString()
                                );
                                item.put(
                                    "nota",
                                    bd.stripTrailingZeros().toPlainString()
                                );
                            } else {
                                item.put("nota", "-");
                            }

                            // Badge CSS — precomputado porque Mustache es logic-less.
                            String estadoClase;
                            switch (estado != null ? estado : "") {
                                case "APROBADO":
                                    estadoClase =
                                        "bg-green-500/20 text-green-300 border-green-500/40";
                                    break;
                                case "PROMOCION":
                                    estadoClase =
                                        "bg-yellow-400/20 text-yellow-200 border-yellow-400/40";
                                    break;
                                case "REGULAR":
                                    estadoClase =
                                        "bg-blue-500/20 text-blue-300 border-blue-500/40";
                                    break;
                                case "INSCRIPTO":
                                    estadoClase =
                                        "bg-purple-500/20 text-purple-300 border-purple-500/40";
                                    break;
                                case "REPROBADO":
                                    estadoClase =
                                        "bg-red-500/20 text-red-300 border-red-500/40";
                                    break;
                                case "LIBRE":
                                    estadoClase =
                                        "bg-orange-500/20 text-orange-300 border-orange-500/40";
                                    break;
                                default:
                                    estadoClase =
                                        "bg-white/5 text-white/30 border-white/10";
                                    break;
                            }
                            item.put("estadoClase", estadoClase);

                            // anioHeader: mismo formato periodo_vista de GET /carrera/materias.
                            // Ejemplo: "1° Año - I Cuat.", "1° Año - II Cuat.", "2° Año - Anual"
                            // Se emite solo en la primera materia de cada grupo año+cuatrimestre.
                            int anio = ((Number) row.get("anio_cursada")).intValue();
                            String cuatrimestre =
                                row.get("tipo_cuatrimestre") != null
                                    ? (String) row.get("tipo_cuatrimestre")
                                    : "PRIMER_CUATRIMESTRE";
                            String grupoActual = anio + "_" + cuatrimestre;

                            if (!grupoActual.equals(grupoAnterior)) {
                                String labelCuatri;
                                switch (cuatrimestre) {
                                    case "PRIMER_CUATRIMESTRE":
                                        labelCuatri = "I Cuat.";
                                        break;
                                    case "SEGUNDO_CUATRIMESTRE":
                                        labelCuatri = "II Cuat.";
                                        break;
                                    case "ANUAL":
                                        labelCuatri = "Anual";
                                        break;
                                    case "VERANO":
                                        labelCuatri = "Verano";
                                        break;
                                    default:
                                        labelCuatri = cuatrimestre;
                                        break;
                                }
                                item.put("anioHeader", anio + "° Año - " + labelCuatri);
                                grupoAnterior = grupoActual;
                            }

                            materias.add(item);
                        }

                        model.put("materias", materias);
                        return new ModelAndView(model, "historia_academica.mustache");
                    },
                    new MustacheTemplateEngine()
                );

        get("/estudiante/inscripcion/cursada", (req, res) -> {
                    String currentUsername = req
                        .session()
                        .attribute("currentUserUsername");
                    String userRole = req.session().attribute("userRole");
                    if (currentUsername == null || !"ESTUDIANTE".equals(userRole)) {
                        res.redirect("/");
                        return null;
                    }

                    List<Map> studentRows = Base.findAll(
                        "SELECT s.usuario_id, s.plan_estudio_id FROM student s " +
                            "JOIN users u ON u.id = s.usuario_id WHERE u.nombre_usuario = ?",
                        currentUsername
                    );
                    if (studentRows.isEmpty()) {
                        res.redirect("/dashboard?error=No+se+encontro+el+estudiante");
                        return null;
                    }
                    int alumnoId = (
                        (Number) studentRows.get(0).get("usuario_id")
                    ).intValue();
                    int planId = (
                        (Number) studentRows.get(0).get("plan_estudio_id")
                    ).intValue();

                    List<Map<String, Object>> materiasDisponibles = new ArrayList<>();

                    // 1. Obtener todas las materias del plan de estudios via SQL directo
                    List<Map> materiasPlan = Base.findAll(
                        "SELECT codigo, nombre, anio_cursada FROM Materia WHERE plan_estudio_id = ?",
                        planId
                    );

                    for (Map m : materiasPlan) {
                        // ActiveJDBC devuelve los valores numéricos como Long o BigInteger según el driver
                        int materiaCodigo = ((Number) m.get("codigo")).intValue();

                        // 2. Excluir las que ya están en Estado_Academico
                        List<Map> estadoRows = Base.findAll(
                            "SELECT id FROM Estado_Academico WHERE usuario_id = ? AND materia_codigo = ?",
                            alumnoId,
                            materiaCodigo
                        );

                        if (!estadoRows.isEmpty()) {
                            continue; // Ya tiene registro, la salteamos
                        }

                        // 3. Validar correlatividades tipo 'CURSAR'
                        List<Map> requisitos = Base.findAll(
                            "SELECT materia_correlativa_codigo, condicion FROM Correlatividad WHERE materia_codigo = ? AND tipo_requisito = 'CURSAR'",
                            materiaCodigo
                        );

                        boolean cumpleCorrelatividades = true;
                        for (Map reqItem : requisitos) {
                            int reqCodigo = (
                                (Number) reqItem.get("materia_correlativa_codigo")
                            ).intValue();
                            String condicionRequerida = (String) reqItem.get(
                                "condicion"
                            );

                            List<Map> estadoReqRows = Base.findAll(
                                "SELECT estado FROM Estado_Academico WHERE usuario_id = ? AND materia_codigo = ?",
                                alumnoId,
                                reqCodigo
                            );

                            if (estadoReqRows.isEmpty()) {
                                cumpleCorrelatividades = false;
                                break;
                            }

                            String estadoActual = (String) estadoReqRows
                                .get(0)
                                .get("estado");

                            if (
                                "APROBADA".equals(condicionRequerida) &&
                                !"APROBADO".equals(estadoActual)
                            ) {
                                cumpleCorrelatividades = false;
                                break;
                            }

                            if (
                                "REGULAR".equals(condicionRequerida) &&
                                (!"REGULAR".equals(estadoActual) &&
                                    !"APROBADO".equals(estadoActual))
                            ) {
                                cumpleCorrelatividades = false;
                                break;
                            }
                        }

                        if (cumpleCorrelatividades) {
                            Map<String, Object> matData = new HashMap<>();
                            matData.put("codigo", materiaCodigo);
                            matData.put("nombre", m.get("nombre"));
                            matData.put("anio_cursada", m.get("anio_cursada"));
                            materiasDisponibles.add(matData);
                        }
                    }

                    Map<String, Object> viewData = new HashMap<>();
                    viewData.put("materias", materiasDisponibles);

                    String successMessage = req.queryParams("message");
                    String errorMessage = req.queryParams("error");
                    if (successMessage != null) viewData.put(
                        "successMessage",
                        successMessage
                    );
                    if (errorMessage != null) viewData.put(
                        "errorMessage",
                        errorMessage
                    );

                    return new spark.template.mustache.MustacheTemplateEngine().render(
                        new spark.ModelAndView(
                            viewData,
                            "inscripcion_materias.mustache"
                        )
                    );
                });

        post("/estudiante/inscripcion/cursada", (req, res) -> {
                    String currentUsername = req
                        .session()
                        .attribute("currentUserUsername");
                    String userRole = req.session().attribute("userRole");
                    if (currentUsername == null || !"ESTUDIANTE".equals(userRole)) {
                        res.status(403);
                        return "No autorizado";
                    }

                    User u = User.findFirst("nombre_usuario = ?", currentUsername);
                    Student s = Student.findById(u.getId());
                    int alumnoId = s.getInteger("usuario_id");

                    String materiaCodigoStr = req.queryParams("materia_codigo");
                    if (materiaCodigoStr == null || materiaCodigoStr.isEmpty()) {
                        res.redirect(
                            "/estudiante/inscripcion/cursada?error=Materia+no+especificada"
                        );
                        return null;
                    }
                    int materiaCodigo = Integer.parseInt(materiaCodigoStr);
                    Materia materiaObj = Materia.findById(materiaCodigo);

                    if (
                        materiaObj == null ||
                        materiaObj.getInteger("plan_estudio_id") !=
                            s.getInteger("plan_estudio_id")
                    ) {
                        res.redirect(
                            "/estudiante/inscripcion/cursada?error=Materia+no+valida"
                        );
                        return null;
                    }

                    // 1. Validar que no exista en Estado_Academico
                    List<Map> estadoRows = org.javalite.activejdbc.Base.findAll(
                        "SELECT id FROM Estado_Academico WHERE usuario_id = ? AND materia_codigo = ?",
                        alumnoId,
                        materiaCodigo
                    );
                    if (!estadoRows.isEmpty()) {
                        res.redirect(
                            "/estudiante/inscripcion/cursada?error=Ya+te+encuentras+inscripto+en+esta+materia"
                        );
                        return null;
                    }

                    // 2. Re-validar Correlatividades tipo 'CURSAR'
                    List<Map> requisitos = org.javalite.activejdbc.Base.findAll(
                        "SELECT materia_correlativa_codigo, condicion FROM Correlatividad WHERE materia_codigo = ? AND tipo_requisito = 'CURSAR'",
                        materiaCodigo
                    );

                    for (Map reqItem : requisitos) {
                        int reqCodigo = (
                            (Number) reqItem.get("materia_correlativa_codigo")
                        ).intValue();
                        String condicionRequerida = (String) reqItem.get("condicion");

                        List<Map> estadoReqRows = org.javalite.activejdbc.Base.findAll(
                            "SELECT estado FROM Estado_Academico WHERE usuario_id = ? AND materia_codigo = ?",
                            alumnoId,
                            reqCodigo
                        );

                        if (estadoReqRows.isEmpty()) {
                            res.redirect(
                                "/estudiante/inscripcion/cursada?error=No+cumples+las+correlatividades"
                            );
                            return null;
                        }

                        String estadoActual = (String) estadoReqRows
                            .get(0)
                            .get("estado");

                        if (
                            "APROBADA".equals(condicionRequerida) &&
                            !"APROBADO".equals(estadoActual)
                        ) {
                            res.redirect(
                                "/estudiante/inscripcion/cursada?error=No+cumples+las+correlatividades"
                            );
                            return null;
                        }
                        if (
                            "REGULAR".equals(condicionRequerida) &&
                            (!"REGULAR".equals(estadoActual) &&
                                !"APROBADO".equals(estadoActual))
                        ) {
                            res.redirect(
                                "/estudiante/inscripcion/cursada?error=No+cumples+las+correlatividades"
                            );
                            return null;
                        }
                    }

                    // 3. Persistir en Estado_Academico
                    try {
                        org.javalite.activejdbc.Base.exec(
                            "INSERT INTO Estado_Academico (usuario_id, materia_codigo, estado) VALUES (?, ?, ?)",
                            alumnoId,
                            materiaCodigo,
                            "INSCRIPTO"
                        );
                        res.redirect(
                            "/estudiante/inscripcion/cursada?message=Inscripcion+exitosa"
                        );
                        return null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        res.redirect(
                            "/estudiante/inscripcion/cursada?error=Ocurrio+un+error+al+inscribirse"
                        );
                        return null;
                    }
                });

        post("/estudiante/inscripcion-parcial", (req, res) -> {
                    String username = req.session().attribute("currentUserUsername");
                    String role = req.session().attribute("userRole");
                    if (username == null || !"ESTUDIANTE".equals(role)) {
                        res.status(403);
                        return "No autorizado";
                    }

                    List<Map> studentRows = Base.findAll(
                        "SELECT s.usuario_id FROM student s JOIN users u ON u.id = s.usuario_id WHERE u.nombre_usuario = ?",
                        username
                    );
                    if (studentRows.isEmpty()) {
                        res.redirect("/dashboard");
                        return null;
                    }
                    int alumnoId = (
                        (Number) studentRows.get(0).get("usuario_id")
                    ).intValue();
                    String anuncioIdStr = req.queryParams("anuncio_id");

                    if (anuncioIdStr == null || anuncioIdStr.isEmpty()) {
                        res.redirect(
                            "/estudiante/aula-virtual?error=" +
                                URLEncoder.encode(
                                    "Parcial no especificado.",
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                        return null;
                    }

                    try {
                        int anuncioId = Integer.parseInt(anuncioIdStr);
                        // Verificar que el anuncio sea de tipo EXAMEN
                        List<Map> anuncioRows = Base.findAll(
                            "SELECT tipo FROM Anuncio WHERE id = ?",
                            anuncioId
                        );
                        if (
                            anuncioRows.isEmpty() ||
                            !"EXAMEN".equals(anuncioRows.get(0).get("tipo"))
                        ) {
                            res.redirect(
                                "/estudiante/aula-virtual?error=" +
                                    URLEncoder.encode(
                                        "El anuncio no corresponde a un parcial.",
                                        StandardCharsets.UTF_8.toString()
                                    )
                            );
                            return null;
                        }
                        Base.exec(
                            "INSERT INTO Inscripcion_Parcial (usuario_id, anuncio_id) VALUES (?, ?)",
                            alumnoId,
                            anuncioId
                        );
                        res.redirect(
                            "/estudiante/aula-virtual?success=" +
                                URLEncoder.encode(
                                    "Asistencia confirmada al parcial.",
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                    } catch (Exception e) {
                        res.redirect(
                            "/estudiante/aula-virtual?error=" +
                                URLEncoder.encode(
                                    "Ya confirmaste asistencia a este parcial.",
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                    }
                    return null;
                });

        get("/estudiante/edit/:id", (req, res) -> {
                Boolean loggedIn = req.session().attribute("loggedIn");
                String  userRole = req.session().attribute("userRole");
                if (!Boolean.TRUE.equals(loggedIn)) { res.redirect("/login"); return null; }
                if (!"ADMIN".equals(userRole) && !"SECRETARIA".equals(userRole)) {
                    res.status(403);
                    Map<String, Object> em = new HashMap<>();
                    em.put("errorMessage", "Acceso denegado.");
                    return new ModelAndView(em, "error.mustache");
                }
    
                int estudianteId = Integer.parseInt(req.params("id"));
                List<Map> rows = Base.findAll(
                    "SELECT u.id, u.nombre, u.apellido, u.dni, u.direccion, u.telefono, " +
                    "       u.nombre_usuario, s.legajo, s.tipo_estudiante " +
                    "FROM users u JOIN student s ON s.usuario_id = u.id " +
                    "WHERE u.id = ? AND u.nivel_acceso = 'ESTUDIANTE'",
                    estudianteId
                );
                if (rows.isEmpty()) {
                    res.redirect("/configuracion?error=" + URLEncoder.encode(
                        "Estudiante no encontrado.", StandardCharsets.UTF_8.toString()));
                    return null;
                }
                Map e = rows.get(0);
                String tipo = (String) e.get("tipo_estudiante");
    
                Map<String, Object> model = new HashMap<>();
                model.put("id",             ((Number) e.get("id")).intValue());
                model.put("nombre",         e.get("nombre"));
                model.put("apellido",       e.get("apellido"));
                model.put("dni",            e.get("dni"));
                model.put("direccion",      e.get("direccion") != null ? e.get("direccion") : "");
                model.put("telefono",       e.get("telefono")  != null ? e.get("telefono")  : "");
                model.put("nombre_usuario", e.get("nombre_usuario"));
                model.put("legajo",         e.get("legajo"));
                // Flags para pre-seleccionar el <select> de tipo_estudiante en Mustache
                model.put("esRegular",      "REGULAR".equals(tipo));
                model.put("esVocacional",   "VOCACIONAL".equals(tipo));
                model.put("esIntercambio",  "INTERCAMBIO".equals(tipo));
    
                String err = req.queryParams("error");
                if (err != null && !err.isEmpty()) model.put("errorMessage", err);
    
                return new ModelAndView(model, "estudiante_edit_form.mustache");
            }, new MustacheTemplateEngine());

        post("/estudiante/edit/:id", (req, res) -> {
                Boolean loggedIn = req.session().attribute("loggedIn");
                String  userRole = req.session().attribute("userRole");
                if (!Boolean.TRUE.equals(loggedIn)) { res.redirect("/login"); return null; }
                if (!"ADMIN".equals(userRole) && !"SECRETARIA".equals(userRole)) {
                    res.status(403); return "Acceso denegado.";
                }
    
                int estudianteId = Integer.parseInt(req.params("id"));
    
                String nombre        = req.queryParams("nombre");
                String apellido      = req.queryParams("apellido");
                String dni           = req.queryParams("dni");
                String direccion     = req.queryParams("direccion");
                String telefono      = req.queryParams("telefono");
                String tipoEstudiante = req.queryParams("tipo_estudiante");
    
                if (nombre == null || nombre.isBlank() || apellido == null || apellido.isBlank()) {
                    res.redirect("/estudiante/edit/" + estudianteId + "?error=" + URLEncoder.encode(
                        "Nombre y apellido son obligatorios.", StandardCharsets.UTF_8.toString()));
                    return null;
                }
                List<String> tiposValidos = Arrays.asList("REGULAR", "VOCACIONAL", "INTERCAMBIO");
                if (tipoEstudiante == null || !tiposValidos.contains(tipoEstudiante)) {
                    res.redirect("/estudiante/edit/" + estudianteId + "?error=" + URLEncoder.encode(
                        "Tipo de estudiante inválido.", StandardCharsets.UTF_8.toString()));
                    return null;
                }
    
                try {
                    Base.openTransaction();
                    Base.exec(
                        "UPDATE users SET nombre = ?, apellido = ?, dni = ?, " +
                        "                 direccion = ?, telefono = ? WHERE id = ?",
                        nombre.trim(), apellido.trim(), dni != null ? dni.trim() : "",
                        direccion, telefono, estudianteId
                    );
                    Base.exec(
                        "UPDATE student SET tipo_estudiante = ? WHERE usuario_id = ?",
                        tipoEstudiante, estudianteId
                    );
                    Base.commitTransaction();
                    res.redirect("/configuracion?message=" + URLEncoder.encode(
                        "Estudiante actualizado correctamente.", StandardCharsets.UTF_8.toString()) + "#estudiantes");
                } catch (Exception e) {
                    Base.rollbackTransaction();
                    e.printStackTrace();
                    res.redirect("/estudiante/edit/" + estudianteId + "?error=" + URLEncoder.encode(
                        "Error al guardar: " + e.getMessage(), StandardCharsets.UTF_8.toString()));
                }
                return null;
            });

    }
}
