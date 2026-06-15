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

public class NotaController {
    public static void register() {
        post("/docente/materia/:materiaId/nota", (req, res) -> {
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

                    String studentIdParam = req.queryParams("student_id");
                    String valorParam = req.queryParams("valor");

                    if (
                        studentIdParam == null ||
                        studentIdParam.isEmpty() ||
                        valorParam == null ||
                        valorParam.isEmpty()
                    ) {
                        res.redirect(
                            "/docente/materia/" +
                                materiaId +
                                "?error=" +
                                URLEncoder.encode(
                                    "Debés seleccionar un alumno e ingresar una nota.",
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                        return "";
                    }

                    String estadoCursada = req.queryParams("estado_cursada");
                    if (
                        estadoCursada == null || estadoCursada.isEmpty()
                    ) estadoCursada = "REGULAR";

                    try {
                        int studentId = Integer.parseInt(studentIdParam);
                        double valor = Double.parseDouble(valorParam);

                        if (valor < 0 || valor > 10) throw new IllegalArgumentException(
                            "La nota debe estar entre 0 y 10."
                        );

                        // Validar que el alumno esté inscripto o regular en la materia
                        List<Map> estadoRows = Base.findAll(
                            "SELECT estado FROM Estado_Academico WHERE usuario_id = ? AND materia_codigo = ?",
                            studentId,
                            materiaId
                        );
                        Map estadoRow = estadoRows.isEmpty() ? null : estadoRows.get(0);
                        if (
                            estadoRow == null ||
                            (!"INSCRIPTO".equals(estadoRow.get("estado")) &&
                                !"REGULAR".equals(estadoRow.get("estado")))
                        ) {
                            throw new SecurityException(
                                "El alumno no está inscripto o no es válido para recibir nota en esta materia."
                            );
                        }

                        // Guardar la nota con instancia CURSADA
                        Nota nota = new Nota();
                        nota.set("materia_periodo_id", mp.getId());
                        nota.set("student_id", studentId);
                        nota.set("teacher_id", teacherId);
                        nota.set("valor", valor);
                        nota.set("instancia", "CURSADA");
                        nota.saveIt();

                        // Actualizar Estado_Academico: si PROMOCION, guardar como APROBADO en el motor de correlatividades
                        String estadoFinal = "PROMOCION".equals(estadoCursada)
                            ? "PROMOCION"
                            : estadoCursada;
                        Base.exec(
                            "INSERT INTO Estado_Academico (usuario_id, materia_codigo, estado) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE estado = VALUES(estado)",
                            studentId,
                            materiaId,
                            estadoFinal
                        );

                        res.redirect(
                            "/docente/materia/" +
                                materiaId +
                                "?message=" +
                                URLEncoder.encode(
                                    "Nota y estado de cursada registrados correctamente.",
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
                                    "Error al registrar la nota: " + e.getMessage(),
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                    }
                    return "";
                });

        get(
                    "/docente/notas-finales",
                    (req, res) -> {
                        String role = req.session().attribute("userRole");
                        if (role == null || !"DOCENTE".equals(role)) {
                            res.redirect("/dashboard");
                            return null;
                        }
                        Integer userId = req.session().attribute("userId");
                        Teacher teacher = Teacher.findFirst("usuario_id = ?", userId);
                        if (teacher == null) {
                            res.redirect("/dashboard");
                            return null;
                        }
                        int teacherId = teacher.getInteger("usuario_id");

                        // Mesas de materias que el docente tiene asignadas
                        List<Map> mesas = Base.findAll(
                            "SELECT me.id, me.fecha, m.nombre AS nombreMateria, me.materia_codigo " +
                                "FROM mesas_examen me " +
                                "JOIN Materia m ON m.codigo = me.materia_codigo " +
                                "JOIN Docente_Materia dm ON dm.materia_id = me.materia_codigo " +
                                "WHERE dm.teacher_id = ? " +
                                "ORDER BY me.fecha DESC",
                            teacherId
                        );

                        List<Map<String, Object>> mesasList = new ArrayList<>();
                        for (Map m : mesas) {
                            Map<String, Object> mm = new HashMap<>();
                            mm.put("id", ((Number) m.get("id")).intValue());
                            mm.put("fecha", m.get("fecha"));
                            mm.put("nombreMateria", m.get("nombreMateria"));
                            mm.put("materiaCodigo", m.get("materia_codigo"));
                            mesasList.add(mm);
                        }

                        Map<String, Object> model = new HashMap<>();
                        model.put("mesas", mesasList);
                        String success = req.queryParams("success");
                        String error = req.queryParams("error");
                        if (success != null) model.put("successMessage", success);
                        if (error != null) model.put("errorMessage", error);

                        return new ModelAndView(model, "carga_finales.mustache");
                    },
                    new MustacheTemplateEngine()
                );

        get(
                    "/docente/notas-finales/:mesaId",
                    (req, res) -> {
                        String role = req.session().attribute("userRole");
                        if (role == null || !"DOCENTE".equals(role)) {
                            res.redirect("/dashboard");
                            return null;
                        }
                        Integer userId = req.session().attribute("userId");
                        Teacher teacher = Teacher.findFirst("usuario_id = ?", userId);
                        if (teacher == null) {
                            res.redirect("/dashboard");
                            return null;
                        }
                        int teacherId = teacher.getInteger("usuario_id");
                        int mesaId = Integer.parseInt(req.params("mesaId"));

                        // Verificar que la mesa pertenezca a una materia del docente
                        List<Map> mesaRows = Base.findAll(
                            "SELECT me.id, me.fecha, me.materia_codigo, m.nombre AS nombreMateria " +
                                "FROM mesas_examen me JOIN Materia m ON m.codigo = me.materia_codigo " +
                                "JOIN Docente_Materia dm ON dm.materia_id = me.materia_codigo " +
                                "WHERE me.id = ? AND dm.teacher_id = ?",
                            mesaId,
                            teacherId
                        );
                        if (mesaRows.isEmpty()) {
                            res.redirect(
                                "/docente/notas-finales?error=" +
                                    URLEncoder.encode(
                                        "No tenés acceso a esa mesa.",
                                        StandardCharsets.UTF_8.toString()
                                    )
                            );
                            return null;
                        }
                        Map mesa = mesaRows.get(0);

                        // Solo los inscriptos a esta mesa específica
                        List<Map> inscriptosDB = Base.findAll(
                            "SELECT ie.usuario_id, u.nombre, u.apellido, s.legajo " +
                                "FROM inscripciones_examen ie " +
                                "JOIN users u ON u.id = ie.usuario_id " +
                                "JOIN student s ON s.usuario_id = ie.usuario_id " +
                                "WHERE ie.mesa_id = ? " +
                                "ORDER BY u.apellido ASC",
                            mesaId
                        );

                        List<Map<String, Object>> inscriptosList = new ArrayList<>();
                        for (Map i : inscriptosDB) {
                            Map<String, Object> im = new HashMap<>();
                            im.put(
                                "usuarioId",
                                ((Number) i.get("usuario_id")).intValue()
                            );
                            im.put("nombre", i.get("nombre") + " " + i.get("apellido"));
                            im.put("legajo", i.get("legajo"));
                            inscriptosList.add(im);
                        }

                        Map<String, Object> model = new HashMap<>();
                        model.put("mesaId", mesaId);
                        model.put("fecha", mesa.get("fecha"));
                        model.put("nombreMateria", mesa.get("nombreMateria"));
                        model.put("materiaCodigo", mesa.get("materia_codigo"));
                        model.put("inscriptos", inscriptosList);
                        String success = req.queryParams("success");
                        String error = req.queryParams("error");
                        if (success != null) model.put("successMessage", success);
                        if (error != null) model.put("errorMessage", error);

                        return new ModelAndView(model, "carga_finales_acta.mustache");
                    },
                    new MustacheTemplateEngine()
                );

        post("/docente/notas-finales/:mesaId", (req, res) -> {
                    String role = req.session().attribute("userRole");
                    if (role == null || !"DOCENTE".equals(role)) {
                        res.redirect("/dashboard");
                        return null;
                    }
                    Integer userId = req.session().attribute("userId");
                    Teacher teacher = Teacher.findFirst("usuario_id = ?", userId);
                    if (teacher == null) {
                        res.redirect("/dashboard");
                        return null;
                    }
                    int teacherId = teacher.getInteger("usuario_id");
                    int mesaId = Integer.parseInt(req.params("mesaId"));

                    String studentIdStr = req.queryParams("student_id");
                    String valorStr = req.queryParams("valor");

                    if (
                        studentIdStr == null ||
                        valorStr == null ||
                        studentIdStr.isEmpty() ||
                        valorStr.isEmpty()
                    ) {
                        res.redirect(
                            "/docente/notas-finales/" +
                                mesaId +
                                "?error=" +
                                URLEncoder.encode(
                                    "Datos incompletos.",
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                        return null;
                    }

                    try {
                        int studentId = Integer.parseInt(studentIdStr);
                        double valor = Double.parseDouble(valorStr);

                        if (valor < 0 || valor > 10) throw new IllegalArgumentException(
                            "Nota fuera de rango."
                        );

                        // Obtener el período de la materia desde la mesa
                        List<Map> mesaRows = Base.findAll(
                            "SELECT me.materia_codigo FROM mesas_examen me " +
                                "JOIN Docente_Materia dm ON dm.materia_id = me.materia_codigo " +
                                "WHERE me.id = ? AND dm.teacher_id = ?",
                            mesaId,
                            teacherId
                        );
                        if (mesaRows.isEmpty()) throw new IllegalStateException(
                            "Mesa no autorizada."
                        );
                        int materiaCodigo = (
                            (Number) mesaRows.get(0).get("materia_codigo")
                        ).intValue();

                        List<Map> periodoRows = Base.findAll(
                            "SELECT id FROM Materia_Periodo WHERE materia_codigo = ? LIMIT 1",
                            materiaCodigo
                        );
                        if (periodoRows.isEmpty()) throw new IllegalStateException(
                            "No hay período activo."
                        );
                        int periodoId = (
                            (Number) periodoRows.get(0).get("id")
                        ).intValue();

                        // Guardar la nota con instancia FINAL
                        Nota nota = new Nota();
                        nota.set("materia_periodo_id", periodoId);
                        nota.set("student_id", studentId);
                        nota.set("teacher_id", teacherId);
                        nota.set("valor", valor);
                        nota.set("instancia", "FINAL");
                        nota.saveIt();

                        // Determinar nuevo estado: aprueba con >= 4
                        String nuevoEstado = (valor >= 4.0) ? "APROBADO" : "REGULAR";

                        Base.exec(
                            "INSERT INTO Estado_Academico (usuario_id, materia_codigo, estado) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE estado = VALUES(estado)",
                            studentId,
                            materiaCodigo,
                            nuevoEstado
                        );

                        res.redirect(
                            "/docente/notas-finales/" +
                                mesaId +
                                "?success=" +
                                URLEncoder.encode(
                                    "Calificación registrada.",
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        res.redirect(
                            "/docente/notas-finales/" +
                                mesaId +
                                "?error=" +
                                URLEncoder.encode(
                                    "Error: " + e.getMessage(),
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                    }
                    return null;
                });

    }
}
