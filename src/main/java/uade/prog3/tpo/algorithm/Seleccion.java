package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
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
     *
     * Misma funcion objetivo que greedy: maximizar la ganancia total sin
     * superar la capacidad en kg, con cada paquete entero o nada.
     *
     * dp[i][j] = mejor ganancia usando solo los primeros i paquetes con una
     * capacidad de j kg. Para el paquete i (peso w, valor v):
     *   - si no entra (w > j):  dp[i][j] = dp[i-1][j]
     *   - si entra:             dp[i][j] = max(dp[i-1][j], dp[i-1][j-w] + v)
     *                                          no tomarlo   tomarlo
     * La respuesta es dp[n][W]. Hay (n+1)*(W+1) celdas y cada una es O(1).
     */
    public Resultado programacionDinamica(List<Item> items, int capacidad) {
        if (capacidad < 0) {
            throw new IllegalArgumentException("La capacidad no puede ser negativa: " + capacidad);
        }
        int n = items.size();
        int[] pesos = new int[n];
        for (int i = 0; i < n; i++) {
            pesos[i] = pesoEntero(items.get(i));
        }

        // fila 0 (ningun paquete) y columna 0 (capacidad 0) quedan en 0
        double[][] dp = new double[n + 1][capacidad + 1];
        for (int i = 1; i <= n; i++) {
            int w = pesos[i - 1];
            double v = items.get(i - 1).getValor();
            for (int j = 0; j <= capacidad; j++) {
                dp[i][j] = dp[i - 1][j];                            // no tomar el paquete i
                if (w <= j && dp[i - 1][j - w] + v > dp[i][j]) {
                    dp[i][j] = dp[i - 1][j - w] + v;                // tomarlo
                }
            }
        }

        // Recuperar el camino: si dp[i][j] cambio respecto de la fila de arriba,
        // el paquete i se tomo y hay que seguir desde la capacidad que le quedaba.
        Deque<Item> elegidos = new ArrayDeque<>();
        double pesoTotal = 0;
        int j = capacidad;
        for (int i = n; i >= 1; i--) {
            if (dp[i][j] != dp[i - 1][j]) {
                Item item = items.get(i - 1);
                elegidos.push(item);    // push agrega adelante: quedan en el orden de entrada
                pesoTotal += item.getPeso();
                j -= pesos[i - 1];
            }
        }
        return new Resultado(new ArrayList<>(elegidos), dp[n][capacidad], pesoTotal,
                "programacion dinamica (mochila 0/1)");
    }

    /** La tabla se indexa por kg: el peso tiene que ser un entero no negativo. */
    private int pesoEntero(Item item) {
        double peso = item.getPeso();
        if (peso < 0 || peso != Math.floor(peso)) {
            throw new IllegalArgumentException("El paquete " + item.getId()
                    + " tiene peso " + peso + ": la mochila 0/1 con PD requiere pesos enteros no negativos");
        }
        return (int) peso;
    }
}
