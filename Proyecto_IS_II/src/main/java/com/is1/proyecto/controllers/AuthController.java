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

public class AuthController {
    public static void register(ObjectMapper objectMapper) {
        get(
                    "/login",
                    (req, res) -> {
                        Map<String, Object> model = new HashMap<>();
                        String errorMessage = req.queryParams("error");
                        String successMessage = req.queryParams("message");
                        if (errorMessage != null && !errorMessage.isEmpty()) {
                            model.put("errorMessage", errorMessage);
                        }
                        if (successMessage != null && !successMessage.isEmpty()) {
                            model.put("successMessage", successMessage);
                        }
                        return new ModelAndView(model, "login.mustache");
                    },
                    new MustacheTemplateEngine()
                );

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
                );

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
