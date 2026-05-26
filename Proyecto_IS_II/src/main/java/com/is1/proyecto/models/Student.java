package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.IdName; // <- Importación nueva
import org.javalite.activejdbc.annotations.Table;

@Table("student")
@IdName("usuario_id") // <- Le avisamos a ActiveJDBC cuál es la PK real
@BelongsTo(parent = User.class, foreignKeyName = "usuario_id")
public class Student extends Model {

    public String getLegajo() {
        return getString("legajo");
    }

    public void setLegajo(String legajo) {
        set("legajo", legajo);
    }

    public String getTipoEstudiante() {
        return getString("tipo_estudiante");
    }

    public void setTipoEstudiante(String tipoEstudiante) {
        set("tipo_estudiante", tipoEstudiante);
    }

    public User getUser() {
        return parent(User.class);
    }
}
