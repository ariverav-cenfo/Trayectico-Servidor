package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.model.AresepFare;
import com.example.trayecticoservidor.model.EquivalenciaRamal;
import com.example.trayecticoservidor.model.TipoCoincidencia;
import ctpcliente.model.Ramal;
import ctpcliente.model.Ruta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CruceTarifasServiceTest {

    private final CruceTarifasService service = new CruceTarifasService();

    private static AresepFare tarifa(String ruta, String ramal, String nombreRamal, String fraccion, String nombreFraccion) {
        return new AresepFare(ruta, nombreRamal, ramal, nombreRamal, fraccion, nombreFraccion, null,
                new BigDecimal("240.00"), BigDecimal.ZERO, null, null, null, null, null,
                LocalDate.of(2026, 10, 28), null, null, null, null);
    }

    @Test
    void completaConCerosSoloLosCodigosNumericos() {
        assertEquals("0002", CruceTarifasService.codigoExacto("02"));
        assertEquals("0427", CruceTarifasService.codigoExacto(" 427 "));
        assertEquals("1001", CruceTarifasService.codigoExacto("1001"));
        assertEquals("66 BS", CruceTarifasService.codigoExacto("66 BS"));
    }

    @Test
    void extraeElNumeroInicialSinSufijos() {
        assertEquals(Optional.of("0066"), CruceTarifasService.codigoNumerico("66 BS"));
        assertEquals(Optional.of("0617"), CruceTarifasService.codigoNumerico("617-618"));
        assertEquals(Optional.of("0002"), CruceTarifasService.codigoNumerico("0002"));
        assertEquals(Optional.empty(), CruceTarifasService.codigoNumerico("I-1"));
    }

    @Test
    void normalizaTildesBarrioViceversaYSignos() {
        String ctp = CruceTarifasService.normalizarNombre("SAN JOSÉ - B° LA PITAHAYA ");
        String aresep = CruceTarifasService.normalizarNombre("SAN JOSE-Bº LA PITAHAYA Y VICEVERSA");
        assertEquals("SAN JOSE BARRIO LA PITAHAYA", ctp);
        assertEquals(ctp, aresep);
        assertEquals("", CruceTarifasService.normalizarNombre(null));
    }

    @Test
    void noConfundeBarrioConPalabrasQueEmpiezanConB() {
        assertEquals("EL BOSQUE", CruceTarifasService.normalizarNombre("El Bosque"));
    }

    @Test
    void calculaLaSimilitudEntreCeroYUno() {
        assertEquals(1.0, CruceTarifasService.similitud("", ""));
        assertEquals(1.0, CruceTarifasService.similitud("ABC", "ABC"));
        assertEquals(0.0, CruceTarifasService.similitud("ABC", "XYZ"));
        assertEquals(0.75, CruceTarifasService.similitud("ABCD", "ABCX"));
    }

    @Test
    void clasificaRutasExactasSoloPorNumeroYSinCoincidencia() {
        List<AresepFare> tarifas = List.of(
                tarifa("02", "R0012", "SAN JOSE-SABANA-CEMENTERIO", "T1", "SAN JOSE-SABANA-CEMENTERIO"),
                tarifa("80", "R0241", "SAN JOSE-SAN JUAN DE DIOS", "T2", "SAN JOSE-SAN JUAN DE DIOS"),
                tarifa("80 A", "R0242", "SAN JOSE-ALAJUELITA", "T3", "SAN JOSE-ALAJUELITA"),
                tarifa("66 BS", "R0213", "SAN JOSE-EL BOSQUE", "T4", "SAN JOSE-EL BOSQUE"));

        Map<String, TipoCoincidencia> rutas = service.cruzarRutas(List.of("0002", "0080", "0066", "0082"), tarifas);

        assertEquals(TipoCoincidencia.EXACTA, rutas.get("0002"));
        assertEquals(TipoCoincidencia.EXACTA, rutas.get("0080"));
        assertEquals(TipoCoincidencia.SOLO_NUMERO, rutas.get("0066"));
        assertEquals(TipoCoincidencia.SIN_COINCIDENCIA, rutas.get("0082"));
    }

    @Test
    void cruzaRamalesPorNombreDentroDeLaMismaRuta() {
        List<AresepFare> tarifas = List.of(
                tarifa("02", "R0012", "SAN JOSE-SABANA-CEMENTERIO", "T1", "SAN JOSE-SABANA-CEMENTERIO"),
                tarifa("02", "R0013", "SAN JOSE-BARRIO LA PITAHAYA", "T2", "SAN JOSE-Bº LA PITAHAYA"),
                tarifa("10", "R0066", "SAN JOSE-HOSPITAL MEJICO POR LA URUCA", "T3", "SAN JOSE-HOSPITAL MEJICO POR LA URUCA"),
                tarifa("10", "R0074", "SAN JOSE-LA CARPIO POR PISTA", "T4", "SAN JOSE-LA CARPIO POR PISTA"),
                tarifa("44", "R0153", "SAN JOSE-VISTA DE MAR", "T5", "SAN JOSE-VISTA DE MAR"),
                tarifa("44", "R0154", "SAN JOSE-VISTA DE MAR", "T6", "SAN JOSE-VISTA DE MAR"));
        Map<Ruta, List<Ramal>> ctp = new LinkedHashMap<>();
        ctp.put(new Ruta(56, "0002"), List.of(
                new Ramal(244, "0002-A", "SAN JOSÉ - SABANA CEMENTERIO "),
                new Ramal(243, "0002-B", "SAN JOSÉ - B° LA PITAHAYA")));
        ctp.put(new Ruta(10, "0010"), List.of(
                new Ramal(1, "0010-A", "SAN JOSÉ - HOSPITAL MÉXICO POR LA URUCA"),
                new Ramal(2, "0010-B", "SAN JOSÉ - PARQUE DE DIVERSIONES")));
        ctp.put(new Ruta(44, "0044"), List.of(new Ramal(3, "0044-B", "SAN JOSÉ - VISTA DE MAR")));
        ctp.put(new Ruta(82, "0082"), List.of(new Ramal(4, "0082-A", "SAN JOSÉ - ESCAZÚ")));

        List<EquivalenciaRamal> r = service.cruzarRamales(ctp, tarifas);

        assertEquals(6, r.size());
        assertEquals(TipoCoincidencia.NOMBRE_EXACTO, r.get(0).tipo());
        assertEquals("R0012", r.get(0).codigoRamalAresep());
        assertEquals("02", r.get(0).codigoRutaAresep());
        assertEquals(TipoCoincidencia.NOMBRE_EXACTO, r.get(1).tipo());
        assertEquals("R0013", r.get(1).codigoRamalAresep());
        assertEquals(TipoCoincidencia.NOMBRE_PARECIDO, r.get(2).tipo());
        assertEquals("R0066", r.get(2).codigoRamalAresep());
        assertTrue(r.get(2).puntaje() >= CruceTarifasService.UMBRAL_PARECIDO);
        assertEquals(TipoCoincidencia.REVISAR, r.get(3).tipo());
        assertTrue(r.get(3).puntaje() < CruceTarifasService.UMBRAL_PARECIDO);
        assertEquals(TipoCoincidencia.NOMBRE_AMBIGUO, r.get(4).tipo());
        assertEquals("R0153", r.get(4).codigoRamalAresep());
        assertEquals(TipoCoincidencia.SIN_RUTA_ARESEP, r.get(5).tipo());
        assertNull(r.get(5).codigoRamalAresep());
    }

    @Test
    void escribeCsvConEncabezadoYCamposEntreComillasCuandoHaceFalta() {
        List<EquivalenciaRamal> filas = List.of(
                new EquivalenciaRamal("0002", 244, "0002-A", "SAN JOSÉ, SABANA", "02", "R0012",
                        "SAN JOSE-SABANA-CEMENTERIO", TipoCoincidencia.NOMBRE_EXACTO, 1.0),
                new EquivalenciaRamal("0082", 4, "0082-A", "DICE \"ESCAZU\"", null, null, null,
                        TipoCoincidencia.SIN_RUTA_ARESEP, 0.0));

        String[] lineas = service.aCsv(filas).split("\n");

        assertEquals(3, lineas.length);
        assertTrue(lineas[0].startsWith("codigo_ruta_ctp,id_ramal_ctp,"));
        assertEquals("0002,244,0002-A,\"SAN JOSÉ, SABANA\",02,R0012,SAN JOSE-SABANA-CEMENTERIO,NOMBRE_EXACTO,1.00",
                lineas[1]);
        assertEquals("0082,4,0082-A,\"DICE \"\"ESCAZU\"\"\",,,,SIN_RUTA_ARESEP,0.00", lineas[2]);
    }
    @Test
    void losNombresVaciosNoProducenCoincidenciaExacta() {
        List<AresepFare> tarifas = List.of(tarifa("02", "R0012", "-", "T1", "---"));
        Map<Ruta, List<Ramal>> ctp = new LinkedHashMap<>();
        ctp.put(new Ruta(56, "0002"), List.of(
                new Ramal(1, "0002-A", null),
                new Ramal(2, "0002-B", "  "),
                new Ramal(3, "0002-C", "SAN JOSÉ - SABANA")));

        List<EquivalenciaRamal> r = service.cruzarRamales(ctp, tarifas);

        for (EquivalenciaRamal e : r) {
            assertEquals(TipoCoincidencia.REVISAR, e.tipo());
            assertNull(e.codigoRamalAresep());
            assertEquals(0.0, e.puntaje());
        }
    }

    @Test
    void noMezclaRamalesConElMismoCodigoEnRutasDelMismoNumero() {
        List<AresepFare> tarifas = List.of(
                tarifa("80", "R0100", "SAN JOSE-SAN JUAN DE DIOS", "T1", "SAN JOSE-SAN JUAN DE DIOS"),
                tarifa("80 A", "R0100", "SAN JOSE-ALAJUELITA", "T2", "SAN JOSE-ALAJUELITA"));
        Map<Ruta, List<Ramal>> ctp = new LinkedHashMap<>();
        ctp.put(new Ruta(80, "0080"), List.of(
                new Ramal(1, "0080-A", "SAN JOSÉ - SAN JUAN DE DIOS"),
                new Ramal(2, "0080-B", "SAN JOSÉ - ALAJUELITA")));

        List<EquivalenciaRamal> r = service.cruzarRamales(ctp, tarifas);

        assertEquals(TipoCoincidencia.NOMBRE_EXACTO, r.get(0).tipo());
        assertEquals("80", r.get(0).codigoRutaAresep());
        assertEquals(TipoCoincidencia.NOMBRE_EXACTO, r.get(1).tipo());
        assertEquals("80 A", r.get(1).codigoRutaAresep());
    }

    @Test
    void coincidePorElNombreDelFraccionamientoCuandoDifiereDelRamal() {
        List<AresepFare> tarifas = List.of(
                tarifa("09", "R0040", "SAN JOSE-ESCAZU", "T1", "SAN JOSE-ESCAZU POR GUACHIPELIN"));
        Map<Ruta, List<Ramal>> ctp = new LinkedHashMap<>();
        ctp.put(new Ruta(9, "0009"), List.of(new Ramal(1, "0009-A", "SAN JOSÉ - ESCAZÚ POR GUACHIPELÍN")));

        EquivalenciaRamal e = service.cruzarRamales(ctp, tarifas).getFirst();

        assertEquals(TipoCoincidencia.NOMBRE_EXACTO, e.tipo());
        assertEquals("R0040", e.codigoRamalAresep());
        assertEquals("SAN JOSE-ESCAZU", e.nombreRamalAresep());
    }
}
