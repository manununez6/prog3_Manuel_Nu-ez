# Documentación de endpoints

> Plantilla. Cada grupo la completa con SUS endpoints, sus ejemplos reales
> y su análisis de complejidad. Un endpoint sin esta ficha no suma puntos.

---

### GET /api/grafo/dfs

Recorrido en profundidad desde un nodo: avanza por un camino hasta que no
puede seguir y recién ahí retrocede para probar el siguiente vecino.

**Parámetros:** `origen` (id del nodo de partida, ej. `DEP`)

**Ejemplo:** `GET /api/grafo/dfs?origen=DEP`

**Respuesta** (200, ids en orden de visita):
```json
["DEP", "SNT", "MIC", "REC", "PAL", "ALM", "CAB", "FLO", "VUR", "BEL"]
```

El orden depende de cómo quedan ordenados los vecinos en la lista de
adyacencia. El ejemplo supone que Neo4j devuelve los nodos y las rutas en el
mismo orden en que los guarda `CargaInicial`; `RecorridosTest` verifica este
resultado sobre la red armada en ese orden.

**Errores:** `origen` inexistente → 400 `{"estado":"ERROR","detalle":"No existe el nodo con id: ..."}`

**Complejidad:** O(V + E).
- Cada vértice se marca como visitado al entrar a `dfs(u)` y nunca se vuelve a
  entrar a un vértice marcado: hay como mucho V llamadas → O(V).
- En cada llamada se recorre la lista de vecinos de `u` una sola vez. Sumando
  todas las listas se recorren todas las aristas (2E en no dirigido) → O(E).
- Espacio: O(V) por el arreglo de visitados y la pila de llamadas (en el peor
  caso, un camino que pasa por todos los vértices).

**Estructura usada:** recursión (pila de llamadas) + arreglo `boolean[] visitado`
+ lista de adyacencia de `Grafo`.

---

### GET /api/grafo/bfs

Recorrido en anchura desde un nodo: visita primero todos los vecinos del
origen, después los vecinos de esos, y así por niveles.

**Parámetros:** `origen` (id del nodo de partida, ej. `DEP`)

**Ejemplo:** `GET /api/grafo/bfs?origen=DEP`

**Respuesta** (200, ids en orden de visita):
```json
["DEP", "SNT", "MIC", "CAB", "REC", "ALM", "PAL", "FLO", "BEL", "VUR"]
```

Por niveles (cantidad de rutas desde el depósito):
nivel 0: DEP · nivel 1: SNT, MIC, CAB · nivel 2: REC, ALM, PAL, FLO · nivel 3: BEL, VUR.
Vale la misma aclaración del DFS sobre el orden de los vecinos.

**Errores:** `origen` inexistente → 400 `{"estado":"ERROR","detalle":"No existe el nodo con id: ..."}`

**Complejidad:** O(V + E).
- Cada vértice se marca como visitado **al encolarlo**, así que entra a la
  cola una sola vez: V encolados y V desencolados, cada uno O(1) → O(V).
- Al desencolar un vértice se recorre su lista de vecinos una sola vez. En
  total se recorren todas las listas → O(E).
- Espacio: O(V) por la cola y el arreglo de visitados.

**Estructura usada:** cola FIFO (`ArrayDeque`) + arreglo `boolean[] visitado`
+ lista de adyacencia de `Grafo`.

BFS da el camino con **menos rutas**, no el de **menos km**: llega a VUR en
3 rutas (por PAL), pero el camino más corto en km es DEP→CAB→FLO→VUR (19 km).
Para eso está Dijkstra.

---

### GET /api/grafo/dijkstra

Camino de menor distancia (km) entre dos nodos, con el recorrido completo.

**Parámetros:** `origen` (id), `destino` (id)

**Ejemplo:** `GET /api/grafo/dijkstra?origen=DEP&destino=VUR`

**Respuesta** (200):
```json
{ "vertices": ["DEP", "CAB", "FLO", "VUR"], "costoTotal": 19.0 }
```

Otros casos verificados en `CaminosMinimosTest`:

