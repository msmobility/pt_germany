package skimCalculator;

import ch.sbb.matsim.analysis.skims.FloatMatrix;
import ch.sbb.matsim.routing.pt.raptor.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiPredicate;

public class MyPtSkimMatrices {

    private MyPtSkimMatrices() {
    }

    public static <T> MyPtSkimMatrices.PtIndicators<T> calculateSkimMatrices(SwissRailRaptorData raptorData,
                                                                             Map<T, SimpleFeature> zones,
                                                                             Map<T, Coord[]> coordsPerZone,
                                                                             double minDepartureTime, double maxDepartureTime,
                                                                             double stepSize_seconds, RaptorParameters parameters, int numberOfThreads, BiPredicate<TransitLine, TransitRoute> trainDetector) {
        // prepare calculation
        MyPtSkimMatrices.PtIndicators<T> pti = new MyPtSkimMatrices.PtIndicators<>(zones.keySet());

        // do calculation
        ConcurrentLinkedQueue<T> originZones = new ConcurrentLinkedQueue<>(zones.keySet());

        Counter counter = new Counter("PT-FrequencyMatrix-" + Time.writeTime(minDepartureTime) + "-" + Time.writeTime(maxDepartureTime) + " zone ", " / " + zones.size());
        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            SwissRailRaptor raptor = new SwissRailRaptor(raptorData, null, null, null);
            MyPtSkimMatrices.RowWorker<T> worker = new MyPtSkimMatrices.RowWorker<>(originZones, zones.keySet(), coordsPerZone, pti, raptor, parameters, minDepartureTime, maxDepartureTime, stepSize_seconds, counter, trainDetector);
            threads[i] = new Thread(worker, "PT-FrequencyMatrix-" + Time.writeTime(minDepartureTime) + "-" + Time.writeTime(maxDepartureTime) + "-" + i);
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

        for (T fromZoneId : zones.keySet()) {
            for (T toZoneId : zones.keySet()) {
                float count = pti.dataCountMatrix.get(fromZoneId, toZoneId);
                if (count == 0) {
                    pti.adaptionTimeMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.frequencyMatrix.set(fromZoneId, toZoneId, 0);
                    pti.distanceMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.travelTimeMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.accessTimeMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.egressTimeMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.transferCountMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.trainDistanceShareMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                    pti.trainTravelTimeShareMatrix.set(fromZoneId, toZoneId, Float.POSITIVE_INFINITY);
                } else {
                    float avgFactor = 1.0f / count;
                    float adaptionTime = pti.adaptionTimeMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.travelTimeMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.accessTimeMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.egressTimeMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.trainDistanceShareMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.trainTravelTimeShareMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    pti.transferCountMatrix.multiply(fromZoneId, toZoneId, avgFactor);
                    float frequency = (float) ((maxDepartureTime - minDepartureTime) / adaptionTime / 4.0);
                    pti.frequencyMatrix.set(fromZoneId, toZoneId, frequency);
                }
            }
        }

        return pti;
    }

    static class RowWorker<T> implements Runnable {
        private final ConcurrentLinkedQueue<T> originZones;
        private final Set<T> destinationZones;
        private final Map<T, Coord[]> coordsPerZone;
        private final MyPtSkimMatrices.PtIndicators<T> pti;
        private final SwissRailRaptor raptor;
        private final RaptorParameters parameters;
        private final double minDepartureTime;
        private final double maxDepartureTime;
        private final double stepSize;
        private final Counter counter;
        private final BiPredicate<TransitLine, TransitRoute> trainDetector;

