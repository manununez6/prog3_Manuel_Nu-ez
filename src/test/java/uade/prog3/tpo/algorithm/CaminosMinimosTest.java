package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.service.Grafo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Dijkstra sobre la red logistica de CargaInicial.
 *
 * Los tres primeros casos son justamente aquellos donde el camino mas corto
 * NO es el directo (o no hay directo): si la relajacion estuviera mal,
 * Dijkstra devolveria 6, 10 o un camino mas largo.
 */
class CaminosMinimosTest {

    private final CaminosMinimos caminosMinimos = new CaminosMinimos();

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

    @Test
    void depositoAMicrocentroPasaPorSanTelmo() {
        // directo DEP-MIC = 6, por San Telmo = 3 + 2 = 5
        CaminosMinimos.Camino c = caminosMinimos.dijkstra(redLogistica(), "DEP", "MIC");
        assertEquals(5.0, c.costoTotal());
        assertEquals(List.of("DEP", "SNT", "MIC"), c.vertices());
    }

    @Test
    void palermoAVillaUrquizaPasaPorBelgrano() {
        // directo PAL-VUR = 10, por Belgrano = 5 + 4 = 9
        CaminosMinimos.Camino c = caminosMinimos.dijkstra(redLogistica(), "PAL", "VUR");
        assertEquals(9.0, c.costoTotal());
        assertEquals(List.of("PAL", "BEL", "VUR"), c.vertices());
    }

    @Test
    void depositoAVillaUrquizaPasaPorFlores() {
        // 6 + 4 + 9 = 19; por Palermo y Belgrano seria 21 o mas
        CaminosMinimos.Camino c = caminosMinimos.dijkstra(redLogistica(), "DEP", "VUR");
        assertEquals(19.0, c.costoTotal());
        assertEquals(List.of("DEP", "CAB", "FLO", "VUR"), c.vertices());
    }

    @Test
    void origenIgualADestinoCuestaCero() {
        CaminosMinimos.Camino c = caminosMinimos.dijkstra(redLogistica(), "PAL", "PAL");
        assertEquals(0.0, c.costoTotal());
        assertEquals(List.of("PAL"), c.vertices());
    }

    @Test
    void aristaNegativaFallaConMensajeClaro() {
        Grafo g = new Grafo(true);
        g.agregarArista("A", "B", 4);
        g.agregarArista("A", "C", 5);
        g.agregarArista("C", "B", -3);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> caminosMinimos.dijkstra(g, "A", "B"));
        assertTrue(e.getMessage().contains("negativ"), e.getMessage());
        assertTrue(e.getMessage().contains("C->B"), e.getMessage());
    }

    @Test
    void verticesNoConectadosFallanConMensajeClaro() {
        Grafo g = new Grafo(false);
        g.agregarArista("A", "B", 1);
        g.agregarArista("C", "D", 1);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> caminosMinimos.dijkstra(g, "A", "D"));
        assertTrue(e.getMessage().contains("no estan conectados"), e.getMessage());
    }

    @Test
    void idInexistenteFalla() {
        assertThrows(IllegalArgumentException.class,
                () -> caminosMinimos.dijkstra(redLogistica(), "DEP", "ZZZ"));
    }
}
