package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * UNIDAD: Grafos II (clase 5)
 * PUNTAJE: forma parte de los 3 puntos de "Dijkstra, Prim, Kruskal"
 *
 * ATENCION: el arbol de recubrimiento minimo solo tiene sentido sobre un
 * grafo NO DIRIGIDO y CONEXO. Al cargar el grafo hay que pedirlo con
 * grafoService.cargar(false).
 */
@Component
public class ArbolRecubrimiento {

    /** Una arista del arbol resultante, con ids de negocio. */
    public record AristaMst(String origen, String destino, double costo) { }

    /** Arbol de recubrimiento minimo completo. */
    public record Mst(List<AristaMst> aristas, double costoTotal) { }

    /** Arista candidata con indices internos: la usan la cola de Prim y el orden de Kruskal. */
    private record Candidata(int desde, int hasta, double costo) { }

    private final Ordenamiento ordenamiento;

    public ArbolRecubrimiento(Ordenamiento ordenamiento) {
        this.ordenamiento = ordenamiento;
    }

    /**
     * Prim: hace crecer un unico arbol desde un vertice inicial, tomando en
     * cada paso la arista de menor costo que cruza de dentro hacia afuera.
     *
     * Requisitos:
     *   - arreglos key[], parent[] e inMST[], o bien una cola de prioridad
     *   - el resultado debe tener exactamente V-1 aristas si el grafo es conexo
     *   - si el grafo NO es conexo, fallar con un mensaje claro
     *
     * Complejidad esperada: O(V^2) con busqueda lineal, O(E log V) con heap.
     * Hay que decir cual de las dos implementaron y por que.
     *
     * Implementada con heap (PriorityQueue de aristas candidatas): O(E log V).
     * Se eligio porque la red logistica es rala (E = 16, cercano a V = 10,
     * lejos de V^2 = 100); con busqueda lineal seria O(V^2) sin importar
     * cuantas aristas haya.
     * Cada arista entra a la cola a lo sumo una vez por extremo, asi que hay
     * O(E) add/poll de O(log E) = O(log V) cada uno.
     */
    public Mst prim(Grafo grafo, String origenId) {
        validarNoDirigido(grafo);
        int origen = grafo.indiceDe(origenId);
        int n = grafo.cantidadVertices();
        boolean[] enMst = new boolean[n];
        List<AristaMst> aristas = new ArrayList<>();
        double costoTotal = 0;

        PriorityQueue<Candidata> cola = new PriorityQueue<>(
                (x, y) -> Double.compare(x.costo(), y.costo()));
        agregarAlArbol(grafo, origen, enMst, cola);

        while (!cola.isEmpty() && aristas.size() < n - 1) {
            Candidata c = cola.poll();
            if (enMst[c.hasta()]) {
                continue;               // los dos extremos ya estan en el arbol: formaria ciclo
            }
            aristas.add(new AristaMst(grafo.idDe(c.desde()), grafo.idDe(c.hasta()), c.costo()));
            costoTotal += c.costo();
            agregarAlArbol(grafo, c.hasta(), enMst, cola);
        }

        validarConexo(grafo, aristas);
        return new Mst(aristas, costoTotal);
    }

    /** Marca u como parte del arbol y encola las aristas que salen de u hacia afuera. */
    private void agregarAlArbol(Grafo grafo, int u, boolean[] enMst, PriorityQueue<Candidata> cola) {
        enMst[u] = true;
        for (Grafo.Arista a : grafo.vecinos(u)) {
            if (!enMst[a.destino()]) {
                cola.add(new Candidata(u, a.destino(), a.costo()));
            }
        }
    }

    /**
     * Kruskal: ordena TODAS las aristas por costo y las agrega salteando las
     * que formarian ciclo.
     *
     * Requisitos:
     *   - implementar Union-Find (find y union) a mano, sin librerias
     *   - la deteccion de ciclo es: find(u) == find(v)
     *   - terminar al llegar a V-1 aristas
     *
     * Complejidad esperada: O(E log E).
     *
     * Para el coloquio: saber justificar en que caso conviene Prim y en cual
     * Kruskal, en funcion de la densidad del grafo.
     *
     * Las aristas se ordenan con el MergeSort propio de Ordenamiento:
     * O(E log E). Despues cada arista hace dos find y a lo sumo un union,
     * casi O(1) cada uno con compresion de caminos y union por rango.
     */
    public Mst kruskal(Grafo grafo) {
        validarNoDirigido(grafo);
        int n = grafo.cantidadVertices();

        // en no dirigido cada arista esta dos veces (u->v y v->u): se toma solo u < v
        List<Candidata> todas = new ArrayList<>();
        for (int u = 0; u < n; u++) {
            for (Grafo.Arista a : grafo.vecinos(u)) {
                if (u < a.destino()) {
                    todas.add(new Candidata(u, a.destino(), a.costo()));
                }
            }
        }
        List<Candidata> ordenadas = ordenamiento.mergeSort(todas, Comparator.comparingDouble(Candidata::costo));

        UnionFind conjuntos = new UnionFind(n);
        List<AristaMst> aristas = new ArrayList<>();
        double costoTotal = 0;
        for (Candidata c : ordenadas) {
            if (aristas.size() == n - 1) {
                break;
            }
            if (conjuntos.find(c.desde()) == conjuntos.find(c.hasta())) {
                continue;               // ya estan conectados: formaria ciclo
            }
            conjuntos.union(c.desde(), c.hasta());
            aristas.add(new AristaMst(grafo.idDe(c.desde()), grafo.idDe(c.hasta()), c.costo()));
            costoTotal += c.costo();
        }

        validarConexo(grafo, aristas);
        return new Mst(aristas, costoTotal);
    }

    private void validarNoDirigido(Grafo grafo) {
        if (grafo.esDirigido()) {
            throw new IllegalStateException("El arbol de recubrimiento minimo requiere un grafo NO dirigido");
        }
    }

    /** Un arbol que cubre los V vertices tiene exactamente V-1 aristas: si hay menos, no es conexo. */
    private void validarConexo(Grafo grafo, List<AristaMst> aristas) {
        int esperadas = grafo.cantidadVertices() - 1;
        if (aristas.size() < esperadas) {
            throw new IllegalStateException("El grafo no es conexo: el arbol solo llego a "
                    + aristas.size() + " de las " + esperadas + " aristas necesarias");
        }
    }

    /**
     * Union-Find (conjuntos disjuntos) sobre los indices 0..n-1.
     * Cada conjunto es un arbol; su raiz lo representa.
     */
    private static class UnionFind {
        private final int[] padre;
        private final int[] rango;

        UnionFind(int n) {
            padre = new int[n];
            rango = new int[n];
            for (int i = 0; i < n; i++) {
                padre[i] = i;           // al principio cada vertice es su propio conjunto
            }
        }

        /** Raiz del conjunto de x. Compresion de caminos: cuelga x directo de la raiz. */
        int find(int x) {
            if (padre[x] != x) {
                padre[x] = find(padre[x]);
            }
            return padre[x];
        }

        /** Une los conjuntos de a y b. Union por rango: el arbol mas bajo cuelga del mas alto. */
        void union(int a, int b) {
            int ra = find(a);
            int rb = find(b);
            if (ra == rb) {
                return;
            }
            if (rango[ra] < rango[rb]) {
                padre[ra] = rb;
            } else if (rango[ra] > rango[rb]) {
                padre[rb] = ra;
            } else {
                padre[rb] = ra;
                rango[ra]++;
            }
        }
    }
}
