# Diagrama Original


```mermaid
classDiagram
    class Usuario {
        - DNI : int
        - Nombre : char
        - Apellido : char
        - Dirección : char
        - Telefono : int
        - Usuario : char
        - Contraseña : char
    }

    class Estudiante {
        - E-Mail : char
        - fecha_ingreso : char
        - tipo : Tipo_Estudiante
    }

    class ADMINISTRATIVO {
    }

    class Docente {
    }

    class Queja {
        - id_queja : int
        - descripcion : char
    }

    class Beca {
        - id_Beca : int
        - descripcion : char
    }

    class Estado {
        - tipo : Tipo_Estado
    }

    class Carrera {
        - Nombre : char
        - Descripcion : char
        - id_Carrera : int
    }

    class Plan_Estudio {
        - Descripcion : char
        - Horas_Plan_Estudio : int
    }

    class Materia {
        - Codigo : int
        - Cant_Horas : int
    }

    class Materia_Periodo {
        - tipo : Tipo_Cuatrimestre
    }

    class NOTA {
        - Promedio : double
    }

    class Correlatividad {
    }

    class EquipoDocente {
        - MateriaQueDicta : char
        - Cantidad_Integrantes : int
    }

    class Rol {
        - tipo : Rol_Docente
    }

    class SolicitudAula {
        - DocenteQueSolicita : char
        - Horario : char
        - Motivo : char
    }

    class Aula {
        - Pabellon : int
        - Numero : int
    }

    class Tipo_Estado {
        <<enumeration>>
        Bueno
        Regular
        Malo
    }

    class Tipo_Estudiante {
        <<enumeration>>
        Ingresante
        Avanzado
    }

    class Rol_Docente {
        <<enumeration>>
        Responsable_Catedra
        Jefe_TP
        Ayudante
    }

    class Tipo_Cuatrimestre {
        <<enumeration>>
        Primero
        Segundo
    }

    Usuario <|-- Estudiante
    Usuario <|-- ADMINISTRATIVO
    Usuario <|-- Docente

    Estudiante "0..*" --> "1" Queja : Reclamo
    Estudiante "1..*" --> "0..*" Beca : Becas
    Estudiante "1" --> "1" Estado : Estado

    Estudiante "1..*" ..> "1" ADMINISTRATIVO : Consultas
    Docente "1..*" ..> "1" ADMINISTRATIVO : Consultas

    Docente "1.*" --> "1" SolicitudAula : SolicitudAula
    SolicitudAula --> "1" Aula

    Estudiante "1..*" --> "1" Materia_Periodo : NOTA
    Materia_Periodo "1" --> "1..*" EquipoDocente : Rol
    EquipoDocente "1" --> "1" Rol

    Materia_Periodo "1..*" --> "1..*" Materia : Cuatrimestre

    Carrera "1" --> "1..*" Plan_Estudio : Plan
    Plan_Estudio "1" --> "1..*" Materia : Tiene

    Materia "1" --> "0..*" Correlatividad : Correlatividad

    NOTA ..> Materia_Periodo

```

Diagrama del proyecto creado durante Ingeniería de Software I.


# Diagrama Nuevas Funcionalidades

