package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.client.PhotonClient;
import com.example.trayecticoservidor.dto.LugarGeocodificadoDTO;
import com.example.trayecticoservidor.exception.GeocodificacionException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio encargado de buscar y transformar lugares geográficos
 * utilizando el servicio Photon.
 */
@Service
public class GeocodificacionService {

    private static final int MIN_CARACTERES_BUSQUEDA = 3;

    private final PhotonClient photonClient;

    /**
     * Construye el servicio de geocodificación.
     *
     * @param photonClient Cliente encargado de consultar Photon.
     */
    public GeocodificacionService(PhotonClient photonClient) {
        this.photonClient = photonClient;
    }

    /**
     * Busca lugares por nombre dentro de los límites geográficos de la GAM.
     *
     * <p>Puede devolver cero, uno o varios resultados. Los resultados
     * se filtran, transforman y eliminan duplicados.</p>
     *
     * @param nombre Nombre del lugar que se desea buscar.
     * @return Lista inmutable de lugares encontrados; vacía si no hay coincidencias.
     * @throws IllegalArgumentException Si el nombre es nulo, vacío o tiene menos de tres caracteres.
     * @throws IllegalStateException Si ocurre un error al consultar Photon.
     */
    public List<LugarGeocodificadoDTO> buscarLugar(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del lugar es obligatorio"
            );
        }

        String consultaOriginal = nombre.strip();

        String consulta = extraerNombreBase(consultaOriginal);

        if (consulta.length() < MIN_CARACTERES_BUSQUEDA) {
            throw new IllegalArgumentException(
                    "El nombre del lugar debe tener al menos tres caracteres"
            );
        }

        JsonNode respuesta;

        try {
            respuesta = photonClient.buscar(consulta);
        } catch (RuntimeException e) {
            throw new GeocodificacionException(
                    "No fue posible consultar el servicio de geocodificación. "
                            + "Intente nuevamente más tarde.",
                    e
            );
        }

        JsonNode features = respuesta.path("features");

        List<LugarGeocodificadoDTO> lugares = new ArrayList<>();
        Set<String> lugaresVistos = new LinkedHashSet<>();

        for (JsonNode feature : features) {

            JsonNode properties = feature.path("properties");

            // Se excluyen los resultados que representan rutas.
            if ("route".equals(properties.path("osm_key").asText())) {
                continue;
            }

            JsonNode coordinates = feature.path("geometry").path("coordinates");

            // Se descartan resultados sin coordenadas válidas.
            if (!coordinates.isArray()
                    || coordinates.size() < 2
                    || !coordinates.get(0).isNumber()
                    || !coordinates.get(1).isNumber()) {
                continue;
            }

            // Photon devuelve [lon, lat].
            double lon = coordinates.get(0).asDouble();
            double lat = coordinates.get(1).asDouble();

            String nombreLugar = obtenerNombre(properties);
            String detalle = obtenerDetalle(properties);

            String clave = nombreLugar + "|" + detalle;

            // Se conserva solamente la primera aparición de cada lugar.
            if (lugaresVistos.add(clave)) {
                lugares.add(new LugarGeocodificadoDTO(
                        nombreLugar,
                        detalle,
                        lat,
                        lon
                ));
            }
        }

        // Puede contener cero, uno o varios elementos.
        return List.copyOf(filtrarPorUbicacion(
                lugares,
                extraerUbicacion(consultaOriginal)
        ));
    }

    /**
     * Obtiene el nombre del lugar o construye uno utilizando calle y número.
     *
     * @param properties Propiedades del resultado de Photon.
     * @return Nombre del lugar.
     */
    private String obtenerNombre(JsonNode properties) {

        String nombre = texto(properties, "name");

        if (nombre != null) {
            return nombre;
        }

        String calle = texto(properties, "street");
        String numero = texto(properties, "housenumber");

        String direccion = String.join(
                " ",
                List.of(
                        calle == null ? "" : calle,
                        numero == null ? "" : numero
                )
        ).strip();

        return direccion.isEmpty() ? "Lugar sin nombre" : direccion;
    }

    /**
     * Construye el detalle geográfico utilizando distrito, ciudad y provincia.
     *
     * @param properties Propiedades del resultado de Photon.
     * @return Detalle geográfico sin valores repetidos.
     */
    private String obtenerDetalle(JsonNode properties) {

        Set<String> partes = new LinkedHashSet<>();

        agregarSiExiste(partes, texto(properties, "district"));
        agregarSiExiste(partes, texto(properties, "city"));
        agregarSiExiste(partes, texto(properties, "state"));

        return String.join(", ", partes);
    }

    /**
     * Lee una propiedad de texto de la respuesta de Photon.
     *
     * @param properties Propiedades del resultado.
     * @param campo Nombre de la propiedad.
     * @return Texto normalizado o null si no existe.
     */
    private String texto(JsonNode properties, String campo) {

        JsonNode valor = properties.get(campo);

        if (valor == null || valor.isNull() || !valor.isValueNode()) {
            return null;
        }

        String texto = valor.asText().strip();

        return texto.isEmpty() ? null : texto;
    }

    /**
     * Agrega un valor al conjunto si existe.
     *
     * @param partes Conjunto de valores geográficos.
     * @param valor Valor que se desea agregar.
     */
    private void agregarSiExiste(Set<String> partes, String valor) {

        if (valor != null) {
            partes.add(valor);
        }
    }

    private String extraerNombreBase(String consulta) {
        int inicio = consulta.indexOf('(');

        if (inicio < 0) {
            return consulta;
        }

        return consulta.substring(0, inicio).strip();
    }

    private String extraerUbicacion(String consulta) {
        int inicio = consulta.indexOf('(');
        int fin = consulta.lastIndexOf(')');

        if (inicio < 0 || fin <= inicio) {
            return null;
        }

        String ubicacion = consulta.substring(inicio + 1, fin).strip();

        return ubicacion.isEmpty() ? null : ubicacion;
    }

    private List<LugarGeocodificadoDTO> filtrarPorUbicacion(
            List<LugarGeocodificadoDTO> lugares,
            String ubicacion) {

        if (ubicacion == null) {
            return lugares;
        }

        String ubicacionNormalizada = ubicacion.toLowerCase(Locale.ROOT);

        List<LugarGeocodificadoDTO> filtrados = lugares.stream()
                .filter(lugar -> lugar.detalle() != null
                        && lugar.detalle().toLowerCase(Locale.ROOT)
                        .contains(ubicacionNormalizada))
                .toList();

        // Si no hay coincidencias con la ubicación indicada,
        // conservamos los resultados originales para solicitar aclaración.
        return filtrados.isEmpty() ? lugares : filtrados;
    }
}
