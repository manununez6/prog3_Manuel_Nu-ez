# TPO Programacion III 2026 - Red logistica en CABA

## Integrantes

| Nombre completo | Usuario de GitHub |
|---|---|
| Manuel Nuñez | [manununez6](https://github.com/manununez6) |

## Dominio

Modelamos un **deposito en Barracas que reparte paquetes a barrios de CABA**
con una camioneta.

| Concepto del TPO | En este dominio |
|---|---|
| Nodo | Deposito o punto de entrega (`tipo` = `DEPOSITO` o `ENTREGA`) |
| Arista | Ruta entre dos puntos. Es de doble mano, por eso el grafo es **NO dirigido** |
| Costo | Distancia en km (valores aproximados) |
| Item | Paquete: `peso` en kg y `valor` = ganancia en $ por entregarlo |
| Capacidad | La camioneta carga hasta **50 kg** |

### Las 3 preguntas del enunciado

1. **¿Qué es una arista, y qué número tiene encima?**
   Una ruta entre dos puntos de la red, con su distancia en km. Por ejemplo:
   *"la ruta entre el Deposito (DEP) y San Telmo (SNT) cuesta 3 km"*.

2. **¿Tiene sentido preguntar "cuál es el camino más barato de A a B"?**
   Sí, porque para llegar a un barrio casi siempre se pasa por otros. Por ejemplo,
   ir del Deposito a Microcentro por la ruta directa son 6 km, pero pasando por
   San Telmo son 5 km (DEP→SNT→MIC). Y para llegar a Villa Urquiza, el camino
   más corto es DEP→CAB→FLO→VUR = 19 km.

3. **¿Hay algo que seleccionar bajo una restricción de capacidad?**
   Sí: hay 6 paquetes que suman 110 kg y la camioneta lleva solo 50 kg. Hay que
   elegir qué paquetes cargar para maximizar la ganancia. Ver [Paquetes](#paquetes).

## Grafo

Datos cargados por `CargaInicial.java`: 10 nodos y 16 rutas.

### Nodos

| Id | Nombre | Tipo |
|---|---|---|
| DEP | Deposito Central (Barracas) | DEPOSITO |
| SNT | San Telmo | ENTREGA |
| MIC | Microcentro | ENTREGA |
| REC | Recoleta | ENTREGA |
| PAL | Palermo | ENTREGA |
| BEL | Belgrano | ENTREGA |
| ALM | Almagro | ENTREGA |
| CAB | Caballito | ENTREGA |
| FLO | Flores | ENTREGA |
| VUR | Villa Urquiza | ENTREGA |

### Rutas

Cada ruta se guarda una sola vez en Neo4j. La vuelta la agrega `Grafo` al
cargarlo como no dirigido.

| Ruta | km | | Ruta | km |
|---|---|---|---|---|
| DEP - SNT | 3 | | PAL - ALM | 4 |
| DEP - MIC | 6 | | PAL - BEL | 5 |
| DEP - CAB | 6 | | PAL - VUR | 10 |
| SNT - MIC | 2 | | BEL - VUR | 4 |
| MIC - REC | 3 | | ALM - CAB | 3 |
| MIC - ALM | 5 | | CAB - PAL | 6 |
| REC - PAL | 4 | | CAB - FLO | 4 |
| REC - ALM | 4 | | FLO - VUR | 9 |

### Diagrama (km en cada arista)

```
  VUR ----------- 4 --------- BEL
   | \                         |
   |  \                        | 5
   |   \                       |
   |    \                      |
 9 |     '------- 10 -------- PAL ------- 4 ----- REC
   |                         / |                 / |
   |                        /  |                /  |
   |                .- 6 --'   | 4             /   | 3
   |               /           |    .--- 4 ---'    |
   |              /            |   /               |
  FLO --- 4 -- CAB ---- 3 --- ALM ------- 5 ----- MIC
                  \                  .--- 6 -----/ |
                   \                /              | 2
                    \              /               |
                     \            /                |
                      '-- 6 - DEP ------- 3 ----- SNT
```

### Casos donde lo directo no es lo mas corto

Sirven para verificar que Dijkstra relaje bien las aristas:

- **DEP → MIC**: la ruta directa cuesta 6 km, pero DEP→SNT→MIC cuesta 3 + 2 = **5 km**.
- **PAL → VUR**: la ruta directa cuesta 10 km, pero PAL→BEL→VUR cuesta 5 + 4 = **9 km**.

Si Dijkstra devuelve 6 o 10, la relajacion esta mal.

## Paquetes

| Id | Peso (kg) | Valor ($) | Ratio ($/kg) | Destino |
|---|---|---|---|---|
| P1 | 30 | 9000 | 300 | BEL - Belgrano |
| P2 | 25 | 7000 | 280 | PAL - Palermo |
| P3 | 25 | 7000 | 280 | REC - Recoleta |
| P4 | 15 | 3600 | 240 | CAB - Caballito |
| P5 | 10 | 2000 | 200 | FLO - Flores |
| P6 | 5 | 800 | 160 | VUR - Villa Urquiza |

### Contraejemplo: greedy no da el optimo (capacidad 50 kg)

**Greedy por ratio valor/peso** recorre los paquetes de mayor a menor ratio y
toma cada uno si todavia entra:

| Paquete | Ratio | ¿Entra? | Carga acumulada | Ganancia acumulada |
|---|---|---|---|---|
| P1 | 300 | sí | 30 kg | $9000 |
| P2 | 280 | no (55 > 50) | 30 kg | $9000 |
| P3 | 280 | no (55 > 50) | 30 kg | $9000 |
| P4 | 240 | sí | 45 kg | $12600 |
| P5 | 200 | no (55 > 50) | 45 kg | $12600 |
| P6 | 160 | sí | 50 kg | $13400 |

Resultado greedy: **{P1, P4, P6} = $13400**.

**Optimo (mochila 0/1 con programacion dinamica): {P2, P3} = 50 kg, $14000.**

Greedy pierde $600 porque, al tomar P1 primero, la camioneta queda sin lugar
para dos paquetes de 25 kg que juntos valen mas.

## Como correrlo

Requisitos: Java 17+ y Docker.

```bash
# 1. Crear el .env y elegir una contraseña (minimo 8 caracteres)
cp .env.example .env          # editar NEO4J_PASSWORD

# 2. Cargar las variables del .env en la terminal
set -a; source .env; set +a

# 3. Levantar Neo4j con Docker, con la misma contraseña del .env
docker run -d --name neo4j-tpo \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/$NEO4J_PASSWORD \
  neo4j:5

# 4. Levantar la aplicacion (la primera vez carga los datos de CargaInicial)
./mvnw spring-boot:run

# 5. Verificar, en otra terminal
curl "http://localhost:8080/api/grafo/resumen"
# -> {"vertices":10,"aristas":16,"dirigido":false,"ids":[...]}
```

> **Importante:** si la base ya tiene nodos, la carga inicial **no corre** (se
> ve en el log: *"La base ya tiene datos. No se ejecuta la carga inicial."*).
> Si quedaron datos viejos, borrarlos desde Neo4j Browser
> (http://localhost:7474) con `MATCH (n) DETACH DELETE n` y reiniciar la app.
> La carga inicial se desactiva con `app.seed.enabled=false`.

Tests: `./mvnw test`. Ejemplos de requests en `docs/api.http`.

## Estructura

```
model/          Nodo, Conexion (arista con costo), Item
repository/     acceso a Neo4j — repositorios BLOQUEANTES, sin Mono/Flux
service/        Grafo (lista de adyacencia en memoria) y GrafoService (carga)
algorithm/      >>> ACÁ VA TODO EL TRABAJO <<<  los pendientes son stubs con TODO
controller/     endpoints ya cableados — NO poner lógica algorítmica acá
seed/           CargaInicial: la red logistica de CABA
```

## Algoritmos

| Algoritmo | Endpoint | Estado |
|---|---|---|
| DFS | `GET /api/grafo/dfs?origen=DEP` | **implementado** |
| BFS | `GET /api/grafo/bfs?origen=DEP` | **implementado** |
| Dijkstra | `GET /api/grafo/dijkstra?origen=DEP&destino=MIC` | **implementado** |
| Floyd-Warshall | `GET /api/grafo/floyd` | **implementado** |
| UCS (opcional) | `GET /api/grafo/ucs?origen=DEP&destino=VUR` | pendiente |
| Prim | `GET /api/grafo/prim?origen=DEP` | **implementado** |
| Kruskal | `GET /api/grafo/kruskal` | **implementado** |
| QuickSort | `GET /api/seleccion/quicksort?criterio=ratio` | **implementado** |
| MergeSort | `GET /api/seleccion/mergesort?criterio=peso` | **implementado** |
| Greedy | `GET /api/seleccion/greedy?capacidad=50` | **implementado** |
| Mochila 0/1 (PD) | `GET /api/seleccion/dinamica?capacidad=50` | **implementado** |
| Backtracking | `GET /api/grafo/rutas?origen=DEP&destino=VUR&costoMaximo=30` | **implementado** |
| Branch & Bound | `GET /api/seleccion/repartir?contenedores=3` | **implementado** |

Los endpoints pendientes responden `501` con `{"estado":"PENDIENTE", ...}`.

## Informe de complejidades

V = vértices, E = aristas, n = paquetes, W = capacidad en kg, k = camionetas,
b = vecinos por nodo y S = límite de saltos. El espacio es el **adicional** a la
entrada, sin contar lo que se devuelve. El detalle de cada justificación está en
`docs/ENDPOINTS.md`.

| Algoritmo | Recurrencia | Temporal | Espacial | Estructura que la sostiene |
|---|---|---|---|---|
| DFS | — | O(V + E) | O(V) | Lista de adyacencia, `boolean[] visitado`, pila de recursión |
| BFS | — | O(V + E) | O(V) | Lista de adyacencia, `boolean[] visitado`, cola FIFO (`ArrayDeque`) |
| Dijkstra | `dist[v] = min(dist[v], dist[u] + c)` | O((V + E) log V) | O(V + E) | Heap binario (`PriorityQueue`), `dist[]`, `predecesor[]` |
| Floyd-Warshall | `d[i][j] = min(d[i][j], d[i][k] + d[k][j])` | O(V³) | O(V²) | Matriz de adyacencia |
| Prim | — | O(E log V) | O(V + E) | Heap binario de aristas candidatas, `boolean[] enMst` |
| Kruskal | — | O(E log E) = O(E log V) | O(V + E) | Lista de aristas, MergeSort propio, Union-Find (`padre[]`, `rango[]`) |
| QuickSort | Promedio: T(n) = 2T(n/2) + O(n). Peor: T(n) = T(n−1) + O(n) | O(n log n) promedio, O(n²) peor | O(n), con pila O(log n) promedio y O(n) peor | `ArrayList`, partición de Lomuto, pivote del medio |
| MergeSort | T(n) = 2T(n/2) + O(n) | O(n log n) | O(n) | Listas auxiliares para la mezcla |
| Greedy | La de MergeSort | O(n log n) | O(n) | Lista ordenada con MergeSort propio |
| Mochila 0/1 (PD) | `dp[i][j] = max(dp[i-1][j], dp[i-1][j-w] + v)` | O(n · W), pseudopolinómica | O(n · W) | Matriz `dp[n+1][W+1]` |
| Backtracking | T(S) = b · T(S−1) + O(b) | O(b^S) con límite de saltos, factorial en el peor caso (grafo completo) | O(V) | Recursión, `boolean[] visitado`, camino actual |
| Branch & Bound | T(i) = k · T(i+1) + O(k) | O(n · kⁿ) en el peor caso | O(n + k) | Recursión, `double[] carga`, `int[] asignacion` |

Dijkstra y Floyd-Warshall muestran su relación de relajación, porque no son
recursivos. La poda de Backtracking y Branch & Bound no mejora el peor caso, pero
reduce mucho los nodos explorados en la práctica.

## Antes de entregar

- [x] `./mvnw test` pasa
- [x] no hay ninguna contraseña en el código ni en `application.properties`
- [x] cada algoritmo implementado tiene al menos un test
- [x] `docs/ENDPOINTS.md` documenta entrada y salida de cada endpoint
- [x] el README explica **qué dominio eligieron y qué representa el costo**

Ver `GUIA-TPO.md` para el paso a paso.

## Uso de IA

Elegí el dominio, armé el grafo y los paquetes, y definí el orden de trabajo por hitos.
Usé Claude como apoyo para configurar el entorno (Docker, WSL, Neo4j) y organizar el plan, y Claude Code como apoyo para generar una primera versión de los algoritmos y sus tests. Completé y revisé cada parte antes de commitearla, verifiqué los resultados de cada endpoint contra valores esperados calculados sobre el grafo (por ejemplo, Dijkstra DEP→VUR = 19 km y la mochila con 50 kg = $14.000).
