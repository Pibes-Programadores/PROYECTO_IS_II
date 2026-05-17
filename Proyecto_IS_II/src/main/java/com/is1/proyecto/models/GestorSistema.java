package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.annotations.BelongsTo;

@Table("gestorSistema")
@BelongsTo(parent = User.class, foreignKeyName = "usuario_id")
public class GestorSistema extends Model {

    public String getAreaResponsabilidad() { return getString("area_responsabilidad"); }
    public void setAreaResponsabilidad(String areaResponsabilidad) { set("area_responsabilidad", areaResponsabilidad); }

    public User getUser() {
        return parent(User.class);
    }
}