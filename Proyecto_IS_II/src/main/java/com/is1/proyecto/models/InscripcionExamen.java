package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.annotations.BelongsTo;

@Table("Inscripcion_Examen")
@BelongsTo(parent = User.class, foreignKeyName = "usuario_id")
public class InscripcionExamen extends Model {

    // Getter y Setter para el Alumno (FK)
    public int getUsuarioId() {
        return getInteger("usuario_id");
    }
    public void setUsuarioId(int id) {
        set("usuario_id", id);
    }

    // Getter y Setter para la Mesa de Examen (FK)
    public int getMesaId() {
        return getInteger("mesa_id");
    }
    public void setMesaId(int id) {
        set("mesa_id", id);
    }

    // Método para obtener directo el objeto del Usuario/Alumno relacionado
    public User getUser() {
        return parent(User.class);
    }
}