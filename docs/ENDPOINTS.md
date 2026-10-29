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

Mismas 9 aristas y **32 km** que Prim, en otro orden. Descarta PAL-ALM (4)
porque PAL y ALM ya estaban conectados por REC, y nunca llega a mirar las
rutas de 5 km o más que no entran (corta al llegar a 9 aristas).

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

_(completar — incluir un caso donde greedy NO dé el óptimo)_

---

### GET /api/seleccion/dinamica

_(completar — incluir la tabla dp del ejemplo)_

---

### GET /api/grafo/rutas

_(completar — incluir cantidad de nodos explorados con y sin poda)_

---

### GET /api/seleccion/repartir

_(completar — incluir cuál es la cota y por qué es optimista)_
