package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.CalculoViajeDAO;
import com.example.trayecticoservidor.dto.*;
import com.example.trayecticoservidor.model.TramoRutaInterno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Proposito: Service para el calculo de los viajes, aqui se construyen los valores de los tramos, response y otros datos enviados al front con el calculo del viaje
 * Contiene el metodo para calcular las distancias en metros entre dos puntos geograficos usando la formula de haversine, usando el radio aproximado de la tierra contra los puntos de latitud y longitud ingresados
 *
 */
@Service
public class CalculoViajeServiceImpl implements CalculoViajeService{
    private static final Logger log = LoggerFactory.getLogger(CalculoViajeServiceImpl.class);
    private final CalculoViajeDAO calculoViajeDAO;
    private final ObjectMapper objectMapper;

    public CalculoViajeServiceImpl(CalculoViajeDAO calculoViajeDAO, ObjectMapper objectMapper) {
        this.calculoViajeDAO = calculoViajeDAO;
        this.objectMapper = objectMapper;
    }

    @Override
    public ViajeResponseDTO calcularViaje(ViajeRequestDTO request){
        if (request == null || request.origen() ==null || request.destino() == null ||
                request.origen().lat() == null || request.origen().lon() == null || request.destino().lat() == null || request.destino().lon() == null){
            throw new IllegalArgumentException("Las coordenadas de origen y destino son obligatorias.");
        }
        BigDecimal origLat = request.origen().lat();
        BigDecimal origLon = request.origen().lon();
        BigDecimal destLat = request.destino().lat();
        BigDecimal destLon = request.destino().lon();

        if (origLat.doubleValue() < - 90 || origLat.doubleValue() > 90 ||
        destLat.doubleValue() < - 90 || destLat.doubleValue() > 90 ||
        origLon.doubleValue() < - 180 || origLon.doubleValue() > 180 ||
        destLon.doubleValue() < - 180 || destLon.doubleValue() > 180){
            throw new IllegalArgumentException("Las coordenadas de origen o destino son invalidas");
        }

        //1. Llamar opciones de trayecto interno desde BD
        List<List<TramoRutaInterno>> resultadosBase = calculoViajeDAO.buscarOpcionesViaje(
          origLat, origLon, destLat, destLon, 1000.0
        );

        List<OpcionesViajeResponseDTO> opcionesViajeResponseDTOS = new ArrayList<>();

        for (List<TramoRutaInterno> opcionRaw: resultadosBase){
            List<TramoResponseDTO> tramosFinales = new ArrayList<>();
            int totalDistanciaCaminando = 0;
            BigDecimal costoTotal = BigDecimal.ZERO;
            boolean costoIndeterminado = false;
            int numTransbordos = 0;
            int busCount = 0;

            BigDecimal latActual = origLat;
            BigDecimal lonActual = origLon;

            for (TramoRutaInterno tramo : opcionRaw){
                if ("bus".equals(tramo.tipo())){
                    busCount++;

                    //Agregar segmento a pie hacia la parada
                    int distCaminata = calcularDistanciaMetros(latActual, lonActual, tramo.paradaAbordajeLat(), tramo.paradaAbordajeLon());
                    totalDistanciaCaminando += distCaminata;

                    GeoJsonLineStringDTO geoCaminata = crearLineStringGeoJson(lonActual, latActual, tramo.paradaAbordajeLon(), tramo.paradaAbordajeLat());
                    tramosFinales.add(TramoResponseDTO.segmentoCaminata(distCaminata, geoCaminata));

                    //Construccion de DTOs de paradas
                    ParaResponseDTO abordaje = new ParaResponseDTO(
                            tramo.paradaAbordajeCodigo(), tramo.paradaAbordajeNombre(),
                            tramo.paradaAbordajeLat(), tramo.paradaAbordajeLon()
                    );

                    ParaResponseDTO descenso = new ParaResponseDTO(
                            tramo.paradaDescensoCodigo(), tramo.paradaDescensoNombre(),
                            tramo.paradaDescensoLat(), tramo.paradaDescensoLon()
                    );

                    //Procesando GeoJSON geometry en bus
                    GeoJsonLineStringDTO geoBus = parseGeoJson(tramo.geoJsonGeometry());

                    //Acumular tarifas
                    if (tramo.tarifa() != null && !costoIndeterminado){
                        costoTotal = costoTotal.add(tramo.tarifa());
                    } else{
                                costoIndeterminado = true;
                            }
                    //Crear segmento en bus
                    TramoResponseDTO tramoBus = new TramoResponseDTO(
                            "bus", tramo.distanciaMetros(), geoBus,
                            tramo.codigoRuta(), tramo.nombreRuta(),
                            tramo.codigoRamal(), tramo.nombreRamal(),
                            tramo.codigoRecorrido(), abordaje, descenso,
                            tramo.tarifa(), tramo.aceptaPagoElectronico()
                    );
                    tramosFinales.add(tramoBus);

                    //Actualizar marcador de posicion actual
                    latActual = tramo.paradaDescensoLat();
                    lonActual = tramo.paradaDescensoLon();
                        }
                    }
            //Porcion final a pie hasta punto destino
            int distCaminataFinal = calcularDistanciaMetros(latActual, lonActual, destLat, destLon);
            totalDistanciaCaminando += distCaminataFinal;
            GeoJsonLineStringDTO geoCaminataFinal = crearLineStringGeoJson(lonActual, latActual, destLon, destLat);
            tramosFinales.add(TramoResponseDTO.segmentoCaminata(distCaminataFinal, geoCaminataFinal));

            //Transbordos se calcula como -1 al conteo de segmentos en bus
            numTransbordos = Math.max(0, busCount -1);

            opcionesViajeResponseDTOS.add(new OpcionesViajeResponseDTO(
                    numTransbordos,
                    totalDistanciaCaminando,
                    costoIndeterminado ? null : costoTotal,
                    tramosFinales
            ));
                }
        //Acomodar viajes por cantidad de transbordos
        opcionesViajeResponseDTOS.sort((o1, o2) -> Integer.compare(
                o1.transbordos(), o2.transbordos()));
                return new ViajeResponseDTO(opcionesViajeResponseDTOS);
            }
            private GeoJsonLineStringDTO crearLineStringGeoJson(BigDecimal lon1, BigDecimal lat1, BigDecimal lon2, BigDecimal lat2){
        return new GeoJsonLineStringDTO(List.of(
                List.of(lon1,lat1),
                List.of(lon2, lat2)
        ));
        }
        private GeoJsonLineStringDTO parseGeoJson(String geoJsonStr){
        try {
            return objectMapper.readValue(geoJsonStr, GeoJsonLineStringDTO.class);
        } catch (Exception e) {
            log.error("No se pudo leer el GeoJSON del tramo; se devuelve sin geometría", e);
            return null;
        }
    }
    private int calcularDistanciaMetros(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2){
        double RadioPlaneta = 6371000;
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLng = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue())) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 -a));
        return (int) Math.round(RadioPlaneta * c);
    }
}
