package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("Materia")
public class Materia extends Model {
    // Maneja automáticamente: id, plan_estudio_id, nombre, anio_cursada, carga_horaria_total
}