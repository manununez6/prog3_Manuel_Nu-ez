package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * UNIDAD: Grafos II (clase 5) y PD sobre grafos (clase 8)
 * PUNTAJE: forma parte de los 3 puntos de "Dijkstra, Prim, Kruskal"
 *          y de 1 punto de programacion dinamica si implementan Floyd.
 */
@Component
public class CaminosMinimos {

    /** Resultado de una consulta de camino minimo. */
    public record Camino(List<String> vertices, double costoTotal) { }

    /** Entrada de la cola de prioridad: un vertice con la distancia con la que se encolo. */
    private record Entrada(int vertice, double distancia) { }

    /**
     * Dijkstra: camino de costo minimo desde un origen hasta un destino.
     *
     * Requisitos:
     *   - usar cola de prioridad (PriorityQueue), no busqueda lineal del minimo
     *   - implementar la relajacion de aristas
     *   - mantener un arreglo de predecesores para poder RECONSTRUIR el camino,
     *     no solo devolver la distancia
     *   - validar que no haya aristas de costo negativo y fallar con un mensaje
     *     claro si las hay
     *
     * Complejidad esperada: O((V + E) log V) con heap binario.
     *
     * Para el coloquio: saber explicar por que Dijkstra es un algoritmo greedy
     * y por que los pesos negativos lo rompen.
     *
     * La cola puede tener varias entradas del mismo vertice (una por cada vez
     * que se mejoro su distancia). En vez de actualizar la prioridad, que
     * PriorityQueue no soporta en O(log n), se encola de nuevo y al sacar una
     * entrada vieja (distancia mayor a dist[u]) se descarta. Hay como mucho
     * E entradas, y log E = O(log V).
     *
     * No corta al sacar el destino: calcula las distancias a todos los
     * vertices. La parada temprana es justamente lo que agrega UCS.
     */
    public Camino dijkstra(Grafo grafo, String origenId, String destinoId) {
        int origen = grafo.indiceDe(origenId);
        int destino = grafo.indiceDe(destinoId);
        validarCostosNoNegativos(grafo);

        int n = grafo.cantidadVertices();
        double[] dist = new double[n];
        int[] predecesor = new int[n];
        Arrays.fill(dist, Grafo.INFINITO);
        Arrays.fill(predecesor, -1);

        PriorityQueue<Entrada> cola = new PriorityQueue<>(
                (x, y) -> Double.compare(x.distancia(), y.distancia()));
        dist[origen] = 0;
        cola.add(new Entrada(origen, 0));

        while (!cola.isEmpty()) {
            Entrada actual = cola.poll();
            int u = actual.vertice();
            if (actual.distancia() > dist[u]) {
                continue;               // entrada vieja: u ya se cerro con una distancia menor
            }
            for (Grafo.Arista a : grafo.vecinos(u)) {
                int v = a.destino();
                // relajacion: ¿llegar a v pasando por u es mas barato que lo que tenia?
                if (dist[u] + a.costo() < dist[v]) {
                    dist[v] = dist[u] + a.costo();
                    predecesor[v] = u;
                    cola.add(new Entrada(v, dist[v]));
                }
            }
        }

        if (dist[destino] == Grafo.INFINITO) {
            throw new IllegalStateException(
                    "No hay camino de " + origenId + " a " + destinoId + ": no estan conectados");
        }
        return new Camino(reconstruirCamino(grafo, predecesor, destino), dist[destino]);
    }

    /** Dijkstra asume costos >= 0: con uno negativo, un vertice ya cerrado podria mejorar. */
    private void validarCostosNoNegativos(Grafo grafo) {
        for (int u = 0; u < grafo.cantidadVertices(); u++) {
            for (Grafo.Arista a : grafo.vecinos(u)) {
                if (a.costo() < 0) {
                    throw new IllegalStateException("Dijkstra no admite costos negativos: la arista "
                            + grafo.idDe(u) + "->" + grafo.idDe(a.destino()) + " cuesta " + a.costo());
                }
            }
        }
    }

    /** Sigue los predecesores desde el destino hasta el origen (el que tiene -1). */
    private List<String> reconstruirCamino(Grafo grafo, int[] predecesor, int destino) {
        Deque<String> pila = new ArrayDeque<>();
        for (int v = destino; v != -1; v = predecesor[v]) {
            pila.push(grafo.idDe(v));   // push agrega adelante: queda origen primero
        }
        return new ArrayList<>(pila);
    }

    /**
     * Floyd-Warshall: costo minimo entre TODOS los pares de vertices.
     *
     * Requisitos:
     *   - trabajar sobre la matriz de adyacencia (grafo.matrizDeAdyacencia())
     *   - el bucle del vertice intermedio k va SIEMPRE por fuera de los de i y j
     *   - detectar ciclos de costo negativo revisando la diagonal al terminar
     *
     * Complejidad esperada: O(V^3) en tiempo, O(V^2) en espacio.
     *
     * Devuelve un mapa "origen->destino" con el costo minimo de cada par.
     */
    public Map<String, Double> floydWarshall(Grafo grafo) {
        throw new PendienteDeImplementar("Floyd-Warshall");
    }

    /**
     * UCS (Uniform Cost Search): igual que Dijkstra pero con parada temprana
     * al expandir el destino.
     *
     * OPCIONAL. Solo suma si ya estan implementados Dijkstra y Floyd.
     * Si lo hacen, en el coloquio hay que poder mostrar la diferencia de
     * nodos expandidos respecto de Dijkstra sobre la misma consulta.
     */
    public Camino ucs(Grafo grafo, String origenId, String destinoId) {
        throw new PendienteDeImplementar("UCS");
    }
}
