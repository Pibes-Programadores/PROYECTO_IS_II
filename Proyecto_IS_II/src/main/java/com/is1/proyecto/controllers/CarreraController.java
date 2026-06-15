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

public class CarreraController {
    public static void register() {
        get(
                    "/carrera/new",
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

                        return new ModelAndView(model, "carrera_form.mustache");
                    },
                    new MustacheTemplateEngine()
                );

        post("/carrera/new", (req, res) -> {
                    String userRole = req.session().attribute("userRole");
                    if (
                        userRole == null ||
                        (!userRole.equals("ADMIN") && !userRole.equals("SECRETARIA"))
                    ) {
                        res.status(403);
                        return "Acceso denegado.";
                    }

                    // Captura de datos del frontend
                    String nombre = req.queryParams("nombre");
                    String duracionAnios = req.queryParams("duracion_anios");
                    String tituloOtorgado = req.queryParams("titulo_otorgado");
                    String anioResolucion = req.queryParams("anio_resolucion");
                    String estado = req.queryParams("estado");

                    // Validación rigurosa de nulidad y vacíos
                    if (
                        nombre == null ||
                        duracionAnios == null ||
                        tituloOtorgado == null ||
                        anioResolucion == null ||
                        estado == null ||
                        nombre.isBlank() ||
                        duracionAnios.isBlank() ||
                        tituloOtorgado.isBlank() ||
                        anioResolucion.isBlank() ||
                        estado.isBlank()
                    ) {
                        String errorMsg = URLEncoder.encode(
                            "Todos los campos obligatorios deben completarse.",
                            StandardCharsets.UTF_8.toString()
                        );
                        res.redirect("/carrera/new?error=" + errorMsg);
                        return "";
                    }

                    try {
                        // Abrimos transacción para asegurar que no se cree una Carrera sin su Plan correspondiente
                        Base.openTransaction();

                        // Instanciar y persistir la Carrera
                        Carrera carrera = new Carrera();
                        carrera.set("nombre", nombre);
                        carrera.set("duracion_anios", Integer.parseInt(duracionAnios));
                        carrera.set("titulo_otorgado", tituloOtorgado);
                        carrera.saveIt();

                        // Instanciar y persistir el Plan_Estudio usando el ID generado automáticamente
                        PlanEstudio plan = new PlanEstudio();
                        plan.set("carrera_id", carrera.getId());
                        plan.set("anio_resolucion", Integer.parseInt(anioResolucion));
                        plan.set("estado", estado);
                        plan.saveIt();

                        // Confirmamos los cambios en la BD
                        Base.commitTransaction();

                        String successMsg = URLEncoder.encode(
                            "Carrera '" +
                                nombre +
                                "' y Plan inicial registrados con éxito.",
                            StandardCharsets.UTF_8.toString()
                        );
                        res.redirect("/carrera/new?message=" + successMsg);
                        return "";
                    } catch (Exception e) {
                        // Si algo falla (ej: violación de restricción UNIQUE en el nombre de carrera), cancelamos todo
                        Base.rollbackTransaction();
                        e.printStackTrace();
                        String errorMsg = URLEncoder.encode(
                            "Error al procesar el alta. Verifique si el nombre de la carrera ya existe.",
                            StandardCharsets.UTF_8.toString()
                        );
                        res.redirect("/carrera/new?error=" + errorMsg);
                        return "";
                    }
                });

