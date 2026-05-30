package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.Table;

@Table("Docente_Materia")
@BelongsTo(parent = Teacher.class, foreignKeyName = "teacher_id")
public class DocenteMateria extends Model {

    public Teacher getTeacher() {
        return parent(Teacher.class);
    }

    public Materia getMateria() {
        return parent(Materia.class);
    }
}
