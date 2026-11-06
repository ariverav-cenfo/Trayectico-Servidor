# Trayectico-Servidor
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

`src/main/resources/db/migration`

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

## 10. Importar tarifas de ARESEP

Cada integrante debe ejecutar la importación en su base local. Los datos cargados
no se comparten mediante Git; se comparten el código y las migraciones.

Primero iniciar PostgreSQL con Docker (sección 4) e instalar la biblioteca CTP
(sección 5). La conexión actual del servidor es `localhost:5433/TrayecticoDB`.
No es necesario instalar PostgreSQL Server ni pgAdmin en Windows.

### Credenciales en PowerShell

Las variables configuradas en IntelliJ o en `docker/.env` no se transfieren
automáticamente a PowerShell. En la misma terminal donde se ejecutará Maven:

```powershell
$env:DB_USER = Read-Host "Usuario de PostgreSQL"
$credencial = Get-Credential -UserName $env:DB_USER -Message "Contraseña de PostgreSQL"
$env:DB_PASSWORD = $credencial.GetNetworkCredential().Password
Remove-Variable credencial
```

Usar las credenciales con las que se inicializó el contenedor. Estas variables
permanecen disponibles durante la sesión de esa terminal; no se guardan en Git.

### Ejecutar la importación

Desde la raíz del proyecto:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--aresep.import-on-startup=true --spring.main.web-application-type=none"
```

Flyway aplica `V5__aresep_fares.sql`. Despues se consulta el servicio publico de
ARESEP y se importan las tarifas. La opcion esta desactivada por defecto;
el arranque normal no descarga datos. Esta ejecucion manual no programa actualizaciones diarias.

El proceso valida la respuesta completa antes de escribir y guarda todos los lotes
en una transaccion. Un error conserva la carga anterior y se registra en la salida.
Repetirlo actualiza por `codigo_fraccion` sin duplicar filas; una vigencia anterior
no reemplaza una mas reciente. No se eliminan tarifas ausentes de una descarga:
todavia debe verificarse el alcance del servicio y su tratamiento de bajas.
Los codigos ARESEP se conservan sin asumir equivalencias con CTP.

La URL puede configurarse con `ARESEP_URL`; el tiempo limite de respuesta se
configura con `aresep.timeout-seconds` (120 segundos por defecto).
`fecha_importacion` indica la ultima importacion de cada tarifa, no su fecha de vigencia.

La migración `V5__aresep_fares.sql` crea `payment_method` y la migración `V6__correct_ctp_schema.sql` lo renombra a `metodo_pago`, un campo de
texto opcional para registrar manualmente los métodos de pago verificados.
`NULL` significa que no se conoce el dato. Se pueden anotar varios métodos, por
ejemplo `Efectivo, Tarjeta`, únicamente cuando estén confirmados. La importación
de ARESEP no modifica este campo. Flyway aplica la migración al iniciar el servidor
contra una base nueva. Crear o iniciar el contenedor por sí solo no ejecuta Flyway.
Si esta migración ya se aplicó antes de agregar la columna, editar el archivo no actualiza esa
base y puede provocar un error de checksum. Esa base necesita una actualización
coordinada de su esquema e historial de migraciones antes del siguiente arranque.

La confirmación de éxito es el mensaje `Importacion ARESEP completada`.
`BUILD SUCCESS` por sí solo no confirma la carga: revisar también los errores
de arranque de Spring Boot y Flyway.

### Comprobar los datos sin pgAdmin

Entrar a PostgreSQL desde PowerShell, con `DB_USER` configurada:

```powershell
docker exec -it postgres_db psql -U $env:DB_USER -d TrayecticoDB
```

Dentro de `psql`, listar las tablas con `\dt` y consultar:

```sql
SELECT count(*) AS tarifas, max(fecha_importacion) AS ultima_importacion
FROM tarifas_aresep;

