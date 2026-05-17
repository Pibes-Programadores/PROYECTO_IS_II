package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.annotations.BelongsTo;
import java.sql.Timestamp;

@Table("sesion")
@BelongsTo(parent = User.class, foreignKeyName = "usuario_id")
public class Sesion extends Model {

    public String getToken() { return getString("token"); }
    public void setToken(String token) { set("token", token); }

    public Timestamp getFechaInicio() { return getTimestamp("fecha_inicio"); }
    public void setFechaInicio(Timestamp fechaInicio) { set("fecha_inicio", fechaInicio); }

    public Timestamp getFechaExpiracion() { return getTimestamp("fecha_expiracion"); }
    public void setFechaExpiracion(Timestamp fechaExpiracion) { set("fecha_expiracion", fechaExpiracion); }

    public User getUser() {
        return parent(User.class);
    }
}
