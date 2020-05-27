package skimCalculator;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MyBeelineDistanceMatrix {

    private MyBeelineDistanceMatrix() {
    }

    public static <T> MyFloatMatrix<T> calculateBeelineDistanceMatrix(Map<T, SimpleFeature> zones, Map<T, Coord[]> coordsPerZone, int numberOfThreads) {
        // prepare calculation
        MyFloatMatrix<T> matrix = new MyFloatMatrix<>(zones.keySet(), 0.0f);

        int numberOfPointsPerZone = coordsPerZone.values().iterator().next().length;

        // do calculation
        ConcurrentLinkedQueue<T> originZones = new ConcurrentLinkedQueue<>(zones.keySet());

        Counter counter = new Counter("MyBeelineDistanceMatrix zone ", " / " + zones.size());
        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            MyBeelineDistanceMatrix.RowWorker<T> worker = new MyBeelineDistanceMatrix.RowWorker<>(originZones, zones.keySet(), coordsPerZone, matrix, counter);
            threads[i] = new Thread(worker, "MyBeelineDistanceMatrix-" + i);
            threads[i].start();
        }

        // wait until all threads have finished
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        matrix.multiply((float) (1.0 / numberOfPointsPerZone / numberOfPointsPerZone));
        return matrix;
    }

    private static class RowWorker<T> implements Runnable {
        private final ConcurrentLinkedQueue<T> originZones;
        private final Set<T> destinationZones;
        private final Map<T, Coord[]> coordsPerZone;
        private final MyFloatMatrix<T> matrix;
        private final Counter counter;

        RowWorker(ConcurrentLinkedQueue<T> originZones, Set<T> destinationZones, Map<T, Coord[]> coordsPerZone, MyFloatMatrix<T> matrix, Counter counter) {
            this.originZones = originZones;
            this.destinationZones = destinationZones;
            this.coordsPerZone = coordsPerZone;
            this.matrix = matrix;
            this.counter = counter;
        }

        public void run() {
            while (true) {
                T fromZoneId = this.originZones.poll();
                if (fromZoneId == null) {
                    return;
                }

                this.counter.incCounter();
                Coord[] fromCoords = this.coordsPerZone.get(fromZoneId);
                if (fromCoords != null) {
                    for (Coord fromCoord : fromCoords) {

                        for (T toZoneId : this.destinationZones) {
                            Coord[] toCoords = this.coordsPerZone.get(toZoneId);
                            if (toCoords != null) {
                                for (Coord toCoord : toCoords) {
                                    double dist = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
                                    this.matrix.add(fromZoneId, toZoneId, (float) dist);
                                }
                            } else {
                                // this might happen if a zone has no geometry, for whatever reason...
                                this.matrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                            }
                        }
                    }
                } else {
                    // this might happen if a zone has no geometry, for whatever reason...
                    for (T toZoneId : this.destinationZones) {
                        this.matrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    }
                }
            }
        }
    }
}