| Consulta | Camino | km | Directo |
|---|---|---|---|
| `origen=DEP&destino=MIC` | DEP → SNT → MIC | 5 | 6 |
| `origen=PAL&destino=VUR` | PAL → BEL → VUR | 9 | 10 |
| `origen=DEP&destino=VUR` | DEP → CAB → FLO → VUR | 19 | no hay |

**Errores:**
- id inexistente → 400 `{"estado":"ERROR","detalle":"No existe el nodo con id: ..."}`
- alguna arista con costo negativo → 422 `{"estado":"ERROR","detalle":"Dijkstra no admite costos negativos: la arista X->Y cuesta ..."}`
- origen y destino no conectados → 422 `{"estado":"ERROR","detalle":"No hay camino de X a Y: no estan conectados"}`

**Complejidad:** O((V + E) log V) en tiempo, O(V + E) en espacio.
- Inicializar `dist` y `predecesor` y validar que no haya costos negativos: O(V + E).
- Cada vez que una relajación mejora `dist[v]` se encola una entrada nueva.
  Cada arista puede mejorar a lo sumo una vez a su destino desde un `u` dado,
  así que hay como mucho E + 1 entradas en la cola.
- Cada `add` y cada `poll` del heap cuesta O(log(tamaño de la cola)) = O(log E),
  y como E ≤ V², log E ≤ 2 log V = O(log V).
- Cada vértice se procesa (se recorren sus vecinos) una sola vez: las entradas
  viejas se descartan en O(1) con `if (distancia > dist[u]) continue`. En total
  se recorren E aristas.
- Sumando: O(V + E) entradas × O(log V) por operación de heap = **O((V + E) log V)**.

Con búsqueda lineal del mínimo en lugar de heap sería O(V²).

**Estructura usada:** `PriorityQueue` (heap binario) ordenada por distancia +
arreglos `double[] dist` e `int[] predecesor` + lista de adyacencia de `Grafo`.
El camino se reconstruye siguiendo `predecesor` desde el destino hasta el origen.

---

### GET /api/grafo/floyd

Distancia mínima (km) entre **todos** los pares de nodos.

**Parámetros:** ninguno

**Ejemplo:** `GET /api/grafo/floyd`

**Respuesta** (200, un mapa `"origen->destino"`; recortado, son 100 pares):
```json
{ "DEP->DEP": 0.0, "DEP->SNT": 3.0, "DEP->MIC": 5.0, "DEP->VUR": 19.0, "PAL->VUR": 9.0, "...": "..." }
```

Los pares sin camino no aparecen en el mapa. En esta red no pasa porque es conexa.

Matriz completa de km sobre la red. Coincide con Dijkstra en los 100 pares
(`CaminosMinimosTest`):

| | DEP | SNT | MIC | REC | PAL | BEL | ALM | CAB | FLO | VUR |
|---|---|---|---|---|---|---|---|---|---|---|
| **DEP** | 0 | 3 | 5 | 8 | 12 | 17 | 9 | 6 | 10 | 19 |
| **SNT** | 3 | 0 | 2 | 5 | 9 | 14 | 7 | 9 | 13 | 18 |
| **MIC** | 5 | 2 | 0 | 3 | 7 | 12 | 5 | 8 | 12 | 16 |
| **REC** | 8 | 5 | 3 | 0 | 4 | 9 | 4 | 7 | 11 | 13 |
| **PAL** | 12 | 9 | 7 | 4 | 0 | 5 | 4 | 6 | 10 | 9 |
| **BEL** | 17 | 14 | 12 | 9 | 5 | 0 | 9 | 11 | 13 | 4 |
| **ALM** | 9 | 7 | 5 | 4 | 4 | 9 | 0 | 3 | 7 | 13 |
| **CAB** | 6 | 9 | 8 | 7 | 6 | 11 | 3 | 0 | 4 | 13 |
| **FLO** | 10 | 13 | 12 | 11 | 10 | 13 | 7 | 4 | 0 | 9 |
| **VUR** | 19 | 18 | 16 | 13 | 9 | 4 | 13 | 13 | 9 | 0 |

**Errores:** ciclo de costo negativo → 422 `{"estado":"ERROR","detalle":"Floyd-Warshall encontro un ciclo de costo negativo que pasa por X: los costos minimos no estan definidos"}`.
En un grafo no dirigido, una sola arista negativa ya es un ciclo negativo (ir y volver por ella).

