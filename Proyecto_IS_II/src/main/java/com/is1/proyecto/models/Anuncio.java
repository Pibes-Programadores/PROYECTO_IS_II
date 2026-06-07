package com.is1.proyecto.models;
 
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.BelongsToPolymorphic;
 
@Table("Anuncio")
@BelongsTo(parent = Teacher.class, foreignKeyName = "teacher_id")
public class Anuncio extends Model {
 
    public int getMateriaPeriodoId()        { return getInteger("materia_periodo_id"); }
    public void setMateriaPeriodoId(int id) { set("materia_periodo_id", id); }
 
    public int getTeacherId()               { return getInteger("teacher_id"); }
    public void setTeacherId(int id)        { set("teacher_id", id); }
 
    public String getTipo()                 { return getString("tipo"); }
    public void setTipo(String tipo)        { set("tipo", tipo); }
 
    public String getTitulo()               { return getString("titulo"); }
    public void setTitulo(String titulo)    { set("titulo", titulo); }
 
    public String getContenido()            { return getString("contenido"); }
    public void setContenido(String c)      { set("contenido", c); }
 
    /** Nullable — solo presente cuando tipo = 'EXAMEN' */
    public java.sql.Date getFechaExamen()           { return (java.sql.Date) get("fecha_examen"); }
    public void setFechaExamen(java.sql.Date fecha) { set("fecha_examen", fecha); }
 
    public java.sql.Timestamp getFechaCreacion()    { return getTimestamp("fecha_creacion"); }
 
    public Teacher getTeacher() { return parent(Teacher.class); }
}