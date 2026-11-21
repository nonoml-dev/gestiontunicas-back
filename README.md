# Gestión de túnicas · Backend

API REST para gestionar el reparto anual de túnicas de la Hermandad de la Quinta Angustia.

## Requisitos

- JDK 25, con `JAVA_HOME` apuntando a él.
- PostgreSQL 18.
- Maven no hace falta: se usa el wrapper `mvnw`.

## Variables de entorno

| Variable | Obligatoria | Uso |
|---|:-:|---|
| `DB_PASSWORD` | Sí | Contraseña del usuario `gestiontunicas` de PostgreSQL |
| `JWT_SECRET` | Solo fuera de `dev` | Clave de firma de los tokens, de al menos 32 caracteres |
| `SPRING_PROFILES_ACTIVE` | No | Perfil activo. Por defecto, `dev` |
| `ADMIN_USERNAME` | Solo en la primera instalación fuera de `dev` | Usuario del primer administrador |
| `ADMIN_PASSWORD` | Solo en la primera instalación fuera de `dev` | Contraseña inicial, de al menos 8 caracteres. Hay que cambiarla al entrar |

El primer administrador solo se crea si no hay ninguno activo. Después, esas dos variables se pueden quitar: cambiarlas no modifica ningún usuario.

## Arranque desde cero (Windows, PowerShell)

**1. Crear el usuario y la base de datos** con un usuario administrador de PostgreSQL:

```sql
CREATE ROLE gestiontunicas LOGIN PASSWORD 'tu-contraseña';
CREATE DATABASE gestiontunicas OWNER gestiontunicas;
```

**2. Definir la contraseña** en la terminal:

```powershell
$env:DB_PASSWORD = 'tu-contraseña'
```

**3. Arrancar.** Si la base de datos está vacía, se crean las tablas. Si ya tiene datos, se conservan:

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8080`. En el perfil `dev` se crea el usuario de pruebas `admin` con contraseña `1234`.

## Producción

Se activa con `SPRING_PROFILES_ACTIVE=prod`. **Si no se define, la aplicación arranca en `dev`**, con el usuario de pruebas `admin` / `1234`.

Estas variables son obligatorias, y sin alguna de ellas la aplicación no arranca:

| Variable | Uso |
|---|---|
| `DB_URL` | Conexión JDBC, por ejemplo `jdbc:postgresql://db:5432/gestiontunicas` |
| `DB_USERNAME` | Usuario de PostgreSQL |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `JWT_SECRET` | Clave propia de esta instalación, de al menos 32 caracteres. **No reutilices la de `dev`, que es pública** |

Para generar la clave en PowerShell:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
```

En este perfil, Swagger está desactivado, Actuator solo expone `/actuator/health` sin detalles y Hibernate valida el esquema sin modificarlo. No hay CORS porque Nginx sirve el front y la API en el mismo origen. El puerto del backend no debe publicarse fuera de la red interna.

## Esquema de la base de datos

- `src/main/resources/sql/esquema.sql` crea las tablas solo si la base de datos está vacía.
- `src/main/resources/sql/postcreacion-esquema.sql` añade extensiones e índices en cada arranque, sin repetirlos.
- `src/main/resources/sql/datos-iniciales.sql` añade los 5 roles y la configuración que falten, sin sobrescribir valores guardados.
- **Si cambias una entidad, actualiza también `esquema.sql`.** El CI falla si no coinciden.
- En una base de datos ya instalada, aplica el cambio con su `ALTER TABLE`, porque `esquema.sql` no modifica tablas existentes.

> ⚠️ No uses `ddl-auto=create`: **borra todos los datos** y los índices de `postcreacion-esquema.sql` hasta el siguiente arranque.

## Tests

Usan la base de datos local y no borran sus datos:

```powershell
.\mvnw.cmd test
```

## Documentación de la API

Con la aplicación arrancada: `http://localhost:8080/swagger-ui.html`

## Trabajo con Git

- Flujo de ramas: **tarea → rama del hito → `develop`**, siempre por PR. `main` no se toca hasta la v1.0.
- Para cambiar solo mayúsculas o minúsculas de un fichero, usa `git mv -f Viejo.java Nuevo.java`. Windows no distingue el cambio y, sin esto, el proyecto deja de compilar en un clon nuevo.
- No subas nunca secretos ni ficheros con datos de hermanos.
