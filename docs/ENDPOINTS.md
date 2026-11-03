# Documentación de endpoints

Fichas de todos los endpoints implementados, con los datos de `CargaInicial`
(red logística de CABA y paquetes P1 a P6). UCS es opcional y no está implementado:
`GET /api/grafo/ucs` responde `501`, por eso no tiene ficha.

**Cómo se obtuvieron los ejemplos:** son respuestas HTTP reales de los controllers
y los algoritmos, capturadas con MockMvc y los datos que guarda `CargaInicial`,
pero con los repositorios simulados, sin una base Neo4j. Si Neo4j devuelve los
nodos en otro orden, puede cambiar el orden de los resultados y, en
`/repartir`, los conteos de nodos o qué camioneta lleva cada paquete. Las
distancias, los costos totales y la carga máxima no cambian.

**Sobre las complejidades:** cada una es la del algoritmo. Antes de ejecutarlo,
el endpoint hace una sola consulta (`findAll`) y los algoritmos trabajan en memoria,
sin consultar la base.

---

### GET /api/grafo/resumen

Metadatos del grafo cargado. No es un algoritmo del TPO: sirve para verificar la
conexión a la base y los datos.

**Parámetros:** `dirigido` (opcional, por defecto `false`). Las rutas son de doble
mano, así que la red se usa como no dirigida.

**Ejemplo:** `GET /api/grafo/resumen`

**Respuesta** (200):
```json
{"vertices": 10, "dirigido": false, "aristas": 16, "ids": ["DEP","SNT","MIC","REC","PAL","BEL","ALM","CAB","FLO","VUR"]}
```

**Errores:** este endpoint no define errores propios.

**Complejidad:** O(V + E) en tiempo y en espacio.
- Armar `Grafo` recorre cada nodo y cada una de sus conexiones una vez: O(V + E).
- Contar las aristas suma el tamaño de cada lista de adyacencia: O(V).

**Estructura usada:** lista de adyacencia de `Grafo` + `HashMap` de id a índice.

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

**Errores:**
- `origen` inexistente → 400 `{"estado":"ERROR","detalle":"No existe el nodo con id: ..."}`
- falta un parámetro obligatorio → 400 (respuesta estándar de Spring, sin `estado` ni `detalle`)

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

**Errores:**
- `origen` inexistente → 400 `{"estado":"ERROR","detalle":"No existe el nodo con id: ..."}`
- falta un parámetro obligatorio → 400 (respuesta estándar de Spring, sin `estado` ni `detalle`)

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
- falta un parámetro obligatorio → 400 (respuesta estándar de Spring, sin `estado` ni `detalle`)
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

