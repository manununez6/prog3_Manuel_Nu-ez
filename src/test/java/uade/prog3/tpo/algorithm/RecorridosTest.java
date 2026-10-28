package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.service.Grafo;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de DFS y BFS implementados en Recorridos.
 *
 * El orden de visita depende del orden de la lista de adyacencia, que es
 * el orden en que se agregan las aristas. Por eso los grafos se arman a
 * mano y los ordenes esperados se pueden seguir con lapiz y papel.
 */
class RecorridosTest {

    private final Recorridos recorridos = new Recorridos();

    /**
     * Grafo chico NO dirigido, con un ciclo A-B-D-C-A y un vertice F aislado:
     *
     *     A --- B
     *     |     |
     *     C --- D --- E        F
     *
     * Vecinos: A=[B,C]  B=[A,D]  C=[A,D]  D=[B,C,E]  E=[D]  F=[]
     */
    private Grafo grafoChico() {
        Grafo g = new Grafo(false);
        g.agregarArista("A", "B", 1);
        g.agregarArista("A", "C", 1);
        g.agregarArista("B", "D", 1);
        g.agregarArista("C", "D", 1);
        g.agregarArista("D", "E", 1);
        g.agregarVertice("F");
        return g;
    }

    /** La red de CargaInicial, con las rutas en el mismo orden en que se guardan. */
    private Grafo redLogistica() {
        Grafo g = new Grafo(false);
        g.agregarArista("DEP", "SNT", 3);
        g.agregarArista("DEP", "MIC", 6);
        g.agregarArista("DEP", "CAB", 6);
        g.agregarArista("SNT", "MIC", 2);
        g.agregarArista("MIC", "REC", 3);
        g.agregarArista("MIC", "ALM", 5);
        g.agregarArista("REC", "PAL", 4);
        g.agregarArista("REC", "ALM", 4);
        g.agregarArista("PAL", "ALM", 4);
        g.agregarArista("PAL", "BEL", 5);
        g.agregarArista("PAL", "VUR", 10);
        g.agregarArista("BEL", "VUR", 4);
        g.agregarArista("ALM", "CAB", 3);
        g.agregarArista("CAB", "PAL", 6);
        g.agregarArista("CAB", "FLO", 4);
        g.agregarArista("FLO", "VUR", 9);
        return g;
    }

    private void assertSinRepetidos(List<String> orden) {
        assertEquals(orden.size(), new HashSet<>(orden).size(), "hay vertices repetidos: " + orden);
    }

    @Test
    void dfsVaEnProfundidadAntesDeVolver() {
        // A -> B -> D (B ya visitado) -> C (A y D visitados), vuelve a D -> E
        List<String> orden = recorridos.dfs(grafoChico(), "A");
        assertEquals(List.of("A", "B", "D", "C", "E"), orden);
        assertSinRepetidos(orden);
    }

    @Test
    void bfsVaPorNiveles() {
        // nivel 0: A   nivel 1: B, C   nivel 2: D   nivel 3: E
        List<String> orden = recorridos.bfs(grafoChico(), "A");
        assertEquals(List.of("A", "B", "C", "D", "E"), orden);
        assertSinRepetidos(orden);
    }

    @Test
    void noVisitanVerticesInalcanzables() {
        assertFalse(recorridos.dfs(grafoChico(), "A").contains("F"));
        assertFalse(recorridos.bfs(grafoChico(), "A").contains("F"));
        assertEquals(List.of("F"), recorridos.dfs(grafoChico(), "F"));
        assertEquals(List.of("F"), recorridos.bfs(grafoChico(), "F"));
    }

    @Test
    void enDirigidoSoloSiguenElSentidoDeLasAristas() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 1);
        g.agregarArista("C", "A", 1);
        assertEquals(List.of("A", "B"), recorridos.dfs(g, "A"));
        assertEquals(List.of("A", "B"), recorridos.bfs(g, "A"));
    }

    @Test
    void recorridosSobreLaRedLogisticaDesdeElDeposito() {
        List<String> dfs = recorridos.dfs(redLogistica(), "DEP");
        List<String> bfs = recorridos.bfs(redLogistica(), "DEP");

        assertEquals(List.of("DEP", "SNT", "MIC", "REC", "PAL", "ALM", "CAB", "FLO", "VUR", "BEL"), dfs);
        assertEquals(List.of("DEP", "SNT", "MIC", "CAB", "REC", "ALM", "PAL", "FLO", "BEL", "VUR"), bfs);
        assertSinRepetidos(dfs);
        assertSinRepetidos(bfs);
        assertEquals(10, dfs.size(), "el grafo es conexo: DFS debe llegar a todos");
        assertEquals(10, bfs.size(), "el grafo es conexo: BFS debe llegar a todos");
    }

    @Test
    void origenInexistenteFalla() {
        assertThrows(IllegalArgumentException.class, () -> recorridos.dfs(grafoChico(), "Z"));
        assertThrows(IllegalArgumentException.class, () -> recorridos.bfs(grafoChico(), "Z"));
    }
}
