## Carga de equivalencias y tarifas por ramal (SCRUM-91)

La tabla `equivalencias_ramales` (migración `V8__crear_equivalencias_ramales.sql`) une cada
ramal del CTP con su ramal de ARESEP. Con ella se puede consultar la tarifa de un ramal del CTP.
Se llena desde `data/equivalencias-ramales.csv` con un importador; nunca a mano en la base.

### Qué se carga y qué no

- Solo las filas `NOMBRE_EXACTO` del CSV. Los demás tipos (`NOMBRE_AMBIGUO`, `NOMBRE_PARECIDO`,
  `REVISAR`, `SIN_RUTA_ARESEP`) se descartan hasta confirmarlos a mano.
- Solo los ramales que ya están importados en `ramales`. El resto se carga cuando se importe
  el catálogo completo del CTP; basta con volver a correr la carga.
- Solo los ramales de ARESEP que tienen tarifa en `tarifas_aresep`.
- `acepta_pago_electronico` queda en `NULL` (no verificado). Volver a correr la carga no lo
  modifica: si alguien lo llenó a mano, se conserva.

El `CHECK` de `tipo_coincidencia` acepta todos los tipos del CSV para que, una vez confirmados
los ambiguos y parecidos, se puedan cargar sin otra migración.

### Orden de carga desde una base vacía

La carga no es una migración de Flyway: Flyway corre antes de que existan rutas y tarifas, y la
llave foránea a `ramales` no dejaría cargar nada. Por eso hay que seguir este orden, en la misma
terminal y con las credenciales configuradas (sección 10, "Credenciales en PowerShell"):

1. Levantar PostgreSQL (sección 4).
2. Importar las rutas del CTP (sección 11). Al arrancar, Flyway aplica las migraciones.
3. Importar las tarifas de ARESEP (sección 10).
4. Cargar las equivalencias:

   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--trayectico.importar-equivalencias=data/equivalencias-ramales.csv --spring.main.web-application-type=none"
   ```

Si se corre antes de importar las rutas, todo se descarta como "ramal no importado"; si se corre
antes de las tarifas, como "sin tarifa en ARESEP". En ambos casos basta con completar los pasos
anteriores y repetirla. Repetirla no duplica filas: actualiza por `id_ramal_ctp`.

La confirmación es la línea `Carga de equivalencias terminada`. El 8 de noviembre de 2026, con las
rutas de SCRUM-87, dio: 1485 filas leídas, 39 cargadas; descartadas 710 por tipo de coincidencia,
736 por ramal no importado y 0 sin tarifa en ARESEP.

### Tarifa de un ramal

ARESEP publica una tarifa por fraccionamiento (tramo). `EquivalenciaRamalDAO.buscarTarifaRamal`
devuelve la tarifa del viaje completo (la más alta) y la cantidad de fraccionamientos del ramal.
Si el ramal tiene un solo fraccionamiento, la tarifa es exacta; si tiene varios, depende de dónde
suba y baje el usuario y no es exacta. Todos los ramales de la 0002 y de las rutas de SCRUM-87
tienen un solo fraccionamiento.

### Comprobar los datos

Con `DB_USER` configurada, esta consulta muestra la tarifa de cada ramal de la 0002 y de las rutas
de SCRUM-87. Los `NOMBRE_EXACTO` deben tener tarifa; los demás aparecen vacíos, igual que la 0403,
que ARESEP no publica.

```sql
SELECT r.codigo_ctp              AS ruta,
       b.codigo_ctp              AS ramal,
       e.tipo_coincidencia,
       e.codigo_ramal_aresep,
       count(t.id)               AS fracciones,
       max(t.tarifa_regular)     AS tarifa_viaje_completo,
       e.acepta_pago_electronico
FROM rutas r
JOIN ramales b                    ON b.ruta_id = r.id
LEFT JOIN equivalencias_ramales e ON e.id_ramal_ctp = b.id_ctp
LEFT JOIN tarifas_aresep t        ON t.codigo_ramal = e.codigo_ramal_aresep
WHERE r.codigo_ctp IN ('0002','0090','0403','0007','0008','0009','0010','0016')
GROUP BY r.codigo_ctp, b.codigo_ctp, e.tipo_coincidencia, e.codigo_ramal_aresep, e.acepta_pago_electronico
ORDER BY r.codigo_ctp, b.codigo_ctp;
```

Pruebas de la carga (usan Testcontainers, Docker Desktop tiene que estar abierto):

```powershell
.\mvnw.cmd "-Dtest=EquivalenciaRamalDAOTest,CargaEquivalenciasServiceTest" test
```
ARESEP a veces publica el mismo fraccionamiento dos veces (en noviembre de 2026, la T7223, que solo
cambiaba el promedio de kilómetros). Si las dos filas tienen la misma ruta, ramal, tarifas y fecha
de vigencia, se importa la primera y queda un `WARN` en la salida. Si alguno de esos datos cambia,
la importación se rechaza completa, porque no se puede saber cuál tarifa es la correcta.