**Complejidad:** O(V³) en tiempo, O(V²) en espacio.
- **Tiempo:** tres bucles anidados (`k`, `i`, `j`) de V vueltas cada uno, con una
  comparación y una suma por vuelta: V · V · V · O(1) = O(V³). Armar la matriz
  cuesta O(V² + E), y revisar la diagonal, O(V): no cambian el total. No depende
  de E: tarda lo mismo con pocas o muchas aristas.
- **Espacio:** una sola matriz de V × V que se actualiza en el lugar → O(V²).
  Alcanza con una porque, en la iteración `k`, `dist[i][k]` y `dist[k][j]` no
  cambian (salvo que haya un ciclo negativo, que se detecta al final).

**Estructura usada:** matriz de adyacencia (`grafo.matrizDeAdyacencia()`) +
`LinkedHashMap` para la respuesta.

#### ¿Floyd-Warshall o Dijkstra V veces?

Correr Dijkstra desde cada vértice también da todos los pares:

| | Floyd-Warshall | Dijkstra V veces (con heap) |
|---|---|---|
| Tiempo | O(V³) | O(V · (V + E) log V) |
| Grafo ralo (E ≈ V) | O(V³) | **O(V² log V)**: gana |
| Grafo denso (E ≈ V²) | **O(V³)**: gana | O(V³ log V) |
| Costos negativos | **Sí**, si no hay ciclo negativo | No |
| Espacio | O(V²) | O(V²) para guardar todas las distancias, más O(V + E) de cada corrida |
| Implementación | Tres bucles | Necesita heap y lista de adyacencia |

Esta red es rala (V = 10, E = 16), así que asintóticamente conviene Dijkstra V
veces. Con solo 10 nodos la diferencia no se nota y Floyd es más simple. Si solo
hace falta una consulta origen-destino, lo que corresponde es un solo Dijkstra.

---

### GET /api/grafo/prim

Árbol de recubrimiento mínimo: el conjunto de rutas de menor km total que
conecta todos los puntos de la red. Prim lo hace crecer desde un nodo.

**Parámetros:** `origen` (id del nodo desde donde crece el árbol)

**Ejemplo:** `GET /api/grafo/prim?origen=DEP`

**Respuesta** (200, aristas en el orden en que entran al árbol):
```json
{
  "aristas": [
    {"origen": "DEP", "destino": "SNT", "costo": 3.0},
    {"origen": "SNT", "destino": "MIC", "costo": 2.0},
    {"origen": "MIC", "destino": "REC", "costo": 3.0},
    {"origen": "REC", "destino": "PAL", "costo": 4.0},
    {"origen": "REC", "destino": "ALM", "costo": 4.0},
    {"origen": "ALM", "destino": "CAB", "costo": 3.0},
    {"origen": "CAB", "destino": "FLO", "costo": 4.0},
    {"origen": "PAL", "destino": "BEL", "costo": 5.0},
    {"origen": "BEL", "destino": "VUR", "costo": 4.0}
  ],
  "costoTotal": 32.0
}
```

9 aristas (V − 1 con V = 10) y **32 km**. El costo total es el mismo desde
cualquier origen. El conjunto de aristas puede variar porque hay empates:
REC-ALM y PAL-ALM valen 4 los dos.

**Errores:**
- id inexistente → 400 `{"estado":"ERROR","detalle":"No existe el nodo con id: ..."}`
- grafo no conexo → 422 `{"estado":"ERROR","detalle":"El grafo no es conexo: el arbol solo llego a X de las Y aristas necesarias"}`

**Complejidad:** O(E log V) con heap (la que implementamos).
- Cada vértice entra al árbol una vez y en ese momento se encolan sus aristas
  hacia afuera: cada arista se encola a lo sumo dos veces (una por extremo) → O(E) entradas.
- Cada `add`/`poll` del heap es O(log E) = O(log V), porque E ≤ V².
- Las aristas que salen de la cola con los dos extremos ya en el árbol se
  descartan en O(1).
- Total: O(E log V). La versión con arreglo `key[]` y búsqueda lineal del
  mínimo es O(V²) sin importar cuántas aristas haya.

