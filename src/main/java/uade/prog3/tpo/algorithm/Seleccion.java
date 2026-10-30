package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UNIDAD: Greedy (clase 3) y Programacion Dinamica (clase 6)
 * PUNTAJE: 1 punto greedy + 1 punto programacion dinamica
 *
 * Los dos metodos resuelven EL MISMO problema con tecnicas distintas.
 * Esa comparacion es justamente lo que se evalua: no alcanza con que
 * funcionen, hay que poder mostrar un caso donde greedy NO da el optimo.
 */
@Component
public class Seleccion {

    /** Resultado de una seleccion de items bajo restriccion de capacidad. */
    public record Resultado(List<Item> elegidos, double valorTotal, double pesoTotal, String tecnica) { }

    private final Ordenamiento ordenamiento;

    public Seleccion(Ordenamiento ordenamiento) {
        this.ordenamiento = ordenamiento;
    }

    /**
     * GREEDY: seleccionar items maximizando el valor total sin superar la capacidad,
     * ordenando por ratio valor/peso descendente y tomando mientras entren.
     *
     * Requisitos:
     *   - declarar EXPLICITAMENTE cual es la funcion objetivo
     *   - usar el ratio valor/peso como criterio, no el valor a secas
     *   - documentar la complejidad, que esta dominada por el ordenamiento
     *
     * Complejidad esperada: O(n log n).
     *
     * OJO: sobre items indivisibles (mochila 0/1) este algoritmo NO garantiza
     * el optimo. Eso no es un error a corregir: es el punto de la comparacion.
     * En el coloquio hay que traer un juego de datos donde greedy pierda.
     *
     * Funcion objetivo: maximizar la suma de valor (ganancia en $) de los
     * paquetes elegidos, sujeto a que la suma de peso (kg) no supere la
     * capacidad de la camioneta. Cada paquete se toma entero o no se toma.
     *
     * Criterio greedy: ratio valor/peso descendente. Se ordena con el MergeSort
     * propio, O(n log n); al ser estable, los empates de ratio respetan el
     * orden de entrada y el resultado es reproducible. Despues se recorre la
     * lista una vez, O(n): si el paquete entra se toma, si no se saltea y se
     * sigue con el proximo (uno mas liviano puede entrar todavia).
     */
    public Resultado greedy(List<Item> items, double capacidad) {
        if (capacidad < 0) {
            throw new IllegalArgumentException("La capacidad no puede ser negativa: " + capacidad);
        }
        List<Item> porRatio = ordenamiento.mergeSort(items,
                Comparator.comparingDouble(Item::ratioValorPeso).reversed());

        List<Item> elegidos = new ArrayList<>();
        double valorTotal = 0;
        double pesoTotal = 0;
        for (Item item : porRatio) {
            if (pesoTotal + item.getPeso() <= capacidad) {
                elegidos.add(item);
                pesoTotal += item.getPeso();
                valorTotal += item.getValor();
            }
        }
        return new Resultado(elegidos, valorTotal, pesoTotal, "greedy por ratio valor/peso");
    }

    /**
     * PROGRAMACION DINAMICA: mochila 0/1 con tabla dp.
     *
     * Requisitos:
     *   - construir la tabla dp[i][j] de forma iterativa (bottom-up)
     *   - RECUPERAR EL CAMINO: devolver que items componen la solucion optima,
     *     no solo el valor maximo. Eso se hace recorriendo la tabla hacia atras
     *   - los pesos deben ser enteros o discretizarse para indexar la tabla
     *
     * Complejidad esperada: O(n * W) en tiempo y en espacio.
     *
     * Para el coloquio: explicar por que O(n*W) es pseudopolinomica.
     */
    public Resultado programacionDinamica(List<Item> items, int capacidad) {
        throw new PendienteDeImplementar("Mochila 0/1 con programacion dinamica");
    }
}
