# ctp-cliente

Cliente Java (no oficial) para los servicios publicos del Visor de Rutas del Consejo de Transporte
Publico de Costa Rica (https://visortp.ctp.go.cr/).

## Requisitos

- JDK 25 (Oracle JDK, la distribución oficial). Maven no es necesario: el proyecto trae `mvnw`.

## Comandos

| Comando | Que hace |
|---|---|
| `./mvnw test` | Pruebas unitarias (sin red) |
| `./mvnw verify` | Pruebas unitarias, cobertura y analisis estatico (Checkstyle, PMD, CPD, SpotBugs) |
| `./mvnw verify -Pintegracion` | Todo lo anterior mas las pruebas contra los servidores reales |
| `./mvnw -q test-compile exec:java` | Ejemplo de punta a punta con datos reales |

En Windows use `mvnw.cmd` en lugar de `./mvnw`.

## Uso minimo

```java
try (CtpCliente ctp = CtpCliente.crear()) {
    int idRuta = ctp.ids().ruta("0002").id();
    List<ParadaEnRecorrido> paradas = ctp.consultas().paradasDeRuta(idRuta);
}
```

La documentacion completa esta en `../CTP-API-Java.pdf`.
