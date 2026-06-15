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

public class ExamenController {
    public static void register(ObjectMapper objectMapper) {
        get("/estudiante/inscripcion/examen", (req, res) -> {
                    String currentUsernameEx = req.session().attribute("currentUserUsername");
                    String userRoleEx = req.session().attribute("userRole");
            
                    if (currentUsernameEx == null || !"ESTUDIANTE".equals(userRoleEx)) {
                        res.redirect("/login");
                        return null;
                    }
            
                    List<Map> studentRowsEx = Base.findAll(
                        "SELECT s.usuario_id FROM student s " +
                        "JOIN users u ON u.id = s.usuario_id WHERE u.nombre_usuario = ?",
                        currentUsernameEx
                    );
            
                    if (studentRowsEx.isEmpty()) {
                        res.redirect("/dashboard?error=No+se+encontro+el+estudiante");
                        return null;
                    }
            
                    int alumnoId = ((Number) studentRowsEx.get(0).get("usuario_id")).intValue();
                    List<Map<String, Object>> mesasHabilitadas = new ArrayList<>();

                    // 1. Obtener todas las mesas de examen via SQL directo
                    List<Map> todasLasMesas = Base.findAll("SELECT id, materia_codigo, fecha FROM mesas_examen");

                    for (Map mesa : todasLasMesas) {
                        int materiaCodigo = ((Number) mesa.get("materia_codigo")).intValue();

                        // 2. Consultar el Estado Académico del alumno para esta materia
                        List<Map> estadoRows = Base.findAll(
                            "SELECT estado FROM Estado_Academico WHERE usuario_id = ? AND materia_codigo = ?",
                            alumnoId, materiaCodigo
                        );

                        if (!estadoRows.isEmpty()) {
                            String estado = (String) estadoRows.get(0).get("estado");

                            // REGLA DE NEGOCIO PRINCIPAL: Solo rinden quienes estén REGULAR o LIBRE
                            if ("REGULAR".equals(estado) || "LIBRE".equals(estado)) {
                        
                                // 3. Validar correlatividades de tipo 'RENDIR'
                                List<Map> requisitos = Base.findAll(
                                    "SELECT materia_correlativa_codigo FROM Correlatividad WHERE materia_codigo = ? AND tipo_requisito = 'RENDIR'",
                                    materiaCodigo
                                );

                                boolean cumpleCorrelatividades = true;
                                for (Map reqItem : requisitos) {
                                    int reqCodigo = ((Number) reqItem.get("materia_correlativa_codigo")).intValue();

                                    List<Map> estadoReqRows = Base.findAll(
                                        "SELECT estado FROM Estado_Academico WHERE usuario_id = ? AND materia_codigo = ?",
                                        alumnoId, reqCodigo
                                    );

                                    // Si le falta la correlativa o no la tiene APROBADA, no puede rendir la mesa
                                    if (estadoReqRows.isEmpty() || !"APROBADO".equals(estadoReqRows.get(0).get("estado"))) {
                                        cumpleCorrelatividades = false;
                                        break;
                                    }
                                }

                                if (cumpleCorrelatividades) {
                                    Map<String, Object> mesaData = new HashMap<>();
                                    mesaData.put("id", ((Number) mesa.get("id")).intValue());
                                    mesaData.put("materia_codigo", materiaCodigo);
                                    mesaData.put("fecha", mesa.get("fecha"));
                            
                                    // Agregamos el nombre de la materia para que no salga solo el código en la vista
                                    List<Map> matNombreRow = Base.findAll("SELECT nombre FROM Materia WHERE codigo = ?", materiaCodigo);
                                    if (!matNombreRow.isEmpty()) {
                                        mesaData.put("materia_nombre", matNombreRow.get(0).get("nombre"));
                                    } else {
                                        mesaData.put("materia_nombre", "Materia " + materiaCodigo);
                                    }

                                    // Comprobamos si ya está inscripto en esta mesa específica
                                    List<Map> yaInscriptoRow = Base.findAll(
                                        "SELECT 1 FROM inscripciones_examen WHERE usuario_id = ? AND mesa_id = ?",
                                        alumnoId, ((Number) mesa.get("id")).intValue()
                                    );
                                    mesaData.put("yaInscripto", !yaInscriptoRow.isEmpty());

                                    mesasHabilitadas.add(mesaData);
                                }
                            }
                        }
                    }

                    Map<String, Object> viewData = new HashMap<>();
                    viewData.put("mesas", mesasHabilitadas);
                    viewData.put("username", currentUsernameEx);

                    String successMsgEx = req.queryParams("success");
                    String errorMsgEx = req.queryParams("error");
                    if (successMsgEx != null) viewData.put("successMessage", URLDecoder.decode(successMsgEx, StandardCharsets.UTF_8));
                    if (errorMsgEx != null) viewData.put("errorMessage", URLDecoder.decode(errorMsgEx, StandardCharsets.UTF_8));

                    return new ModelAndView(viewData, "inscripcion_examenes.mustache");
                }, new MustacheTemplateEngine());

        post("/estudiante/inscripcion/examen", (req, res) -> {
                    String currentUsernamePost = req
                        .session()
                        .attribute("currentUserUsername");
                    String userRolePost = req.session().attribute("userRole");
                    if (
                        currentUsernamePost == null ||
                        !"ESTUDIANTE".equals(userRolePost)
                    ) {
                        res.status(403);
                        return "No autorizado";
                    }
                    List<Map> studentRowsPost = Base.findAll(
                        "SELECT s.usuario_id FROM student s " +
                            "JOIN users u ON u.id = s.usuario_id WHERE u.nombre_usuario = ?",
                        currentUsernamePost
                    );
                    if (studentRowsPost.isEmpty()) {
                        res.redirect("/dashboard?error=No+se+encontro+el+estudiante");
                        return null;
                    }
                    int alumnoId = (
                        (Number) studentRowsPost.get(0).get("usuario_id")
                    ).intValue();
                    String mesaIdStr = req.queryParams("mesa_id");

                    if (mesaIdStr == null || mesaIdStr.isEmpty()) {
                        res.redirect(
                            "/estudiante/inscripcion/examen?error=Mesa no especificada"
                        );
                        return null;
                    }

                    int mesaId = Integer.parseInt(mesaIdStr);
                    List<Map> mesaRows = Base.findAll(
                        "SELECT id, materia_codigo FROM Mesa_Examen WHERE id = ?",
                        mesaId
                    );

                    // VALIDACIONES DE SEGURIDAD
                    if (mesaRows.isEmpty()) {
                        res.redirect(
                            "/estudiante/inscripcion/examen?error=La+mesa+seleccionada+no+existe"
                        );
                        return null;
                    }

                    int materiaCodigo2 = (
                        (Number) mesaRows.get(0).get("materia_codigo")
                    ).intValue();

                    // 1. Re-verificar Estado Académico
                    List<Map> estadoRows = Base.findAll(
                        "SELECT estado FROM Estado_Academico WHERE usuario_id = ? AND materia_codigo = ?",
                        alumnoId,
                        materiaCodigo2
                    );

                    if (estadoRows.isEmpty()) {
                        res.redirect(
                            "/estudiante/inscripcion/examen?error=No+posees+estado+academico+en+esta+materia"
                        );
                        return null;
                    }

                    String estado = (String) estadoRows.get(0).get("estado");
                    if (!"REGULAR".equals(estado) && !"LIBRE".equals(estado)) {
                        res.redirect(
                            "/estudiante/inscripcion/examen?error=Tu+condicion+academica+no+te+permite+rendir+esta+materia"
                        );
                        return null;
                    }

                    // 2. Re-verificar Correlatividades de tipo 'RENDIR'
                    List<Map> requisitosPost = Base.findAll(
                        "SELECT materia_correlativa_codigo FROM Correlatividad WHERE materia_codigo = ? AND tipo_requisito = 'RENDIR'",
                        materiaCodigo2
                    );

                    for (Map reqItem : requisitosPost) {
                        int reqCodigo = (
                            (Number) reqItem.get("materia_correlativa_codigo")
                        ).intValue();

                        List<Map> estadoReqRows = Base.findAll(
                            "SELECT estado FROM Estado_Academico WHERE usuario_id = ? AND materia_codigo = ?",
                            alumnoId,
                            reqCodigo
                        );

                        if (
                            estadoReqRows.isEmpty() ||
                            !"APROBADO".equals(estadoReqRows.get(0).get("estado"))
                        ) {
                            res.redirect(
                                "/estudiante/inscripcion/examen?error=No+cumples+con+las+correlatividades+requeridas+para+rendir"
                            );
                            return null;
                        }
                    }

                    // 3. Persistir la inscripción en la base de datos
                    try {
                        Base.exec("INSERT INTO inscripciones_examen (usuario_id, mesa_id) VALUES (?, ?)", alumnoId, mesaId);
                        res.redirect(
                            "/estudiante/inscripcion/examen?success=Te+has+inscripto+a+la+mesa+con+exito"
                        );
                    } catch (Exception e) {
                        res.redirect(
                            "/estudiante/inscripcion/examen?error=Ya+te+encuentras+inscripto+en+esta+mesa+de+examen"
                        );
                    }

                    return null;
                });

    }
}
