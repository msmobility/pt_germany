package skimCalculator;

import ch.sbb.matsim.analysis.skims.LeastCostPathTree;
import ch.sbb.matsim.routing.pt.raptor.*;
import nom.tam.fits.BasicHDU;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static skimCalculator.MyCalculateSkimMatricesWithAccessMode.extractXy2LinksNetwork;

public class MyPtSkimMatricesWithAccessMode {

    private static Logger log = Logger.getLogger(MyPtSkimMatricesWithAccessMode.class);

    private MyPtSkimMatricesWithAccessMode() {
    }

    public static <T> MyPtSkimMatricesWithAccessMode.PtIndicators<T> calculateSkimMatrices(SwissRailRaptorData raptorData,
                                                                                           Map<T, SimpleFeature> zones,
                                                                                           Map<T, Coord[]> coordsPerZone,
                                                                                           double minDepartureTime, double maxDepartureTime,
                                                                                           double stepSize_seconds, RaptorParameters parameters, int numberOfThreads, BiPredicate<TransitLine, TransitRoute> trainDetector,
                                                                                           Config config, String networkFilename, Predicate<Link> xy2linksPredicate) {
        //prepare car matrix
        Scenario scenario = ScenarioUtils.createScenario(config);
        log.info("loading network from " + networkFilename);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);

        TravelTime tt;
        tt = new FreeSpeedTravelTime();
        log.info("No events specified. Travel Times will be calculated with free speed travel times.");

        TravelDisutility td = new OnlyTimeDependentTravelDisutility(tt);

        log.info("extracting car-only network");
        final Network carNetwork = NetworkUtils.createNetwork();
        new TransportModeNetworkFilter(scenario.getNetwork()).filter(carNetwork, Collections.singleton(TransportMode.car));

        log.info("filter car-only network for assigning links to locations");
        final Network xy2linksNetwork = extractXy2LinksNetwork(carNetwork, xy2linksPredicate);


        // prepare calculation
        MyPtSkimMatricesWithAccessMode.PtIndicators<T> pti = new MyPtSkimMatricesWithAccessMode.PtIndicators<>(zones.keySet());

        // do calculation
        ConcurrentLinkedQueue<T> originZones = new ConcurrentLinkedQueue<>(zones.keySet());

        Counter counter = new Counter("PT-FrequencyMatrix-" + Time.writeTime(minDepartureTime) + "-" + Time.writeTime(maxDepartureTime) + " zone ", " / " + zones.size());
        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            SwissRailRaptor raptor = new SwissRailRaptor(raptorData, null, null, null);
            MyPtSkimMatricesWithAccessMode.RowWorker<T> worker = new MyPtSkimMatricesWithAccessMode.RowWorker<>(originZones, zones.keySet(),
                    coordsPerZone, pti, raptor, parameters, minDepartureTime, maxDepartureTime, stepSize_seconds, counter, trainDetector,
                    xy2linksNetwork, carNetwork, tt, td);
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
        private final MyPtSkimMatricesWithAccessMode.PtIndicators<T> pti;
        private final SwissRailRaptor raptor;
        private final RaptorParameters parameters;
        private final double minDepartureTime;
        private final double maxDepartureTime;
        private final double stepSize;
        private final Counter counter;
        private final BiPredicate<TransitLine, TransitRoute> trainDetector;
        private final Network xy2linksNetwork;
        private final Network carNetwork;
        private final TravelTime tt;
        private final TravelDisutility td;