**Estructura usada:** `PriorityQueue` de aristas candidatas ordenada por costo +
`boolean[] enMst` + lista de adyacencia de `Grafo`.

---

### GET /api/grafo/kruskal

Árbol de recubrimiento mínimo construido de otra forma: ordena todas las
rutas de menor a mayor y las va agregando si no forman ciclo.

**Parámetros:** ninguno

**Ejemplo:** `GET /api/grafo/kruskal`

**Respuesta** (200, aristas en el orden en que entran al árbol):
```json
{
  "aristas": [
    {"origen": "SNT", "destino": "MIC", "costo": 2.0},
    {"origen": "DEP", "destino": "SNT", "costo": 3.0},
    {"origen": "MIC", "destino": "REC", "costo": 3.0},
    {"origen": "CAB", "destino": "ALM", "costo": 3.0},
    {"origen": "CAB", "destino": "FLO", "costo": 4.0},
    {"origen": "REC", "destino": "PAL", "costo": 4.0},
    {"origen": "REC", "destino": "ALM", "costo": 4.0},
    {"origen": "BEL", "destino": "VUR", "costo": 4.0},
    {"origen": "PAL", "destino": "BEL", "costo": 5.0}
  ],
  "costoTotal": 32.0
}
```

Mismas 9 aristas y **32 km** que Prim, en otro orden. Descarta PAL-ALM (4) y
MIC-ALM (5) porque sus extremos ya estaban conectados. Con PAL-BEL (5) llega a
9 aristas y corta: las rutas de 6, 9 y 10 km ni se miran.

**Errores:**
- grafo no conexo → 422 `{"estado":"ERROR","detalle":"El grafo no es conexo: el arbol solo llego a X de las Y aristas necesarias"}`

**Complejidad:** O(E log E) = O(E log V).
- Juntar las aristas (una sola vez por par, `u < v`): O(V + E).
- Ordenarlas con nuestro MergeSort: **O(E log E)**. Es el paso que domina.
- Por cada arista, dos `find` y a lo sumo un `union`. Con compresión de
  caminos y unión por rango cada operación es casi O(1) (O(α(V)), donde α
  crece tan lento que en la práctica es ≤ 4) → O(E · α(V)).
- Total: O(E log E), y como E ≤ V², log E ≤ 2 log V → O(E log V).

**Estructura usada:** lista de aristas + MergeSort propio (`Ordenamiento`) +
Union-Find (`padre[]` y `rango[]`).

#### ¿Prim o Kruskal? Depende de la densidad

| Grafo | E respecto de V | Conviene | Por qué |
|---|---|---|---|
| Ralo (como esta red: V = 10, E = 16) | E ≈ V | **Kruskal** o Prim con heap | Ordenar pocas aristas es barato, O(E log E) ≈ O(V log V) |
| Denso | E ≈ V² | **Prim con arreglo** O(V²) | Kruskal tendría que ordenar ~V² aristas: O(V² log V), peor que O(V²) |

En resumen: en un grafo **ralo** el costo lo marca E y los dos andan bien
(Kruskal es más simple si las aristas ya vienen en lista). En un grafo
**denso**, Prim con búsqueda lineal evita ordenar todas las aristas.

---

### GET /api/seleccion/greedy

Elige qué paquetes cargar en la camioneta con un criterio greedy.

**Función objetivo:** maximizar la ganancia total ($) de los paquetes
elegidos, sin que la suma de sus pesos (kg) supere la capacidad. Cada
paquete va entero o no va (mochila 0/1).

**Criterio:** ratio valor/peso ($ por kg) de mayor a menor; se toma cada
paquete si todavía entra, y si no entra se sigue con el siguiente.

**Parámetros:** `capacidad` (kg disponibles en la camioneta)

**Ejemplo:** `GET /api/seleccion/greedy?capacidad=50`

**Respuesta** (200):
```json
{
  "elegidos": [
    {"id": "P1", "nombre": "Paquete 1", "peso": 30.0, "valor": 9000.0, "nodoId": "BEL"},
    {"id": "P4", "nombre": "Paquete 4", "peso": 15.0, "valor": 3600.0, "nodoId": "CAB"},
    {"id": "P6", "nombre": "Paquete 6", "peso": 5.0, "valor": 800.0, "nodoId": "VUR"}
  ],
  "valorTotal": 13400.0,
  "pesoTotal": 50.0,
  "tecnica": "greedy por ratio valor/peso"
}
```

