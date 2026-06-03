package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("mesas_examen")
public class MesaExamen extends Model {

    // Getter y Setter para la Materia Asociada (FK)
    public String getMateriaCodigo() {
        return getString("materia_codigo");
    }
    public void setMateriaCodigo(String codigo) {
        set("materia_codigo", codigo);
    }

    // Getter y Setter para la Fecha del Examen
    public String getFecha() {
        return getString("fecha");
    }
    public void setFecha(String fecha) {
        set("fecha", fecha);
    }
}