        RowWorker(ConcurrentLinkedQueue<T> originZones, Set<T> destinationZones, Map<T, Coord[]> coordsPerZone, PtIndicators<T> pti,
                  SwissRailRaptor raptor, RaptorParameters parameters, double minDepartureTime,
                  double maxDepartureTime, double stepSize, Counter counter, BiPredicate<TransitLine, TransitRoute> trainDetector,
                  Network xy2linksNetwork, Network carNetwork, TravelTime tt, TravelDisutility td) {
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
            this.xy2linksNetwork = xy2linksNetwork;
            this.carNetwork = carNetwork;
            this.tt = tt;
            this.td = td;
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

            ch.sbb.matsim.analysis.skims.LeastCostPathTree treeAtOrigin = new ch.sbb.matsim.analysis.skims.LeastCostPathTree(tt, td);
            treeAtOrigin.calculate(carNetwork, NetworkUtils.getNearestNode(carNetwork, fromCoord), 8*3600);

            Collection<TransitStopFacility> fromStops = findStopCandidates(fromCoord, this.raptor, this.parameters);
            Map<Id<TransitStopFacility>, Double> accessTimes = new HashMap<>();
            for (TransitStopFacility stop : fromStops) {
                double distance;
                double accessTime;
                LeastCostPathTree.NodeData nodeData = treeAtOrigin.getTree().get(NetworkUtils.getNearestNode(carNetwork, stop.getCoord()).getId());
                if (nodeData != null){
                    //distance = nodeData.getDistance();
                    accessTime = nodeData.getTime() - 8*3600;
                } else {
                    distance = CoordUtils.calcEuclideanDistance(stop.getCoord(), fromCoord);
                    accessTime = distance / walkSpeed;
                }
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
                        calcForOD(fromZoneId, fromCoord, toZoneId, toCoord, accessTimes, trees, fromStops);

                    }
                }
            }
        }

        private void calcForOD(T fromZoneId, Coord fromCoord, T toZoneId, Coord toCoord, Map<Id<TransitStopFacility>, Double> accessTimes,
                               List<Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo>> trees, Collection<TransitStopFacility> fromStops) {
            double averageShuttleSpeed_ms = 30 / 3.6;
            //too long runtime
            //ch.sbb.matsim.analysis.skims.LeastCostPathTree treeFromDestination = new ch.sbb.matsim.analysis.skims.LeastCostPathTree(tt, td);
            //treeFromDestination.calculate(carNetwork, NetworkUtils.getNearestNode(carNetwork, toCoord), 8*3600);

            Collection<TransitStopFacility> toStops = findStopCandidates(toCoord, this.raptor, this.parameters);
            Map<Id<TransitStopFacility>, Double> egressTimes = new HashMap<>();
            for (TransitStopFacility stop : toStops) {
                double distance;
                double egressTime;
                //LeastCostPathTree.NodeData nodeData = treeFromDestination.getTree().get(NetworkUtils.getNearestNode(carNetwork, stop.getCoord()));
                //if (nodeData != null){
                //distance = nodeData.getDistance();
                //egressTime = nodeData.getTime();
                //} else {
                    distance = CoordUtils.calcEuclideanDistance(stop.getCoord(), toCoord);
                    egressTime = distance / averageShuttleSpeed_ms;
                //}
                egressTimes.put(stop.getId(), egressTime);
            }

            List<MyPtSkimMatricesWithAccessMode.ODConnection> connections = buildODConnections(trees, egressTimes);
            if (connections.isEmpty()) {
                return;
            }

            connections = sortAndFilterConnections(connections);

            double avgAdaptionTime = calcAverageAdaptionTime(connections, minDepartureTime, maxDepartureTime);

            this.pti.adaptionTimeMatrix.add(fromZoneId, toZoneId, (float) avgAdaptionTime);

            Map<MyPtSkimMatricesWithAccessMode.ODConnection, Double> connectionShares = calcConnectionShares(connections, minDepartureTime, maxDepartureTime);

            float accessTime = 0;
            float egressTime = 0;
            float transferCount = 0;
            float travelTime = 0;

            double totalDistance = 0;
            double trainDistance = 0;
            double totalInVehTime = 0;
            double trainInVehTime = 0;

            for (Map.Entry<MyPtSkimMatricesWithAccessMode.ODConnection, Double> e : connectionShares.entrySet()) {
                MyPtSkimMatricesWithAccessMode.ODConnection connection = e.getKey();
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


            ODConnection fastestConnection = findFastestConnection(connections);
            Id<TransitStopFacility> departureStopId = fastestConnection.travelInfo.departureStop;
            TransitStopFacility departureStop = null;
            for (TransitStopFacility stop : fromStops) {
                if (stop.getId().equals(departureStopId)) {
                    departureStop = stop;
                }
            }

            if (departureStop != null && fromZoneId != null && toZoneId != null){
                this.pti.coordinatesOfAccessStation.putIfAbsent(fromZoneId, new HashMap<>());
                this.pti.coordinatesOfAccessStation.get(fromZoneId).put(toZoneId, departureStop.getCoord());
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

        private List<MyPtSkimMatricesWithAccessMode.ODConnection> buildODConnections(List<Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo>> trees, Map<Id<TransitStopFacility>, Double> egressTimes) {
            List<MyPtSkimMatricesWithAccessMode.ODConnection> connections = new ArrayList<>();

            for (Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> tree : trees) {
                for (Map.Entry<Id<TransitStopFacility>, Double> egressEntry : egressTimes.entrySet()) {
                    Id<TransitStopFacility> egressStopId = egressEntry.getKey();
                    Double egressTime = egressEntry.getValue();
                    SwissRailRaptorCore.TravelInfo info = tree.get(egressStopId);
                    if (info != null) {
                        MyPtSkimMatricesWithAccessMode.ODConnection connection = new MyPtSkimMatricesWithAccessMode.ODConnection(info.ptDepartureTime, info.ptTravelTime, info.accessTime, egressTime, info.transferCount, info);
                        connections.add(connection);
                    }
                }
            }

            return connections;
        }

        static List<MyPtSkimMatricesWithAccessMode.ODConnection> sortAndFilterConnections(List<MyPtSkimMatricesWithAccessMode.ODConnection> connections) {
            connections.sort((c1, c2) -> Double.compare((c1.departureTime - c1.accessTime), (c2.departureTime - c2.accessTime)));

            // step forward through all connections and figure out which can be ignore because the earlier one is better
            List<MyPtSkimMatricesWithAccessMode.ODConnection> filteredConnections1 = new ArrayList<>();
            MyPtSkimMatricesWithAccessMode.ODConnection earlierConnection = null;
            for (MyPtSkimMatricesWithAccessMode.ODConnection connection : connections) {
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
            List<MyPtSkimMatricesWithAccessMode.ODConnection> filteredConnections = new ArrayList<>();
            MyPtSkimMatricesWithAccessMode.ODConnection laterConnection = null;

            for (int i = filteredConnections1.size() - 1; i >= 0; i--) {
                MyPtSkimMatricesWithAccessMode.ODConnection connection = filteredConnections1.get(i);
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

        private MyPtSkimMatricesWithAccessMode.ODConnection findFastestConnection(List<MyPtSkimMatricesWithAccessMode.ODConnection> connections) {
            MyPtSkimMatricesWithAccessMode.ODConnection fastest = null;
            for (MyPtSkimMatricesWithAccessMode.ODConnection c : connections) {
                if (fastest == null || c.travelTime < fastest.travelTime) {
                    fastest = c;
                }
            }
            return fastest;
        }

        static double calcAverageAdaptionTime(List<MyPtSkimMatricesWithAccessMode.ODConnection> connections, double minDepartureTime, double maxDepartureTime) {
            double prevDepartureTime = Double.NaN;
            double nextDepartureTime = Double.NaN;
            MyPtSkimMatricesWithAccessMode.ODConnection prevConnection = null;
            MyPtSkimMatricesWithAccessMode.ODConnection nextConnection = null;

            Iterator<MyPtSkimMatricesWithAccessMode.ODConnection> connectionIterator = connections.iterator();
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

        /**
         * calculates the share each connection covers based on minimizing (travelTime + adaptionTime)
         */
        static Map<MyPtSkimMatricesWithAccessMode.ODConnection, Double> calcConnectionShares(List<MyPtSkimMatricesWithAccessMode.ODConnection> connections, double minDepartureTime, double maxDepartureTime) {
            double prevDepartureTime = Double.NaN;
            double nextDepartureTime = Double.NaN;

            MyPtSkimMatricesWithAccessMode.ODConnection prevConnection = null;
            MyPtSkimMatricesWithAccessMode.ODConnection nextConnection = null;

            Map<MyPtSkimMatricesWithAccessMode.ODConnection, Double> shares = new HashMap<>();

            Iterator<MyPtSkimMatricesWithAccessMode.ODConnection> connectionIterator = connections.iterator();
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
                    shares.compute(nextConnection, (c, oldVal) -> (oldVal == null ? 1 : (oldVal + 1)));
                } else if (nextConnection == null) {
                    shares.compute(prevConnection, (c, oldVal) -> (oldVal == null ? 1 : (oldVal + 1)));
                } else {
                    double prevAdaptionTime = time - prevDepartureTime;
                    double nextAdaptionTime = nextDepartureTime - time;
                    double prevTotalTime = prevConnection.travelTime + prevAdaptionTime;
                    double nextTotalTime = nextConnection.travelTime + nextAdaptionTime;

                    if (prevTotalTime < nextTotalTime) {
                        shares.compute(prevConnection, (c, oldVal) -> (oldVal == null ? 1 : (oldVal + 1)));
                    } else {
                        shares.compute(nextConnection, (c, oldVal) -> (oldVal == null ? 1 : (oldVal + 1)));
                    }
                }
            }

            double sum = (maxDepartureTime - minDepartureTime) / 60;
            for (Map.Entry<MyPtSkimMatricesWithAccessMode.ODConnection, Double> e : shares.entrySet()) {
                MyPtSkimMatricesWithAccessMode.ODConnection c = e.getKey();
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
        public final Map<T, Map<T, Coord>> coordinatesOfAccessStation;
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
            this.coordinatesOfAccessStation = new HashMap<>();
        }
    }


}
