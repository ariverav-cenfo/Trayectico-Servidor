package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dto.InterpretacionViajeResponseDTO;
import com.example.trayecticoservidor.dto.LugarGeocodificadoDTO;
import com.example.trayecticoservidor.dto.OrigenDestinoExtraidoDTO;
import com.example.trayecticoservidor.dto.PuntoViajeDTO;
import com.example.trayecticoservidor.exception.GroqException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.trayecticoservidor.config.ReasoningFilteringChatModel;
import org.springframework.ai.openai.OpenAiChatModel;

import java.util.List;

/**
 * Servicio encargado de interpretar solicitudes de viaje mediante IA.
 */
@Service
public class InterpretacionViajeService {

    private final ChatClient chatClient;
    private final GeocodificacionService geocodificacionService;

    /**
     * Construye el servicio de interpretación utilizando el modelo de chat
     * con filtrado de metadatos de razonamiento.
     *
     * @param openAiChatModel Modelo de chat de OpenAI compatible configurado para Groq.
     */
    public InterpretacionViajeService(
            OpenAiChatModel openAiChatModel,
            GeocodificacionService geocodificacionService) {
        this.geocodificacionService = geocodificacionService;

        this.chatClient = ChatClient.builder(
                    new ReasoningFilteringChatModel(openAiChatModel)
        )
            .defaultSystem("""
                Eres un asistente encargado de interpretar solicitudes
                de viaje dentro de la Gran Área Metropolitana de Costa Rica.

                Tu tarea es identificar el lugar de origen y el lugar
                de destino mencionados por el usuario.


                Reglas:
                1. Extrae únicamente los lugares que el usuario menciona
                   explícita o claramente en su mensaje.
                2. No inventes lugares ni completes información que
                   el usuario no haya proporcionado.
                3. Conserva cualquier información geográfica adicional que el usuario
                   proporcione junto al nombre del lugar, como distrito, cantón,
                   provincia, barrio o sede.
                4. No descartes esa información al extraer el origen y el destino.
                5. Si el usuario menciona un lugar acompañado de su ubicación
                   entre paréntesis, incluye también esa ubicación en el campo
                   correspondiente.
                6. No tienes acceso a herramientas de geocodificación.
                   Tu responsabilidad es identificar los nombres de los lugares
                   mencionados por el usuario, no validar si existen.
                7. No determines si un lugar es ambiguo o si tiene coincidencias.
                   Esa validación será realizada posteriormente por el backend.
                8. Si el usuario no menciona el origen o el destino,
                   deja el campo correspondiente en null.
                9. Nunca inventes ni estimes coordenadas.
                10. Si falta el origen o el destino en el mensaje original,
                   deja el campo correspondiente en null.
                11. No calcules rutas, distancias ni tiempos de viaje.
                12. Si no se necesita una aclaración, devuelve null
                    en preguntaAclaracion.
                13. Las coordenadas geográficas nunca deben ser inventadas,
                    estimadas, modificadas ni calculadas.
                14. No calcules coordenadas a partir de direcciones,
                    nombres de lugares u otra información.
                15. No incluyas coordenadas en la pregunta de aclaración.
                16. Utiliza los nombres de los lugares devueltos por
                    buscarLugar cuando exista exactamente una coincidencia.
                17. No calcules ni proporciones rutas, distancias,
                    duraciones, tiempos de llegada o instrucciones
                    de navegación.
                18. Tu responsabilidad termina al identificar el origen
                    y destino y determinar si se necesita una aclaración.
                """)
            .build();
    }

    /**
     * Extrae el origen y destino de un mensaje escrito en lenguaje natural.
     *
     * @param mensaje Solicitud de viaje escrita por el usuario.
     * @return DTO con el origen y destino identificados.
     */

    public InterpretacionViajeResponseDTO extraerOrigenDestino(String mensaje) {

        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException(
                    "El mensaje del usuario no puede estar vacío."
            );
        }

        OrigenDestinoExtraidoDTO resultado;

        try {
            resultado = chatClient.prompt()
                    .user(mensaje)
                    .call()
                    .entity(OrigenDestinoExtraidoDTO.class);

        } catch (RuntimeException e) {
            throw new GroqException(
                    "No fue posible procesar la solicitud con el servicio de IA. "
                            + "Intente nuevamente más tarde.",
                    e
            );
        }

        if (resultado == null) {
            return new InterpretacionViajeResponseDTO(
                    null,
                    null,
                    List.of(),
                    List.of(),
                    generarAclaracion(null)
            );
        }

