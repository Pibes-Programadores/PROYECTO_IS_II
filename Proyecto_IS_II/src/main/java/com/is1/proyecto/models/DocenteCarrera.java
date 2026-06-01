package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modelo que representa la tabla Docente_Carrera.
 * Asocia docentes a carreras para habilitar el filtro contextual
 * del buscador de docentes en el formulario de asignación de materias.
 *
 * PK compuesta: (teacher_id, carrera_id)
 */
@Table("Docente_Carrera")
@BelongsTo(parent = Teacher.class, foreignKeyName = "teacher_id")
public class DocenteCarrera extends Model {

    public int getTeacherId(){
        return getInteger("teacher_id");
    }
    public void setTeacherId(int id){
        set("teacher_id", id);
    }

    public int getCarreraId(){
        return getInteger("carrera_id");
    }
    public void setCarreraId(int id){
        set("carrera_id", id);
    }

    public Teacher getTeacher(){
        return parent(Teacher.class);
    }
}