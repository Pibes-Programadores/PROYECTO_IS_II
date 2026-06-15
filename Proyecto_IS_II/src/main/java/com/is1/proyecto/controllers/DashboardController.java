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

public class DashboardController {
    public static void register() {
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
                        model.put("isEstudiante", "ESTUDIANTE".equals(userRole));
                        model.put("isSecretaria",        "SECRETARIA".equals(userRole));
                        model.put("isAdminOrSecretaria", "ADMIN".equals(userRole) || "SECRETARIA".equals(userRole));

                        return new ModelAndView(model, "dashboard.mustache");
                    },
                    new MustacheTemplateEngine()
                );

    }
}
