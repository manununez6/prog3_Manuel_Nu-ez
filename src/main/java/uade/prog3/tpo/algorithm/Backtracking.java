package uade.prog3.tpo.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayList;
import java.util.List;

/**
 * UNIDAD: Backtracking (clases 9 y 10)      PUNTAJE: 1 punto
 *
 * El problema propuesto es enumerar caminos simples bajo restricciones.
 * Cada grupo puede reemplazarlo por otro de su dominio, siempre que:
 *   - construya la solucion de forma incremental
 *   - tenga un criterio de viabilidad que pode ramas
 *   - deshaga la ultima decision al retroceder
 */
@Component
public class Backtracking {

    private static final Logger log = LoggerFactory.getLogger(Backtracking.class);

    /** Un camino simple encontrado, con su costo acumulado. */
    public record Ruta(List<String> vertices, double costoTotal) { }

    /** Resultado de una busqueda: las rutas validas y cuantos nodos del arbol se visitaron. */
    public record Exploracion(List<Ruta> rutas, long nodosExplorados) { }

    /**
     * Enumerar TODOS los caminos simples de origen a destino que no superen
     * un costo maximo ni una cantidad maxima de saltos.
     *
     * Requisitos:
     *   - camino SIMPLE: no repetir vertices. Marcar y desmarcar el visitado
     *     es literalmente el retroceso del backtracking
     *   - podar en cuanto el costo parcial supere el maximo, sin esperar a
     *     completar el camino
     *   - devolver todas las rutas validas, no la primera
     *
     * Complejidad esperada: exponencial en el peor caso. Hay que poder
     * justificar por que, y explicar cuanto ayuda la poda en la practica.
     *
     * Sugerencia de verificacion: contar los nodos visitados con y sin poda
     * sobre el mismo grafo. Esa comparacion es material de coloquio.
     *
     * Devuelve las rutas de la busqueda con poda y deja en el log la comparacion
     * de nodos explorados. Para eso corre la busqueda dos veces: la corrida sin
     * poda existe solo para esa medicion.
     */
    public List<Ruta> rutasSimples(Grafo grafo, String origenId, String destinoId,
                                   double costoMaximo, int saltosMaximos) {
        Exploracion conPoda = explorar(grafo, origenId, destinoId, costoMaximo, saltosMaximos, true);
        Exploracion sinPoda = explorar(grafo, origenId, destinoId, costoMaximo, saltosMaximos, false);
        log.info("Rutas {}->{} (costoMaximo={}, saltosMaximos={}): {} rutas. Nodos explorados: {} con poda, {} sin poda",
                origenId, destinoId, costoMaximo, saltosMaximos, conPoda.rutas().size(),
                conPoda.nodosExplorados(), sinPoda.nodosExplorados());
        return conPoda.rutas();
    }

    /**
     * Busqueda por backtracking. Con y sin poda encuentra las mismas rutas:
     * sin poda recorre todos los caminos simples y recien controla los limites
     * al llegar al destino.
     *
     * La poda por costo parcial es correcta porque los costos son >= 0: un
     * camino que ya se paso del maximo no puede volver a bajar. Por eso un
     * costo negativo se rechaza.
     */
    public Exploracion explorar(Grafo grafo, String origenId, String destinoId,
                                double costoMaximo, int saltosMaximos, boolean conPoda) {
        int origen = grafo.indiceDe(origenId);
        int destino = grafo.indiceDe(destinoId);
        if (costoMaximo < 0 || saltosMaximos < 0) {
            throw new IllegalArgumentException("costoMaximo y saltosMaximos no pueden ser negativos");
        }
        validarCostosNoNegativos(grafo);

        Busqueda b = new Busqueda(grafo, destino, costoMaximo, saltosMaximos, conPoda);
        b.visitado[origen] = true;
        b.camino.add(grafo.idDe(origen));
        backtrack(b, origen, 0);
        return new Exploracion(b.rutas, b.nodosExplorados);
    }

    private void backtrack(Busqueda b, int u, double costo) {
        b.nodosExplorados++;
        int saltos = b.camino.size() - 1;

        if (u == b.destino) {
            if (costo <= b.costoMaximo && saltos <= b.saltosMaximos) {
                b.rutas.add(new Ruta(new ArrayList<>(b.camino), costo));
            }
            return;
        }

        for (Grafo.Arista a : b.grafo.vecinos(u)) {
            int v = a.destino();
            if (b.visitado[v]) {
                continue;               // camino SIMPLE: no repetir vertices
            }
            double nuevoCosto = costo + a.costo();
            if (b.conPoda && (nuevoCosto > b.costoMaximo || saltos + 1 > b.saltosMaximos)) {
                continue;               // PODA: no se entra a una rama que ya no puede ser valida
            }
            b.visitado[v] = true;       // decidir
            b.camino.add(b.grafo.idDe(v));
            backtrack(b, v, nuevoCosto);
            b.camino.remove(b.camino.size() - 1);   // deshacer: este es el retroceso
            b.visitado[v] = false;
        }
    }

    private void validarCostosNoNegativos(Grafo grafo) {
        for (int u = 0; u < grafo.cantidadVertices(); u++) {
            for (Grafo.Arista a : grafo.vecinos(u)) {
                if (a.costo() < 0) {
                    throw new IllegalStateException("Backtracking no admite costos negativos: la arista "
                            + grafo.idDe(u) + "->" + grafo.idDe(a.destino()) + " cuesta " + a.costo());
                }
            }
        }
    }

    /** Estado de una busqueda: lo que la recursion comparte entre llamadas. */
    private static class Busqueda {
        final Grafo grafo;
        final int destino;
        final double costoMaximo;
        final int saltosMaximos;
        final boolean conPoda;
        final boolean[] visitado;
        final List<String> camino = new ArrayList<>();
        final List<Ruta> rutas = new ArrayList<>();
        long nodosExplorados = 0;

        Busqueda(Grafo grafo, int destino, double costoMaximo, int saltosMaximos, boolean conPoda) {
            this.grafo = grafo;
            this.destino = destino;
            this.costoMaximo = costoMaximo;
            this.saltosMaximos = saltosMaximos;
            this.conPoda = conPoda;
            this.visitado = new boolean[grafo.cantidadVertices()];
        }
    }
}