```mermaid
classDiagram

  

%% ─── ENUMERACIONES ───────────────────────────────────────────

class Tipo_Estado {

<<enumeration>>

Bueno

Regular

Malo

}

  

class Tipo_Estudiante {

<<enumeration>>

Ingresante

Avanzado

}

  

class Rol_Docente {

<<enumeration>>

Responsable_Catedra

Jefe_TP

Ayudante

}

  

class Tipo_Cuatrimestre {

<<enumeration>>

Primero

Segundo

}

  

class Tipo_Calificacion {

<<enumeration>>

Parcial

Recuperatorio

Final

}

  

class Estado_Inscripcion {

<<enumeration>>

Pendiente

Aprobada

Rechazada

}

  

class Estado_Aula {

<<enumeration>>

Disponible

Ocupada

Reservada

}

  

class Tipo_Aviso {

<<enumeration>>

Informativo

Urgente

Academico

}

  

class Nivel_Acceso {

<<enumeration>>

Alumno

Docente

Secretaria

Administrador

}

  

%% ─── USUARIOS ────────────────────────────────────────────────

class Usuario {

- DNI : int

- Nombre : char

- Apellido : char

- Direccion : char

- Telefono : int

- nombreUsuario : char

- contrasena : char

- nivelAcceso : Nivel_Acceso

- activo : boolean

+ login(usuario, contrasena) boolean

+ logout() void

+ cambiarContrasena(nueva) void

+ validarCredenciales() boolean

}

  

class Sesion {

- id_sesion : int

- fechaInicio : datetime

- fechaExpiracion : datetime

- tokenAcceso : char

- activa : boolean

+ iniciar(usuario) void

+ cerrar() void

+ verificarExpiracion() boolean

+ renovar() void

}

  

class Estudiante {

- E_Mail : char

- fecha_ingreso : char

- tipo : Tipo_Estudiante

- legajo : int

+ inscribirseMateria(materia) boolean

+ inscribirseExamen(examen) boolean

+ verCalificaciones() List~Calificacion~

+ verPlanEstudio() Plan_Estudio

+ verificarCorrelatividades(materia) boolean

+ consultarEstado() Estado

}

  

class Docente {

- legajoDocente : int

- email : char

- especialidad : char

+ cargarNota(inscripcion, valor, tipo) void

+ crearAviso(titulo, contenido, tipo) Aviso

+ reservarAula(solicitud) SolicitudAula

+ editarNota(calificacion, nuevoValor) void

+ verMateriasAsignadas() List~Materia~

}

  

class SecretariaAcademica {

- email : char

- sector : char

+ registrarCarrera(carrera) void

+ modificarCarrera(carrera) void

+ darBajaCarrera(id) void

+ registrarMateria(materia) void

+ asignarDocente(docente, materia) void

+ gestionarInscripcion(inscripcion) void

+ responderConsulta(consulta) void

}

  

class GestorSistema {

- email : char

+ altaSecretaria(usuario) void

+ bajaSecretaria(id) void

+ gestionarRoles() void

}

  

class Estado {

- tipo : Tipo_Estado

+ evaluarEstado(promedio) Tipo_Estado

}

  

%% ─── ACADÉMICO ───────────────────────────────────────────────

class Carrera {

- id_Carrera : int

- Nombre : char

- Descripcion : char

- duracionAnios : int

- codigo : char

+ agregarPlan(plan) void

+ getPlanVigente() Plan_Estudio

}

  

class Plan_Estudio {

- id_Plan : int

- Descripcion : char

- Horas_Plan_Estudio : int

- anioVigencia : int

+ getMaterias() List~Materia~

+ calcularHorasTotales() int

}

  

class Materia {

- Codigo : int

- Nombre : char

- Cant_Horas : int

- cupo_maximo : int

- regimen : char

+ getCorrelativas() List~Materia~

+ getCupoDisponible() int

+ agregarCorrelativa(materia) void

}

  

class Correlatividad {

- id_correlativa : int

- obligatoria : boolean

+ validar(historialAlumno) boolean

}

  

class Materia_Periodo {

- id_periodo : int

- tipo : Tipo_Cuatrimestre

- anio : int

- cuposOcupados : int

+ getCuposDisponibles() int

+ estaActiva() boolean

+ agregarInscripcion(inscripcion) void

}

  

%% ─── INSCRIPCIONES ───────────────────────────────────────────

class Inscripcion_Cursada {

- id_inscripcion : int

- fechaInscripcion : datetime

- estado : Estado_Inscripcion

+ validarCorrelatividades() boolean

+ validarCupo() boolean

+ confirmar() void

+ cancelar() void

}

  

class Inscripcion_Examen {

- id_inscripcion : int

- fechaInscripcion : datetime

- estado : Estado_Inscripcion

+ validarRegularidad() boolean

+ confirmar() void

+ cancelar() void

}

  

class Examen {

- id_examen : int

- fecha : datetime

- aula : char

- cupoMaximo : int

+ getInscriptos() List~Estudiante~

+ getCupoDisponible() int

}

  

class Acta_Examen {

- id_acta : int

- fecha : datetime

- cerrada : boolean

+ cerrarActa() void

+ agregarCalificacion(calif) void

+ getCalificaciones() List~Calificacion~

}

  

%% ─── CALIFICACIONES ──────────────────────────────────────────

class Calificacion {

- id_calificacion : int

- valor : double

- fecha : datetime

- tipo : Tipo_Calificacion

+ esAprobada() boolean

+ getValor() double

}

  

class NOTA {

- Promedio : double

+ calcularPromedio() double

+ determinarEstado() Tipo_Estado

}

  

%% ─── EQUIPOS Y ROLES ─────────────────────────────────────────

class EquipoDocente {

- MateriaQueDicta : char

- Cantidad_Integrantes : int

+ agregarDocente(docente, rol) void

+ removerDocente(docente) void

+ getIntegrantes() List~Docente~

}

  

class Rol {

- tipo : Rol_Docente

+ getPermisos() List~char~

}

  

%% ─── AULAS Y SOLICITUDES ─────────────────────────────────────

class Aula {

- Pabellon : int

- Numero : int

- capacidad : int

- estado : Estado_Aula

+ estaDisponible(horario) boolean

+ reservar(solicitud) void

+ liberar() void

}

  

class SolicitudAula {

- id_solicitud : int

- DocenteQueSolicita : char

- Horario : char

- Motivo : char

- fechaSolicitud : datetime

- aprobada : boolean

+ aprobar() void

+ rechazar() void

}

  

%% ─── COMUNICACIÓN ────────────────────────────────────────────

class Aviso {

- id_aviso : int

- titulo : char

- contenido : char

- fechaPublicacion : datetime

- tipo : Tipo_Aviso

- visible : boolean

+ publicar() void

+ archivar() void

}

  

class Consulta {

- id_consulta : int

- descripcion : char

- fecha : datetime

- respondida : boolean

- respuesta : char

+ responder(texto) void

}

  

class Queja {

- id_queja : int

- descripcion : char

- fecha : datetime

- resuelta : boolean

+ marcarResuelta() void

}

  

class Beca {

- id_Beca : int

- descripcion : char

- montoMensual : double

- activa : boolean

+ asignar(estudiante) void

+ revocar() void

}

  

%% ─── RELACIONES ──────────────────────────────────────────────

  

%% Herencia de Usuario

Usuario <|-- Estudiante

Usuario <|-- SecretariaAcademica

Usuario <|-- Docente

Usuario <|-- GestorSistema

  

%% Gestor crea SecretariaAcademica

GestorSistema ..> SecretariaAcademica : crea

  

%% Sesion

Usuario "1" --> "0..*" Sesion : gestiona

  

%% Estudiante

Estudiante "1" --> "1" Estado : tiene

Estudiante "0..*" --> "0..*" Beca : solicita

Estudiante "0..*" --> "0..*" Queja : registra

Estudiante "0..*" --> "0..*" Consulta : realiza

Estudiante "1" --> "0..*" Inscripcion_Cursada : realiza

Estudiante "1" --> "0..*" Inscripcion_Examen : realiza

  

%% Docente

Docente "0..*" --> "0..*" SolicitudAula : genera

Docente "0..*" --> "0..*" Aviso : publica

Docente "1" --> "0..*" Calificacion : carga

  

%% SecretariaAcademica

SecretariaAcademica "1" ..> "0..*" Consulta : responde

SecretariaAcademica "1" ..> "0..*" Queja : gestiona

SecretariaAcademica "1" ..> "0..*" Materia_Periodo : administra

  

%% Académico

Carrera "1" --> "1..*" Plan_Estudio : contiene

Plan_Estudio "1" --> "1..*" Materia : incluye

Materia "1" --> "0..*" Materia : requiere

Materia .. Correlatividad

Correlatividad .. Materia

Materia "1" --> "0..*" Materia_Periodo : ofertada

  

%% Equipos

Materia_Periodo "1" --> "1" EquipoDocente : asignado

EquipoDocente "1" --> "1..*" Rol : define

Docente "1..*" --> "1..*" EquipoDocente : integra

  

%% Inscripciones y calificaciones

Inscripcion_Cursada "0..*" --> "1" Materia_Periodo : corresponde

Inscripcion_Cursada "1" --> "0..*" Calificacion : genera

Inscripcion_Cursada "1" --> "1" NOTA : tiene

  

%% Examenes

Inscripcion_Examen "0..*" --> "1" Examen : para

Examen "1" --> "1" Acta_Examen : registrada

Acta_Examen "1" --> "0..*" Calificacion : contiene

Examen "0..*" --> "1" Materia_Periodo : pertenece

  

%% Aulas

SolicitudAula "0..*" --> "1" Aula : solicita

```