**Caso donde greedy NO da el óptimo** (capacidad 50 kg):

| Paquete | Ratio $/kg | ¿Entra? | Carga | Ganancia |
|---|---|---|---|---|
| P1 | 300 | sí | 30 kg | $9000 |
| P2 | 280 | no (55 > 50) | 30 kg | $9000 |
| P3 | 280 | no (55 > 50) | 30 kg | $9000 |
| P4 | 240 | sí | 45 kg | $12600 |
| P5 | 200 | no (55 > 50) | 45 kg | $12600 |
| P6 | 160 | sí | 50 kg | **$13400** |

El óptimo es **P2 + P3 = 50 kg, $14000**. Greedy pierde $600 porque toma P1
(el mejor ratio) y eso le quita el lugar a dos paquetes que juntos valen más.
Greedy es óptimo en la mochila **fraccional** (se pueden partir paquetes),
no en la 0/1: para el óptimo exacto está `/api/seleccion/dinamica`.

**Errores:** capacidad negativa → 400 `{"estado":"ERROR","detalle":"La capacidad no puede ser negativa: ..."}`

**Complejidad:** O(n log n), con n = cantidad de paquetes.
- Ordenar por ratio con nuestro MergeSort: O(n log n). Recurrencia
  T(n) = 2T(n/2) + O(n), siempre, sin peor caso cuadrático.
- Recorrer la lista ordenada una vez, con una comparación y dos sumas por
  paquete: O(n).
- Total: O(n log n) + O(n) = **O(n log n)**: domina el ordenamiento.
- Espacio: O(n) por la lista ordenada (MergeSort no modifica la de entrada).

**Estructura usada:** lista + MergeSort propio (`Ordenamiento`), estable: ante
empate de ratio (P2 y P3) se respeta el orden de entrada.

---

### GET /api/seleccion/dinamica

Elige qué paquetes cargar con **mochila 0/1 por programación dinámica**:
devuelve siempre la selección óptima.

**Función objetivo:** la misma que greedy: maximizar la ganancia total ($)
sin que la suma de pesos (kg) supere la capacidad, con cada paquete entero o nada.

**Parámetros:** `capacidad` (kg, entero)

**Ejemplo:** `GET /api/seleccion/dinamica?capacidad=50`

**Respuesta** (200):
```json
{
  "elegidos": [
    {"id": "P2", "nombre": "Paquete 2", "peso": 25.0, "valor": 7000.0, "nodoId": "PAL"},
    {"id": "P3", "nombre": "Paquete 3", "peso": 25.0, "valor": 7000.0, "nodoId": "REC"}
  ],
  "valorTotal": 14000.0,
  "pesoTotal": 50.0,
  "tecnica": "programacion dinamica (mochila 0/1)"
}
```

**Errores:**
- capacidad negativa → 400 `{"estado":"ERROR","detalle":"La capacidad no puede ser negativa: ..."}`
- algún paquete con peso no entero → 400 `{"estado":"ERROR","detalle":"El paquete X tiene peso 2.5: la mochila 0/1 con PD requiere pesos enteros no negativos"}`

#### Recurrencia

`dp[i][j]` = mejor ganancia usando solo los primeros `i` paquetes con `j` kg de capacidad.

```
dp[0][j] = 0                                     (sin paquetes no hay ganancia)
dp[i][j] = dp[i-1][j]                            si peso_i > j      (no entra)
dp[i][j] = max(dp[i-1][j], dp[i-1][j-peso_i] + valor_i)   si entra
             no tomarlo    tomarlo
```

La respuesta es `dp[n][W]`. La tabla se llena fila por fila, de arriba hacia
abajo (bottom-up), porque cada fila solo depende de la anterior.

#### Tabla dp del ejemplo (capacidad 50)

La tabla real tiene 51 columnas (j = 0..50). Como todos los pesos son
múltiplos de 5, los valores solo cambian en esas columnas:

