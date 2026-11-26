package com.example.trayecticoservidor.tool;

import com.example.trayecticoservidor.dto.LugarGeocodificadoDTO;
import com.example.trayecticoservidor.service.GeocodificacionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Herramientas de geocodificación disponibles para el modelo de IA.
 */
@Component
public class GeocodificacionTool {

    private final GeocodificacionService geocodificacionService;

    /**
     * Construye la herramienta de geocodificación.
     *
     * @param geocodificacionService Servicio encargado de buscar lugares.
     */
    public GeocodificacionTool(GeocodificacionService geocodificacionService) {
        this.geocodificacionService = geocodificacionService;
    }

    /**
     * Busca un lugar por nombre y devuelve las coincidencias encontradas
     * dentro de la Gran Área Metropolitana.
     *
     * <p>El resultado puede contener cero, uno o varios lugares.
     * El modelo debe utilizar estos resultados para determinar si
     * necesita solicitar una aclaración al usuario.</p>
     *
     * @param nombre Nombre del lugar que se desea buscar.
     * @return Lista de coincidencias con sus coordenadas geográficas.
     */
    @Tool(
            name = "buscarLugar",
            description = """
                Busca un lugar por su nombre dentro de la Gran Área Metropolitana
                de Costa Rica.
        
                Devuelve una lista de coincidencias con nombre, detalle geográfico,
                latitud y longitud.
        
                Si no encuentra coincidencias devuelve una lista vacía.
                Si encuentra varias coincidencias devuelve todas.
        
                Las coordenadas devueltas son las únicas coordenadas válidas
                que pueden utilizarse para identificar el lugar.
        
                No inventes, estimes, modifiques ni calcules coordenadas.
                No selecciones arbitrariamente entre resultados ambiguos.
        
                Esta herramienta únicamente busca y geocodifica lugares.
                No calcula rutas, distancias ni tiempos de viaje.
                """
    )
    public List<LugarGeocodificadoDTO> buscarLugar(
            @ToolParam(description = "Nombre del lugar que se desea buscar") String nombre) {

        return geocodificacionService.buscarLugar(nombre);
    }
}
