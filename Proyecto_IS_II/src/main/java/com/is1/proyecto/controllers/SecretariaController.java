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

public class SecretariaController {
    public static void register() {
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

                        return new ModelAndView(
                            model,
                            "secretariaAcademica_form.mustache"
                        );
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

    }
}