| i \ j | 0 | 5 | 10 | 15 | 20 | 25 | 30 | 35 | 40 | 45 | 50 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 0 (ninguno) | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| P1 (30 kg, $9000) | 0 | 0 | 0 | 0 | 0 | 0 | 9000 | 9000 | 9000 | 9000 | 9000 |
| P2 (25 kg, $7000) | 0 | 0 | 0 | 0 | 0 | 7000 | 9000 | 9000 | 9000 | 9000 | 9000 |
| P3 (25 kg, $7000) | 0 | 0 | 0 | 0 | 0 | 7000 | 9000 | 9000 | 9000 | 9000 | **14000** |
| P4 (15 kg, $3600) | 0 | 0 | 0 | 3600 | 3600 | 7000 | 9000 | 9000 | 10600 | 12600 | 14000 |
| P5 (10 kg, $2000) | 0 | 0 | 2000 | 3600 | 3600 | 7000 | 9000 | 9000 | 11000 | 12600 | 14000 |
| P6 (5 kg, $800) | 0 | 800 | 2000 | 3600 | 4400 | 7000 | 9000 | 9800 | 11000 | 12600 | **14000** |

**Recuperación del camino** (desde `dp[6][50]` hacia arriba): si el valor
cambia respecto de la fila de arriba, ese paquete se tomó y se resta su peso a `j`.

| i | dp[i][j] vs dp[i-1][j] | ¿Se toma? | j queda en |
|---|---|---|---|
| P6 | 14000 = 14000 | no | 50 |
| P5 | 14000 = 14000 | no | 50 |
| P4 | 14000 = 14000 | no | 50 |
| P3 | 14000 ≠ 9000 | **sí** | 50 − 25 = 25 |
| P2 | dp[2][25] = 7000 ≠ dp[1][25] = 0 | **sí** | 25 − 25 = 0 |
| P1 | 0 = 0 | no | 0 |

Resultado: **{P2, P3} = 50 kg, $14000**.

#### Juego de datos donde greedy no da el óptimo

Con los mismos 6 paquetes y 50 kg, greedy por ratio valor/peso toma P1 (300 $/kg),
no le entran P2 ni P3, y completa con P4 y P6: **{P1, P4, P6} = $13400**.
Programación dinámica encuentra **{P2, P3} = $14000**. Greedy pierde $600
(ver la tabla paso a paso en `/api/seleccion/greedy`). Lo verifica
`SeleccionTest.greedyPierdeContraDinamica`.

**Complejidad:** O(n · W) en tiempo y en espacio, con n = cantidad de paquetes
y W = capacidad en kg.
- La tabla tiene (n + 1) × (W + 1) celdas y cada una se calcula en O(1)
  (una comparación y una suma) → O(n · W).
- La recuperación del camino recorre una fila por paquete → O(n).
- Espacio: la tabla completa, O(n · W). Se guarda entera porque hace falta
  para recuperar el camino.

**¿Por qué es pseudopolinómica?** O(n · W) parece polinomial, pero W es el
**valor numérico** de la capacidad, no el tamaño de la entrada. Escribir W
ocupa solo log₂ W bits: si la capacidad pasa de 50 a 50.000.000 kg, la entrada
crece apenas unos dígitos, pero la tabla crece un millón de veces. Medido en
bits de la entrada (b = log W), el costo es O(n · 2^b): exponencial. Por eso
se llama pseudopolinómica: es polinomial en el valor de W, no en su tamaño.
Con capacidades chicas, como los 50 kg de la camioneta, es muy eficiente.

**Estructura usada:** matriz `double[n+1][W+1]` llenada bottom-up + `ArrayDeque`
para devolver los elegidos en el orden de entrada.

---

### GET /api/grafo/rutas

Enumera **todas** las rutas simples (sin repetir puntos) entre dos nodos que no
superen un máximo de km ni de saltos. Usa backtracking.

**Parámetros:**
- `origen`, `destino` (ids)
- `costoMaximo` (km, opcional, por defecto 1000)
- `saltosMaximos` (cantidad de rutas recorridas, opcional, por defecto 10)

**Ejemplo:** `GET /api/grafo/rutas?origen=DEP&destino=VUR&costoMaximo=22&saltosMaximos=5`

