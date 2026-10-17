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

Adicionalmente los environment variables de spring no estan configurados en un archivo .env, estan configurados directamente en el application properties: 
1- En intellij junto a la opcion de run seleccionar el drop down
2- En el drop down seleccionar "Edit configurations" 
3- En "Environment variables" seleccionar "Edit environment variables" 
4- Agregar variables, el nombre de la variable es igual que a como estan definidos en el application settings pero sin el $ 
5- Click OK
6- Click Apply y despues OK


## 1. Prerrequisitos

Instalar las siguientes herramientas antes de iniciar:

- **Java JDK 25 LTS**
- **Git**
- **Docker Desktop**
- **IntelliJ IDEA**
- **Maven Wrapper**, incluido en el repositorio mediante `mvnw` y `mvnw.cmd`

## 2. Clonar el repositorio

```bash
git clone https://github.com/ariverav-cenfo/Trayectico-Servidor.git

cd Trayectico-Servidor
```

## 3. Configuración de variables de entorno

El proyecto utiliza dos ubicaciones para variables de entorno:

- `.env` en la raíz del proyecto.
- `docker/.env`, utilizado para la configuración relacionada con Docker.

### Preparación

1. Crear los archivos `.env`.
2. Ambos .env necesitan las variables: `DB_USER=AgregarValor` y `DB_PASSWORD=AgregarValor`.
3. No subir contraseñas, tokens ni credenciales reales al repositorio.

### Configuración en IntelliJ IDEA

1. Abrir el proyecto `Trayectico-Servidor`.
2. Ir a **Run → Edit Configurations**.
3. Seleccionar la configuración de ejecución del servidor o crear una configuración de tipo Spring Boot.
4. Configurar las variables de entorno que necesita el backend, agregando la ruta de .env en Environment variables. Usar el .env de la raíz del proyecto.
5. Guardar los cambios.

**Importante:** IntelliJ no carga automáticamente cualquier archivo `.env` por el simple hecho de existir. Se deben introducir en la configuración de ejecución.

## 4. PostgreSQL con Docker Compose

Docker Compose se utiliza para levantar la base de datos PostgreSQL. El servidor se ejecuta por separado.

Abrir Docker Desktop, luego, desde la raíz del proyecto:

```bash
docker compose up -d
```

Para revisar los servicios:

```bash
docker compose ps
```

Para consultar los registros:

```bash
docker compose logs
```

Para detener los servicios:

```bash
docker compose down
```

Antes de iniciar el backend, verificar que PostgreSQL esté ejecutándose y que las credenciales, el puerto y el nombre de la base de datos coincidan con la configuración del servidor.

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

```powershell
.\mvnw.cmd spring-boot:run
```

Antes de utilizarlo, confirmar que el plugin está configurado y que las variables de entorno necesarias están disponibles, si se siguieron los pasos anteriores, esto debería estar cubierto.

El servidor debe conectarse a PostgreSQL y completar la inicialización de la aplicación sin errores.

## 7. Pruebas

Ejecutar las pruebas automatizadas:

```powershell
.\mvnw.cmd test
```

## 8. Migraciones de base de datos: Flyway

Las migraciones se encuentran en:

`db/migrations/`

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

DB_PORT=5432

DB_NAME=example_db

DB_USERNAME=example_user

DB_PASSWORD=replace_me
```

Este bloque es únicamente ilustrativo. Los nombres reales deben coincidir con las variables que consume la aplicación y la configuración de Docker.

## 10. Responsables por bloque

Los responsables todavía no están definidos o no han sido identificados.

| **Bloque** | **Responsable** |
|---|---|
| Backend / Spring Boot | Christian Suncin |
| Base de datos / PostgreSQL | Christian Suncin |
| Migraciones / Flyway | Christian Suncin |
| Integración CTP | Christian Suncin |
| Infraestructura / Docker | Aaron Rivera |
| Frontend / Angular | Gabriel Guerrero |
