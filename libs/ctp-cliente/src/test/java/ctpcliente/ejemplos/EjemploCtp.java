package ctpcliente.ejemplos;

import ctpcliente.CtpCliente;
import ctpcliente.core.CapaWfs;
import ctpcliente.core.Caracterizacion;
import ctpcliente.core.ConsultaWfs;
import ctpcliente.core.ConsultaWms;
import ctpcliente.core.Filtro;
import ctpcliente.core.FormatoWfs;
import ctpcliente.model.Coordenada;
import ctpcliente.model.Desplazamiento;
import ctpcliente.model.Elemento;
import ctpcliente.model.Envolvente;
import ctpcliente.model.ParadaEnRecorrido;
import ctpcliente.model.Ramal;
import ctpcliente.model.Ruta;
import ctpcliente.util.Cql;
import ctpcliente.util.Crtm05;
import ctpcliente.util.Distancia;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Recorrido de punta a punta por los tres servicios, con datos reales.
 *
 * <pre>{@code mvnw -q exec:java}</pre>
 */
public final class EjemploCtp {

    private static final PrintStream OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);

    private EjemploCtp() {
    }

    public static void main(String[] args) throws IOException {
        Path salida = Path.of("target", "ejemplo");
        Files.createDirectories(salida);

        try (CtpCliente ctp = CtpCliente.crear()) {
            titulo("1. Servicio A: del codigo publico al id interno");
            Ruta ruta = ctp.ids().ruta("0002");
            Ramal ramal = ctp.ids().ramal("0002-A");
            Desplazamiento desp = ctp.ids().desplazamiento("0002-A-2");
            OUT.printf("ruta %s -> id %d | ramal %s -> id %d (%s) | recorrido %s -> id %d%n",
                    ruta.codigo(), ruta.id(), ramal.codigo(), ramal.id(), ramal.descripcion(), desp.codigo(), desp.id());

            titulo("2. Servicio B: paradas del recorrido en orden");
            List<ParadaEnRecorrido> paradas = ctp.consultas().paradasDeRecorrido(ruta.id(), ramal.id(), desp.id());
            paradas.stream().limit(5).forEach(p -> OUT.printf("%3d  %-14s %-10s %s%n",
                    p.orden(), p.codigo(), p.tipo(), p.descripcionCorta()));
            OUT.printf("... %d paradas en total%n", paradas.size());

            titulo("3. Utilidades: coordenadas CRTM05 a WGS84");
            Coordenada primeraWgs = Crtm05.aWgs84(paradas.getFirst().ubicacion().orElseThrow());
            OUT.printf("%s esta en lat %.6f, lon %.6f%n", paradas.getFirst().codigo(), primeraWgs.latitud(),
                    primeraWgs.longitud());

            titulo("4. Servicio B: rutas que pasan por esa parada");
            OUT.println(paradas.getFirst().codigo() + " -> " + ctp.consultas().rutasPorParada(paradas.getFirst().codigo()));

            titulo("5. WFS: trazado de la ruta 0002 (GeoJSON en lat/lon)");
            List<Elemento> trazados = ctp.wfs().obtener(ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS)
                    .filtro(Cql.empiezaCon("identificador_desplazamiento", "0002-")).construir()).elementos();
            trazados.forEach(t -> OUT.printf("%s  %5d m  %d vertices%n", t.texto("identificador_desplazamiento").orElse(""),
                    t.entero("longitud").orElse(0L), t.geometria().vertices().size()));

            titulo("6. WFS + utilidades: paradas a menos de 250 m de la primera parada");
            Envolvente caja = Distancia.alrededor(primeraWgs, 250);
            List<Elemento> cercanas = ctp.wfs().obtener(ConsultaWfs.de(CapaWfs.PARADAS_DESPLAZAMIENTO_RUTA_RAMAL)
                    .propiedades("codigo_ruta", "identificador_ramal", "descripcion_corta").conGeometria()
                    .envolvente(caja, ConsultaWfs.WGS84).construir()).elementos();
            cercanas.stream()
                    .filter(e -> Distancia.metros(primeraWgs, e.geometria().vertices().getFirst()) <= 250)
                    .map(e -> e.texto("codigo_ruta").orElse("?"))
                    .distinct().sorted()
                    .forEach(r -> OUT.print(r + " "));
            OUT.println();

            titulo("7. WFS: los 5 recorridos activos mas largos del pais");
            ctp.wfs().obtener(ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS)
                            .propiedades("identificador_desplazamiento", "descripcion", "longitud")
                            .filtro(Cql.y(Cql.igual("estado", "activo"), Cql.mayorQue("longitud", 0)))
                            .ordenarPor("longitud", true).maximo(5).construir())
                    .elementos()
                    .forEach(e -> OUT.printf("%-10s %6.1f km  %s%n", e.texto("identificador_desplazamiento").orElse(""),
                            e.entero("longitud").orElse(0L) / 1000.0, e.texto("descripcion").orElse("")));

            titulo("8. WFS: conteos y recorrido en flujo");
            long total = ctp.wfs().contar(ConsultaWfs.de(CapaWfs.PARADA_CONSULTABLE).construir());
            long conCaseta = ctp.wfs().contar(ConsultaWfs.de(CapaWfs.PARADA_CONSULTABLE)
                    .filtro(Cql.en("tipo_caseta", List.of("2", "3"))).construir());
            OUT.printf("paradas fisicas: %d, con caseta: %d (%.1f %%)%n", total, conCaseta, 100.0 * conCaseta / total);
            long[] anillos = {0};
            ctp.wfs().recorrer(ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).propiedades("sentido").construir(),
                    e -> anillos[0] += e.texto("sentido").filter(s -> s.startsWith("ANILLO")).isPresent() ? 1 : 0);
            OUT.println("recorridos circulares: " + anillos[0]);

            titulo("9. Servicio B + A: rutas sobre la Ruta Nacional 27");
            String idRn27 = ctp.ids().carretera(27).id();
            List<String> rutas27 = ctp.consultas().consultar(Caracterizacion.RUTAS_POR_CARRETERA,
                    Filtro.carreteraPorId(idRn27)).stream().map(f -> f.texto("codigo_ruta").orElse("")).sorted().toList();
            OUT.println(rutas27.size() + " rutas: " + rutas27.subList(0, Math.min(10, rutas27.size())) + " ...");

            titulo("10. WMS y descargas");
            Envolvente centro = Envolvente.de(trazados.stream().flatMap(t -> t.geometria().vertices().stream()).toList());
            byte[] png = ctp.wms().mapa(ConsultaWms.de("personalizada_desplazamientos_de_ruta_ramal")
                    .envolvente(centro, ConsultaWfs.WGS84).vista("ruta", ruta.id()).tamano(600, 400).construir());
            Path mapa = Files.write(salida.resolve("ruta-0002.png"), png);
            Path csv = ctp.wfs().descargar(ConsultaWfs.de(CapaWfs.PARADAS_TERMINALES_UBICACION)
                    .filtro(Cql.igual("provincia", "Heredia")).construir(), FormatoWfs.CSV,
                    salida.resolve("terminales-heredia.csv"));
            OUT.printf("mapa: %s (%d bytes)%ncsv:  %s (%d bytes)%n", mapa, png.length, csv, Files.size(csv));
        }
    }

    private static void titulo(String texto) {
        OUT.println();
        OUT.println("== " + texto);
    }
}
