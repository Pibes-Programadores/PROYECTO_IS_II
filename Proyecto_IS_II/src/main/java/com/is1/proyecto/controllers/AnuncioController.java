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

public class AnuncioController {
    public static void register() {
        post("/docente/materia/:materiaId/anuncio", (req, res) -> {
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

                    // Verificar pertenencia
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

                    // Obtener el MateriaPeriodo vigente
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

                    String tipo = req.queryParams("tipo");
                    String titulo = req.queryParams("titulo");
                    String contenido = req.queryParams("contenido");
                    String fechaExamen = req.queryParams("fecha_examen");

                    try {
                        Anuncio anuncio = new Anuncio();
                        anuncio.set("materia_periodo_id", mp.getId());
                        anuncio.set("teacher_id", teacherId);
                        anuncio.set("tipo", tipo);
                        anuncio.set("titulo", titulo);
                        anuncio.set("contenido", contenido);
                        if (
                            "EXAMEN".equals(tipo) &&
                            fechaExamen != null &&
                            !fechaExamen.isBlank()
                        ) {
                            anuncio.set(
                                "fecha_examen",
                                java.sql.Date.valueOf(fechaExamen)
                            );
                        }
                        anuncio.saveIt();

                        res.redirect(
                            "/docente/materia/" +
                                materiaId +
                                "?message=" +
                                URLEncoder.encode(
                                    "Anuncio publicado correctamente.",
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
                                    "Error al publicar el anuncio: " + e.getMessage(),
                                    StandardCharsets.UTF_8.toString()
                                )
                        );
                    }
                    return "";
                });

    }
}
