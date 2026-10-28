package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.model.Item;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de QuickSort y MergeSort implementados a mano en Ordenamiento.
 *
 * Usan los mismos items que carga CargaInicial (el grafo semilla), para
 * que el resultado se pueda verificar a mano contra los datos reales
 * de la aplicacion.
 */
class OrdenamientoTest {

    private final Ordenamiento ordenamiento = new Ordenamiento();

    /** Los mismos items que CargaInicial guarda en el grafo semilla. */
    private List<Item> itemsDeEjemplo() {
        return List.of(
                new Item("P1", "Paquete 1", 30, 9000, "BEL"),
                new Item("P2", "Paquete 2", 25, 7000, "PAL"),
                new Item("P3", "Paquete 3", 25, 7000, "REC"),
                new Item("P4", "Paquete 4", 15, 3600, "CAB"),
                new Item("P5", "Paquete 5", 10, 2000, "FLO"),
                new Item("P6", "Paquete 6", 5, 800, "VUR")
        );
    }

    private List<Double> valores(List<Item> items) {
        return items.stream().map(Item::getValor).collect(Collectors.toList());
    }

    private List<String> ids(List<Item> items) {
        return items.stream().map(Item::getId).collect(Collectors.toList());
    }

    @Test
    void quickSortOrdenaPorValorAscendente() {
        List<Item> items = itemsDeEjemplo();
        List<Item> ordenado = ordenamiento.quickSort(items, Comparator.comparingDouble(Item::getValor));

        // valores: P6=800, P5=2000, P4=3600, P2=P3=7000, P1=9000.
        // QuickSort no es estable, asi que el orden entre P2 y P3 (empate)
        // no esta garantizado: se comparan los valores, no los ids.
        assertEquals(List.of(800.0, 2000.0, 3600.0, 7000.0, 7000.0, 9000.0), valores(ordenado));
    }

    @Test
    void quickSortNoModificaLaListaDeEntrada() {
        List<Item> items = itemsDeEjemplo();
        List<String> idsOriginales = ids(items);

        ordenamiento.quickSort(items, Comparator.comparingDouble(Item::getValor));

        assertEquals(idsOriginales, ids(items), "la lista original no debe cambiar de orden");
    }

    @Test
    void mergeSortOrdenaPorPesoAscendente() {
        List<Item> items = itemsDeEjemplo();
        List<Item> ordenado = ordenamiento.mergeSort(items, Comparator.comparingDouble(Item::getPeso));

        // pesos: P6=5, P5=10, P4=15, P2=P3=25, P1=30.
        // MergeSort es estable: P2 sigue antes que P3 en el empate.
        assertEquals(List.of("P6", "P5", "P4", "P2", "P3", "P1"), ids(ordenado));
    }

    @Test
    void mergeSortNoModificaLaListaDeEntrada() {
        List<Item> items = itemsDeEjemplo();
        List<String> idsOriginales = ids(items);

        ordenamiento.mergeSort(items, Comparator.comparingDouble(Item::getPeso));

        assertEquals(idsOriginales, ids(items), "la lista original no debe cambiar de orden");
    }

    @Test
    void mergeSortEsEstableAnteEmpates() {
        // Dos items con el mismo valor: el que ya estaba primero debe
        // seguir apareciendo primero despues de ordenar por valor.
        List<Item> items = List.of(
                new Item("X1", "Empate 1", 1, 20, null),
                new Item("X2", "Empate 2", 2, 20, null),
                new Item("X3", "Menor", 3, 5, null)
        );

        List<Item> ordenado = ordenamiento.mergeSort(items, Comparator.comparingDouble(Item::getValor));

        assertEquals(List.of("X3", "X1", "X2"), ids(ordenado));
    }
}
