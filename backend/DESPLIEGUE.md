# Despliegue: Render (backend) + Neon (base de datos)

Guía paso a paso para dejar la API pública y accesible desde el teléfono.

```
App Android  ──HTTPS──▶  Render (Spring Boot)  ──▶  Neon (PostgreSQL)
     │
     └──▶ SQLite local (Room) + sincronización
```

---

## 1. Neon — crear la base de datos

1. Entra a <https://neon.com> y crea un proyecto.
   - **Region:** la más cercana (por ejemplo `AWS us-east-2`).
   - **Postgres version:** la que venga por defecto.
2. En el panel del proyecto, botón **Connect** → pestaña **Connection string**.
3. Copia la cadena. Se ve así:

   ```
   postgresql://neondb_owner:AbC123xyz@ep-cool-name-123456.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```

4. **Desactiva el toggle "Connection pooling"** para este caso. Render mantiene
   conexiones vivas y el pooler de Neon (PgBouncer) da problemas con Hibernate.
   Usa el host **sin** `-pooler`.

5. Separa los datos en tres partes:

   | Variable | Valor |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://ep-cool-name-123456.us-east-2.aws.neon.tech/neondb?sslmode=require` |
   | `SPRING_DATASOURCE_USERNAME` | `neondb_owner` |
   | `SPRING_DATASOURCE_PASSWORD` | `AbC123xyz` |

   > **Importante:** a la URL hay que cambiarle `postgresql://` por
   > `jdbc:postgresql://`. Es lo único que hay que editar a mano.

No hace falta crear tablas: Hibernate las crea al arrancar (`ddl-auto=update`)
y `DataInitializer` carga los 8 espacios y los 2 usuarios.

---

## 2. GitHub — un solo repositorio (monorepo)

El backend vive **dentro del repo de la app**, en `backend/`:

```
App-Movil-EV-2/
├── app/            ← app Android (Compose)
├── gradle/
├── backend/        ← API Spring Boot
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/
└── render.yaml     ← blueprint de Render
```

Ventajas: un solo link para la defensa, un solo `git push`, y el repo refleja
el diagrama de arquitectura (app + API + BD).

```powershell
cd C:\Users\po-alumno\Downloads\colegio\App-Movil-EV-2
git add backend render.yaml
git commit -m "Agrega backend Spring Boot con configuracion de Render"
git push
```

> El `settings.gradle.kts` de la app solo incluye `:app`, así que el backend
> no interfiere con el build de Android.

---

## 3. Render — crear el Web Service

1. Dashboard → **New** → **Web Service**.
2. Conecta el repo `App-Movil-EV-2`.
3. Configuración:

   | Campo | Valor |
   |---|---|
   | **Language / Runtime** | `Docker` |
   | **Branch** | `main` |
   | **Root Directory** | `backend` ← **clave** |
   | **Dockerfile Path** | `./Dockerfile` |
   | **Instance Type** | `Free` |
   | **Health Check Path** | `/api/health` |

   **Root Directory = `backend`** es lo que hace que Render construya solo la
   API y no intente compilar la app Android.

   Alternativa: en vez de "Web Service", usa **New → Blueprint** y Render lee
   el `render.yaml` de la raíz, que ya trae `rootDir: backend` y un
   `buildFilter` para que un commit de la app no reconstruya el backend.

4. **Environment Variables** (pestaña *Environment*):

   | Key | Value |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://...` |
   | `SPRING_DATASOURCE_USERNAME` | `neondb_owner` |
   | `SPRING_DATASOURCE_PASSWORD` | `...` |
   | `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` |
   | `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75` |

5. **Create Web Service**. El primer build tarda ~4–6 minutos
   (descarga Gradle 9.9.2 y las dependencias).

---

## 4. Verificar

Tu URL queda como `https://marcela-paz-api.onrender.com`.

```powershell
curl https://marcela-paz-api.onrender.com/api/health
```

Debe responder:

```json
{ "estado": "ok", "servicio": "marcela-paz-backend", "baseDeDatos": "ok" }
```

Si `baseDeDatos` dice `error`, revisa que la URL empiece con `jdbc:postgresql://`
y que no tenga `-pooler` en el host.

Prueba también el seed:

```powershell
curl https://marcela-paz-api.onrender.com/api/espacios
curl https://marcela-paz-api.onrender.com/api/auth/login -Method Post -ContentType "application/json" -Body '{"email":"docente@colegio.cl","password":"123456"}'
```

Swagger: `https://marcela-paz-api.onrender.com/swagger-ui.html`

---

## 5. Conectar la app Android

En `App-Movil-EV-2`, el `BASE_URL` de Retrofit debe quedar con la URL de Render:

```kotlin
private const val BASE_URL = "https://marcela-paz-api.onrender.com/"
```

**Siempre con `/` al final** (Retrofit lo exige) y **https**, no http.

---

## 6. Antes de la defensa ⚠️

El plan gratuito de Render **duerme el servicio a los 15 minutos** sin tráfico.
La primera petición después de eso tarda **30–60 segundos**.

Antes de entrar a defender, abre en el navegador:

```
https://marcela-paz-api.onrender.com/api/health
```

Cuando responda `"estado":"ok"`, ya está despierto. Hazlo unos 10 minutos antes
y no dejes que pasen 15 minutos sin tocarlo.

Neon también suspende el compute en el plan gratuito, pero despierta en ~1 segundo
y no afecta la demo.

---

## 7. Problemas típicos

| Síntoma | Causa | Solución |
|---|---|---|
| `Connection refused` a `localhost:5432` | Se levantó con perfil `local` y no hay Postgres local | Verifica `SPRING_PROFILES_ACTIVE=prod` |
| `FATAL: password authentication failed` | Password con caracteres especiales sin escapar | Regenera la contraseña en Neon sin `@ : / ?` |
| `The connection attempt failed` + `-pooler` | PgBouncer no soporta prepared statements | Quita `-pooler` del host |
| Build falla en `./gradlew` | `gradlew` sin permiso de ejecución | Ya está resuelto en el `Dockerfile` (`sed` + `chmod`) |
| Arranca pero 502 | Puerto distinto de `$PORT` | `server.port=${PORT:8080}` ya está en `application.properties` |
| Los datos desaparecen al reiniciar | `ddl-auto=create-drop` | Ya está en `update` |