# Diagrama de Arquitectura del Sistema

```mermaid
flowchart TB
    %% Definición de estilos para Obsidian
    classDef actor fill:#f9d0c4,stroke:#333,stroke-width:2px,color:#000;
    classDef view fill:#fcf4cb,stroke:#333,stroke-width:2px,color:#000;
    classDef controller fill:#c4e0f9,stroke:#333,stroke-width:2px,color:#000;
    classDef model fill:#c4f9d0,stroke:#333,stroke-width:2px,color:#000;
    classDef db fill:#e2c4f9,stroke:#333,stroke-width:2px,color:#000;

    %% --- ACTORES ---
    subgraph Actores [Usuarios del Sistema]
        U1(Estudiantes):::actor
        U2(Docentes):::actor
        U3(Administrativos / Secretaría):::actor
        U4(Gestores del Sistema):::actor
    end

    %% --- CAPA DE PRESENTACIÓN (FRONTEND) ---
    subgraph CapaVista [Capa de Presentación - Frontend]
        UI[Navegador Web]:::view
        Vistas[Plantillas Mustache + TailwindCSS]:::view
    end

    %% --- CAPA DE APLICACIÓN (BACKEND) ---
    subgraph CapaLogica [Capa de Aplicación - Backend Java]
        Spark[Controlador Web y Enrutador\nSpark Java]:::controller
        LNegocio[Lógica de Negocio\nSesiones, Roles, Logs]:::controller
        Modelos[Modelos de Dominio - ActiveJDBC\nUser, Teacher, Gestor, etc.]:::model
        Config[DBConfigSingleton\nPatrón Singleton]:::model
    end

    %% --- CAPA DE PERSISTENCIA (DATOS) ---
    subgraph CapaDatos [Capa de Persistencia]
        DB[(Base de Datos\nSQLite / MySQL / PostgreSQL)]:::db
    end

    %% --- RELACIONES Y FLUJOS ---
    U1 -->|Interactúan| UI
    U2 -->|Interactúan| UI
    U3 -->|Interactúan| UI
    U4 -->|Interactúan| UI

    UI -->|Peticiones HTTP GET/POST| Spark
    Spark -->|Renderiza| Vistas
    Vistas -.->|Devuelve HTML| UI

    Spark -->|Ejecuta lógica| LNegocio
    LNegocio -->|Manipula entidades| Modelos
    Modelos -->|Obtiene conexión DB| Config
    
    %% Corrección de sintaxis para Obsidian
    Modelos -->|Consultas SQL ORM| DB
    DB -->|Resultados| Modelos
```

