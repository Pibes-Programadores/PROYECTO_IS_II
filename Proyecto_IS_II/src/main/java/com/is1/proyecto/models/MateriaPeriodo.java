package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("Materia_Periodo")
public class MateriaPeriodo extends Model {
    // Maneja automáticamente: id, materia_id, anio_academico, tipo_cuatrimestre
}