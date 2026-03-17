-- Elimina la tabla 'users' si ya existe para asegurar un inicio limpio


-- Crea la tabla 'users' con los campos originales, adaptados para SQLite
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT, -- Clave primaria autoincremental para SQLite
    name TEXT NOT NULL UNIQUE,          -- Nombre de usuario (TEXT es el tipo de cadena recomendado para SQLite), con restricción UNIQUE
    password TEXT NOT NULL           -- Contraseña hasheada (TEXT es el tipo de cadena recomendado para SQLite)
);



CREATE TABLE teacher (
    -- 1. Usamos un 'id' autoincremental, igual que en 'users'
    id INTEGER PRIMARY KEY AUTOINCREMENT, 
    
    -- 2. El DNI es un campo de texto, separado, y nos aseguramos de que sea ÚNICO
    dni TEXT UNIQUE NOT NULL, 
    
    name TEXT NOT NULL,
    lastName TEXT NOT NULL,
    
    -- 3. 'adress' (cuidado, se escribe 'address' con doble 'd')
    address TEXT, 
    
    -- 4. El teléfono como TEXT
    phone INTEGER
);