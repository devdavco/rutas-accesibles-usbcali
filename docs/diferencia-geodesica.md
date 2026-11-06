# Diferencia de distancias entre el prototipo y PostGIS

**Decisión:** el sistema mide la longitud de cada tramo con PostGIS sobre `geography`, es decir,
sobre el elipsoide WGS84. No se usa la esfera del prototipo. Las distancias reales difieren un poco
de las del prototipo; este documento explica la causa y su magnitud.

## Causa

| | Prototipo de validación | Sistema (servicio-negocio) |
|---|---|---|
| Fórmula | Haversine (`js/editor-datos.js`) | `ST_Distance(a::geography, b::geography)` |
| Modelo de la Tierra | Esfera de radio medio 6 371,0 km | Elipsoide WGS84 |
| Redondeo | Cada tramo a 0,1 m | Sin redondeo (en la respuesta, a centímetros) |

Sobre el elipsoide, el radio de curvatura cambia según la dirección. A la latitud del campus
(3,34° N):

| Dirección del tramo | Radio de curvatura WGS84 | Frente a la esfera (6 371,0 km) | Diferencia medida en los tramos |
|---|---|---|---|
| Norte–sur | 6 335,7 km (meridiano) | −0,555 % | −0,543 % (51 tramos) |
| Este–oeste | 6 378,2 km (primer vertical) | +0,113 % | +0,104 % (42 tramos) |

Por eso los tramos norte–sur miden menos en PostGIS y los este–oeste un poco más. Lo medido
coincide con la teoría: en los 107 tramos la diferencia promedio es −0,247 %. El redondeo del
prototipo a 0,1 m solo aporta hasta 0,05 m por tramo (0,024 m en promedio).

## Magnitud por tramo (107 tramos)

| Medida | Valor |
|---|---|
| Diferencia máxima | 0,540 m (tramo 66–67: PostGIS 101,46 m, prototipo 102,0 m) |
| Diferencia promedio (en valor absoluto) | 0,079 m |
| Tramos más cortos en PostGIS | 75 de 107 |

La prueba `CargaGrafoTest` de servicio-negocio verifica que ningún tramo difiera más de 1 m.

## Magnitud por ruta (15 pares de la prueba de extremo a extremo)

En una ruta las diferencias de sus tramos se suman. Las rutas del campus van sobre todo en
sentido norte–sur: las entradas están al sur y los edificios al norte. Por eso todas salen más
cortas con PostGIS, hasta 2,29 m (0,31 %). **La secuencia de puntos es la misma en los 15
pares**: la diferencia no alcanza para cambiar ninguna ruta elegida.

| Par | Destino | Prototipo (m) | PostGIS (m) | Diferencia (m) | Diferencia (%) | Secuencia |
|---|---|---:|---:|---:|---:|---|
| 1 → 2 | El Cedro (Entrada Lateral) | 418,90 | 417,45 | −1,45 | −0,35 | igual |
| 1 → 5 | Los Cerezos (Entrada Trasera 1) | 335,20 | 333,90 | −1,30 | −0,39 | igual |
| 1 → 6 | Farallones | 470,70 | 468,90 | −1,80 | −0,38 | igual |
| 1 → 7 | Horizontes (Cafetería Central) | 359,50 | 357,66 | −1,84 | −0,51 | igual |
| 1 → 9 | El Lago (Entrada Lateral) | 442,20 | 440,49 | −1,71 | −0,39 | igual |
| 1 → 11 | Biblioteca | 317,10 | 315,82 | −1,28 | −0,40 | igual |
| 1 → 39 | Los Naranjos | 648,60 | 646,90 | −1,70 | −0,26 | igual |
| 1 → 42 | Higuerones 1 (Entrada) | 729,60 | 727,31 | −2,29 | −0,31 | igual |
| 28 → 3 | El Cedro (Entrada Trasera) | 126,60 | 126,22 | −0,38 | −0,30 | igual |
| 28 → 20 | Los Cerezos (Entrada Trasera 2) | 472,20 | 471,62 | −0,58 | −0,12 | igual |
| 28 → 32 | El Lago (Entrada de Estudiantes) | 45,10 | 45,04 | −0,06 | −0,13 | igual |
| 28 → 44 | Higuerones 2 (Entrada) | 373,00 | 371,60 | −1,40 | −0,38 | igual |
| 28 → 49 | Parque Tecnológico | 94,90 | 94,86 | −0,04 | −0,04 | igual |
| 49 → 6 | Farallones | 601,50 | 600,66 | −0,84 | −0,14 | igual |
| 42 → 11 | Biblioteca | 417,60 | 416,67 | −0,93 | −0,22 | igual |

Origen 1 = Entrada Principal; origen 28 = Entrada de Estudiantes (Parqueadero).
Medido el 2026-11-06 con la pila completa (gateway → servicio-grafos → servicio-negocio → PostGIS
`postgis/postgis:18-3.7`).

## Cómo se valida el algoritmo

La prueba de extremo a extremo (`RutasPrototipoTest` en servicio-grafos) compara el Dijkstra del
sistema con el del prototipo **sobre los mismos datos**: servicio-grafos recibe las longitudes del
prototipo. Así se aísla el algoritmo de la forma de medir. En esas condiciones, los 15 pares dan la
misma secuencia de puntos y una diferencia de distancia de 0,00 m. La tabla anterior es la
comparación aparte con los datos reales: muestra cuánto cambia la distancia por usar el elipsoide.

## Cómo reproducir

1. Levantar la base de datos y los tres servicios (ver `README.md`, "Levantar todo para probar").
2. Para cada par de `backend/servicio-grafos/src/test/resources/prototipo/rutas-esperadas.json`,
   consultar `GET http://localhost:8080/api/rutas?origen=…&destino=…` y comparar `distanciaM` y
   `puntos[*].id` con `metros` y `ruta` del archivo.
