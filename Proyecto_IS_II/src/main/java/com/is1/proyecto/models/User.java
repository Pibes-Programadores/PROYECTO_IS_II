package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("users")
public class User extends Model {

    public String getNombre() {
        return getString("nombre");
    }

    public void setNombre(String n) {
        set("nombre", n);
    }

    public String getApellido() {
        return getString("apellido");
    }

    public void setApellido(String a) {
        set("apellido", a);
    }

    public String getDni() {
        return getString("dni");
    }

    public void setDni(String d) {
        set("dni", d);
    }

    public String getDireccion() {
        return getString("direccion");
    }

    public void setDireccion(String dir) {
        set("direccion", dir);
    }

    public String getTelefono() {
        return getString("telefono");
    }

    public void setTelefono(String t) {
        set("telefono", t);
    }

    public String getNombreUsuario() {
        return getString("nombre_usuario");
    }

    public void setNombreUsuario(String u) {
        set("nombre_usuario", u);
    }

    public String getPassword() {
        return getString("password");
    }

    public void setPassword(String p) {
        set("password", p);
    }

    public void setNivelAcceso(String l) {
        set("nivel_acceso", l);
    }
}