SELECT codigo_ruta, codigo_fraccion, nombre_fraccion, tarifa_regular, fecha_vigencia
FROM tarifas_aresep
ORDER BY codigo_fraccion
LIMIT 20;
```

Repetir la importación y consultar de nuevo. Si ARESEP devuelve los mismos
registros, la cantidad se mantiene y `fecha_importacion` se actualiza para las tarifas
aceptadas. Si publica fraccionamientos nuevos, la cantidad puede aumentar.
Salir de `psql` con `\q` antes de ejecutar comandos de PowerShell.

### Problemas frecuentes

- `password authentication failed for user "${DB_USER}"`: falta configurar
  `DB_USER` en la terminal que ejecuta Maven. Configurar también `DB_PASSWORD`.
- Error de contraseña con un usuario real: comprobar las credenciales originales
  del contenedor; cambiar `docker/.env` no cambia la contraseña de un volumen existente.
- `relation "tarifas_aresep" does not exist`: comprobar que el servidor se conectó
  a esta misma base y que Flyway aplicó `V6__correct_ctp_schema.sql` sin errores.
- Indicador `TrayecticoDB-#`: hay una sentencia pendiente. Cancelarla con Ctrl+C
  y volver a escribir la consulta SQL terminada en punto y coma.

Pruebas unitarias del importador, sin consultar ARESEP ni usar PostgreSQL:

```powershell
.\mvnw.cmd "-Dtest=AresepClientTest,AresepImportServiceTest" test
```

## 11. Importar rutas del CTP

Igual que con ARESEP, cada integrante corre la importación en su base local. Antes hay que
levantar PostgreSQL (sección 4), instalar la biblioteca CTP (sección 5) y configurar las
credenciales en la terminal (sección 10, "Credenciales en PowerShell").

### Ejecutar la importación

Desde la raíz del proyecto, con el grupo de rutas de SCRUM-87:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--trayectico.importar-ruta=0002,0090,0403,0007,0008,0009,0010,0016 --spring.main.web-application-type=none"
```

Para una sola ruta, `--trayectico.importar-ruta=0002`. Las comillas son necesarias:
sin ellas, PowerShell puede partir el argumento por las comas y por el `-D`.

- Cada ruta se lee del CTP y se guarda en su propia transacción. Solo se importan los
  recorridos que el CTP marca como activos.
- Si una ruta falla (el CTP no responde, el código no existe o una parada viene sin
  ubicación), se registra con `ERROR` y se sigue con las demás. Al final, si alguna
  falló, el arranque termina con error y el mensaje dice cuáles y por qué.
- Reimportar no duplica: actualiza por `codigo_ctp`. Para reintentar basta con correr
  solo las rutas que fallaron, por ejemplo `--trayectico.importar-ruta=0403`.
- Se ignoran los espacios y los códigos repetidos. Una lista sin códigos da error
  antes de consultar al CTP.

La confirmación de éxito es `Importacion terminada: 8 de 8 rutas importadas`, y antes,
una línea `Ruta ... importada` por cada ruta. Igual que con ARESEP, `BUILD SUCCESS`
por sí solo no confirma la carga.

### Grupo de rutas de SCRUM-87

Con solo la 0002 no hay transbordos que calcular. El grupo son las rutas que comparten
paradas con la 0002, elegidas con `RutasConectadas`
(`libs/ctp-cliente/src/test/java/ctpcliente/ejemplos/`). El 28 de octubre de 2026 la
0002 tenía 38 paradas distintas y 23 rutas compartían al menos una con ella. Se tomaron
las que comparten 4 o más, que eran 8, y se excluyó la 0300 (ver abajo). Quedan 7, dentro
del rango de 5 a 10 del ticket. Con un corte en 3 habrían sido 11.

| Ruta | Paradas compartidas con la 0002 | Ejemplo de parada compartida |
|---|---|---|
| 0002 | Ruta base del grupo | |
| 0090 | 7 | PBA-10103-002 (contiguo a Terminal Puntarenas) |
| 0403 | 6 | PBA-10102-049 (en frente al Más por Menos) |
| 0007 | 5 | PBA-10103-007 (en el Parque Beneméritos) |
| 0008 | 5 | PBA-10102-100 (Katana) |
| 0009 | 4 | PBA-10102-020 (casi en frente del Palí) |
| 0010 | 4 | PBA-10102-037 (detrás de la Toyota) |
| 0016 | 4 | PBA-10101-021 (antigua Litografía Casa Gráfica) |

**Ruta excluida: 0300.** También comparte 4 paradas con la 0002, pero su recorrido
`0300-M-1` trae dos paradas distintas con el mismo orden (24): PBA-30304-075 (El Cierre)
y PBA-30303-018 (sobre calle vieja a Cartago). La restricción `uq_stop_displacement_order`
de la V2 no permite dos paradas en la misma posición de un recorrido, así que la
importación de la 0300 falla y se revierte completa. Falta decidir qué hacer con ese
dato del CTP (por ejemplo, saltarse el recorrido o quedarse con una de las dos paradas);
mientras tanto, la 0300 queda fuera del grupo.

**Ruta sin tarifa: 0403.** No aparece en los datos de tarifas de ARESEP (SCRUM-36). Se
mantiene en el grupo porque comparte 6 paradas con la 0002, pero según la decisión del
2 de noviembre de 2026 no se le carga tarifa. El primer viaje debe usar rutas que sí tienen
tarifa.

`RutasConectadas` cuenta las paradas de todos los recorridos de la 0002, y el importador
solo guarda los activos. Por eso, en la base pueden quedar menos paradas compartidas que
las de la tabla. La consulta de la siguiente sección muestra las que quedaron: el 28 de
octubre de 2026, con estas 8 rutas, devolvió 58 paradas compartidas por al menos dos
rutas, y la que más comparte es PBA-10103-007 (Parque Beneméritos), con 5.

Para repetir la elección, desde `libs/ctp-cliente`:

```powershell
.\mvnw.cmd -q test-compile dependency:build-classpath "-Dmdep.outputFile=target/cp.txt"
java '-Djava.util.logging.SimpleFormatter.format=%4$s %5$s%6$s%n' -cp "target/classes;target/test-classes;$(Get-Content target/cp.txt)" ctpcliente.ejemplos.RutasConectadas 0002
```

### Comprobar los datos

Entrar a `psql` como en la sección 10:

```powershell
docker exec -it postgres_db psql -U $env:DB_USER -d TrayecticoDB
```

Rutas importadas, con sus ramales y recorridos:

```sql
SELECT r.codigo_ctp AS ruta,
       count(DISTINCT b.id) AS ramales,
       count(DISTINCT d.id) AS recorridos
