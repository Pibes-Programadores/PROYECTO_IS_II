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

public class AulaVirtualController {
    public static void register() {
        get(
                    "/estudiante/aula-virtual",
                    (req, res) -> {
                        if (
                            req.session().attribute("userRole") == null ||
                            !req.session().attribute("userRole").equals("ESTUDIANTE")
                        ) {
                            res.redirect("/login");
                            return null;
                        }

                        int alumnoId = req.session().attribute("userId");
                        Map<String, Object> model = new HashMap<>();

                        // Buscar materias válidas para el alumno (incluimos PROMOCION por si acaso ya se cargaron así)
                        List<Map> materias = Base.findAll(
                            "SELECT m.codigo, m.nombre, ea.estado " +
                                "FROM Estado_Academico ea " +
                                "JOIN Materia m ON ea.materia_codigo = m.codigo " +
                                "WHERE ea.usuario_id = ? AND ea.estado IN ('INSCRIPTO', 'REGULAR', 'APROBADO', 'PROMOCION')",
                            alumnoId
                        );

                        model.put("materias", materias);
                        return new ModelAndView(
                            model,
                            "aula_virtual_selector.mustache"
                        );
                    },
                    new MustacheTemplateEngine()
                );

        get(
                    "/estudiante/aula-virtual/materia",
                    (req, res) -> {
                        if (
                            req.session().attribute("userRole") == null ||
                            !req.session().attribute("userRole").equals("ESTUDIANTE")
                        ) {
                            res.redirect("/login");
                            return null;
                        }

                        int alumnoId = req.session().attribute("userId");
                        String materiaCodigoStr = req.queryParams("materia_codigo");

                        if (materiaCodigoStr == null || materiaCodigoStr.isBlank()) {
                            res.redirect("/estudiante/aula-virtual");
                            return null;
                        }

                        int materiaCodigo = Integer.parseInt(materiaCodigoStr);

                        // 1. Validar Seguridad: ¿El estudiante está realmente inscripto en esta materia?
                        List<Map> estadoAcademico = Base.findAll(
                            "SELECT ea.estado, m.nombre " +
                                "FROM Estado_Academico ea " +
                                "JOIN Materia m ON ea.materia_codigo = m.codigo " +
                                "WHERE ea.usuario_id = ? AND ea.materia_codigo = ?",
                            alumnoId,
                            materiaCodigo
                        );

                        if (estadoAcademico.isEmpty()) {
                            res.redirect(
                                "/dashboard?error=No+tienes+acceso+a+esta+materia"
                            );
                            return null;
                        }

                        String estadoAlumno = (String) estadoAcademico
                            .get(0)
                            .get("estado");
                        String materiaNombre = (String) estadoAcademico
                            .get(0)
                            .get("nombre");

                        Map<String, Object> model = new HashMap<>();
                        model.put("materia_codigo", materiaCodigo);
                        model.put("materia_nombre", materiaNombre);
                        model.put("estado_alumno", estadoAlumno);

                        // 2. Buscar el ID del periodo activo de la materia para cruzar anuncios y notas
                        List<Map> periodos = Base.findAll(
                            "SELECT id FROM Materia_Periodo WHERE materia_codigo = ? ORDER BY anio_academico DESC LIMIT 1",
                            materiaCodigo
                        );

                        if (!periodos.isEmpty()) {
                            int materiaPeriodoId = (
                                (Number) periodos.get(0).get("id")
                            ).intValue();

                            // A. Buscar Nota
                            List<Map> notas = Base.findAll(
                                "SELECT valor FROM Nota WHERE student_id = ? AND materia_periodo_id = ?",
                                alumnoId,
                                materiaPeriodoId
                            );
                            if (notas.isEmpty()) {
                                model.put("sinNota", true);
                            } else {
                                model.put("sinNota", false);
                                model.put("nota_valor", notas.get(0).get("valor"));
                            }

                            // B. Buscar Docentes y Aulas
                            List<Map> docentesAulas = Base.findAll(
                                "SELECT u.nombre as docente_nombre, u.apellido as docente_apellido, a.aula " +
                                    "FROM Aula_Asignacion a " +
                                    "JOIN users u ON a.teacher_id = u.id " +
                                    "WHERE a.materia_periodo_id = ?",
                                materiaPeriodoId
                            );
                            model.put("docentes_aulas", docentesAulas);

                            // C. Buscar Anuncios
                            List<Map> anuncios = Base.findAll(
                                "SELECT an.titulo, an.contenido, an.tipo, an.fecha_examen, " +
                                    "CONCAT(u.nombre, ' ', u.apellido) as autor, " +
                                    "DATE_FORMAT(an.fecha_creacion, '%d/%m/%Y %H:%i') as fecha " +
                                    "FROM Anuncio an " +
                                    "JOIN users u ON an.teacher_id = u.id " +
                                    "WHERE an.materia_periodo_id = ? " +
                                    "ORDER BY an.fecha_creacion DESC",
                                materiaPeriodoId
                            );

                            List<Map<String, Object>> anunciosProcesados =
                                new java.util.ArrayList<>();
                            for (Map an : anuncios) {
                                Map<String, Object> anDto = new HashMap<>(an);
                                anDto.put("esExamen", "EXAMEN".equals(an.get("tipo")));
                                anunciosProcesados.add(anDto);
                            }
                            model.put("anuncios", anunciosProcesados);
                        } else {
                            // Si la materia no tiene periodos configurados
                            model.put("sinNota", true);
                            model.put("docentes_aulas", new java.util.ArrayList<>());
                            model.put("anuncios", new java.util.ArrayList<>());
                        }

                        return new ModelAndView(model, "aula_virtual_tablero.mustache");
                    },
                    new MustacheTemplateEngine()
                );

        get(
                    "/estudiante/aula-virtual",
                    (req, res) -> {
                        String username = req
                            .session()
                            .attribute("currentUserUsername");
                        String role = req.session().attribute("userRole");
                        if (username == null || !"ESTUDIANTE".equals(role)) {
                            res.redirect("/login");
                            return null;
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

                        // Materias en las que el alumno está INSCRIPTO (cursando)
                        List<Map> materiasInscriptas = Base.findAll(
                            "SELECT ea.materia_codigo, m.nombre FROM Estado_Academico ea " +
                                "JOIN Materia m ON m.codigo = ea.materia_codigo " +
                                "WHERE ea.usuario_id = ? AND ea.estado = 'INSCRIPTO'",
                            alumnoId
                        );

                        List<Map<String, Object>> materiasConAnuncios =
                            new ArrayList<>();
                        for (Map mat : materiasInscriptas) {
                            int codigo = (
                                (Number) mat.get("materia_codigo")
                            ).intValue();

                            // Buscar el período vigente de esta materia
                            List<Map> periodoRows = Base.findAll(
                                "SELECT id FROM Materia_Periodo WHERE materia_codigo = ? LIMIT 1",
                                codigo
                            );
                            if (periodoRows.isEmpty()) continue;
                            int periodoId = (
                                (Number) periodoRows.get(0).get("id")
                            ).intValue();

                            // Anuncios del período
                            List<Map> anunciosDB = Base.findAll(
                                "SELECT a.id, a.tipo, a.titulo, a.contenido, a.fecha_examen " +
                                    "FROM Anuncio a WHERE a.materia_periodo_id = ? ORDER BY a.fecha_creacion DESC",
                                periodoId
                            );

                            List<Map<String, Object>> anunciosList = new ArrayList<>();
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

                                // Verificar si el alumno ya se inscribió a este parcial
                                if ("EXAMEN".equals(a.get("tipo"))) {
                                    int anuncioId = ((Number) a.get("id")).intValue();
                                    List<Map> yaInscripto = Base.findAll(
                                        "SELECT id FROM Inscripcion_Parcial WHERE usuario_id = ? AND anuncio_id = ?",
                                        alumnoId,
                                        anuncioId
                                    );
                                    anuncioMap.put(
                                        "yaInscripto",
                                        !yaInscripto.isEmpty()
                                    );
                                }
                                anunciosList.add(anuncioMap);
                            }

                            Map<String, Object> materiaMap = new HashMap<>();
                            materiaMap.put("codigo", codigo);
                            materiaMap.put("nombre", mat.get("nombre"));
                            materiaMap.put("anuncios", anunciosList);
                            materiasConAnuncios.add(materiaMap);
                        }

                        Map<String, Object> model = new HashMap<>();
                        model.put("materias", materiasConAnuncios);
                        String success = req.queryParams("success");
                        String error = req.queryParams("error");
                        if (success != null) model.put("successMessage", success);
                        if (error != null) model.put("errorMessage", error);

                        return new ModelAndView(model, "aula_virtual.mustache");
                    },
                    new MustacheTemplateEngine()
                );

    }
}
