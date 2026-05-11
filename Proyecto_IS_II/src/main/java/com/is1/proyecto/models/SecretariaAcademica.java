package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.activejdbc.annotations.BelongsTo;

@Table("secretariaAcademica")
@BelongsTo(parent = User.class, foreignKeyName = "usuario_id")
public class SecretariaAcademica extends Model {

    public String getOficina() { return getString("oficina"); }
    public void setOficina(String oficina) { set("oficina", oficina); }

    public String getInterno() { return getString("interno"); }
    public void setInterno(String interno) { set("interno", interno); }

    public User getUser() {
        return parent(User.class);
    }
}