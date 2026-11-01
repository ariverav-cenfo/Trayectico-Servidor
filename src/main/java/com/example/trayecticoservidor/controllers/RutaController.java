package com.example.trayecticoservidor.controllers;

import com.example.trayecticoservidor.dto.RutaResumenDTO;
import com.example.trayecticoservidor.dto.RutaDetalleDTO;
import com.example.trayecticoservidor.service.CatalogoRutasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rutas")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class RutaController {

    private final CatalogoRutasService catalogoRutasService;

    @GetMapping
    public ResponseEntity<List<RutaResumenDTO>> getRutas()
    {
        log.debug("Solicitando el listado general de rutas");
        List<RutaResumenDTO> rutas = catalogoRutasService.obtenerRutas();
        return ResponseEntity.ok().body(rutas);
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<RutaDetalleDTO> getRutaPorCodigo(@PathVariable String codigo){
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