**Respuesta** (200, un mapa `"origen->destino"`; son 100 pares, acá solo la fila de DEP):
```json
{"DEP->DEP":0.0,"DEP->SNT":3.0,"DEP->MIC":5.0,"DEP->REC":8.0,"DEP->PAL":12.0,"DEP->BEL":17.0,"DEP->ALM":9.0,"DEP->CAB":6.0,"DEP->FLO":10.0,"DEP->VUR":19.0}
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

**Errores:** este endpoint no recibe parámetros. Ciclo de costo negativo → 422 `{"estado":"ERROR","detalle":"Floyd-Warshall encontro un ciclo de costo negativo que pasa por X: los costos minimos no estan definidos"}`.
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

**Sobre esta red (V = 10, E = 16):** el triple bucle de Floyd hace **1.000
comparaciones** (V³ = 10³). Correr Dijkstra desde los 10 vértices expande **100
nodos** (10 por corrida) y revisa **320 aristas** (32 por corrida, porque cada una
de las 16 rutas se ve desde sus dos extremos). Además, la cola de prioridad saca
112 entradas: esos 100 nodos expandidos más **12 entradas repetidas** que se
descartan. Son conteos reales sobre el grafo de `CargaInicial`, sin contar la
validación de costos negativos que hace Dijkstra antes de empezar. Si cambia el orden de carga, las repetidas
pueden ser hasta 14 (lo probé con 5.000 órdenes al azar); los otros números no
cambian. Las operaciones de Floyd son O(1), mientras que cada entrada de la cola
cuesta además O(log E) al encolarla y al sacarla.

Esta red es rala (V = 10, E = 16), así que asintóticamente conviene Dijkstra V
veces. Con solo 10 nodos, esa diferencia de operaciones no llega a notarse en el
tiempo de respuesta y Floyd es más simple. Si solo
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
- falta un parámetro obligatorio → 400 (respuesta estándar de Spring, sin `estado` ni `detalle`)
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
    {"origen": "ALM", "destino": "CAB", "costo": 3.0},
    {"origen": "REC", "destino": "PAL", "costo": 4.0},
    {"origen": "REC", "destino": "ALM", "costo": 4.0},
    {"origen": "BEL", "destino": "VUR", "costo": 4.0},
    {"origen": "CAB", "destino": "FLO", "costo": 4.0},
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

### GET /api/seleccion/quicksort

Ordena los paquetes con QuickSort, de menor a mayor según el criterio. Devuelve
una lista nueva: no modifica la de entrada.

**Parámetros:** `criterio` (opcional, por defecto `ratio`): `valor`, `peso` o `ratio`
(valor/peso)

**Ejemplo:** `GET /api/seleccion/quicksort?criterio=ratio`

**Respuesta** (200):
```json
[
  {"id": "P6", "nombre": "Paquete 6", "peso": 5.0, "valor": 800.0, "nodoId": "VUR"},
  {"id": "P5", "nombre": "Paquete 5", "peso": 10.0, "valor": 2000.0, "nodoId": "FLO"},
  {"id": "P4", "nombre": "Paquete 4", "peso": 15.0, "valor": 3600.0, "nodoId": "CAB"},
  {"id": "P2", "nombre": "Paquete 2", "peso": 25.0, "valor": 7000.0, "nodoId": "PAL"},
  {"id": "P3", "nombre": "Paquete 3", "peso": 25.0, "valor": 7000.0, "nodoId": "REC"},
  {"id": "P1", "nombre": "Paquete 1", "peso": 30.0, "valor": 9000.0, "nodoId": "BEL"}
]
```

Ratios: P6 = 160, P5 = 200, P4 = 240, P2 = P3 = 280, P1 = 300. QuickSort no es
estable: no garantiza el orden entre P2 y P3, que empatan.

**Errores:** criterio desconocido → 400 `{"estado":"ERROR","detalle":"Criterio invalido: x. Use valor, peso o ratio."}`

**Complejidad:** O(n log n) en promedio y O(n²) en el peor caso.
- **Partición:** recorre el rango una vez comparando contra el pivote: O(n).
- **Caso promedio:** el pivote parte el rango en dos mitades parecidas.
  T(n) = 2T(n/2) + O(n) = **O(n log n)**: hay log n niveles y cada uno cuesta O(n).
- **Peor caso:** cada partición deja 0 y n−1 elementos.
  T(n) = T(n−1) + O(n) = **O(n²)**. Usar el elemento del medio como pivote evita
  que pase con listas ya ordenadas, pero sigue existiendo alguna entrada adversaria.
- **Espacio:** O(n) por la copia de la lista, más la pila de recursión: O(log n) en
  promedio y O(n) en el peor caso.

**Estructura usada:** `ArrayList` (copia de la entrada) con partición de Lomuto en
el lugar y pivote en el elemento del medio.

---

### GET /api/seleccion/mergesort

Ordena los paquetes con MergeSort, de menor a mayor según el criterio. Es estable:
ante un empate respeta el orden de entrada. Devuelve una lista nueva.

**Parámetros:** `criterio` (opcional, por defecto `ratio`): `valor`, `peso` o `ratio`
(valor/peso)

**Ejemplo:** `GET /api/seleccion/mergesort?criterio=peso`

**Respuesta** (200):
```json
[
  {"id": "P6", "nombre": "Paquete 6", "peso": 5.0, "valor": 800.0, "nodoId": "VUR"},
  {"id": "P5", "nombre": "Paquete 5", "peso": 10.0, "valor": 2000.0, "nodoId": "FLO"},
  {"id": "P4", "nombre": "Paquete 4", "peso": 15.0, "valor": 3600.0, "nodoId": "CAB"},
  {"id": "P2", "nombre": "Paquete 2", "peso": 25.0, "valor": 7000.0, "nodoId": "PAL"},
  {"id": "P3", "nombre": "Paquete 3", "peso": 25.0, "valor": 7000.0, "nodoId": "REC"},
  {"id": "P1", "nombre": "Paquete 1", "peso": 30.0, "valor": 9000.0, "nodoId": "BEL"}
]
```

P2 y P3 empatan en 25 kg: P2 queda primero porque ya venía antes en la entrada.

**Errores:** criterio desconocido → 400 `{"estado":"ERROR","detalle":"Criterio invalido: x. Use valor, peso o ratio."}`

**Complejidad:** O(n log n) siempre.
- **Recurrencia:** T(n) = 2T(n/2) + O(n). Siempre divide por la mitad, sin importar
  los datos, y mezclar dos listas ordenadas es O(n).
- **Regla práctica:** a = 2, b = 2, k = 1. Como a = bᵏ, T(n) = O(nᵏ log n) = **O(n log n)**.
- **Espacio:** O(n) por las listas auxiliares de la mezcla, más O(log n) de la pila
  de recursión.

**Estructura usada:** listas auxiliares (`ArrayList`) para la mezcla.

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

**Errores:**
- capacidad negativa → 400 `{"estado":"ERROR","detalle":"La capacidad no puede ser negativa: ..."}`
- falta un parámetro obligatorio → 400 (respuesta estándar de Spring, sin `estado` ni `detalle`)

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
- capacidad con decimales (ej. `12.5`) → 400 `{"estado":"ERROR","detalle":"For input string: \"12.5\""}`
- falta un parámetro obligatorio → 400 (respuesta estándar de Spring, sin `estado` ni `detalle`)
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
- falta un parámetro obligatorio → 400 (respuesta estándar de Spring, sin `estado` ni `detalle`)
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

Reparte los paquetes entre `k` camionetas iguales minimizando los kg de la más
cargada (problema de *makespan*). Usa Branch & Bound.

**Función objetivo:** minimizar la carga máxima entre las camionetas. Todos los
paquetes se reparten, sin límite de capacidad.

**Parámetros:** `contenedores` (cantidad de camionetas, `k` ≥ 1)

**Ejemplo:** `GET /api/seleccion/repartir?contenedores=3`

**Respuesta** (200):
```json
{
  "porContenedor": {
    "Camioneta 1": [
      {"id": "P1", "nombre": "Paquete 1", "peso": 30.0, "valor": 9000.0, "nodoId": "BEL"},
      {"id": "P5", "nombre": "Paquete 5", "peso": 10.0, "valor": 2000.0, "nodoId": "FLO"}
    ],
    "Camioneta 2": [
      {"id": "P2", "nombre": "Paquete 2", "peso": 25.0, "valor": 7000.0, "nodoId": "PAL"},
      {"id": "P4", "nombre": "Paquete 4", "peso": 15.0, "valor": 3600.0, "nodoId": "CAB"}
    ],
    "Camioneta 3": [
      {"id": "P3", "nombre": "Paquete 3", "peso": 25.0, "valor": 7000.0, "nodoId": "REC"},
      {"id": "P6", "nombre": "Paquete 6", "peso": 5.0, "valor": 800.0, "nodoId": "VUR"}
    ]
  },
  "cargaMaxima": 40.0,
  "nodosExplorados": 52,
  "nodosPodados": 60
}
```

Con `k = 3` las cargas quedan en 40, 40 y 30 kg. No se puede bajar de 40: los
pesos son múltiplos de 5, y con un máximo de 35 entrarían solo 3 × 35 = 105 kg,
menos que los 110 kg que hay.

**Errores:**
- `contenedores` menor a 1 → 400 `{"estado":"ERROR","detalle":"La cantidad de contenedores debe ser al menos 1"}`
- falta un parámetro obligatorio → 400 (respuesta estándar de Spring, sin `estado` ni `detalle`)
- algún paquete con peso negativo → 400 `{"estado":"ERROR","detalle":"El paquete X tiene peso negativo: ..."}`

#### La cota y por qué es optimista

En cada nodo, con algunos paquetes ya ubicados:

```
cota = max( carga máxima actual , suma de todos los pesos / k )
```

Es una **cota inferior**: ninguna solución de esa rama puede terminar por debajo
de ese valor. Es **optimista** porque nunca promete algo peor que lo que la rama
puede lograr, y cada término lo garantiza por separado:

- **Carga máxima actual:** agregar paquetes nunca baja una carga. La camioneta
  más cargada hoy termina con esa carga o más.
- **Suma de pesos / k:** al final todos los paquetes están repartidos, y el
  promedio de las `k` cargas es exactamente ese valor. El máximo de varios
  números nunca es menor que su promedio.

Como cada término es un piso del resultado, el mayor de los dos también lo es.
Puede quedar por debajo del valor real: con `k = 3` la raíz da 110/3 = 36,67 y el
óptimo es 40. Esa diferencia es válida y solo significa que se poda algo menos.

**Podar:** si `cota >= mejor encontrado`, ninguna solución de la rama puede ser
estrictamente mejor, así que abandonarla no pierde el óptimo. Si la cota fuera
pesimista (mayor que el mejor valor real de la rama) podría descartar la rama
que contiene el óptimo y el resultado sería peor, **sin ningún error visible**.

`RamificacionYPodaTest` compara el resultado contra fuerza bruta para varios
juegos de datos y valores de `k`.

#### Nodos explorados y podados

Con los paquetes en el orden de `CargaInicial`. Los conteos cambian si cambia el
orden (el óptimo no):

| k | Carga máxima | Explorados | Podados | Árbol sin poda | Ahorro |
|---|---|---|---|---|---|
| 1 | 110 | 7 | 0 | 7 | 0 % |
| 2 | 55 | 28 | 5 | 127 | 78 % |
| 3 | 40 | 52 | 60 | 1.093 | 95 % |
| 4 | 30 | 45 | 68 | 5.461 | 99 % |

Un nodo podado no se cuenta como explorado, y todo lo que colgaba de él ni se
genera. Por eso explorados + podados da mucho menos que el árbol completo.

**Complejidad:** exponencial, O(n · kⁿ) en el peor caso, con n paquetes.
- **Árbol:** cada paquete puede ir a cualquiera de las `k` camionetas. Hay kⁿ
  hojas y 1 + k + … + kⁿ = O(kⁿ) nodos para k ≥ 2.
- **Por nodo:** calcular la cota de cada hijo es O(1), porque la suma de pesos
  se calcula una vez y la carga máxima se actualiza al avanzar.
- **Por solución mejor:** copiar la asignación cuesta O(n). De ahí el factor n.
- **Espacio:** O(n + k) para la recursión, las cargas y la asignación.

La poda no cambia el peor caso, pero en la práctica recorta muchísimo, como
muestra la tabla.

**Estructura usada:** recursión + `double[] carga` + `int[] asignacion` (con
copia de la mejor).

---

## Verificación a mano (Hito 5)

Cálculos paso a paso sobre la red de CargaInicial, comparados con lo que devuelven los endpoints.

### Dijkstra de DEP a VUR

En cada paso se cierra el nodo no cerrado con menor distancia y se relajan sus vecinos.
Entre paréntesis, el predecesor. ✓ = nodo cerrado.

| Paso | Cierro | DEP | SNT | MIC | REC | PAL | BEL | ALM | CAB | FLO | VUR |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 0 | — | 0 | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ | ∞ |
| 1 | DEP | 0 ✓ | 3 (DEP) | 6 (DEP) | ∞ | ∞ | ∞ | ∞ | 6 (DEP) | ∞ | ∞ |
| 2 | SNT | ✓ | 3 ✓ | **5 (SNT)** | ∞ | ∞ | ∞ | ∞ | 6 | ∞ | ∞ |
| 3 | MIC | ✓ | ✓ | 5 ✓ | 8 (MIC) | ∞ | ∞ | 10 (MIC) | 6 | ∞ | ∞ |
| 4 | CAB | ✓ | ✓ | ✓ | 8 | 12 (CAB) | ∞ | **9 (CAB)** | 6 ✓ | 10 (CAB) | ∞ |
| 5 | REC | ✓ | ✓ | ✓ | 8 ✓ | 12 | ∞ | 9 | ✓ | 10 | ∞ |
| 6 | ALM | ✓ | ✓ | ✓ | ✓ | 12 | ∞ | 9 ✓ | ✓ | 10 | ∞ |
| 7 | FLO | ✓ | ✓ | ✓ | ✓ | 12 | ∞ | ✓ | ✓ | 10 ✓ | 19 (FLO) |
| 8 | PAL | ✓ | ✓ | ✓ | ✓ | 12 ✓ | 17 (PAL) | ✓ | ✓ | ✓ | 19 |
| 9 | BEL | ✓ | ✓ | ✓ | ✓ | ✓ | 17 ✓ | ✓ | ✓ | ✓ | 19 |
| 10 | VUR | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 19 ✓ |

- Paso 2: MIC baja de 6 a 5 porque DEP→SNT→MIC (3 + 2) es más corto que la ruta directa (6).
- Paso 4: ALM baja de 10 a 9 por DEP→CAB→ALM (6 + 3).
- Rutas revisadas que no mejoran (se compara con <, así que un empate no cambia nada):
  paso 5, REC→PAL (8 + 4 = 12, igual a 12) y REC→ALM (8 + 4 = 12, peor que 9);
  paso 6, ALM→PAL (9 + 4 = 13, peor que 12); paso 8, PAL→VUR (12 + 10 = 22, peor que 19),
  aunque en ese paso PAL→BEL sí actualiza BEL de ∞ a 17; paso 9, BEL→VUR (17 + 4 = 21,
  peor que 19).

Camino reconstruido desde los predecesores: VUR ← FLO ← CAB ← DEP → **DEP, CAB, FLO, VUR = 19 km**.
`GET /api/grafo/dijkstra?origen=DEP&destino=VUR` devuelve el mismo camino y costo 19. ✅

### Kruskal

Rutas ordenadas por km. Se agrega una ruta solo si une dos grupos distintos (find(u) != find(v)).

| # | Ruta | km | ¿Mismo grupo? | ¿Se agrega? | Total |
|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | SNT-MIC | 2 | No | Sí | 2 |
| 2 | DEP-SNT | 3 | No | Sí | 5 |
| 3 | MIC-REC | 3 | No | Sí | 8 |
| 4 | ALM-CAB | 3 | No | Sí | 11 |
| 5 | REC-PAL | 4 | No | Sí | 15 |
| 6 | REC-ALM | 4 | No (une {DEP,SNT,MIC,REC,PAL} con {ALM,CAB}) | Sí | 19 |
| 7 | PAL-ALM | 4 | Sí | No, formaría ciclo | 19 |
| 8 | BEL-VUR | 4 | No | Sí | 23 |
| 9 | CAB-FLO | 4 | No | Sí | 27 |
| 10 | MIC-ALM | 5 | Sí | No, formaría ciclo | 27 |
| 11 | PAL-BEL | 5 | No (une el grupo grande con {BEL,VUR}) | Sí | **32** |

Con 9 rutas (V − 1) se termina; no hace falta mirar DEP-MIC, DEP-CAB, CAB-PAL (6), FLO-VUR (9) ni PAL-VUR (10).
`GET /api/grafo/kruskal` devuelve 9 aristas y costo total 32. ✅

### Prim desde DEP

En cada paso se elige la ruta más corta que sale del árbol hacia un nodo que todavía no está.

| Paso | Ruta elegida | km | Nodo nuevo | Total |
|:---:|:---:|:---:|:---:|:---:|
| 1 | DEP-SNT | 3 | SNT | 3 |
| 2 | SNT-MIC | 2 | MIC | 5 |
| 3 | MIC-REC | 3 | REC | 8 |
| 4 | REC-PAL | 4 | PAL | 12 |
| 5 | REC-ALM | 4 | ALM | 16 |
| 6 | ALM-CAB | 3 | CAB | 19 |
| 7 | CAB-FLO | 4 | FLO | 23 |
| 8 | PAL-BEL | 5 | BEL | 28 |
| 9 | BEL-VUR | 4 | VUR | **32** |

En el paso 4 hay empate entre REC-PAL y REC-ALM (4 km), y en el paso 5 entre REC-ALM y
PAL-ALM (4 km). Según cómo se desempate, el árbol puede tener otras aristas, pero el
costo total siempre es 32: todo árbol de recubrimiento mínimo tiene el mismo costo.
En estas dos tablas, Prim y Kruskal eligen las mismas 9 rutas (32 km). `GET /api/grafo/prim?origen=DEP` devuelve 9 aristas y costo total 32. ✅