FROM rutas r
       LEFT JOIN ramales b ON b.ruta_id = r.id
       LEFT JOIN recorridos d ON d.ramal_id = b.id
GROUP BY r.codigo_ctp
ORDER BY r.codigo_ctp;
```

Paradas compartidas por al menos dos rutas. Si devuelve filas, hay transbordos posibles:

```sql
SELECT s.codigo_ctp AS parada,
       s.descripcion AS descripcion,
       string_agg(DISTINCT r.codigo_ctp, ', ' ORDER BY r.codigo_ctp) AS rutas,
       count(DISTINCT r.id) AS cantidad_rutas
FROM paradas s
       JOIN paradas_recorridos sd ON sd.parada_id = s.id
       JOIN recorridos d ON d.id = sd.recorrido_id
       JOIN ramales b ON b.id = d.ramal_id
       JOIN rutas r ON r.id = b.ruta_id
GROUP BY s.id, s.codigo_ctp, s.descripcion
HAVING count(DISTINCT r.id) >= 2
ORDER BY cantidad_rutas DESC, s.codigo_ctp;
```

Para comprobar que reimportar no duplica, correr este conteo, repetir la importación y
correrlo de nuevo. Si el CTP no cambió nada entre las dos corridas, los números son iguales:

```sql
SELECT (SELECT count(*) FROM route)             AS rutas,
       (SELECT count(*) FROM branch)            AS ramales,
       (SELECT count(*) FROM displacement)      AS recorridos,
       (SELECT count(*) FROM stop)              AS paradas,
       (SELECT count(*) FROM stop_displacement) AS paradas_en_recorridos;
```

Pruebas del importador, sin consultar al CTP:

```powershell
.\mvnw.cmd "-Dtest=ImportadorRutaTest,RutaDAOTest" test
```

`RutaDAOTest` usa Testcontainers, así que Docker Desktop tiene que estar abierto.

## 12. Responsables por bloque

| **Bloque** | **Responsable** |
|---|---|
| Backend / Spring Boot | Christian Suncin |
| Base de datos / PostgreSQL | Christian Suncin |
| Migraciones / Flyway | Christian Suncin |
| Integración CTP | Christian Suncin |
| Infraestructura / Docker | Aaron Rivera |
| Frontend / Angular | Gabriel Guerrero |