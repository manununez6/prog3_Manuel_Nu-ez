package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Branch and Bound con los paquetes de CargaInicial.
 *
 * El makespan optimo no depende del orden de los paquetes, pero la cantidad
 * de nodos si: los conteos valen para el orden en que estan escritos aca.
 */
class RamificacionYPodaTest {

    private final RamificacionYPoda bnb = new RamificacionYPoda();

    /** Los mismos paquetes que CargaInicial: suman 110 kg. */
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

    private List<Item> conPesos(double... pesos) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < pesos.length; i++) {
            items.add(new Item("I" + i, "Item " + i, pesos[i], 1, null));
        }
        return items;
    }

    /** Fuerza bruta sin ninguna poda: prueba las k^n asignaciones. */
    private double makespanPorFuerzaBruta(List<Item> items, double[] carga, int i) {
        if (i == items.size()) {
            double maximo = 0;
            for (double c : carga) {
                maximo = Math.max(maximo, c);
            }
            return maximo;
        }
        double mejor = Double.POSITIVE_INFINITY;
        for (int c = 0; c < carga.length; c++) {
            carga[c] += items.get(i).getPeso();
            mejor = Math.min(mejor, makespanPorFuerzaBruta(items, carga, i + 1));
            carga[c] -= items.get(i).getPeso();
        }
        return mejor;
    }

    private void assertAsignacionConsistente(RamificacionYPoda.Asignacion a, List<Item> items, int k) {
        assertEquals(k, a.porContenedor().size());
        List<String> ubicados = new ArrayList<>();
        double maximo = 0;
        for (List<Item> contenido : a.porContenedor().values()) {
            double carga = 0;
            for (Item item : contenido) {
                ubicados.add(item.getId());
                carga += item.getPeso();
            }
            maximo = Math.max(maximo, carga);
        }
        assertEquals(items.size(), ubicados.size(), "cada paquete va exactamente una vez");
        assertEquals(items.stream().map(Item::getId).sorted().toList(), ubicados.stream().sorted().toList());
        assertEquals(a.cargaMaxima(), maximo, "cargaMaxima debe coincidir con el reparto devuelto");
    }

    @Test
    void conDosCamionetasLaCargaMaximaEs55() {
        // 55 = 110 / 2: es el reparto perfecto, no se puede bajar
        RamificacionYPoda.Asignacion a = bnb.repartir(paquetes(), 2);
        assertEquals(55.0, a.cargaMaxima());
        assertAsignacionConsistente(a, paquetes(), 2);
    }

    @Test
    void conTresCamionetasLaCargaMaximaEs40() {
        // 110 / 3 = 36.67, y con pesos multiplos de 5 no alcanza 35: 3 x 35 = 105 < 110
        RamificacionYPoda.Asignacion a = bnb.repartir(paquetes(), 3);
        assertEquals(40.0, a.cargaMaxima());
        assertAsignacionConsistente(a, paquetes(), 3);
    }

    @Test
    void laPodaNoPierdeElOptimo() {
        List<Item> reves = new ArrayList<>(paquetes());
        Collections.reverse(reves);
        List<List<Item>> juegos = List.of(paquetes(), reves, conPesos(7, 7, 6, 6, 5, 4, 4), conPesos(9, 1, 1, 1, 1));

        for (List<Item> items : juegos) {
            for (int k = 1; k <= 4; k++) {
                RamificacionYPoda.Asignacion a = bnb.repartir(items, k);
                assertEquals(makespanPorFuerzaBruta(items, new double[k], 0), a.cargaMaxima(), items + " k=" + k);
                assertAsignacionConsistente(a, items, k);
            }
        }
    }

    @Test
    void cuentaNodosExploradosYPodados() {
        // {k, explorados, podados, nodos del arbol completo}
        long[][] casos = {
                {2, 28, 5, 127},
                {3, 52, 60, 1093},
                {4, 45, 68, 5461},
        };
        for (long[] c : casos) {
            RamificacionYPoda.Asignacion a = bnb.repartir(paquetes(), (int) c[0]);
            assertEquals(c[1], a.nodosExplorados(), "k=" + c[0]);
            assertEquals(c[2], a.nodosPodados(), "k=" + c[0]);
            assertTrue(a.nodosExplorados() < c[3], "debe explorar menos que el arbol completo");
        }
    }

    @Test
    void ejemploChicoCalculadoAMano() {
        // pesos 5, 4, 3 con k=2: promedio 6, optimo {5} y {4, 3} = 7
        RamificacionYPoda.Asignacion a = bnb.repartir(conPesos(5, 4, 3), 2);
        assertEquals(7.0, a.cargaMaxima());
        assertEquals(10, a.nodosExplorados());
        assertEquals(3, a.nodosPodados());
    }

    @Test
    void unaCamionetaLlevaTodo() {
        RamificacionYPoda.Asignacion a = bnb.repartir(paquetes(), 1);
        assertEquals(110.0, a.cargaMaxima());
        assertEquals(0, a.nodosPodados(), "hay una sola rama");
    }

    @Test
    void conMasCamionetasQuePaquetesMandaElMasPesado() {
        RamificacionYPoda.Asignacion a = bnb.repartir(paquetes(), 7);
        assertEquals(30.0, a.cargaMaxima());
        assertAsignacionConsistente(a, paquetes(), 7);
    }

    @Test
    void sinPaquetesLaCargaEsCero() {
        RamificacionYPoda.Asignacion a = bnb.repartir(List.of(), 3);
        assertEquals(0.0, a.cargaMaxima());
        assertEquals(3, a.porContenedor().size());
    }

    @Test
    void parametrosInvalidosFallan() {
        assertThrows(IllegalArgumentException.class, () -> bnb.repartir(paquetes(), 0));
        assertThrows(IllegalArgumentException.class, () -> bnb.repartir(conPesos(3, -1), 2));
    }
}
