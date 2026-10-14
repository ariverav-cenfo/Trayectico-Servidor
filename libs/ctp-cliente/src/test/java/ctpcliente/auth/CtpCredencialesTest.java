package ctpcliente.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CtpCredencialesTest {

    @Test
    void sinVariableUsaElTokenPublico() {
        assertEquals(CtpCredenciales.TOKEN_PUBLICO, CtpCredenciales.desde(null).tokenWfs());
        assertEquals(CtpCredenciales.TOKEN_PUBLICO, CtpCredenciales.desde("   ").tokenWfs());
    }

    @Test
    void laVariableReemplazaAlTokenYSeRecorta() {
        assertEquals("OTRO", CtpCredenciales.desde("  OTRO ").tokenWfs());
    }

    @Test
    void tokenVacioEsInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new CtpCredenciales(" "));
        assertThrows(NullPointerException.class, () -> new CtpCredenciales(null));
    }

    @Test
    void toStringNoExponeElTokenCompleto() {
        String s = CtpCredenciales.publicas().toString();

        assertFalse(s.contains(CtpCredenciales.TOKEN_PUBLICO));
        assertEquals("CtpCredenciales[tokenWfs=3YNc****]", s);
        assertEquals("CtpCredenciales[tokenWfs=****]", new CtpCredenciales("abc").toString());
    }
}
