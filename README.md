# Trayectico-Servidor

Variables .env

El docker compose necesita un archivo .env con las variables que pueda ocupar el compose en el mismo folder:
Docker 
  --->docker-compose.yml
  --->.env

Actualmente el compose necesita las siguientes variables para la BD
DB_USER=USUARIOEJEMPLO
DB_PASSWORD=PASSWORDEJEMPLO

*Para la contraseña en el docker, postgre puede tener problemas interpretando valores como / $ "
 Se escapa con $$

Adicionalmente los environment variables de spring no estan configurados en un archivo .env, estan configurados directamente en el application.properties: 
- En intellij junto a la opcion de run seleccionar el drop down
- En el drop down seleccionar "Edit configurations" 
- En "Environment variables" seleccionar "Edit environment variables" 
- Agregar variables, el nombre de la variable es igual que a como estan definidos en el application.properties pero sin el $ 
- Click OK
- Click Apply y despues OK


## 1. Prerrequisitos

Instalar las siguientes herramientas antes de iniciar:

- **Java JDK 25 LTS**
- **Git**
- **Docker Desktop**
- **IntelliJ IDEA**

## 2. Clonar el repositorio

```bash
git clone https://github.com/ariverav-cenfo/Trayectico-Servidor.git

cd Trayectico-Servidor
```

## 3. Configuración de variables de entorno

El proyecto utiliza variables de entorno para configurar la conexión a PostgreSQL.

- Docker utiliza el archivo `docker/.env`.
- Spring Boot utiliza las variables configuradas en IntelliJ IDEA.

### Preparación

1. Crear el archivo `docker/.env` a partir de `docker/.env.example`, si está disponible.
2. Configurar las siguientes variables en `docker/.env`:

   ```dotenv
   DB_USER=AgregarValor
   DB_PASSWORD=AgregarValor
   ```

3. No subir contraseñas, tokens ni credenciales reales al repositorio.

### Configuración en IntelliJ IDEA

1. Abrir el proyecto `Trayectico-Servidor`.
2. Ir a **Run → Edit Configurations**.
3. Seleccionar la configuración de ejecución del servidor o crear una configuración de tipo **Spring Boot**.
4. En **Environment variables**, agregar las variables que necesita Spring Boot, utilizando los nombres y valores correspondientes a la configuración del proyecto.
5. Guardar los cambios.

**Importante:** IntelliJ IDEA no carga automáticamente las variables de un archivo `.env` por el simple hecho de que exista. Las variables deben configurarse en la ejecución de Spring Boot.

## 4. Iniciar y administrar PostgreSQL con Docker

Desde la raíz del proyecto, ejecuta los siguientes comandos.

### Iniciar los contenedores

```powershell
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d
```

### Verificar el estado de los contenedores

```powershell
docker compose -f docker/docker-compose.yml ps
```

### Consultar los logs

```powershell
docker compose -f docker/docker-compose.yml logs -f
```

### Detener los contenedores

```powershell
docker compose -f docker/docker-compose.yml down
```

**Importante:** Las variables `POSTGRES_USER` y `POSTGRES_PASSWORD` se utilizan para inicializar PostgreSQL cuando se crea el volumen por primera vez. Si cambias las credenciales después, el cambio no se aplica automáticamente a una base de datos existente.

Para reiniciar la base de datos local con las nuevas credenciales, ejecuta:

```powershell
docker compose -f docker/docker-compose.yml down -v
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d
```

**Advertencia:** `down -v` elimina los volúmenes asociados al proyecto y, por tanto, los datos locales de PostgreSQL. Úsalo únicamente si puedes perder esos datos.

## 5. Instalar la librería CTP

La librería CTP se encuentra en:

`libs/ctp-cliente/`

Es un proyecto Maven independiente que debe instalarse localmente antes de compilar el servidor.

En Windows PowerShell:

```powershell
cd libs/ctp-cliente

.\mvnw.cmd install -DskipTests
```

Regresar a la raíz del proyecto:

```powershell
cd ../..
```

La instalación debe finalizar con `BUILD SUCCESS`.

## 6. Ejecutar el servidor

Desde la raíz de `Trayectico-Servidor`, compilar el proyecto:

```powershell
.\mvnw.cmd compile
```

### Iniciar Spring Boot

Desde IntelliJ IDEA, abre la configuración de ejecución de Spring Boot, verifica que estén configuradas las variables de entorno indicadas en la sección 3 y haz clic en **Run** para iniciar la aplicación.

El servidor debe conectarse a PostgreSQL y completar la inicialización de la aplicación sin errores.

## 7. Pruebas

Ejecutar las pruebas automatizadas:

```powershell
.\mvnw.cmd test
```

## 8. Migraciones de base de datos: Flyway

Las migraciones se encuentran en:

`src/main/wresources/db/migration`

Las migraciones deben seguir la convención de nombres de Flyway, por ejemplo:

```text
V1__initial_schema.sql
V2__add_example_table.sql
V3__update_example_column.sql
```

Reglas de trabajo:

- Utilizar versiones incrementales y únicas.
- Seguir el formato `V<version>__<description>.sql`.
- Escribir descripciones breves y representativas del cambio.
- No reutilizar versiones existentes.
- No modificar migraciones que ya se hayan aplicado en entornos compartidos; crear una migración nueva para cambios posteriores.
- Revisar el contenido y los efectos de cada migración antes de integrar.

## 9. Archivos de entorno y Git

En `.gitignore`, incluir como mínimo:

```gitignore
# Environment variables

.env

docker/.env
```

Mantener plantillas versionadas con valores ficticios:

- `.env.example`
- `docker/.env.example`

Las plantillas deben incluir los nombres de las variables necesarias y valores ilustrativos, nunca credenciales reales.

Ejemplo de formato:

```dotenv
DB_HOST=localhost
DB_PORT=5433
DB_NAME=DBDEEJEMPLO
DB_USER=USUARIOEJEMPLO
DB_PASSWORD=PASSWORDEJEMPLO
```

Este bloque es únicamente ilustrativo. Los nombres reales deben coincidir con las variables que consume la aplicación y la configuración de Docker.

## 10. Responsables por bloque

| **Bloque** | **Responsable** |
|---|---|
| Backend / Spring Boot | Christian Suncin |
| Base de datos / PostgreSQL | Christian Suncin |
| Migraciones / Flyway | Christian Suncin |
| Integración CTP | Christian Suncin |
| Infraestructura / Docker | Aaron Rivera |
| Frontend / Angular | Gabriel Guerrero |