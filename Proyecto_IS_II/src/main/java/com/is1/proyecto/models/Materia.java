package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.IdName;
import org.javalite.activejdbc.annotations.Table;

@Table("Materia")
@IdName("codigo")
public class Materia extends Model {
    // PK: codigo (no AUTO_INCREMENT), plan_estudio_id, nombre, anio_cursada, carga_horaria_total
}