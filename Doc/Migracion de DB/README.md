## 📝 Resumen de que se hizo

Se realizó la migración completa del motor de persistencia de **SQLite** a **MySQL/MariaDB** para escalar el sistema según los requerimientos de Ingeniería de Software II.

### Modificaciones clave:

- **`pom.xml`**:
    
    - Se eliminó el driver de SQLite y se añadió el **MySQL Connector/J 8.3.0**.
        
    - Se configuró el plugin de **instrumentación de ActiveJDBC** (`3.4-j11`) vinculado a las propiedades del proyecto. Esto es vital porque inyecta el código de base de datos en los modelos `.class` después de compilar.
        
- **`DBConfigSingleton.java`**:
    
    - Se refactorizó para usar el driver de MySQL.
        
    - Se implementó una lógica de **variables de entorno**: el sistema busca `DB_URL`, `DB_USER` y `DB_PASS`. Si no las encuentra, usa por defecto los datos de desarrollo local (`localhost`, usuario `dev`).
        
- **`App.java`**:
    
    - Se optimizaron los filtros `before` y `afterAfter` para abrir y cerrar conexiones de forma segura usando el Singleton.
        
- **`scheme.sql`**:
    
    - Se portó el esquema de tablas. Cambiamos los tipos `TEXT` por `VARCHAR` y la sintaxis de autoincremento de SQLite (`AUTOINCREMENT`) por la de MySQL (`AUTO_INCREMENT`).
        

---

## 🚀 Guía de puesta en marcha

Para cualquiera que quiera correr el proyecto en su PC, debe seguir estos pasos exactos para que el sistema no "explote" al iniciar:

### 1. Preparar el motor (Servidor)

Deben tener instalado MySQL o MariaDB.

- **Paso A:** Asegurarse de que el servicio esté corriendo (`systemctl start mariadb` en Linux o desde el panel de XAMPP en Windows).
    
- **Paso B:** Crear el usuario de desarrollo. Desde la consola de MySQL:
    
    SQL
    
    ```bash
    CREATE USER 'dev'@'localhost' IDENTIFIED BY ''; -- Contraseña vacía
    GRANT ALL PRIVILEGES ON proyecto_is_ii.* TO 'dev'@'localhost';
    FLUSH PRIVILEGES;
    ```
    

### 2. Crear la Base de Datos

Deben crear la "caja" vacía antes de ejecutar el programa:
A) Mediante MySQL Workbench deben crear una nueva conexión:
![[Paso1.png]]

B) Una vez le damos al (*+*) Se nos abre la siguiente pestaña:
![[Paso2.png]]
IMPORTANTE DEJARLO TAL CUAL COMO FIGURA EN LA IMAGEN. ADEMAS POR LAS DUDAS, DARLE A *CLEAR* EN *PASSWORD*.

C) Una vez dentro de la conexión, en *Query 1* pegamos lo siguiente:

```SQL
CREATE DATABASE proyecto_is_ii;
```

Luego, le damos a refrescar en la sección de *Schemas*. Debería quedarnos:
![[Paso3.png]]
Fijarse bien de que la DB *proyecto_is_ii* salga en **NEGRITA**.

D) Ya tenemos la DB, ahora carguemos las tablas, vamos a *File* arriba a la izquierda y le damos a *Open SQL Script* y seleccionamos nuestro scheme.sql que esta en *PROYECTO_IS_II/Proyecto_IS_II/src/main/resources/*:
![[Paso4.png]]
Una vez dentro de *scheme* le damos al rayito, refrescamos en Schemas bien a la izquierda de las pestañas *Query 1* y *scheme*, se debería ver tal que así:
![[Paso5.png]]


Con todo esto ya tendriamos la nueva DB con las nuevas tablas `users` y `teacher`.


### 3. Compilación y Ejecución

Para que ActiveJDBC funcione, **no basta con darle al "Play" del IDE**. Debe usar la terminal en la raíz del proyecto con este comando:

Compilar por primera vez con:

```Bash
mvn clean compile process-classes exec:java -Dexec.mainClass="com.is1.proyecto.App"
```

Luego se puede compilar con:

```Bash
mvn compile process-classes exec:java
```

> [!IMPORTANT] **¿Por qué `process-classes`?** > Es el paso que activa la "instrumentación". Si lo omite, verá un error `500 Internal Server Error` avisando que los modelos no han sido instrumentados.

---

### Solución de problemas comunes (FAQ para el grupo):

- **Error "Address already in use":** Significa que el puerto 8080 está ocupado. Deben cerrar cualquier instancia previa de Java o cambiar el puerto en `App.java`.

- Solución: Esto pasa cuando cerramos el servidor y lo volvemos a abrir sin cerrar el puerto antes. Para solucionarlo, una vez cerramos el server en consola escribimos:

```Bash
	sudo lsof -i :8080
```

Debería salirnos:

```Bash
COMMAND    PID USER  FD   TYPE DEVICE SIZE/OFF NODE NAME  
java    118173 luka 125u  IPv6 474185      0t0  TCP *:http-alt (LISTEN)
```

Copiamos el PID, luego ejecutamos:

```Bash
kill -9 NRO_PID_COPIADO
```

Si tenes mas de 1 con java, usar este comando para cada uno de ellos y ya se soluciona.

- **Error "Access denied for user dev":** Revisar que el usuario `dev` haya sido creado correctamente y tenga permisos sobre la DB.