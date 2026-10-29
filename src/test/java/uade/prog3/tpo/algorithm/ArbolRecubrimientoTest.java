package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.service.Grafo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Prim y Kruskal sobre la red logistica de CargaInicial.
 *
 * El MST puede no ser unico (hay pesos repetidos: REC-ALM y PAL-ALM valen 4),
 * asi que se verifica la cantidad de aristas, el costo total y que cubra
 * todos los vertices, no el conjunto exacto de aristas.
 */
class ArbolRecubrimientoTest {

    private final ArbolRecubrimiento arbol = new ArbolRecubrimiento(new Ordenamiento());

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

    /** Dos componentes: {A, B, C} y {D, E}. */
    private Grafo noConexo() {
        Grafo g = new Grafo(false);
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", 2);
        g.agregarArista("D", "E", 3);
        return g;
    }

    private void assertEsArbolDeLaRed(ArbolRecubrimiento.Mst mst) {
        assertEquals(9, mst.aristas().size(), "V-1 aristas con V=10");
        assertEquals(32.0, mst.costoTotal());

        double suma = 0;
        Set<String> cubiertos = new HashSet<>();
        for (ArbolRecubrimiento.AristaMst a : mst.aristas()) {
            suma += a.costo();
            cubiertos.add(a.origen());
            cubiertos.add(a.destino());
        }
        assertEquals(mst.costoTotal(), suma, "costoTotal debe ser la suma de las aristas");
        assertEquals(10, cubiertos.size(), "el arbol debe tocar los 10 vertices");
    }

    @Test
    void primSobreLaRedLogistica() {
        assertEsArbolDeLaRed(arbol.prim(redLogistica(), "DEP"));
    }

    @Test
    void primDaElMismoCostoDesdeCualquierOrigen() {
        for (String origen : List.of("DEP", "PAL", "VUR", "FLO")) {
            assertEquals(32.0, arbol.prim(redLogistica(), origen).costoTotal(), "origen " + origen);
        }
    }

    @Test
    void kruskalSobreLaRedLogistica() {
        assertEsArbolDeLaRed(arbol.kruskal(redLogistica()));
    }

    @Test
    void kruskalNoUsaLasRutasCaras() {
        // las rutas de 6, 9 y 10 km nunca entran: siempre hay alternativa mas barata
        for (ArbolRecubrimiento.AristaMst a : arbol.kruskal(redLogistica()).aristas()) {
            assertTrue(a.costo() <= 5, "no deberia usar " + a);
        }
    }

    @Test
    void primFallaSiNoEsConexo() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> arbol.prim(noConexo(), "A"));
        assertTrue(e.getMessage().contains("no es conexo"), e.getMessage());
    }

    @Test
    void kruskalFallaSiNoEsConexo() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> arbol.kruskal(noConexo()));
        assertTrue(e.getMessage().contains("no es conexo"), e.getMessage());
    }

    @Test
    void fallanSobreGrafoDirigido() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 1);
        assertThrows(IllegalStateException.class, () -> arbol.prim(g, "A"));
        assertThrows(IllegalStateException.class, () -> arbol.kruskal(g));
    }
}
