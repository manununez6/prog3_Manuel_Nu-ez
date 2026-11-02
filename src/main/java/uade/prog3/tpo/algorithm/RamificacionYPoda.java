package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UNIDAD: Branch and Bound (clase 12)       PUNTAJE: 1 punto
 *
 * Es backtracking mas una cota. La diferencia con la clase Backtracking
 * es el criterio de poda: alli se podaba por VIABILIDAD, aca se poda por
 * OPTIMALIDAD.
 */
@Component
public class RamificacionYPoda {

    /** Asignacion de items a contenedores o responsables. */
    public record Asignacion(Map<String, List<Item>> porContenedor, double cargaMaxima,
                             long nodosExplorados, long nodosPodados) { }

    /**
     * Repartir items entre k contenedores identicos minimizando la carga del
     * contenedor mas cargado (problema de makespan).
     *
     * Requisitos:
     *   - ramificar: cada item puede ir a cualquiera de los k contenedores
     *   - acotar: definir una cota INFERIOR del makespan alcanzable desde el
     *     nodo actual. Una cota valida y simple es
     *         max( carga maxima actual , suma de todos los pesos / k )
     *   - podar: si la cota es mayor o igual al mejor makespan ya encontrado,
     *     abandonar la rama
     *   - la cota tiene que ser OPTIMISTA. Si subestima el potencial de una
     *     rama se pierden soluciones validas y el resultado deja de ser optimo
     *
     * Complejidad esperada: exponencial, k^n en el peor caso, mucho menor con
     * buena poda.
     *
     * Sugerencia: reportar en la respuesta cuantos nodos se exploraron y
     * cuantos se podaron. Es la mejor evidencia de que la poda funciona.
     *
     * En este dominio: repartir los paquetes entre k camionetas minimizando
     * los kg de la mas cargada.
     *
     * La cota es optimista porque nunca supera el mejor makespan real de la rama:
     *   - las cargas solo crecen, asi que el final no baja de la carga maxima actual
     *   - el maximo de k cargas nunca es menor que su promedio, y el promedio
     *     final es exactamente suma de pesos / k
     * Si cota >= mejor, ninguna solucion de la rama puede mejorar a la que ya
     * se tiene, asi que podarla no pierde el optimo. Por eso tambien se rechazan
     * pesos negativos: con ellos una carga podria bajar y la cota dejaria de valer.
     */
    public Asignacion repartir(List<Item> items, int cantidadContenedores) {
        if (cantidadContenedores < 1) {
            throw new IllegalArgumentException("La cantidad de contenedores debe ser al menos 1");
        }
        double sumaPesos = 0;
        for (Item item : items) {
            if (item.getPeso() < 0) {
                throw new IllegalArgumentException("El paquete " + item.getId()
                        + " tiene peso negativo: " + item.getPeso());
            }
            sumaPesos += item.getPeso();
        }

        Busqueda b = new Busqueda(items, cantidadContenedores, sumaPesos / cantidadContenedores);
        ramificar(b, 0, 0);

        Map<String, List<Item>> porContenedor = new LinkedHashMap<>();
        for (int c = 0; c < cantidadContenedores; c++) {
            porContenedor.put("Camioneta " + (c + 1), new ArrayList<>());
        }
        for (int i = 0; i < items.size(); i++) {
            porContenedor.get("Camioneta " + (b.mejorAsignacion[i] + 1)).add(items.get(i));
        }
        return new Asignacion(porContenedor, b.mejorMakespan, b.nodosExplorados, b.nodosPodados);
    }

    /** Decide a que contenedor va el item i. Los items 0..i-1 ya estan ubicados. */
    private void ramificar(Busqueda b, int i, double cargaMaximaActual) {
        b.nodosExplorados++;

        if (i == b.items.size()) {
            // solucion completa: si llego aca es porque mejora a la anterior
            b.mejorMakespan = cargaMaximaActual;
            b.mejorAsignacion = b.asignacion.clone();
            return;
        }

        double peso = b.items.get(i).getPeso();
        for (int c = 0; c < b.carga.length; c++) {
            double cargaAnterior = b.carga[c];
            double nuevaMaxima = Math.max(cargaMaximaActual, cargaAnterior + peso);
            double cota = Math.max(nuevaMaxima, b.promedio);

            if (cota >= b.mejorMakespan) {
                b.nodosPodados++;       // PODA por optimalidad: esta rama no puede mejorar
                continue;
            }
            b.carga[c] = cargaAnterior + peso;
            b.asignacion[i] = c;
            ramificar(b, i + 1, nuevaMaxima);
            b.carga[c] = cargaAnterior;     // deshacer
        }
    }

    /** Estado de una busqueda: lo que la recursion comparte entre llamadas. */
    private static class Busqueda {
        final List<Item> items;
        final double promedio;
        final double[] carga;
        final int[] asignacion;
        int[] mejorAsignacion;
        double mejorMakespan = Double.POSITIVE_INFINITY;
        long nodosExplorados = 0;
        long nodosPodados = 0;

        Busqueda(List<Item> items, int contenedores, double promedio) {
            this.items = items;
            this.promedio = promedio;
            this.carga = new double[contenedores];
            this.asignacion = new int[items.size()];
        }
    }
}
