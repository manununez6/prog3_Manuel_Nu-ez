package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.service.Grafo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del backtracking de rutas simples sobre la red logistica de CargaInicial.
 *
 * El orden de las rutas depende de la lista de adyacencia, asi que se comparan
 * como conjunto. La cantidad de nodos explorados no depende de ese orden.
 */
class BacktrackingTest {

    private final Backtracking backtracking = new Backtracking();

    /** La red de CargaInicial, no dirigida, distancias en km. */
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

    private Set<List<String>> caminos(List<Backtracking.Ruta> rutas) {
        return rutas.stream().map(Backtracking.Ruta::vertices).collect(Collectors.toSet());
    }

    @Test
    void depositoAVillaUrquizaCon22KmY5SaltosDa6Rutas() {
        List<Backtracking.Ruta> rutas = backtracking.rutasSimples(redLogistica(), "DEP", "VUR", 22, 5);

        assertEquals(6, rutas.size());
        assertEquals(Set.of(
                List.of("DEP", "CAB", "FLO", "VUR"),                // 19 km
                List.of("DEP", "CAB", "PAL", "BEL", "VUR"),         // 21 km
                List.of("DEP", "CAB", "PAL", "VUR"),                // 22 km
                List.of("DEP", "SNT", "MIC", "REC", "PAL", "VUR"),  // 22 km
                List.of("DEP", "MIC", "REC", "PAL", "BEL", "VUR"),  // 22 km
                List.of("DEP", "CAB", "ALM", "PAL", "BEL", "VUR")   // 22 km
        ), caminos(rutas));
    }

    @Test
    void cadaRutaEsSimpleYRespetaLosLimites() {
        for (Backtracking.Ruta r : backtracking.rutasSimples(redLogistica(), "DEP", "VUR", 30, 6)) {
            assertEquals(r.vertices().size(), new HashSet<>(r.vertices()).size(), "repite vertices: " + r);
            assertEquals("DEP", r.vertices().get(0));
            assertEquals("VUR", r.vertices().get(r.vertices().size() - 1));
            assertTrue(r.costoTotal() <= 30, r.toString());
            assertTrue(r.vertices().size() - 1 <= 6, r.toString());
        }
    }

    @Test
    void conYSinPodaEncuentranLasMismasRutasYLaPodaExploraMenos() {
        // {costoMaximo, saltosMaximos, rutas, nodos con poda, nodos sin poda}
        double[][] casos = {
                {18, 5, 0, 49, 154},
                {19, 5, 1, 56, 154},
                {22, 5, 6, 77, 154},
                {30, 6, 27, 130, 154},
                {1000, 3, 2, 26, 154},
                {1000, 10, 47, 154, 154},
        };
        for (double[] c : casos) {
            String caso = "costoMaximo=" + c[0] + " saltosMaximos=" + c[1];
            Backtracking.Exploracion con = backtracking.explorar(redLogistica(), "DEP", "VUR", c[0], (int) c[1], true);
            Backtracking.Exploracion sin = backtracking.explorar(redLogistica(), "DEP", "VUR", c[0], (int) c[1], false);

            assertEquals((int) c[2], con.rutas().size(), caso);
            assertEquals(caminos(con.rutas()), caminos(sin.rutas()), caso);
            assertEquals((long) c[3], con.nodosExplorados(), caso);
            assertEquals((long) c[4], sin.nodosExplorados(), caso);
        }
    }

    @Test
    void elCostoMinimoCoincideConDijkstra() {
        // con 19 km sobrevive una sola ruta, la de Dijkstra; con 18 no hay ninguna
        List<Backtracking.Ruta> rutas = backtracking.rutasSimples(redLogistica(), "DEP", "VUR", 19, 5);
        assertEquals(1, rutas.size());
        assertEquals(new CaminosMinimos().dijkstra(redLogistica(), "DEP", "VUR").vertices(), rutas.get(0).vertices());
        assertTrue(backtracking.rutasSimples(redLogistica(), "DEP", "VUR", 18, 5).isEmpty());
    }

    @Test
    void desmarcaAlVolverParaQueOtrasRutasReusenElVertice() {
        // el diamante A-B-D / A-C-D no repite vertices dentro de una ruta,
        // pero D tiene que quedar disponible para la segunda rama
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 1);
        g.agregarArista("A", "C", 1);
        g.agregarArista("B", "D", 1);
        g.agregarArista("C", "D", 1);

        assertEquals(Set.of(List.of("A", "B", "D"), List.of("A", "C", "D")),
                caminos(backtracking.rutasSimples(g, "A", "D", 10, 5)));
    }

    @Test
    void origenIgualADestinoEsLaRutaVacia() {
        List<Backtracking.Ruta> rutas = backtracking.rutasSimples(redLogistica(), "PAL", "PAL", 10, 5);
        assertEquals(1, rutas.size());
        assertEquals(List.of("PAL"), rutas.get(0).vertices());
        assertEquals(0.0, rutas.get(0).costoTotal());
    }

    @Test
    void costoNegativoFalla() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 5);
        g.agregarArista("B", "C", -3);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> backtracking.rutasSimples(g, "A", "C", 3, 5));
        assertTrue(e.getMessage().contains("B->C"), e.getMessage());
    }

    @Test
    void parametrosInvalidosFallan() {
        assertThrows(IllegalArgumentException.class,
                () -> backtracking.rutasSimples(redLogistica(), "DEP", "VUR", -1, 5));
        assertThrows(IllegalArgumentException.class,
                () -> backtracking.rutasSimples(redLogistica(), "DEP", "VUR", 22, -1));
        assertThrows(IllegalArgumentException.class,
                () -> backtracking.rutasSimples(redLogistica(), "DEP", "ZZZ", 22, 5));
    }
}
