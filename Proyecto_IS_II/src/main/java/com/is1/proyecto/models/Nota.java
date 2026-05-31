package com.is1.proyecto.models;
 
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.annotations.BelongsTo;
 
@Table("Nota")
@BelongsTo(parent = Teacher.class, foreignKeyName = "teacher_id")
public class Nota extends Model {
 
    public int getMateriaPeriodoId()        { return getInteger("materia_periodo_id"); }
    public void setMateriaPeriodoId(int id) { set("materia_periodo_id", id); }
 
    public int getStudentId()               { return getInteger("student_id"); }
    public void setStudentId(int id)        { set("student_id", id); }
 
    public int getTeacherId()               { return getInteger("teacher_id"); }
    public void setTeacherId(int id)        { set("teacher_id", id); }
 
    public double getValor()                { return getDouble("valor"); }
    public void setValor(double valor)      { set("valor", valor); }
 
    public java.sql.Timestamp getFechaCarga() { return getTimestamp("fecha_carga"); }
 
    public Teacher getTeacher() { return parent(Teacher.class); }
}