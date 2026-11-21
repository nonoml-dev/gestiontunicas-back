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

**3. Crear las tablas en el primer arranque.** Hibernate las genera con `create`.

> ⚠️ `create` **borra todos los datos** de la base de datos. Úsalo solo con una base de datos nueva.

```powershell
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'create'
.\mvnw.cmd spring-boot:run
```

Cuando termine de arrancar, páralo con `Ctrl+C` y **quita la variable**:

```powershell
Remove-Item Env:SPRING_JPA_HIBERNATE_DDL_AUTO
```

**4. Arrancar con normalidad.** A partir de aquí los datos se conservan:

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8080`. En el perfil `dev` se crea el usuario de pruebas `admin` con contraseña `1234`.

## Tests

Necesitan la base de datos local con las tablas ya creadas:

```powershell
.\mvnw.cmd test
```

> ⚠️ Comprueba antes que `SPRING_JPA_HIBERNATE_DDL_AUTO` no está definida: con `create`, los tests también borrarían los datos.

## Documentación de la API

Con la aplicación arrancada: `http://localhost:8080/swagger-ui.html`

## Trabajo con Git

- Flujo de ramas: **tarea → rama del hito → `develop`**, siempre por PR. `main` no se toca hasta la v1.0.
- Para cambiar solo mayúsculas o minúsculas de un fichero, usa `git mv -f Viejo.java Nuevo.java`. Windows no distingue el cambio y, sin esto, el proyecto deja de compilar en un clon nuevo.
- No subas nunca secretos ni ficheros con datos de hermanos.
