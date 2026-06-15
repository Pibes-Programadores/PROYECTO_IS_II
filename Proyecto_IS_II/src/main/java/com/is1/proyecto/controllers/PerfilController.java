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

public class PerfilController {
    public static void register() {
        get("/perfil", (req, res) -> {
                Boolean loggedIn = req.session().attribute("loggedIn");
                if (!Boolean.TRUE.equals(loggedIn)) {
                    res.redirect("/login");
                    return null;
                }
    
                Object userIdObj = req.session().attribute("userId");
                int    userId    = ((Number) userIdObj).intValue();
                String userRole  = req.session().attribute("userRole");
    
                // Datos base del usuario
                List<Map> userRows = Base.findAll(
                    "SELECT nombre, apellido, dni, direccion, telefono, " +
                    "       nombre_usuario, nivel_acceso, foto_perfil " +
                    "FROM users WHERE id = ?",
                    userId
                );
                if (userRows.isEmpty()) {
                    res.redirect("/login");
                    return null;
                }
                Map user = userRows.get(0);
    
                Map<String, Object> model = new HashMap<>();
                model.put("nombre",         user.get("nombre"));
                model.put("apellido",       user.get("apellido"));
                model.put("dni",            user.get("dni"));
                model.put("direccion",      user.get("direccion") != null ? user.get("direccion") : "—");
                model.put("telefono",       user.get("telefono")  != null ? user.get("telefono")  : "—");
                model.put("nombre_usuario", user.get("nombre_usuario"));
                model.put("nivel_acceso",   user.get("nivel_acceso"));
                String fotoActual = user.get("foto_perfil") != null
                    ? (String) user.get("foto_perfil")
                    : "/img/default-avatar.png";
                model.put("foto_perfil", fotoActual);
    
                // Datos específicos según rol
                if ("DOCENTE".equals(userRole)) {
                    model.put("isDocente", true);
                    List<Map> rows = Base.findAll(
                        "SELECT legajo_docente, cuil, email, especialidad " +
                        "FROM teacher WHERE usuario_id = ?", userId
                    );
                    if (!rows.isEmpty()) {
                        Map t = rows.get(0);
                        model.put("legajo_docente", t.get("legajo_docente"));
                        model.put("cuil",           t.get("cuil"));
                        model.put("email",          t.get("email"));
                        model.put("especialidad",   t.get("especialidad") != null ? t.get("especialidad") : "—");
                    }
    
                } else if ("ESTUDIANTE".equals(userRole)) {
                    model.put("isEstudiante", true);
                    List<Map> rows = Base.findAll(
                        "SELECT s.legajo, s.tipo_estudiante, c.nombre AS carrera " +
                        "FROM student s " +
                        "JOIN Plan_Estudio pe ON pe.id = s.plan_estudio_id " +
                        "JOIN Carrera c       ON c.id  = pe.carrera_id " +
                        "WHERE s.usuario_id = ?", userId
                    );
                    if (!rows.isEmpty()) {
                        Map s = rows.get(0);
                        model.put("legajo",          s.get("legajo"));
                        model.put("tipo_estudiante", s.get("tipo_estudiante"));
                        model.put("carrera",         s.get("carrera"));
                    }
    
                } else if ("SECRETARIA".equals(userRole)) {
                    model.put("isSecretaria", true);
                    List<Map> rows = Base.findAll(
                        "SELECT oficina, interno FROM secretariaAcademica WHERE usuario_id = ?", userId
                    );
                    if (!rows.isEmpty()) {
                        Map sa = rows.get(0);
                        model.put("oficina", sa.get("oficina") != null ? sa.get("oficina") : "—");
                        model.put("interno", sa.get("interno") != null ? sa.get("interno") : "—");
                    }
    
                } else if ("ADMIN".equals(userRole)) {
                    model.put("isAdminRol", true);
                    List<Map> rows = Base.findAll(
                        "SELECT area_responsabilidad FROM gestorSistema WHERE usuario_id = ?", userId
                    );
                    if (!rows.isEmpty()) {
                        Object area = rows.get(0).get("area_responsabilidad");
                        model.put("area_responsabilidad", area != null ? area : "—");
                    }
                }
    
                String success = req.queryParams("message");
                String error   = req.queryParams("error");
                if (success != null && !success.isEmpty()) model.put("successMessage", success);
                if (error   != null && !error.isEmpty())   model.put("errorMessage",   error);
    
                return new ModelAndView(model, "perfil.mustache");
            }, new MustacheTemplateEngine());

        post("/perfil/foto", (req, res) -> {
                Boolean loggedIn = req.session().attribute("loggedIn");
                if (!Boolean.TRUE.equals(loggedIn)) {
                    res.redirect("/login");
                    return null;
                }
    
                Object userIdObj = req.session().attribute("userId");
                int    userId    = ((Number) userIdObj).intValue();
    
                // Configurar multipart ANTES de llamar getPart()
                req.raw().setAttribute(
                    "org.eclipse.jetty.multipartConfig",
                    new MultipartConfigElement(
                        System.getProperty("java.io.tmpdir"),
                        2L * 1024 * 1024,  // máx tamaño de archivo: 2 MB
                        4L * 1024 * 1024,  // máx tamaño de request: 4 MB
                        0                  // umbral en memoria
                    )
                );
    
                try {
                    Part fotoPart = req.raw().getPart("foto");
    
                    if (fotoPart == null || fotoPart.getSize() == 0) {
                        res.redirect("/perfil?error=" + URLEncoder.encode(
                            "No se recibió ningún archivo.", StandardCharsets.UTF_8.toString()));
                        return null;
                    }
    
                    String submittedName = fotoPart.getSubmittedFileName();
                    if (submittedName == null || !submittedName.contains(".")) {
                        res.redirect("/perfil?error=" + URLEncoder.encode(
                            "Nombre de archivo inválido.", StandardCharsets.UTF_8.toString()));
                        return null;
                    }
    
                    String ext = submittedName.substring(submittedName.lastIndexOf('.') + 1).toLowerCase();
                    if (!Arrays.asList("jpg", "jpeg", "png").contains(ext)) {
                        res.redirect("/perfil?error=" + URLEncoder.encode(
                            "Solo se permiten imágenes JPG o PNG.", StandardCharsets.UTF_8.toString()));
                        return null;
                    }
    
                    String contentType = fotoPart.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        res.redirect("/perfil?error=" + URLEncoder.encode(
                            "El archivo no es una imagen válida.", StandardCharsets.UTF_8.toString()));
                        return null;
                    }
    
                    // Guardar con nombre controlado (evita path traversal)
                    String STATIC_DIR_LOCAL = System.getProperty("user.dir") + "/public";
                    String UPLOAD_DIR_LOCAL = STATIC_DIR_LOCAL + "/img/uploads";
                    Files.createDirectories(Paths.get(UPLOAD_DIR_LOCAL));
    
                    String savedName = "perfil_" + userId + "." + ext;
                    Path   targetPath = Paths.get(UPLOAD_DIR_LOCAL, savedName);
    
                    try (InputStream is = fotoPart.getInputStream()) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
    
                    String fotoUrl = "/img/uploads/" + savedName;
                    Base.exec("UPDATE users SET foto_perfil = ? WHERE id = ?", fotoUrl, userId);
    
                    res.redirect("/perfil?message=" + URLEncoder.encode(
                        "Foto de perfil actualizada exitosamente.", StandardCharsets.UTF_8.toString()));
    
                } catch (Exception e) {
                    e.printStackTrace();
                    res.redirect("/perfil?error=" + URLEncoder.encode(
                        "Error al procesar la imagen: " + e.getMessage(), StandardCharsets.UTF_8.toString()));
                }
                return null;
            });

    }
}
