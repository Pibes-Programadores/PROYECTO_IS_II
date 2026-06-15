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

public class ApiController {
    public static void register(ObjectMapper objectMapper) {
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
                            item.put("id", m.get("id"));
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

        get("/api/docentes-search", (req, res) -> {
                    res.type("application/json");

                    String carreraIdParam = req.queryParams("carrera_id");
                    String query = req.queryParams("query");

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
                            carreraId,
                            like,
                            like,
                            like
                        );

                        List<Map<String, Object>> resultado = new ArrayList<>();
                        for (Map row : rows) {
                            Map<String, Object> item = new HashMap<>();
                            item.put("id", row.get("id"));
                            // Formato: LEGAJO - APELLIDO, Nombre
                            String label =
                                row.get("legajo") +
                                " — " +
                                row.get("apellido") +
                                ", " +
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

    }
}
