package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.Table;

@Table("teacher")
@BelongsTo(parent = User.class, foreignKeyName = "usuario_id")
public class Teacher extends Model {

    public String getLegajoDocente() {
        return getString("legajo_docente");
    }

    public void setLegajoDocente(String l) {
        set("legajo_docente", l);
    }

    public String getCuil() {
        return getString("cuil");
    }

    public void setCuil(String c) {
        set("cuil", c);
    }

    public String getEmail() {
        return getString("email");
    }

    public void setEmail(String e) {
        set("email", e);
    }

    public String getEspecialidad() {
        return getString("especialidad");
    }

    public void setEspecialidad(String es) {
        set("especialidad", es);
    }

    public User getUser() {
        return parent(User.class);
    }
}