        String origen = normalizarLugar(resultado.origen());
        String destino = normalizarLugar(resultado.destino());

        List<LugarGeocodificadoDTO> origenes = origen != null
                ? geocodificacionService.buscarLugar(origen)
                : List.of();

        List<LugarGeocodificadoDTO> destinos = destino != null
                ? geocodificacionService.buscarLugar(destino)
                : List.of();

        PuntoViajeDTO puntoOrigen = origenes.size() == 1
                ? convertirPunto(origenes.get(0))
                : null;

        PuntoViajeDTO puntoDestino = destinos.size() == 1
                ? convertirPunto(destinos.get(0))
                : null;

        String aclaracion = null;

        // Si falta información en la solicitud, se pregunta al usuario.
        if (origen == null || destino == null) {
            aclaracion = generarAclaracion(resultado);

            // Si ambos lugares fueron proporcionados, pero no se identificaron
            // de forma única, se explica el problema encontrado.
        } else if (puntoOrigen == null || puntoDestino == null) {
            aclaracion = generarAclaracionResultados(
                    origen, destino, origenes, destinos
            );
        }

        return new InterpretacionViajeResponseDTO(
                puntoOrigen,
                puntoDestino,
                puntoOrigen == null ? origenes : List.of(),
                puntoDestino == null ? destinos : List.of(),
                aclaracion
        );
    }

    /**
     * Normaliza el nombre de un lugar extraído por el modelo.
     *
     * @param lugar Nombre del lugar.
     * @return Nombre sin espacios innecesarios o null si está vacío.
     */
    private String normalizarLugar(String lugar) {
        if (lugar == null || lugar.isBlank()) {
            return null;
        }

        return lugar.trim();
    }

    /**
     * Genera una pregunta para solicitar los datos de viaje faltantes
     * o aclarar lugares no identificados de forma única.
     *
     * @param resultado Origen y destino extraídos.
     * @return Pregunta de aclaración o null si no se necesita.
     */
    public String generarAclaracion(OrigenDestinoExtraidoDTO resultado) {

        if (resultado == null) {
            return "No fue posible interpretar su solicitud. "
                    + "¿Podría indicar el origen y el destino?";
        }

        if (resultado.preguntaAclaracion() != null
                && !resultado.preguntaAclaracion().isBlank()) {
            return resultado.preguntaAclaracion();
        }

        boolean faltaOrigen = resultado.origen() == null;
        boolean faltaDestino = resultado.destino() == null;

        if (faltaOrigen && faltaDestino) {
            return "¿Cuál es el origen y el destino de su viaje?";
        }

        if (faltaOrigen) {
            return "¿Desde qué lugar desea iniciar el viaje?";
        }

        if (faltaDestino) {
            return "¿Cuál es el destino de su viaje?";
        }

        return null;
    }

    private PuntoViajeDTO convertirPunto(LugarGeocodificadoDTO lugar) {
        return new PuntoViajeDTO(
                lugar.nombre(),
                lugar.lat(),
                lugar.lon()
        );
    }

    private String generarAclaracionResultados(
            String origen,
            String destino,
            List<LugarGeocodificadoDTO> origenes,
            List<LugarGeocodificadoDTO> destinos) {

        StringBuilder pregunta = new StringBuilder();

        if (origen == null || origenes.size() != 1) {
            pregunta.append(describirProblema("origen", origenes));
        }

        if (destino == null || destinos.size() != 1) {
            if (!pregunta.isEmpty()) {
                pregunta.append(" ");
            }

            pregunta.append(describirProblema("destino", destinos));
        }

        return pregunta.toString().trim();
    }

    private String describirProblema(
            String tipo,
            List<LugarGeocodificadoDTO> lugares) {

        if (lugares.isEmpty()) {
            return "No se encontró el " + tipo
                    + ". Por favor, indique otro lugar.";
        }

        String opciones = lugares.stream()
                .map(lugar -> lugar.nombre() + " (" + lugar.detalle() + ")")
                .collect(java.util.stream.Collectors.joining("; "));

        return "Para el " + tipo
                + ", seleccione una de las siguientes opciones: "
                + opciones + ".";
    }

    public InterpretacionViajeResponseDTO confirmarSeleccion(
            PuntoViajeDTO origen,
            PuntoViajeDTO destino) {

        return new InterpretacionViajeResponseDTO(
                origen,
                destino,
                List.of(),
                List.of(),
                null
        );
    }
}