### Explicación breve de los componentes del diagrama:

1. **Usuarios del Sistema (Actores):** Representa a los tres grandes grupos de personas que van a interactuar con el sistema web (estudiantes, docentes y personal administrativo).
    
2. **Capa de Presentación:** Muestra la interfaz gráfica del sistema. El usuario interactúa a través del navegador, el cual recibe páginas HTML generadas a partir de **plantillas Mustache** e interfaces diseñadas con TailwindCSS.
    
3. **Capa de Aplicación (Backend):**
    
    - **Controlador (Spark Java):** Es el punto de entrada de las peticiones (login, registros, carga de notas). Su trabajo es recibir la solicitud, dirigirla a la lógica correspondiente y devolver la vista (Mustache).
        
    - **Modelos (ActiveJDBC):** Son las clases como `User` o `Teacher` que representan la lógica del negocio y las entidades mapeadas desde la base de datos de forma directa.
        
    - **Configuración (DBConfigSingleton):** Se ilustra explícitamente porque es un componente vital para el manejo eficiente de la base de datos evitando múltiples conexiones concurrentes mediante el patrón Singleton.
        
4. **Capa de Persistencia:** Muestra la base de datos que están usando. Si bien en desarrollo usan `dev.db` (SQLite), la arquitectura contempla el uso eventual de MySQL o PostgreSQL para producción como se planea en los objetivos del equipo.


