package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.annotations.BelongsTo;

@Table("Plan_Estudio")
@BelongsTo(parent = Carrera.class, foreignKeyName = "carrera_id")
public class PlanEstudio extends Model {
    // Mapea automáticamente: id, carrera_id, anio_resolucion, estado
}