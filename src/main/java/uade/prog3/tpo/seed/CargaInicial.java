package uade.prog3.tpo.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.ItemRepository;
import uade.prog3.tpo.repository.NodoRepository;

import java.util.List;

/**
 * Carga la red logistica de CABA para que la aplicacion sea probable
 * apenas se levanta, sin tener que cargar datos a mano.
 *
 * Dominio:
 *   - Nodo   = deposito o punto de entrega (tipo DEPOSITO | ENTREGA)
 *   - Arista = ruta entre dos nodos; costo = distancia en km
 *   - Item   = paquete; peso = kg, valor = ganancia en $
 *
 * Las rutas son de doble mano, asi que el grafo se usa como NO dirigido.
 * Cada ruta se guarda UNA sola vez; la vuelta la arma el Grafo en memoria.
 *
 * El grafo elegido tiene, a proposito:
 *   - 10 vertices y 16 aristas: chico para verificar a mano, grande para
 *     que los algoritmos no den resultados triviales
 *   - todas las distancias positivas, para que Dijkstra sea aplicable
 *   - es conexo, para que Prim y Kruskal tengan solucion
 *   - caminos donde el mas corto NO es el directo, para que se note la
 *     relajacion de aristas
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class CargaInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    private final NodoRepository nodoRepository;
    private final ItemRepository itemRepository;

    public CargaInicial(NodoRepository nodoRepository, ItemRepository itemRepository) {
        this.nodoRepository = nodoRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public void run(String... args) {
        if (nodoRepository.count() > 0) {
            log.info("La base ya tiene datos. No se ejecuta la carga inicial.");
            return;
        }

        Nodo dep = new Nodo("DEP", "Deposito Central (Barracas)", "DEPOSITO", 0);
        Nodo snt = new Nodo("SNT", "San Telmo", "ENTREGA", 0);
        Nodo mic = new Nodo("MIC", "Microcentro", "ENTREGA", 0);
        Nodo rec = new Nodo("REC", "Recoleta", "ENTREGA", 0);
        Nodo pal = new Nodo("PAL", "Palermo", "ENTREGA", 0);
        Nodo bel = new Nodo("BEL", "Belgrano", "ENTREGA", 0);
        Nodo alm = new Nodo("ALM", "Almagro", "ENTREGA", 0);
        Nodo cab = new Nodo("CAB", "Caballito", "ENTREGA", 0);
        Nodo flo = new Nodo("FLO", "Flores", "ENTREGA", 0);
        Nodo vur = new Nodo("VUR", "Villa Urquiza", "ENTREGA", 0);

        // Rutas en km. Casos donde lo directo NO es lo mas corto:
        //   - DEP->MIC directo cuesta 6, pero DEP->SNT->MIC cuesta 3+2=5.
        //   - PAL->VUR directo cuesta 10, pero PAL->BEL->VUR cuesta 5+4=9.
        // Sirven para verificar que la relajacion de aristas este bien hecha.
        dep.conectar(snt, 3);
        dep.conectar(mic, 6);
        dep.conectar(cab, 6);
        snt.conectar(mic, 2);
        mic.conectar(rec, 3);
        mic.conectar(alm, 5);
        rec.conectar(pal, 4);
        rec.conectar(alm, 4);
        pal.conectar(alm, 4);
        pal.conectar(bel, 5);
        pal.conectar(vur, 10);
        bel.conectar(vur, 4);
        alm.conectar(cab, 3);
        cab.conectar(pal, 6);
        cab.conectar(flo, 4);
        flo.conectar(vur, 9);

        nodoRepository.saveAll(List.of(dep, snt, mic, rec, pal, bel, alm, cab, flo, vur));

        // Paquetes para los problemas de seleccion bajo restriccion
        // (peso en kg, valor = ganancia en $, nodoId = destino de entrega).
        //
        // Calibrado a proposito: con la camioneta de 50 kg, greedy por ratio
        // valor/peso toma P1 (300 $/kg), descarta P2 y P3 porque no entran,
        // toma P4, descarta P5 y toma P6: {P1, P4, P6} = 13400. El optimo real
        // es {P2, P3} con 14000. Es el contraejemplo que se necesita para
        // comparar greedy contra programacion dinamica.
        itemRepository.saveAll(List.of(
                new Item("P1", "Paquete 1", 30, 9000, "BEL"),
                new Item("P2", "Paquete 2", 25, 7000, "PAL"),
                new Item("P3", "Paquete 3", 25, 7000, "REC"),
                new Item("P4", "Paquete 4", 15, 3600, "CAB"),
                new Item("P5", "Paquete 5", 10, 2000, "FLO"),
                new Item("P6", "Paquete 6", 5, 800, "VUR")
        ));

        log.info("Carga inicial completada: 10 nodos, 16 rutas, 6 paquetes.");
        log.info("Para desactivarla: app.seed.enabled=false");
    }
}
