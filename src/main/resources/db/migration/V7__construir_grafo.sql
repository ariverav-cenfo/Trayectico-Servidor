-- V7: Construcción del grafo para el planificador.

-- Parámetros provisionales de la primera versión. Revisar y ajustar en la versión 2:
--   Velocidad promedio del bus: 20 km/h.
--   Velocidad promedio caminando: 4 km/h.
--   Radio máximo de caminata: 300 metros.
--   Penalización por abordaje: 5 minutos.

CREATE OR REPLACE FUNCTION construir_grafo()
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_velocidad_bus_kmh CONSTANT DOUBLE PRECISION := 20;
    v_velocidad_caminando_kmh CONSTANT DOUBLE PRECISION := 4;
    v_radio_caminata_m CONSTANT DOUBLE PRECISION := 300;
    v_penalizacion_abordaje_min CONSTANT DOUBLE PRECISION := 5;
BEGIN

    -- 1. Limpiar el grafo anterior.
    TRUNCATE TABLE aristas, vertices RESTART IDENTITY;

    -- 2. Crear un vértice físico por cada parada.
    INSERT INTO vertices (
        tipo,
        parada_id,
        recorrido_id,
        orden_parada,
        geometria
    )
    SELECT
        'parada',
        p.id,
        NULL,
        NULL,
        p.geometria
    FROM paradas p
    ORDER BY p.id;

    -- 3. Crear un vértice por cada parada de un recorrido activo.
    INSERT INTO vertices (
        tipo,
        parada_id,
        recorrido_id,
        orden_parada,
        geometria
    )
    SELECT
        'en_bus',
        pr.parada_id,
        pr.recorrido_id,
        pr.orden_parada,
        p.geometria
    FROM paradas_recorridos pr
             JOIN recorridos r
                  ON r.id = pr.recorrido_id
             JOIN paradas p
                  ON p.id = pr.parada_id
    WHERE r.activo = TRUE
    ORDER BY pr.recorrido_id, pr.orden_parada;

    -- 4. Crear aristas para abordar.
    INSERT INTO aristas (
        source,
        target,
        tipo,
        recorrido_id,
        cost,
        reverse_cost
    )
    SELECT
        vp.id,
        vb.id,
        'abordar',
        vb.recorrido_id,
        v_penalizacion_abordaje_min,
        -1
    FROM vertices vp
             JOIN vertices vb
                  ON vb.parada_id = vp.parada_id
                      AND vb.tipo = 'en_bus'
    WHERE vp.tipo = 'parada';

    -- 5. Crear aristas para descender.
    INSERT INTO aristas (
        source,
        target,
        tipo,
        recorrido_id,
        cost,
        reverse_cost
    )
    SELECT
        vb.id,
        vp.id,
        'descender',
        vb.recorrido_id,
        0,
        -1
    FROM vertices vb
             JOIN vertices vp
                  ON vp.parada_id = vb.parada_id
                      AND vp.tipo = 'parada'
    WHERE vb.tipo = 'en_bus';

    -- 6. Crear aristas de viaje entre paradas consecutivas.
    WITH ordenados AS (
        SELECT
            pr.recorrido_id,
            pr.parada_id,
            pr.orden_parada,
            LEAD(pr.parada_id) OVER (
                    PARTITION BY pr.recorrido_id
                    ORDER BY pr.orden_parada
                ) AS siguiente_parada_id,
            LEAD(pr.orden_parada) OVER (
                    PARTITION BY pr.recorrido_id
                    ORDER BY pr.orden_parada
                ) AS siguiente_orden,
            ROW_NUMBER() OVER (
                    PARTITION BY pr.recorrido_id
                    ORDER BY pr.orden_parada
                ) AS posicion,
            COUNT(*) OVER (
                    PARTITION BY pr.recorrido_id
                ) AS total
        FROM paradas_recorridos pr
                 JOIN recorridos r
                      ON r.id = pr.recorrido_id
        WHERE r.activo = TRUE
    ),
         pares AS (
             -- Conexiones normales entre paradas consecutivas.
             SELECT
                 recorrido_id,
                 parada_id AS origen_id,
                 orden_parada AS orden_origen,
                 siguiente_parada_id AS destino_id,
                 siguiente_orden AS orden_destino,
                 FALSE AS es_circular
             FROM ordenados
             WHERE siguiente_parada_id IS NOT NULL

             UNION ALL

             -- Conexión de la última parada con la primera.
             -- únicamente para recorridos circulares.
             SELECT
                 o.recorrido_id,
                 o.parada_id,
                 o.orden_parada,
                 primero.parada_id,
                 primero.orden_parada,
                 TRUE
             FROM ordenados o
                      JOIN ordenados primero
                        ON primero.recorrido_id = o.recorrido_id
                        AND primero.posicion = 1
                      JOIN recorridos r ON r.id = o.recorrido_id
             WHERE o.posicion = o.total
               AND o.total > 1
               AND (ST_IsClosed(r.geometria) OR o.parada_id = primero.parada_id)
         ),
         fracciones_base AS (
             SELECT
                 pr.recorrido_id,
                 pr.orden_parada,
                 ST_LineLocatePoint(
                         r.geometria,
                         p.geometria
                 ) AS fraccion,
                 r.geometria AS geometria_recorrido,
                 ROW_NUMBER() OVER (
                    PARTITION BY pr.recorrido_id
                    ORDER BY pr.orden_parada DESC
                ) AS posicion_inversa
             FROM paradas_recorridos pr
                      JOIN recorridos r
                           ON r.id = pr.recorrido_id
                      JOIN paradas p
                           ON p.id = pr.parada_id
             WHERE r.activo = TRUE
               AND r.geometria IS NOT NULL
               AND p.geometria IS NOT NULL
         ),
         fracciones_con_anterior AS (
             SELECT
                 *,
                 LAG(fraccion) OVER (
                    PARTITION BY recorrido_id
                    ORDER BY orden_parada
                ) AS fraccion_anterior
             FROM fracciones_base
         ),
         fracciones_ajustadas AS (
             SELECT
                 recorrido_id,
                 orden_parada,
                 CASE
                     WHEN ST_IsClosed(geometria_recorrido)
                         AND posicion_inversa = 1
                         AND fraccion < fraccion_anterior
                         THEN 1.0
                     ELSE fraccion
                     END AS fraccion
             FROM fracciones_con_anterior
         ),
         distancias AS (
             SELECT
                 par.recorrido_id,
                 vo.id AS source_id,
                 vd.id AS target_id,
                 vo.parada_id AS parada_origen_id,
                 vd.parada_id AS parada_destino_id,
                 par.es_circular,
                 r.geometria AS geometria_recorrido,
                 fo.fraccion AS fraccion_origen,
                 fd.fraccion AS fraccion_destino
             FROM pares par
                      JOIN recorridos r
                           ON r.id = par.recorrido_id
                      JOIN vertices vo
                           ON vo.tipo = 'en_bus'
                               AND vo.recorrido_id = par.recorrido_id
                               AND vo.orden_parada = par.orden_origen
                      JOIN vertices vd
                           ON vd.tipo = 'en_bus'
                               AND vd.recorrido_id = par.recorrido_id
                               AND vd.orden_parada = par.orden_destino
                      JOIN fracciones_ajustadas fo
                           ON fo.recorrido_id = par.recorrido_id
                               AND fo.orden_parada = par.orden_origen
                      JOIN fracciones_ajustadas fd
                           ON fd.recorrido_id = par.recorrido_id
                               AND fd.orden_parada = par.orden_destino
             WHERE r.geometria IS NOT NULL
         )
    INSERT INTO aristas (
            source,
            target,
            tipo,
            recorrido_id,
            cost,
            reverse_cost,
            geometria
        )
    SELECT
        source_id,
        target_id,
        'viajar',
        recorrido_id,
         CASE
             WHEN es_circular AND parada_origen_id = parada_destino_id
                 THEN 0

             WHEN es_circular
                 THEN (1 - fraccion_origen + fraccion_destino)
                 * ST_Length(geometria_recorrido)

             ELSE ABS(fraccion_origen - fraccion_destino)
                 * ST_Length(geometria_recorrido)
         END / (v_velocidad_bus_kmh * 1000 / 60),
        -1,
        CASE
            WHEN es_circular AND fraccion_destino < fraccion_origen THEN NULL
            ELSE ST_LineSubstring(
                geometria_recorrido,
                LEAST(fraccion_origen, fraccion_destino),
                GREATEST(fraccion_origen, fraccion_destino)
            )
        END
    FROM distancias
    WHERE source_id <> target_id;

    -- 7. Crear aristas de caminata entre paradas cercanas.
    -- reverse_cost = cost permite caminar en ambas direcciones.
    INSERT INTO aristas (
        source,
        target,
        tipo,
        recorrido_id,
        cost,
        reverse_cost,
        geometria
    )
    SELECT
        v1.id,
        v2.id,
        'caminar',
        NULL,
        ST_Distance(p1.geometria, p2.geometria)
            / (v_velocidad_caminando_kmh * 1000 / 60),
        ST_Distance(p1.geometria, p2.geometria)
            / (v_velocidad_caminando_kmh * 1000 / 60),
        ST_MakeLine(p1.geometria, p2.geometria)
    FROM paradas p1
             JOIN paradas p2
                  ON p1.id < p2.id
             JOIN vertices v1
                  ON v1.parada_id = p1.id
                      AND v1.tipo = 'parada'
             JOIN vertices v2
                  ON v2.parada_id = p2.id
                      AND v2.tipo = 'parada'
    WHERE p1.geometria IS NOT NULL
      AND p2.geometria IS NOT NULL
      AND ST_DWithin(
            p1.geometria,
            p2.geometria,
            v_radio_caminata_m
          );

END;
$$;
