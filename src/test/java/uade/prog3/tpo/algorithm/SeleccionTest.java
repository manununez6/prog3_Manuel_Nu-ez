package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.model.Item;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de seleccion bajo restriccion de capacidad, con los paquetes de
 * CargaInicial y la camioneta de 50 kg.
 */
class SeleccionTest {

    private final Seleccion seleccion = new Seleccion(new Ordenamiento());

    /** Los mismos paquetes que CargaInicial (peso en kg, valor en $). */
    private List<Item> paquetes() {
        return List.of(
                new Item("P1", "Paquete 1", 30, 9000, "BEL"),
                new Item("P2", "Paquete 2", 25, 7000, "PAL"),
                new Item("P3", "Paquete 3", 25, 7000, "REC"),
                new Item("P4", "Paquete 4", 15, 3600, "CAB"),
                new Item("P5", "Paquete 5", 10, 2000, "FLO"),
                new Item("P6", "Paquete 6", 5, 800, "VUR")
        );
    }

    private List<String> ids(List<Item> items) {
        return items.stream().map(Item::getId).collect(Collectors.toList());
    }

    @Test
    void greedyConLaCamionetaDe50Kg() {
        // ratios: P1=300, P2=P3=280, P4=240, P5=200, P6=160
        // P1 entra (30), P2 y P3 no (55), P4 entra (45), P5 no (55), P6 entra (50)
        Seleccion.Resultado r = seleccion.greedy(paquetes(), 50);

        assertEquals(List.of("P1", "P4", "P6"), ids(r.elegidos()));
        assertEquals(13400.0, r.valorTotal());
        assertEquals(50.0, r.pesoTotal());
    }

    @Test
    void greedyNoEsOptimoEnMochila01() {
        // P2 + P3 pesan 50 y valen 14000: greedy se queda 600 por debajo
        Seleccion.Resultado r = seleccion.greedy(paquetes(), 50);
        assertTrue(r.valorTotal() < 14000.0);
    }

    @Test
    void sigueBuscandoDespuesDeUnPaqueteQueNoEntra() {
        // con 35 kg: P1 entra (30), P2, P3, P4 y P5 no, P6 si (35)
        Seleccion.Resultado r = seleccion.greedy(paquetes(), 35);
        assertEquals(List.of("P1", "P6"), ids(r.elegidos()));
        assertEquals(9800.0, r.valorTotal());
    }

    @Test
    void capacidadCeroNoEligeNada() {
        Seleccion.Resultado r = seleccion.greedy(paquetes(), 0);
        assertTrue(r.elegidos().isEmpty());
        assertEquals(0.0, r.valorTotal());
    }

    @Test
    void entranTodosSiSobraLugar() {
        // los 6 pesan 110 kg
        Seleccion.Resultado r = seleccion.greedy(paquetes(), 200);
        assertEquals(6, r.elegidos().size());
        assertEquals(110.0, r.pesoTotal());
        assertEquals(29400.0, r.valorTotal());
    }

    @Test
    void capacidadNegativaFalla() {
        assertThrows(IllegalArgumentException.class, () -> seleccion.greedy(paquetes(), -1));
    }

    @Test
    void dinamicaConLaCamionetaDe50Kg() {
        Seleccion.Resultado r = seleccion.programacionDinamica(paquetes(), 50);

        assertEquals(List.of("P2", "P3"), ids(r.elegidos()));
        assertEquals(14000.0, r.valorTotal());
        assertEquals(50.0, r.pesoTotal());
    }

    @Test
    void greedyPierdeContraDinamica() {
        Seleccion.Resultado greedy = seleccion.greedy(paquetes(), 50);
        Seleccion.Resultado dinamica = seleccion.programacionDinamica(paquetes(), 50);

        assertEquals(13400.0, greedy.valorTotal());
        assertEquals(14000.0, dinamica.valorTotal());
        assertEquals(600.0, dinamica.valorTotal() - greedy.valorTotal());
    }

    @Test
    void dinamicaNuncaEsPeorQueGreedy() {
        for (int capacidad = 0; capacidad <= 120; capacidad += 5) {
            double greedy = seleccion.greedy(paquetes(), capacidad).valorTotal();
            Seleccion.Resultado dinamica = seleccion.programacionDinamica(paquetes(), capacidad);
            assertTrue(dinamica.valorTotal() >= greedy, "capacidad " + capacidad);
            assertTrue(dinamica.pesoTotal() <= capacidad, "capacidad " + capacidad);
        }
    }

    @Test
    void elValorDevueltoCoincideConLosElegidos() {
        // la recuperacion del camino tiene que ser consistente con dp[n][W]
        Seleccion.Resultado r = seleccion.programacionDinamica(paquetes(), 40);
        double suma = r.elegidos().stream().mapToDouble(Item::getValor).sum();
        assertEquals(r.valorTotal(), suma);
        assertEquals(List.of("P1", "P5"), ids(r.elegidos()));   // 30 + 10 = 40 kg
        assertEquals(11000.0, r.valorTotal());
    }

    @Test
    void dinamicaRechazaPesosNoEnteros() {
        List<Item> items = List.of(new Item("X", "Fraccionario", 2.5, 100, null));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> seleccion.programacionDinamica(items, 10));
        assertTrue(e.getMessage().contains("enteros"), e.getMessage());
    }

    @Test
    void dinamicaConCapacidadNegativaFalla() {
        assertThrows(IllegalArgumentException.class, () -> seleccion.programacionDinamica(paquetes(), -1));
    }
}
