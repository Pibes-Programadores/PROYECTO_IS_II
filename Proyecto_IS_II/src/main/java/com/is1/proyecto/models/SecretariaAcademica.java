package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.IdName; // <- Importación nueva
import org.javalite.activejdbc.annotations.Table;

@Table("secretariaAcademica")
@IdName("usuario_id") // <- Le avisamos a ActiveJDBC cuál es la PK real
@BelongsTo(parent = User.class, foreignKeyName = "usuario_id")
public class SecretariaAcademica extends Model {

    public String getOficina() {
        return getString("oficina");
    }

    public void setOficina(String oficina) {
        set("oficina", oficina);
    }

    public String getInterno() {
        return getString("interno");
    }

    public void setInterno(String interno) {
        set("interno", interno);
    }

    public User getUser() {
        return parent(User.class);
    }
}
