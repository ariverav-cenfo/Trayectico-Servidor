package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.RutaDAO;
import com.example.trayecticoservidor.dto.RutaImportada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Importa una o varias rutas del CTP al arrancar la aplicación: las lee y las guarda.
 *
 * <p>Solo se activa si la app arranca con {@code --trayectico.importar-ruta=<códigos>}, por ejemplo
 * {@code --trayectico.importar-ruta=0002} o {@code --trayectico.importar-ruta=0002,0090,0403}.
 * Sin ese argumento esta clase no existe, así que ni el arranque normal ni el CI llaman al CTP.
 *
 * <p>Cada ruta se lee y se guarda por separado, en su propia transacción. Si una falla (el CTP no responde,
 * el código no existe, una parada viene sin ubicación o la base rechaza un dato), se registra el error y se
 * sigue con las demás. Al final, si alguna falló, se lanza una excepción con la lista: así el arranque termina
 * con error y no parece una importación completa. Reimportar no duplica, así que basta con volver a correr
 * solo las rutas que fallaron.
 */
@Component
@ConditionalOnProperty("trayectico.importar-ruta")
public class ImportadorRuta implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportadorRuta.class);

    private final LectorRutaCtpService lector;
    private final RutaDAO rutaDAO;
    private final List<String> codigosRuta;

    /**
     * Crea el importador y revisa la lista de rutas antes de que arranque la importación.
     *
     * @param lector      lee cada ruta del CTP
     * @param rutaDAO     guarda cada ruta en la base
     * @param codigosRuta valor de {@code trayectico.importar-ruta}: uno o varios códigos separados por coma
     * @throws IllegalArgumentException si la lista no trae ningún código; se revisa antes de llamar al CTP
     */
    public ImportadorRuta(LectorRutaCtpService lector, RutaDAO rutaDAO,
                          @Value("${trayectico.importar-ruta}") String codigosRuta) {
        this.lector = lector;
        this.rutaDAO = rutaDAO;
        this.codigosRuta = codigos(codigosRuta);
    }

    /**
     * Importa las rutas en el orden en que vienen en la lista.
     *
     * @param args argumentos de arranque; no se usan, la lista viene de la propiedad
     * @throws IllegalStateException si al menos una ruta no se pudo importar, con el código y el motivo de cada una
     */
    @Override
    public void run(ApplicationArguments args) {
        List<String> fallidas = new ArrayList<>();
        for (String codigo : codigosRuta) {
            try {
                importar(codigo);
            } catch (RuntimeException e) {
                // Se registra con la traza completa y se sigue con la siguiente ruta
                log.error("Ruta {} no se importo: {}", codigo, e.getMessage(), e);
                fallidas.add(codigo + " (" + e.getMessage() + ")");
            }
        }

        if (codigosRuta.size() > 1) {
            log.info("Importacion terminada: {} de {} rutas importadas",
                    codigosRuta.size() - fallidas.size(), codigosRuta.size());
        }
        if (!fallidas.isEmpty()) {
            throw new IllegalStateException("No se importaron " + fallidas.size() + " de "
                    + codigosRuta.size() + " rutas: " + String.join(", ", fallidas));
        }
    }

    /**
     * Lee una ruta del CTP, la guarda y registra cuántos ramales y paradas trajo.
     *
     * @param codigoRuta código público, por ejemplo {@code 0002}
     */
    private void importar(String codigoRuta) {
        RutaImportada ruta = lector.leer(codigoRuta);
        rutaDAO.guardar(ruta);

        long paradas = ruta.ramales().stream()
                .flatMap(r -> r.desplazamientos().stream())
                .mapToLong(d -> d.paradas().size())
                .sum();
        log.info("Ruta {} importada: {} ramales, {} paradas en recorridos",
                ruta.codigo(), ruta.ramales().size(), paradas);
    }

    /**
     * Convierte el valor de la propiedad en la lista de rutas a importar.
     *
     * <p>Quita los espacios alrededor de cada código, ignora los elementos vacíos ({@code 0002,,0006})
     * y deja cada código una sola vez, en el orden de su primera aparición.
     *
     * @param valor por ejemplo {@code "0002, 0090,0403"}
     * @return los códigos sin espacios ni repetidos
     * @throws IllegalArgumentException si no queda ningún código
     */
    static List<String> codigos(String valor) {
        Set<String> codigos = new LinkedHashSet<>();
        for (String parte : valor.split(",")) {
            String codigo = parte.strip();
            if (!codigo.isEmpty() && !codigos.add(codigo)) {
                log.warn("La ruta {} viene repetida en trayectico.importar-ruta; se importa una sola vez", codigo);
            }
        }
        if (codigos.isEmpty()) {
            throw new IllegalArgumentException(
                    "trayectico.importar-ruta no trae ningun codigo de ruta, por ejemplo 0002 o 0002,0090");
        }
        return List.copyOf(codigos);
    }
}