package com.example.trayecticoservidor.controller;

import com.example.trayecticoservidor.dto.RutaAdminDTO;
import com.example.trayecticoservidor.dto.RutaEstadoRequestDTO;
import com.example.trayecticoservidor.service.RutaAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/rutas")
@RequiredArgsConstructor
public class RutaAdminController {
    private final RutaAdminService rutaAdminService;

    /**
     * Obtiene todas las rutas con todos su campos (Para el admin) y recorridos asociados
     */
    @GetMapping
    public ResponseEntity<List<RutaAdminDTO>> ListarRutasAdmin(){
        log.debug("Admin: solicitando listado completo de rutas");
        return ResponseEntity.ok(rutaAdminService.listarRutasAdmin());
    }
    /**
     * Obtiene el detalle de una ruta especifica basado en codigo ctp, ej: 0403
     */
    @GetMapping("/{codigo}")
    public ResponseEntity<RutaAdminDTO> obtenerRutaAdmin(@PathVariable String codigo){
        log.debug("Admin: solicitando detalle de ruta {}", codigo);
        return rutaAdminService.obtenerRutaAdmin(codigo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    /**
     * Habilita o deshabilita todos los recorridos asociados a una ruta.
     * Body esperado {"activo": false} o {"activo":true}
     */
    @PatchMapping("/{codigo}/estado")
    public ResponseEntity<RutaAdminDTO> cambiarEstadoRuta(
            @PathVariable String codigo,
            @RequestBody RutaEstadoRequestDTO request){
        log.debug("Admin: cambiando  estado de ruta {} a activo={}", codigo, request.activo());
        return rutaAdminService.cambiarEstadoRuta(codigo, request.activo())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
