package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.BelongsToParents;
import org.javalite.activejdbc.annotations.Table;

@Table("Estado_Academico")
@BelongsToParents({
    @BelongsTo(foreignKeyName = "usuario_id", parent = Student.class),
    @BelongsTo(foreignKeyName = "materia_codigo", parent = Materia.class)
})
public class EstadoAcademico extends Model {

    public int getUsuarioId() {
        return getInteger("usuario_id");
    }

    public void setUsuarioId(int usuarioId) {
        set("usuario_id", usuarioId);
    }

    public int getMateriaCodigo() {
        return getInteger("materia_codigo");
    }

    public void setMateriaCodigo(int materiaCodigo) {
        set("materia_codigo", materiaCodigo);
    }

    public String getEstado() {
        return getString("estado");
    }

    public void setEstado(String estado) {
        set("estado", estado);
    }

    public Student getStudent() {
        return parent(Student.class);
    }

    public Materia getMateria() {
        return parent(Materia.class);
    }
}