**Respuesta** (200, 6 rutas; el orden depende de la lista de adyacencia):
```json
[
  {"vertices": ["DEP","SNT","MIC","REC","PAL","VUR"], "costoTotal": 22.0},
  {"vertices": ["DEP","MIC","REC","PAL","BEL","VUR"], "costoTotal": 22.0},
  {"vertices": ["DEP","CAB","ALM","PAL","BEL","VUR"], "costoTotal": 22.0},
  {"vertices": ["DEP","CAB","PAL","BEL","VUR"], "costoTotal": 21.0},
  {"vertices": ["DEP","CAB","PAL","VUR"], "costoTotal": 22.0},
  {"vertices": ["DEP","CAB","FLO","VUR"], "costoTotal": 19.0}
]
```

Si ninguna ruta cumple los límites, la respuesta es `[]`.

**Log:** la comparación de nodos explorados queda en el log de la aplicación:
```
Rutas DEP->VUR (costoMaximo=22.0, saltosMaximos=5): 6 rutas. Nodos explorados: 77 con poda, 154 sin poda
```

**Errores:**
- id inexistente → 400 `{"estado":"ERROR","detalle":"No existe el nodo con id: ..."}`
- `costoMaximo` o `saltosMaximos` negativos → 400 `{"estado":"ERROR","detalle":"costoMaximo y saltosMaximos no pueden ser negativos"}`
- alguna arista con costo negativo → 422 `{"estado":"ERROR","detalle":"Backtracking no admite costos negativos: la arista X->Y cuesta ..."}`

#### Comparación con y sin poda

Cada nodo explorado es una llamada recursiva, o sea un camino parcial. Medido
sobre la red de CABA, de DEP a VUR (`BacktrackingTest`):

| costoMaximo | saltosMaximos | Rutas | Nodos con poda | Nodos sin poda | Ahorro |
|---|---|---|---|---|---|
| 18 | 5 | 0 | 49 | 154 | 68 % |
| 19 | 5 | 1 | 56 | 154 | 64 % |
| 22 | 5 | 6 | 77 | 154 | 50 % |
| 30 | 6 | 27 | 130 | 154 | 16 % |
| 1000 | 3 | 2 | 26 | 154 | 83 % |
| 1000 | 10 | 47 | 154 | 154 | 0 % |

- Con y sin poda encuentran **las mismas rutas**. La poda solo evita explorar
  ramas que no pueden terminar en una ruta válida.
- Cuanto más ajustados son los límites, más ayuda.
- Con límites que no restringen nada (última fila) la poda no ahorra nada: por
  eso **no cambia el peor caso**.
- Con 19 km sobrevive una sola ruta, la de Dijkstra (DEP→CAB→FLO→VUR). Con 18 km
  no hay ninguna.

La poda por costo es correcta porque las distancias son ≥ 0: un camino parcial
que ya se pasó del máximo no puede volver a bajar. Por eso se rechazan los
costos negativos.

**Complejidad:** exponencial en el peor caso.
- **Cuántos caminos hay:** en un grafo completo, de origen a destino se puede
  pasar por cualquier subconjunto de los otros V − 2 vértices, en cualquier
  orden. Son Σ (V−2)! / (V−2−k)! ≈ e · (V−2)! caminos simples, que crece de
  forma factorial. Con V = 10 ya son 109.601.
- **Costo total:** hay que listarlos todos y copiar cada ruta cuesta O(V): **O(V · V!)**
  como cota. Ningún algoritmo puede evitarlo si esas rutas son la salida pedida.
- **Con límite de saltos S:** el árbol tiene profundidad S y se abre en b ramas
  por nodo (b = cantidad de vecinos): **O(b^S)**. En esta red b ronda 2E/V = 3,2.
- **Espacio:** O(V) para `visitado`, el camino y la pila de recursión, más lo
  que ocupen las rutas devueltas.

**Estructura usada:** recursión + `boolean[] visitado` + lista del camino actual
(se marca y agrega al avanzar, se desmarca y quita al volver).

---

### GET /api/seleccion/repartir

_(completar — incluir cuál es la cota y por qué es optimista)_
