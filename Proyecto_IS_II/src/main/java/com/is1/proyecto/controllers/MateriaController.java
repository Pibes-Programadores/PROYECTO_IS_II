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

public class MateriaController {
    public static void register() {
        get(
                    "/materia/new",
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

                        // Consulta SQL limpia para armar el selector dinámico de Carreras con sus respectivos Planes Vigentes
                        List<Map> planesDropdown = Base.findAll(
                            "SELECT p.id as id, CONCAT(c.nombre, ' (Plan Resol: ', p.anio_resolucion, ')') as descripcion " +
                                "FROM Plan_Estudio p JOIN Carrera c ON p.carrera_id = c.id WHERE p.estado = 'VIGENTE' ORDER BY c.nombre ASC"
                        );

                        // Traemos las materias cargadas para poblar el mapa de correlatividades recursivo
                        List<Map> materiasExistentes = Base.findAll(
                            "SELECT codigo, nombre FROM Materia ORDER BY codigo ASC"
                        );

                        model.put("planes", planesDropdown);
                        model.put("materiasExistentes", materiasExistentes);

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

                        return new ModelAndView(model, "materia_form.mustache");
                    },
                    new MustacheTemplateEngine()
                );

        post("/materia/new", (req, res) -> {
                    String userRole = req.session().attribute("userRole");
                    if (
                        userRole == null ||
                        (!userRole.equals("ADMIN") && !userRole.equals("SECRETARIA"))
                    ) {
                        res.status(403);
                        return "Acceso denegado.";
                    }

                    // Captura de parámetros
                    String codigoStr = req.queryParams("codigo"); // Viene como texto desde el HTML
                    String planEstudioId = req.queryParams("plan_estudio_id");
                    String nombre = req.queryParams("nombre");
                    String anioCursada = req.queryParams("anio_cursada");
                    String cargaHorariaTotal = req.queryParams("carga_horaria_total");
                    String tipoCuatrimestre = req.queryParams("tipo_cuatrimestre");

                    String sentidoCorrelatividad = req.queryParams(
                        "sentido_correlatividad"
                    );
                    String condicion = req.queryParams("condicion");

                    if (
                        codigoStr == null ||
                        planEstudioId == null ||
                        nombre == null ||
                        anioCursada == null ||
                        tipoCuatrimestre == null ||
                        codigoStr.isBlank() ||
                        planEstudioId.isBlank() ||
                        nombre.isBlank() ||
                        anioCursada.isBlank()
                    ) {
                        String errorMsg = URLEncoder.encode(
                            "Error: Complete todos los campos obligatorios.",
                            StandardCharsets.UTF_8.toString()
                        );
                        res.redirect("/materia/new?error=" + errorMsg);
                        return "";
                    }

                    try {
                        // Parseamos los números clave
                        int codigoNumerico = Integer.parseInt(codigoStr.trim());

                        if (codigoNumerico < 0) {
                            throw new IllegalArgumentException(
                                "El código de la materia no puede ser un número negativo."
                            );
                        }

                        int anioPropuestoMateria = Integer.parseInt(anioCursada.trim());

                        Base.openTransaction();

                        // Validaciones de regla de negocio
                        PlanEstudio plan = PlanEstudio.findById(planEstudioId);
                        if (plan == null) throw new IllegalArgumentException(
                            "El Plan de Estudio seleccionado no existe."
                        );

                        Carrera carrera = Carrera.findById(plan.get("carrera_id"));
                        int maxAniosCarrera = carrera.getInteger("duracion_anios");

                        if (anioPropuestoMateria > maxAniosCarrera) {
                            throw new IllegalArgumentException(
                                "No se puede asignar a " +
                                    anioPropuestoMateria +
                                    "° año. La carrera '" +
                                    carrera.get("nombre") +
                                    "' dura " +
                                    maxAniosCarrera +
                                    " años."
                            );
                        }

                        // Tarea A: Guardar Materia con PK numérica
                        if (cargaHorariaTotal != null && !cargaHorariaTotal.isBlank()) {
                            Base.exec(
                                "INSERT INTO Materia (codigo, plan_estudio_id, nombre, anio_cursada, carga_horaria_total) VALUES (?, ?, ?, ?, ?)",
                                codigoNumerico,
                                Integer.parseInt(planEstudioId),
                                nombre,
                                anioPropuestoMateria,
                                Integer.parseInt(cargaHorariaTotal)
                            );
                        } else {
                            Base.exec(
                                "INSERT INTO Materia (codigo, plan_estudio_id, nombre, anio_cursada) VALUES (?, ?, ?, ?)",
                                codigoNumerico,
                                Integer.parseInt(planEstudioId),
                                nombre,
                                anioPropuestoMateria
                            );
                        }

                        // Tarea B: Guardar Periodo
                        int anioActualDinamico = java.time.Year.now().getValue();
                        MateriaPeriodo mp = new MateriaPeriodo();
                        mp.set("materia_codigo", codigoNumerico); // Relación numérica
                        mp.set("anio_academico", anioActualDinamico);
                        mp.set("tipo_cuatrimestre", tipoCuatrimestre);
                        mp.saveIt();

                        // Tarea C: Correlatividad
                        // 1. En lugar de queryParams (String), usamos queryParamsValues (Array de Strings)
                        String[] correlativas = req.queryParamsValues(
                            "materia_correlativa_codigo"
                        );
                        String[] tiposRequisito = req.queryParamsValues(
                            "tipo_requisito"
                        );
                        String[] condiciones = req.queryParamsValues("condicion");
                        String[] sentidos = req.queryParamsValues(
                            "sentido_correlatividad"
                        );

                        // 2. Si llegaron datos, los recorremos uno por uno con un bucle FOR
                        if (correlativas != null) {
                            for (int i = 0; i < correlativas.length; i++) {
                                // Si en esta fila dejaron "-- Ninguna --", la saltamos y seguimos con la siguiente
                                if (correlativas[i].equals("none")) continue;

                                // Extraemos los datos específicos de la fila actual del bucle
                                int seleccionadaCodigo = Integer.parseInt(
                                    correlativas[i].trim()
                                );
                                String tipoReq = tiposRequisito[i];
                                String condicionActual = condiciones[i];
                                String sentidoCorr = sentidos[i];

                                if (codigoNumerico == seleccionadaCodigo) {
                                    throw new IllegalArgumentException(
                                        "Una asignatura no puede ser correlativa de sí misma."
                                    );
                                }

                                Materia materiaExistente = Materia.findFirst(
                                    "codigo = ?",
                                    seleccionadaCodigo
                                );
                                MateriaPeriodo periodoExistente =
                                    MateriaPeriodo.findFirst(
                                        "materia_codigo = ?",
                                        seleccionadaCodigo
                                    );

                                if (
                                    materiaExistente == null || periodoExistente == null
                                ) {
                                    throw new IllegalArgumentException(
                                        "La materia correlativa " +
                                            seleccionadaCodigo +
                                            " no es válida."
                                    );
                                }

                                int anioExistente = materiaExistente.getInteger(
                                    "anio_cursada"
                                );
                                String cuatExistente = periodoExistente.getString(
                                    "tipo_cuatrimestre"
                                );

                                int anioRequisito, anioObjetivo;
                                String cuatRequisito, cuatObjetivo;

                                if ("REQUIERE".equals(sentidoCorr)) {
                                    anioRequisito = anioExistente;
                                    cuatRequisito = cuatExistente;
                                    anioObjetivo = anioPropuestoMateria;
                                    cuatObjetivo = tipoCuatrimestre;
                                } else {
                                    anioRequisito = anioPropuestoMateria;
                                    cuatRequisito = tipoCuatrimestre;
                                    anioObjetivo = anioExistente;
                                    cuatObjetivo = cuatExistente;
                                }

                                java.util.function.Function<
                                    String,
                                    Integer
                                > pesoCuatrimestre = c -> {
                                    switch (c) {
                                        case "PRIMER_CUATRIMESTRE":
                                            return 1;
                                        case "ANUAL":
                                            return 1;
                                        case "SEGUNDO_CUATRIMESTRE":
                                            return 2;
                                        case "VERANO":
                                            return 3;
                                        default:
                                            return 0;
                                    }
                                };

                                int pesoReq = pesoCuatrimestre.apply(cuatRequisito);
                                int pesoObj = pesoCuatrimestre.apply(cuatObjetivo);

                                if (anioRequisito > anioObjetivo) {
                                    throw new IllegalArgumentException(
                                        "Inconsistencia Temporal: El requisito pertenece a un año superior."
                                    );
                                } else if (
                                    anioRequisito == anioObjetivo && pesoReq >= pesoObj
                                ) {
                                    throw new IllegalArgumentException(
                                        "Inconsistencia Temporal: El requisito se dicta en paralelo o posterior en el mismo año."
                                    );
                                }

                                // 3. Insertamos en la Base de Datos con los 4 parámetros (incluyendo tipoReq)
                                if ("REQUIERE".equals(sentidoCorr)) {
                                    Base.exec(
                                        "INSERT INTO Correlatividad (materia_codigo, materia_correlativa_codigo, condicion, tipo_requisito) VALUES (?, ?, ?, ?)",
                                        codigoNumerico,
                                        seleccionadaCodigo,
                                        condicionActual,
                                        tipoReq
                                    );
                                } else if ("ES_REQUISITO".equals(sentidoCorr)) {
                                    Base.exec(
                                        "INSERT INTO Correlatividad (materia_codigo, materia_correlativa_codigo, condicion, tipo_requisito) VALUES (?, ?, ?, ?)",
                                        seleccionadaCodigo,
                                        codigoNumerico,
                                        condicionActual,
                                        tipoReq
                                    );
                                }
                            }
                        }

                        Base.commitTransaction();
                        String successMsg = URLEncoder.encode(
                            "Materia [" + codigoNumerico + "] registrada con éxito.",
                            StandardCharsets.UTF_8.toString()
                        );
                        res.redirect("/materia/new?message=" + successMsg);
                        return "";
                    } catch (Exception e) {
                        Base.rollbackTransaction();
                        e.printStackTrace();
                        String errorMsg = URLEncoder.encode(
                            "Error: " + e.getMessage(),
                            StandardCharsets.UTF_8.toString()
                        );
                        res.redirect("/materia/new?error=" + errorMsg);
                        return "";
                    }
                });

    }
}