        RowWorker(ConcurrentLinkedQueue<T> originZones, Set<T> destinationZones, Map<T, Coord[]> coordsPerZone, MyPtSkimMatrices.PtIndicators<T> pti, SwissRailRaptor raptor, RaptorParameters parameters, double minDepartureTime, double maxDepartureTime, double stepSize, Counter counter, BiPredicate<TransitLine, TransitRoute> trainDetector) {
            this.originZones = originZones;
            this.destinationZones = destinationZones;
            this.coordsPerZone = coordsPerZone;
            this.pti = pti;
            this.raptor = raptor;
            this.parameters = parameters;
            this.minDepartureTime = minDepartureTime;
            this.maxDepartureTime = maxDepartureTime;
            this.stepSize = stepSize;
            this.counter = counter;
            this.trainDetector = trainDetector;
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
                        calcForRow(fromZoneId, fromCoord);
                    }
                }
            }
        }

        private void calcForRow(T fromZoneId, Coord fromCoord) {
            double walkSpeed = this.parameters.getBeelineWalkSpeed();

            Collection<TransitStopFacility> fromStops = findStopCandidates(fromCoord, this.raptor, this.parameters);
            Map<Id<TransitStopFacility>, Double> accessTimes = new HashMap<>();
            for (TransitStopFacility stop : fromStops) {
                double distance = CoordUtils.calcEuclideanDistance(fromCoord, stop.getCoord());
                double accessTime = distance / walkSpeed;
                accessTimes.put(stop.getId(), accessTime);
            }

            List<Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo>> trees = new ArrayList<>();

            for (double time = this.minDepartureTime; time < this.maxDepartureTime; time += this.stepSize) {
                Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> tree = this.raptor.calcTree(fromStops, time, this.parameters);
                trees.add(tree);
            }

            for (T toZoneId : this.destinationZones) {
                Coord[] toCoords = this.coordsPerZone.get(toZoneId);
                if (toCoords != null) {
                    for (Coord toCoord : toCoords) {
                        calcForOD(fromZoneId, fromCoord, toZoneId, toCoord, accessTimes, trees);

                    }
                }
            }
        }

        private void calcForOD(T fromZoneId, Coord fromCoord, T toZoneId, Coord toCoord, Map<Id<TransitStopFacility>, Double> accessTimes, List<Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo>> trees) {
            double walkSpeed = this.parameters.getBeelineWalkSpeed();

            Collection<TransitStopFacility> toStops = findStopCandidates(toCoord, this.raptor, this.parameters);
            Map<Id<TransitStopFacility>, Double> egressTimes = new HashMap<>();
            for (TransitStopFacility stop : toStops) {
                double distance = CoordUtils.calcEuclideanDistance(stop.getCoord(), toCoord);
                double egressTime = distance / walkSpeed;
                egressTimes.put(stop.getId(), egressTime);
            }

            List<MyPtSkimMatrices.ODConnection> connections = buildODConnections(trees, egressTimes);
            if (connections.isEmpty()) {
                return;
            }

            connections = sortAndFilterConnections(connections);

            double avgAdaptionTime = calcAverageAdaptionTime(connections, minDepartureTime, maxDepartureTime);

            this.pti.adaptionTimeMatrix.add(fromZoneId, toZoneId, (float) avgAdaptionTime);

            Map<MyPtSkimMatrices.ODConnection, Double> connectionShares = calcConnectionShares(connections, minDepartureTime, maxDepartureTime);

            float accessTime = 0;
            float egressTime = 0;
            float transferCount = 0;
            float travelTime = 0;

            double totalDistance = 0;
            double trainDistance = 0;
            double totalInVehTime = 0;
            double trainInVehTime = 0;

            for (Map.Entry<MyPtSkimMatrices.ODConnection, Double> e : connectionShares.entrySet()) {
                MyPtSkimMatrices.ODConnection connection = e.getKey();
                double share = e.getValue();

                accessTime += share * accessTimes.get(connection.travelInfo.departureStop).floatValue();
                egressTime += share * (float) connection.egressTime;
                transferCount += share * (float) connection.transferCount;
                travelTime += share * (float) connection.totalTravelTime();

                double connTotalDistance = 0;
                double connTrainDistance = 0;
                double connTotalInVehTime = 0;
                double connTrainInVehTime = 0;

                RaptorRoute route = connection.travelInfo.getRaptorRoute();
                for (RaptorRoute.RoutePart part : route.getParts()) {
                    if (part.line != null) {
                        // it's a non-transfer part, an actual pt stage

                        boolean isTrain = this.trainDetector.test(part.line, part.route);
                        double inVehicleTime = part.arrivalTime - part.boardingTime;

                        connTotalDistance += part.distance;
                        connTotalInVehTime += inVehicleTime;

                        if (isTrain) {
                            connTrainDistance += part.distance;
                            connTrainInVehTime += inVehicleTime;
                        }
                    }
                }

                totalDistance += share * connTotalDistance;
                trainDistance += share * connTrainDistance;
                totalInVehTime += share * connTotalInVehTime;
                trainInVehTime += share * connTrainInVehTime;
            }

            float trainShareByTravelTime = (float) (trainInVehTime / totalInVehTime);
            float trainShareByDistance = (float) (trainDistance / totalDistance);

            this.pti.accessTimeMatrix.add(fromZoneId, toZoneId, accessTime);
            this.pti.egressTimeMatrix.add(fromZoneId, toZoneId, egressTime);
            this.pti.transferCountMatrix.add(fromZoneId, toZoneId, transferCount);
            this.pti.travelTimeMatrix.add(fromZoneId, toZoneId, travelTime);
            this.pti.distanceMatrix.add(fromZoneId, toZoneId, (float) totalDistance);
            this.pti.trainDistanceShareMatrix.add(fromZoneId, toZoneId, trainShareByDistance);
            this.pti.trainTravelTimeShareMatrix.add(fromZoneId, toZoneId, trainShareByTravelTime);

            this.pti.dataCountMatrix.add(fromZoneId, toZoneId, 1);
        }

        private List<MyPtSkimMatrices.ODConnection> buildODConnections(List<Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo>> trees, Map<Id<TransitStopFacility>, Double> egressTimes) {
            List<MyPtSkimMatrices.ODConnection> connections = new ArrayList<>();

            for (Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> tree : trees) {
                for (Map.Entry<Id<TransitStopFacility>, Double> egressEntry : egressTimes.entrySet()) {
                    Id<TransitStopFacility> egressStopId = egressEntry.getKey();
                    Double egressTime = egressEntry.getValue();
                    SwissRailRaptorCore.TravelInfo info = tree.get(egressStopId);
                    if (info != null) {
                        MyPtSkimMatrices.ODConnection connection = new MyPtSkimMatrices.ODConnection(info.ptDepartureTime, info.ptTravelTime, info.accessTime, egressTime, info.transferCount, info);
                        connections.add(connection);
                    }
                }
            }

            return connections;
        }

        static List<MyPtSkimMatrices.ODConnection> sortAndFilterConnections(List<MyPtSkimMatrices.ODConnection> connections) {
            connections.sort((c1, c2) -> Double.compare((c1.departureTime - c1.accessTime), (c2.departureTime - c2.accessTime)));

            // step forward through all connections and figure out which can be ignore because the earlier one is better
            List<MyPtSkimMatrices.ODConnection> filteredConnections1 = new ArrayList<>();
            MyPtSkimMatrices.ODConnection earlierConnection = null;
            for (MyPtSkimMatrices.ODConnection connection : connections) {
                if (earlierConnection == null) {
                    filteredConnections1.add(connection);
                    earlierConnection = connection;
                } else {
                    double timeDiff = (connection.departureTime - connection.accessTime) - (earlierConnection.departureTime - earlierConnection.accessTime);
                    if (earlierConnection.totalTravelTime() + timeDiff > connection.totalTravelTime()) {
                        // connection is better to earlierConnection, use it
                        filteredConnections1.add(connection);
                        earlierConnection = connection;
                    }
                }
            }

            // now step backwards through the remaining connections and figure out which can be ignored because the later one is better
            List<MyPtSkimMatrices.ODConnection> filteredConnections = new ArrayList<>();
            MyPtSkimMatrices.ODConnection laterConnection = null;

            for (int i = filteredConnections1.size() - 1; i >= 0; i--) {
                MyPtSkimMatrices.ODConnection connection = filteredConnections1.get(i);
                if (laterConnection == null) {
                    filteredConnections.add(connection);
                    laterConnection = connection;
                } else {
                    double timeDiff = (laterConnection.departureTime - laterConnection.accessTime) - (connection.departureTime - connection.accessTime);
                    if (laterConnection.totalTravelTime() + timeDiff > connection.totalTravelTime()) {
                        // connection is better to laterConnection, use it
                        filteredConnections.add(connection);
                        laterConnection = connection;
                    }
                }
            }

            Collections.reverse(filteredConnections);
            // now the filtered connections are in ascending departure time order
            return filteredConnections;
        }

        private MyPtSkimMatrices.ODConnection findFastestConnection(List<MyPtSkimMatrices.ODConnection> connections) {
            MyPtSkimMatrices.ODConnection fastest = null;
            for (MyPtSkimMatrices.ODConnection c : connections) {
                if (fastest == null || c.travelTime < fastest.travelTime) {
                    fastest = c;
                }
            }
            return fastest;
        }

        static double calcAverageAdaptionTime(List<MyPtSkimMatrices.ODConnection> connections, double minDepartureTime, double maxDepartureTime) {
            double prevDepartureTime = Double.NaN;
            double nextDepartureTime = Double.NaN;
            MyPtSkimMatrices.ODConnection prevConnection = null;
            MyPtSkimMatrices.ODConnection nextConnection = null;

            Iterator<MyPtSkimMatrices.ODConnection> connectionIterator = connections.iterator();
            if (connectionIterator.hasNext()) {
                nextConnection = connectionIterator.next();
                nextDepartureTime = nextConnection.departureTime - nextConnection.accessTime;
            }

            double sum = 0.0;
            int count = 0;
            for (double time = minDepartureTime; time < maxDepartureTime; time += 60.0) {
                double adaptionTime;

                if (time >= nextDepartureTime) {
                    prevDepartureTime = nextDepartureTime;
                    prevConnection = nextConnection;
                    if (connectionIterator.hasNext()) {
                        nextConnection = connectionIterator.next();
                        nextDepartureTime = nextConnection.departureTime - nextConnection.accessTime;
                    } else {
                        nextDepartureTime = Double.NaN;
                        nextConnection = null;
                    }
                }

                if (prevConnection == null) {
                    adaptionTime = nextDepartureTime - time;
                } else if (nextConnection == null) {
                    adaptionTime = time - prevDepartureTime;
                } else {
                    double prevAdaptionTime = time - prevDepartureTime;
                    double nextAdaptionTime = nextDepartureTime - time;
                    double prevTotalTime = prevConnection.travelTime + prevAdaptionTime;
                    double nextTotalTime = nextConnection.travelTime + nextAdaptionTime;

                    if (prevTotalTime < nextTotalTime) {
                        adaptionTime = prevAdaptionTime;
                    } else {
                        adaptionTime = nextAdaptionTime;
                    }
                }

                sum += adaptionTime;
                count++;
            }
            return sum / count;
        }

        /** calculates the share each connection covers based on minimizing (travelTime + adaptionTime)
         */
        static Map<MyPtSkimMatrices.ODConnection, Double> calcConnectionShares(List<MyPtSkimMatrices.ODConnection> connections, double minDepartureTime, double maxDepartureTime) {
            double prevDepartureTime = Double.NaN;
            double nextDepartureTime = Double.NaN;

            MyPtSkimMatrices.ODConnection prevConnection = null;
            MyPtSkimMatrices.ODConnection nextConnection = null;

            Map<MyPtSkimMatrices.ODConnection, Double> shares = new HashMap<>();

            Iterator<MyPtSkimMatrices.ODConnection> connectionIterator = connections.iterator();
            if (connectionIterator.hasNext()) {
                nextConnection = connectionIterator.next();
                nextDepartureTime = nextConnection.departureTime - nextConnection.accessTime;
            }

            for (double time = minDepartureTime; time < maxDepartureTime; time += 60.0) {
                if (time >= nextDepartureTime) {
                    prevDepartureTime = nextDepartureTime;
                    prevConnection = nextConnection;
                    if (connectionIterator.hasNext()) {
                        nextConnection = connectionIterator.next();
                        nextDepartureTime = nextConnection.departureTime - nextConnection.accessTime;
                    } else {
                        nextDepartureTime = Double.NaN;
                        nextConnection = null;
                    }
                }

                if (prevConnection == null) {
                    shares.compute(nextConnection, (c, oldVal) -> (oldVal == null ? 1 : (oldVal+1)));
                } else if (nextConnection == null) {
                    shares.compute(prevConnection, (c, oldVal) -> (oldVal == null ? 1 : (oldVal+1)));
                } else {
                    double prevAdaptionTime = time - prevDepartureTime;
                    double nextAdaptionTime = nextDepartureTime - time;
                    double prevTotalTime = prevConnection.travelTime + prevAdaptionTime;
                    double nextTotalTime = nextConnection.travelTime + nextAdaptionTime;

                    if (prevTotalTime < nextTotalTime) {
                        shares.compute(prevConnection, (c, oldVal) -> (oldVal == null ? 1 : (oldVal+1)));
                    } else {
                        shares.compute(nextConnection, (c, oldVal) -> (oldVal == null ? 1 : (oldVal+1)));
                    }
                }
            }

            double sum = (maxDepartureTime - minDepartureTime) / 60;
            for (Map.Entry<MyPtSkimMatrices.ODConnection, Double> e : shares.entrySet()) {
                MyPtSkimMatrices.ODConnection c = e.getKey();
                shares.put(c, e.getValue() / sum);
            }

            return shares;
        }

        private static Collection<TransitStopFacility> findStopCandidates(Coord coord, SwissRailRaptor raptor, RaptorParameters parameters) {
            Collection<TransitStopFacility> stops = raptor.getUnderlyingData().findNearbyStops(coord.getX(), coord.getY(), parameters.getSearchRadius());
            if (stops.isEmpty()) {
                TransitStopFacility nearest = raptor.getUnderlyingData().findNearestStop(coord.getX(), coord.getY());
                double nearestStopDistance = CoordUtils.calcEuclideanDistance(coord, nearest.getCoord());
                stops = raptor.getUnderlyingData().findNearbyStops(coord.getX(), coord.getY(), nearestStopDistance + parameters.getExtensionRadius());
            }
            return stops;
        }
    }

    static class ODConnection {
        final double departureTime;
        final double travelTime;
        final double accessTime;
        final double egressTime;
        final double transferCount;
        final SwissRailRaptorCore.TravelInfo travelInfo;

        ODConnection(double departureTime, double travelTime, double accessTime, double egressTime, double transferCount, SwissRailRaptorCore.TravelInfo info) {
            this.departureTime = departureTime;
            this.travelTime = travelTime;
            this.accessTime = accessTime;
            this.egressTime = egressTime;
            this.transferCount = transferCount;
            this.travelInfo = info;
        }

        double totalTravelTime() {
            return this.accessTime + this.travelTime + this.egressTime;
        }
    }

    public static class PtIndicators<T> {
        public final MyFloatMatrix<T> adaptionTimeMatrix;
        public final MyFloatMatrix<T> frequencyMatrix;

        public final MyFloatMatrix<T> distanceMatrix;
        public final MyFloatMatrix<T> travelTimeMatrix;
        public final MyFloatMatrix<T> accessTimeMatrix;
        public final MyFloatMatrix<T> egressTimeMatrix;
        public final MyFloatMatrix<T> transferCountMatrix;
        public final MyFloatMatrix<T> trainTravelTimeShareMatrix;
        public final MyFloatMatrix<T> trainDistanceShareMatrix;

        public final MyFloatMatrix<T> dataCountMatrix; // how many values/routes were taken into account to calculate the averages

        PtIndicators(Set<T> zones) {
            this.adaptionTimeMatrix = new MyFloatMatrix<>(zones, 0);
            this.frequencyMatrix = new MyFloatMatrix<>(zones, 0);
            this.distanceMatrix = new MyFloatMatrix<>(zones, 0);
            this.travelTimeMatrix = new MyFloatMatrix<>(zones, 0);
            this.accessTimeMatrix = new MyFloatMatrix<>(zones, 0);
            this.egressTimeMatrix = new MyFloatMatrix<>(zones, 0);
            this.transferCountMatrix = new MyFloatMatrix<>(zones, 0);
            this.dataCountMatrix = new MyFloatMatrix<>(zones, 0);
            this.trainTravelTimeShareMatrix = new MyFloatMatrix<>(zones, 0);
            this.trainDistanceShareMatrix = new MyFloatMatrix<>(zones, 0);
        }
    }


}
