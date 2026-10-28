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
                new Item("I1", "Item 1", 5, 10, "A"),
                new Item("I2", "Item 2", 4, 40, "B"),
                new Item("I3", "Item 3", 6, 30, "C"),
                new Item("I4", "Item 4", 3, 50, "D"),
                new Item("I5", "Item 5", 7, 55, "E")
        );
    }

    private List<String> ids(List<Item> items) {
        return items.stream().map(Item::getId).collect(Collectors.toList());
    }

    @Test
    void quickSortOrdenaPorValorAscendente() {
        List<Item> items = itemsDeEjemplo();
        List<Item> ordenado = ordenamiento.quickSort(items, Comparator.comparingDouble(Item::getValor));

        // valores: I1=10, I3=30, I2=40, I4=50, I5=55
        assertEquals(List.of("I1", "I3", "I2", "I4", "I5"), ids(ordenado));
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

        // pesos: I4=3, I2=4, I1=5, I3=6, I5=7
        assertEquals(List.of("I4", "I2", "I1", "I3", "I5"), ids(ordenado));
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