# Diagrama de Diseño

```mermaid
classDiagram
    %% --- RELACIONES DE HERENCIA ---
    Usuario <|-- Estudiante
    Usuario <|-- Docente
    Usuario <|-- Administrativo
    Usuario <|-- GestorSistema

    %% --- RELACIONES DE ASOCIACIÓN ---
    Estudiante "1" --> "0..*" Inscripcion_Cursada : realiza
    Docente "1" --> "1..*" Materia_Periodo : dicta / asignado_a
    Materia "1" --> "1..*" Materia_Periodo : se oferta como
    Materia_Periodo "1" --> "0..*" Inscripcion_Cursada : recibe
    Inscripcion_Cursada "1" --> "0..*" Calificacion : genera
    Materia "1" --> "0..*" Correlatividad : posee

    %% --- CLASES PRINCIPALES ---
    class Usuario {
        <<Abstract>>
        -String nombre
        -String password
        -Nivel_Acceso rol
        +login()
    }

    class GestorSistema {
        +altaSecretaria(usuario)
        +bajaSecretaria(id)
        +gestionarRoles()
        +verLogsErrores()
    }

    class Estudiante {
        -int legajo
        -Tipo_Estudiante tipo
        +inscribirseMateria()
        +verCalificaciones()
    }

    class Docente {
        -int dni
        -String especialidad
        +cargarNota()
    }

    class Administrativo {
        -String sector
        +registrarMateria()
        +asignarDocente()
    }

    class Materia {
        -String nombre
        -int codigo
        -int cupo_maximo
        +getCorrelativas()
    }

    class Materia_Periodo {
        -int anio
        -Tipo_Cuatrimestre tipo
        +estaActiva()
    }

    class Inscripcion_Cursada {
        -Date fecha
        -Estado_Inscripcion estado
        +validarCorrelatividades()
    }

    class Calificacion {
        -double valor
        -Tipo_Calificacion tipo
        +esAprobada()
    }
```

### Explicación

1. **Herencia (Polimorfismo):** Se centraliza la autenticación y los datos comunes en la clase padre `Usuario`, de la cual heredan `Estudiante`, `Docente` y `Administrativo`. Esto coincide con tu estructura de base de datos donde manejan credenciales compartidas.
    
2. **Separación de Conceptos:** La clase `Materia` representa el concepto abstracto (ej. "Álgebra"), mientras que `Materia_Periodo` representa la instancia real que se dicta en un año y cuatrimestre específico a la que el docente se asigna y el alumno se inscribe.
    
3. **Flujo de Negocio:** Se visualiza claramente cómo una `Inscripcion_Cursada` actúa como puente entre el `Estudiante` y la `Materia_Periodo`, y cómo esta inscripción es la que eventualmente genera una `Calificacion`.






