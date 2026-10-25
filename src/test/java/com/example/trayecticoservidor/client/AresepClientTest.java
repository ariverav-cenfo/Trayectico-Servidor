package com.example.trayecticoservidor.client;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class AresepClientTest {
    private final AresepClient client = new AresepClient(URI.create("http://localhost"), 1);
    private static final String ROW = """
        {"codigoRuta":"I-1","nombreRuta":" DESAMPARADOS-MORAVIA ",
         "codigoRamal":"R0002","nombreRamal":"DESAMPARADOS-MORAVIA",
         "codigoFraccionamiento":"T0001","nombreFraccionamiento":"DESAMPARADOS-MORAVIA",
         "tarifaRegular":"350.00","tarifaAdultoMayor":"0.00","promedioKmViaje":"13.15",
         "fechaVigencia":"2026-09-04T00:00:00.000","provincia":null}
        """;

    private String response(String rows) {
        return "{\"metadata\":{\"success\":true,\"status\":200},\"value\":[" + rows + "]}";
    }

    @Test
    void parsesCodesDecimalsDatesAndNulls() {
        var fare = client.parseFares(response(ROW)).getFirst();
        assertEquals("I-1", fare.routeCode());
        assertEquals("DESAMPARADOS-MORAVIA", fare.routeName());
        assertEquals(new BigDecimal("350.00"), fare.regularFare());
        assertEquals("2026-09-04", fare.effectiveDate().toString());
        assertNull(fare.province());
    }

    @Test
    void rejectsDuplicateFractions() {
        assertThrows(IllegalArgumentException.class, () -> client.parseFares(response(ROW + "," + ROW)));
    }

    @Test
    void rejectsEmptyFailedAndMalformedResponses() {
        for (String json : new String[]{response(""), "{}", "null", "not-json",
                "{\"metadata\":{\"success\":false,\"status\":200},\"value\":[" + ROW + "]}"}) {
            assertThrows(IllegalArgumentException.class, () -> client.parseFares(json));
        }
    }

    @Test
    void rejectsNegativeMissingAndOverpreciseAmounts() {
        for (String value : new String[]{"-1.00", "350.001", ""}) {
            assertThrows(RuntimeException.class,
                    () -> client.parseFares(response(ROW.replace("350.00", value))));
        }
    }
}

