package ctpcliente.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModeloTest {

    @ParameterizedTest
    @CsvSource({"1, ACTIVA", "Activa, ACTIVA", "InActiva, INACTIVA", "2, INACTIVA", "3, DESCONOCIDO", "'', DESCONOCIDO"})
    void estadoParada(String valor, EstadoParada esperado) {
        assertEquals(esperado, EstadoParada.desde(valor));
    }

    @ParameterizedTest
    @CsvSource({"1, NO_EXISTE", "No existe, NO_EXISTE", "2, ARTESANAL", "Parabús, PARABUS", "3, PARABUS", "9, DESCONOCIDO"})
    void tipoCaseta(String valor, TipoCaseta esperado) {
        assertEquals(esperado, TipoCaseta.desde(valor));
    }

    @ParameterizedTest
    @CsvSource({"inicial, INICIAL", "Inicial, INICIAL", "intermedia, INTERMEDIA", "final, FINAL"})
    void tipoParada(String valor, TipoParada esperado) {
        assertEquals(esperado, TipoParada.desde(valor));
    }

    @ParameterizedTest
    @CsvSource({"1-2, IDA", "2-1, REGRESO", "ANILLO, ANILLO", "ANILLO HORARIO, ANILLO_HORARIO",
            "ANILLO ANTIHORARIO, ANILLO_ANTIHORARIO", "x, DESCONOCIDO"})
    void sentido(String valor, Sentido esperado) {
        assertEquals(esperado, Sentido.desde(valor));
    }

    @Test
    void otrosCodigos() {
        assertTrue(Sentido.ANILLO_HORARIO.esAnillo());
        assertEquals(EstadoRecorrido.NO_AUTORIZADO, EstadoRecorrido.desde("no autorizado"));
        assertEquals(EstadoRecorrido.ACTIVO, EstadoRecorrido.desde("activa"));
        assertEquals(TipoServicio.EXPRESO, TipoServicio.desde("3"));
        assertEquals(TipoServicio.DESCONOCIDO, TipoServicio.desde(null));
    }

    @Test
    void valoresConvierteNumerosQueLleganComoTexto() {
        Map<String, Object> m = new HashMap<>();
        m.put("lastre", "34.71");
        m.put("radio", "31");
        m.put("longitud", 3903L);
        m.put("vacio", "  ");
        m.put("nulo", null);
        m.put("texto", "abc");
        FilaConsulta f = new FilaConsulta(m);

        assertEquals(Optional.of(34.71), f.decimal("lastre"));
        assertEquals(Optional.of(31L), f.entero("radio"));
        assertEquals(Optional.of(3903L), f.entero("longitud"));
        assertEquals(Optional.of(3903.0), f.decimal("longitud"));
        assertEquals(Optional.empty(), f.texto("vacio"));
        assertEquals(Optional.empty(), f.texto("nulo"));
        assertEquals(Optional.empty(), f.entero("texto"));
        assertEquals(Optional.empty(), f.entero("lastre"));
        assertEquals(Optional.empty(), f.geometria());
    }

    @Test
    void paradaEnRecorridoDesdeFila() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", 3546L);
        m.put("identificador_parada", "PBA-10104-054");
        m.put("orden_parada", 1L);
        m.put("tipo_parada", "inicial");
        m.put("sentido_desplazamiento", "1-2");
        m.put("tipo_caseta", "3");
        m.put("geometriatextual", "POINT(491641.116 1098301.244)");

        ParadaEnRecorrido p = ParadaEnRecorrido.de(new FilaConsulta(m));

        assertEquals(3546, p.id());
        assertEquals(Sentido.IDA, p.sentido());
        assertEquals(TipoCaseta.PARABUS, p.caseta());
        assertEquals(new Coordenada(491641.116, 1098301.244), p.ubicacion().orElseThrow());
    }

    @Test
    void coleccionYElemento() {
        Elemento e = new Elemento("x.1", null, Map.of("a", 1L));
        ColeccionElementos c = new ColeccionElementos(List.of(e), OptionalLong.of(5));

        assertEquals(1, c.tamano());
        assertTrue(e.geometriaOpcional().isEmpty());
        assertEquals(Optional.of(1L), e.entero("a"));
    }
}
