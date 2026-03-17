package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;


@Table("teacher") // Esta anotación asocia explícitamente el modelo 'Teacher' con la tabla 'teachers' en la DB.
public class Teacher extends Model {

    public Integer getDocumento() {
        return getInteger("dni"); // Obtiene el dni del docente
    }

    public void setDocumento(Integer dni) {
        set("dni", dni); // Establece el valor para la columna 'dni'
    }

    public String getName() {
        return getString("name"); // Obtenemos el nombre
    }

    public void setName(String name) {
        set("name", name); // Establece el valor para la columna 'name'
    }

    public String getLastName(){
        return getString("lastName"); // Obtenemos el apellido
    }

    public void setLastName(String lastName) {
        set("lastName", lastName); // Establece el valor para la columna 'lastName'
    }

    public String getAddress() {
        return getString("address"); // Obetenemos la direccion
    } 

    public void setAddress(String address) {
        set("address", address); // Establece el valor para la columna 'address'
    }

    public Integer getPhone() {
        return getInteger("phone"); // Obtenermos el telefono
    } 

    public void setPhone(Integer phone) {
        set("phone", phone); // Establece el valor para la columna 'phone'
    }
}
