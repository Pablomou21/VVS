package es.udc.paproject.backend.testutil;

import java.util.Map;

import etm.core.aggregation.Aggregate;
import etm.core.configuration.BasicEtmConfigurator;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmMonitor;
import etm.core.monitor.EtmPoint;

/**
 * Utilidad común para las pruebas de rendimiento (no funcionales) con JETM.
 *
 * Ejecuta una operación varias veces sin medir (calentamiento, para que la JIT, las cachés de Hibernate
 * y el pool de conexiones no penalicen las primeras ejecuciones) y después la ejecuta un número fijo de
 * veces midiendo cada ejecución con un EtmPoint. Devuelve las estadísticas agregadas por JETM en milisegundos.
 *
 * Cada medición configura y arranca su propio monitor y lo resetea al final, para que las medidas de
 * distintos tests no se mezclen.
 */
public final class PerformanceMeter {

    /** Número de ejecuciones de calentamiento por defecto (no se miden). */
    public static final int DEFAULT_WARMUP_ITERATIONS = 10;

    /** Número de ejecuciones medidas por defecto. */
    public static final int DEFAULT_MEASURED_ITERATIONS = 50;

    /** Operación a medir. Puede lanzar excepciones comprobadas, como los métodos de los servicios. */
    @FunctionalInterface
    public interface Operation {
        void run() throws Exception;
    }

    /** Resultado de una medición: estadísticas agregadas por JETM, en milisegundos. */
    public record Result(String name, long measurements, double averageMillis, double minMillis, double maxMillis) {

        @Override
        public String toString() {
            return String.format("[JETM] %s: %d mediciones, media %.3f ms, min %.3f ms, max %.3f ms",
                name, measurements, averageMillis, minMillis, maxMillis);
        }
    }

    private PerformanceMeter() {}

    /**
     * Mide la operación con los valores por defecto de calentamiento y de repeticiones.
     */
    public static Result measure(String name, Operation operation) throws Exception {
        return measure(name, DEFAULT_WARMUP_ITERATIONS, DEFAULT_MEASURED_ITERATIONS, operation);
    }

    /**
     * Ejecuta la operación {@code warmupIterations} veces sin medir y después {@code measuredIterations}
     * veces midiendo cada ejecución con JETM.
     */
    public static Result measure(String name, int warmupIterations, int measuredIterations, Operation operation)
            throws Exception {

        EtmManager.reset();
        BasicEtmConfigurator.configure();
        EtmMonitor monitor = EtmManager.getEtmMonitor();
        monitor.start();

        try {
            for (int i = 0; i < warmupIterations; i++) {
                operation.run();
            }

            for (int i = 0; i < measuredIterations; i++) {
                EtmPoint point = monitor.createPoint(name);
                try {
                    operation.run();
                } finally {
                    point.collect();
                }
            }

            Result[] result = new Result[1];
            monitor.render(points -> result[0] = toResult(name, points));

            System.out.println(result[0]);
            return result[0];

        } finally {
            monitor.stop();
            EtmManager.reset();
        }
    }

    private static Result toResult(String name, Map<?, ?> points) {
        Aggregate aggregate = (Aggregate) points.get(name);
        return new Result(name, aggregate.getMeasurements(), aggregate.getAverage(), aggregate.getMin(),
            aggregate.getMax());
    }
}
