package com.example.trayecticoservidor.controller;

import com.example.trayecticoservidor.dto.RutaResumenDTO;
import com.example.trayecticoservidor.dto.RutaDetalleDTO;
import com.example.trayecticoservidor.service.CatalogoRutasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para el dominio de rutas.
 * Provee los endpoints necesarios para consultar el catálogo general de rutas
 * y el detalle específico de cada una, incluyendo sus ramales y desplazamientos.
 */
@Slf4j
@RestController
@RequestMapping("/rutas")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class RutaController {

    private final CatalogoRutasService catalogoRutasService;

    /**
     * Obtiene el listado completo y resumido de las rutas disponibles en el sistema.
     *
     * @return ResponseEntity con una lista de RutaResumenDTO y el código de estado 200 (OK).
     */
    @GetMapping
    public ResponseEntity<List<RutaResumenDTO>> obtenerRutas()
    {
        log.debug("Solicitando el listado general de rutas");
        List<RutaResumenDTO> rutas = catalogoRutasService.obtenerRutas();
        return ResponseEntity.ok().body(rutas);
    }

    /**
     * Obtiene la información detallada de una ruta específica, incluyendo sus ramales y recorridos,
     * basándose en su código identificador.
     *
     * @param codigo El código CTP único de la ruta.
     * @return ResponseEntity con el objeto RutaDetalleDTO si se encuentra (200 OK),
     *         o un estado 404 (Not Found) si la ruta no existe.
     */
    @GetMapping("/{codigo}")
    public ResponseEntity<RutaDetalleDTO> obtenerRutaPorCodigo(@PathVariable String codigo){
        log.debug("Solicitando el detalle de la ruta con el código: {}", codigo);

        return catalogoRutasService.obtenerRutaPorCodigo(codigo).map(
                ruta->{
                    log.debug("Ruta {} encontrada y devuelta exitosamente", codigo);
                    return ResponseEntity.ok().body(ruta);
                }).orElseGet(() ->{
            log.debug("La ruta con el código {} no existe. Retornando 404 Not Found", codigo);
            return ResponseEntity.notFound().build();
        });
    }
}