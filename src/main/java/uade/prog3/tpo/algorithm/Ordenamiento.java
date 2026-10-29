package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UNIDAD: Divide y Conquista (clase 2)     PUNTAJE: 1 punto
 *
 * IMPORTANTE: no vale llamar a Collections.sort ni a Arrays.sort.
 * El punto se asigna por implementar el algoritmo, no por usarlo.
 */
@Component
public class Ordenamiento {

    /**
     * QuickSort sobre una lista de items.
     *
     * Requisitos:
     *   - implementar la particion (partition) a mano
     *   - documentar que estrategia de pivote usaron y por que
     *   - NO modificar la lista de entrada: devolver una lista nueva
     *
     * Estrategia de pivote: el elemento del medio del rango [lo, hi].
     * Se elige el medio (en vez del primero o el ultimo) porque en el
     * peor caso de esas estrategias -una lista ya ordenada o casi
     * ordenada- cada particion queda totalmente desbalanceada (0 y n-1
     * elementos), degradando el algoritmo a O(n^2). Con el pivote del
     * medio ese escenario particular deja de ser el peor caso, aunque
     * siempre existe alguna permutacion adversaria que lo lleva a O(n^2).
     *
     * Complejidad esperada: O(n log n) promedio, O(n^2) peor caso.
     * Hay que poder explicar cuando ocurre el peor caso.
     */
    public List<Item> quickSort(List<Item> items, Comparator<Item> criterio) {
        List<Item> copia = new ArrayList<>(items);
        quickSort(copia, 0, copia.size() - 1, criterio);
        return copia;
    }

    private void quickSort(List<Item> lista, int lo, int hi, Comparator<Item> criterio) {
        if (lo >= hi) {
            return;
        }
        int p = partition(lista, lo, hi, criterio);
        quickSort(lista, lo, p - 1, criterio);
        quickSort(lista, p + 1, hi, criterio);
    }

    /** Particion estilo Lomuto usando el elemento del medio como pivote. */
    private int partition(List<Item> lista, int lo, int hi, Comparator<Item> criterio) {
        int medio = lo + (hi - lo) / 2;
        swap(lista, medio, hi);
        Item pivote = lista.get(hi);

        int i = lo - 1;
        for (int j = lo; j < hi; j++) {
            if (criterio.compare(lista.get(j), pivote) <= 0) {
                i++;
                swap(lista, i, j);
            }
        }
        swap(lista, i + 1, hi);
        return i + 1;
    }

    private void swap(List<Item> lista, int i, int j) {
        Item tmp = lista.get(i);
        lista.set(i, lista.get(j));
        lista.set(j, tmp);
    }

    /**
     * MergeSort sobre una lista de items.
     *
     * Requisitos:
     *   - implementar la mezcla (merge) a mano
     *   - respetar la estabilidad del algoritmo
     *
     * Complejidad esperada: O(n log n) siempre.
     *
     * Para el coloquio: recurrencia T(n) = 2T(n/2) + O(n). Con la regla
     * practica (a=2, b=2, k=1): a = b^k (2 = 2^1), por lo tanto
     * T(n) = O(n^k log n) = O(n log n).
     *
     * Es generico para poder reutilizarlo fuera de los items: Kruskal lo usa
     * para ordenar las aristas por costo.
     */
    public <T> List<T> mergeSort(List<T> items, Comparator<? super T> criterio) {
        if (items.size() <= 1) {
            return new ArrayList<>(items);
        }
        int medio = items.size() / 2;
        List<T> izquierda = mergeSort(items.subList(0, medio), criterio);
        List<T> derecha = mergeSort(items.subList(medio, items.size()), criterio);
        return merge(izquierda, derecha, criterio);
    }

    private <T> List<T> merge(List<T> izquierda, List<T> derecha, Comparator<? super T> criterio) {
        List<T> resultado = new ArrayList<>(izquierda.size() + derecha.size());
        int i = 0;
        int j = 0;
        while (i < izquierda.size() && j < derecha.size()) {
            // <= preserva la estabilidad: ante empate, prioriza el elemento
            // que ya venia primero (izquierda) sobre el de la derecha.
            if (criterio.compare(izquierda.get(i), derecha.get(j)) <= 0) {
                resultado.add(izquierda.get(i++));
            } else {
                resultado.add(derecha.get(j++));
            }
        }
        while (i < izquierda.size()) {
            resultado.add(izquierda.get(i++));
        }
        while (j < derecha.size()) {
            resultado.add(derecha.get(j++));
        }
        return resultado;
    }
}
