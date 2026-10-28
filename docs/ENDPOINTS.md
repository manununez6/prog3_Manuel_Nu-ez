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

_(completar)_

---

### GET /api/grafo/prim

_(completar)_

---

### GET /api/grafo/kruskal

_(completar)_

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