        get(
                    "/carrera/materias",
                    (req, res) -> {
                        if (req.session().attribute("userRole") == null) {
                            res.redirect("/login");
                            return null;
                        }

                        Map<String, Object> model = new HashMap<>();

                        // 1. Cargamos el selector de planes vigentes
                        List<Map> planesDropdown = Base.findAll(
                            "SELECT p.id as id, CONCAT(c.nombre, ' (Plan Resol: ', p.anio_resolucion, ')') as descripcion " +
                                "FROM Plan_Estudio p JOIN Carrera c ON p.carrera_id = c.id WHERE p.estado = 'VIGENTE' ORDER BY c.nombre ASC"
                        );
                        model.put("planes", planesDropdown);

                        // 2. Si se mandó un plan por la URL, buscamos sus materias asignadas
                        String planIdParam = req.queryParams("plan_id");
                        if (planIdParam != null && !planIdParam.isBlank()) {
                            int planId = Integer.parseInt(planIdParam.trim());

                            // Buscamos las materias asociadas ordenadas cronológicamente por año y cuatrimestre
                            List<Map> materiasRaw = Base.findAll(
                                "SELECT m.codigo, m.nombre, m.anio_cursada, mp.tipo_cuatrimestre " +
                                    "FROM Materia m " +
                                    "JOIN Materia_Periodo mp ON m.codigo = mp.materia_codigo " +
                                    "WHERE m.plan_estudio_id = ? " +
                                    "ORDER BY m.anio_cursada ASC, " +
                                    "CASE mp.tipo_cuatrimestre " +
                                    "  WHEN 'PRIMER_CUATRIMESTRE' THEN 1 " +
                                    "  WHEN 'ANUAL' THEN 2 " +
                                    "  WHEN 'SEGUNDO_CUATRIMESTRE' THEN 3 " +
                                    "  WHEN 'VERANO' THEN 4 " +
                                    "  ELSE 5 " +
                                    "END ASC, m.nombre ASC",
                                planId
                            );

                            List<Map<String, Object>> materiasProcesadas =
                                new java.util.ArrayList<>();

                            for (Map mat : materiasRaw) {
                                Map<String, Object> mDto = new HashMap<>(mat);
                                int mCodigo = (int) mat.get("codigo");

                                // Formateamos visualmente el año y cuatrimestre para la primera columna
                                String cuatRaw = (String) mat.get("tipo_cuatrimestre");
                                String cuatVista = cuatRaw;
                                if ("PRIMER_CUATRIMESTRE".equals(cuatRaw)) cuatVista =
                                    "I Cuat.";
                                else if (
                                    "SEGUNDO_CUATRIMESTRE".equals(cuatRaw)
                                ) cuatVista = "II Cuat.";
                                else if ("ANUAL".equals(cuatRaw)) cuatVista = "Anual";
                                else if ("VERANO".equals(cuatRaw)) cuatVista = "Verano";

                                mDto.put(
                                    "periodo_vista",
                                    mat.get("anio_cursada") + "° Año - " + cuatVista
                                );

                                // Buscamos las correlatividades de esta asignatura concreta (AHORA INCLUYE TIPO REQUISITO)
                                List<Map> corrs = Base.findAll(
                                    "SELECT materia_correlativa_codigo, condicion, tipo_requisito FROM Correlatividad WHERE materia_codigo = ?",
                                    mCodigo
                                );

                                StringBuilder aprobadasC = new StringBuilder(); // Para CURSAR (Aprobada)
                                StringBuilder regularesC = new StringBuilder(); // Para CURSAR (Regular)
                                StringBuilder aprobadasR = new StringBuilder(); // Para RENDIR (Aprobada)

                                for (Map c : corrs) {
                                    int corrCod = (int) c.get(
                                        "materia_correlativa_codigo"
                                    );
                                    String cond = (String) c.get("condicion");
                                    String tipoReq = (String) c.get("tipo_requisito"); // Capturamos la nueva columna

                                    if ("RENDIR".equals(tipoReq)) {
                                        if (aprobadasR.length() > 0) aprobadasR.append(
                                            ", "
                                        );
                                        aprobadasR.append(corrCod);
                                    } else {
                                        // Si es para CURSAR, evaluamos la condición
                                        if ("APROBADA".equals(cond)) {
                                            if (
                                                aprobadasC.length() > 0
                                            ) aprobadasC.append(", ");
                                            aprobadasC.append(corrCod);
                                        } else {
                                            if (
                                                regularesC.length() > 0
                                            ) regularesC.append(", ");
                                            regularesC.append(corrCod);
                                        }
                                    }
                                }

                                // Guardamos las 3 variables separadas para que Mustache arme las columnas
                                mDto.put(
                                    "correlativas_aprobadas",
                                    aprobadasC.length() > 0
                                        ? aprobadasC.toString()
                                        : "-"
                                );
                                mDto.put(
                                    "correlativas_regulares",
                                    regularesC.length() > 0
                                        ? regularesC.toString()
                                        : "-"
                                );
                                mDto.put(
                                    "correlativas_rendir",
                                    aprobadasR.length() > 0
                                        ? aprobadasR.toString()
                                        : "-"
                                );

                                materiasProcesadas.add(mDto);
                            }

                            model.put("materias", materiasProcesadas);
                            model.put("mostrarTabla", !materiasProcesadas.isEmpty());
                            model.put("sinMaterias", materiasProcesadas.isEmpty());
                        }

                        return new ModelAndView(model, "materias_list.mustache");
                    },
                    new MustacheTemplateEngine()
                );

    }
}
