package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("Carrera")
public class Carrera extends Model {
    // ActiveJDBC infiere automáticamente las columnas: id, nombre, duracion_anios, titulo_otorgado
}