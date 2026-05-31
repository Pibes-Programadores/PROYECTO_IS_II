package com.is1.proyecto.models;
 
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.annotations.BelongsTo;
 
@Table("Aula_Asignacion")
@BelongsTo(parent = Teacher.class, foreignKeyName = "teacher_id")
public class AulaAsignacion extends Model {
 
    public int getMateriaPeriodoId()        { return getInteger("materia_periodo_id"); }
    public void setMateriaPeriodoId(int id) { set("materia_periodo_id", id); }
 
    public int getTeacherId()               { return getInteger("teacher_id"); }
    public void setTeacherId(int id)        { set("teacher_id", id); }
 
    public String getAula()                 { return getString("aula"); }
    public void setAula(String aula)        { set("aula", aula); }
 
    public java.sql.Timestamp getFechaAsignacion() { return getTimestamp("fecha_asignacion"); }
 
    public Teacher getTeacher() { return parent(Teacher.class); }
}