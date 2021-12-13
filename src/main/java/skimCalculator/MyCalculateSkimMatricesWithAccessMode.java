package skimCalculator;

import ch.sbb.matsim.analysis.skims.StreamingFacilities;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import de.tum.bgu.msm.io.output.OmxMatrixWriter;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import omx.hdf5.OmxConstants;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * This class and the classes called from this one are copied and adapted from
 * https://github.com/SchweizerischeBundesbahnen/matsim-sbb-extensions for customization of outputs and testing purposes
 */
public class MyCalculateSkimMatricesWithAccessMode {

    private static final Logger log = Logger.getLogger(MyCalculateSkimMatricesWithAccessMode.class);

    public static final String CAR_TRAVELTIMES_FILENAME = "car_traveltimes.csv.gz";
    public static final String CAR_DISTANCES_FILENAME = "car_distances.csv.gz";
    public static final String PT_DISTANCES_FILENAME = "pt_distances.csv.gz";
    public static final String PT_TRAVELTIMES_FILENAME = "pt_traveltimes.csv.gz";
    public static final String PT_ACCESSTIMES_FILENAME = "pt_accesstimes.csv.gz";
    public static final String PT_EGRESSTIMES_FILENAME = "pt_egresstimes.csv.gz";
    public static final String PT_FREQUENCIES_FILENAME = "pt_frequencies.csv.gz";
    public static final String PT_ADAPTIONTIMES_FILENAME = "pt_adaptiontimes.csv.gz";
    public static final String PT_TRAINSHARE_BYDISTANCE_FILENAME = "pt_trainshare_bydistance.csv.gz";
    public static final String PT_TRAINSHARE_BYTIME_FILENAME = "pt_trainshare_bytime.csv.gz";
    public static final String PT_TRANSFERCOUNTS_FILENAME = "pt_transfercounts.csv.gz";
    public static final String BEELINE_DISTANCE_FILENAME = "beeline_distances.csv.gz";
    public static final String ZONE_LOCATIONS_FILENAME = "zone_coordinates.csv";

    private final static GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final String PT_ACCESS_STATION_COORDINATES = "access_station_coordinates.csv.gz";

    private final Collection<SimpleFeature> zones;
    private final Map<String, SimpleFeature> zonesById;
    private final String zonesIdAttributeName;
    private final SpatialIndex zonesQt;
    private final String outputDirectory;
    private final int numberOfThreads;
    private Map<String, Coord[]> coordsPerZone = null;
    private final String transportMode;

    public MyCalculateSkimMatricesWithAccessMode(String zonesShapeFilename, String zonesIdAttributeName,
                                                 String outputDirectory, int numberOfThreads,
                                                 String transportMode) {
        this.outputDirectory = outputDirectory;
        this.transportMode = transportMode;
        File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            log.info("create output directory " + outputDirectory);
            outputDir.mkdirs();
        } else {
            log.warn("output directory exists already, might overwrite data. " + outputDirectory);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException("User does not want to overwrite data.");
            }
        }

        this.numberOfThreads = numberOfThreads;

        log.info("loading zones from " + zonesShapeFilename);
        this.zones = new ShapeFileReader().readFileAndInitialize(zonesShapeFilename);
        this.zonesIdAttributeName = zonesIdAttributeName;
        this.zonesById = new HashMap<>();
        this.zonesQt = new Quadtree();
        for (SimpleFeature zone : this.zones) {
            String zoneId = zone.getAttribute(zonesIdAttributeName).toString();
            this.zonesById.put(zoneId, zone);
            Envelope envelope = ((Geometry) (zone.getDefaultGeometry())).getEnvelopeInternal();
            this.zonesQt.insert(envelope, zone);
        }
    }

    public final void calculateSamplingPointsPerZoneFromFacilities(String facilitiesFilename, int numberOfPointsPerZone, Random r, ToDoubleFunction<ActivityFacility> weightFunction) throws IOException {
        // load facilities
        log.info("loading facilities from " + facilitiesFilename);

        Counter facCounter = new Counter("#");
        List<MyCalculateSkimMatricesWithAccessMode.WeightedCoord> facilities = new ArrayList<>();
        new MatsimFacilitiesReader(null, null, new StreamingFacilities(
                f -> {
                    facCounter.incCounter();
                    double weight = weightFunction.applyAsDouble(f);
                    MyCalculateSkimMatricesWithAccessMode.WeightedCoord wf = new MyCalculateSkimMatricesWithAccessMode.WeightedCoord(f.getCoord(), weight);
                    facilities.add(wf);
                }
        )).readFile(facilitiesFilename);
        facCounter.printCounter();

        selectSamplingPoints(facilities, numberOfPointsPerZone, r);
    }

    public final void calculateSamplingPointsPerZoneFromNetwork(String networkFilename, int numberOfPointsPerZone, Random r) throws IOException {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        log.info("loading network from " + networkFilename);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
        List<MyCalculateSkimMatricesWithAccessMode.WeightedCoord> weightedNodes = new ArrayList<>(scenario.getNetwork().getNodes().size());
        for (Node node : scenario.getNetwork().getNodes().values()) {
            weightedNodes.add(new MyCalculateSkimMatricesWithAccessMode.WeightedCoord(node.getCoord(), 1));
        }

        selectSamplingPoints(weightedNodes, numberOfPointsPerZone, r);
    }

    public final void selectSamplingPoints(List<MyCalculateSkimMatricesWithAccessMode.WeightedCoord> locations, int numberOfPointsPerZone, Random r) throws IOException {
        log.info("assign locations to zones...");
        Map<String, List<MyCalculateSkimMatricesWithAccessMode.WeightedCoord>> allCoordsPerZone = new HashMap<>();
        Counter counter = new Counter("# ");
        for (MyCalculateSkimMatricesWithAccessMode.WeightedCoord loc : locations) {
            counter.incCounter();
            String zoneId = findZone(loc.coord);
            if (zoneId != null) {
                allCoordsPerZone.computeIfAbsent(zoneId, k -> new ArrayList<>()).add(loc);
            }
        }
        counter.printCounter();

        // define points per zone
        log.info("choose locations (sampling points) per zone...");

        this.coordsPerZone = new HashMap<>();

        for (Map.Entry<String, List<MyCalculateSkimMatricesWithAccessMode.WeightedCoord>> e : allCoordsPerZone.entrySet()) {
            String zoneId = e.getKey();
            List<MyCalculateSkimMatricesWithAccessMode.WeightedCoord> zoneFacilities = e.getValue();
            double sumWeight = 0.0;
            for (MyCalculateSkimMatricesWithAccessMode.WeightedCoord loc : zoneFacilities) {
                sumWeight += loc.weight;
            }
            Coord[] coords = new Coord[numberOfPointsPerZone];
            for (int i = 0; i < numberOfPointsPerZone; i++) {
                double weight = r.nextDouble() * sumWeight;
                double sum = 0.0;
                MyCalculateSkimMatricesWithAccessMode.WeightedCoord chosenLoc = null;
                for (MyCalculateSkimMatricesWithAccessMode.WeightedCoord loc : zoneFacilities) {
                    sum += loc.weight;
                    if (weight <= sum) {
                        chosenLoc = loc;
                        break;
                    }
                }
                coords[i] = chosenLoc.coord;
            }
            this.coordsPerZone.put(zoneId, coords);
        }
        File coordFile = new File(this.outputDirectory, ZONE_LOCATIONS_FILENAME);
        writeSamplingPointsToFile(coordFile);
    }

    private void writeSamplingPointsToFile(File file) throws IOException {
        log.info("write chosen coordinates to file " + file.getAbsolutePath());
        try (BufferedWriter writer = IOUtils.getBufferedWriter(file.getAbsolutePath())) {
            writer.write("ZONE;POINT_INDEX;X;Y\n");
            for (Map.Entry<String, Coord[]> e : this.coordsPerZone.entrySet()) {
                String zoneId = e.getKey();
                Coord[] coords = e.getValue();
                for (int i = 0; i < coords.length; i++) {
                    Coord coord = coords[i];
                    writer.write(zoneId);
                    writer.write(";");
                    writer.write(Integer.toString(i));
                    writer.write(";");
                    writer.write(Double.toString(coord.getX()));
                    writer.write(";");
                    writer.write(Double.toString(coord.getY()));
                    writer.write("\n");
                }
            }
        }
    }

    public final void loadSamplingPointsFromFile(String filename) throws IOException {
        log.info("loading sampling points from " + filename);
        String expectedHeader = "ZONE,POINT_INDEX,X,Y";
        this.coordsPerZone = new HashMap<>();
        try (BufferedReader reader = IOUtils.getBufferedReader(filename)) {
            String header = reader.readLine();
            if (!expectedHeader.equals(header)) {
                throw new RuntimeException("Bad header, expected '" + expectedHeader + "', got: '" + header + "'.");
            }
            String line;
            int maxIdx = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = StringUtils.explode(line, ',');
                String zoneId = parts[0];
                int idx = Integer.parseInt(parts[1]);
                double x = Double.parseDouble(parts[2]);
                double y = Double.parseDouble(parts[3]);
                final int length = idx > maxIdx ? idx : maxIdx;
                Coord[] coords = this.coordsPerZone.computeIfAbsent(zoneId, k -> new Coord[length + 1]);
                if (coords.length < (idx + 1)) {
                    Coord[] tmp = new Coord[idx + 1];
                    System.arraycopy(coords, 0, tmp, 0, coords.length);
                    coords = tmp;
                    this.coordsPerZone.put(zoneId, coords);
                }
                coords[idx] = new Coord(x, y);
                if (idx > maxIdx) {
                    maxIdx = idx;
                }
            }
        }
    }

    public final void calculateBeelineMatrix() throws IOException {
        log.info("calc beeline distance matrix");
        MyFloatMatrix<String> beelineMatrix = MyBeelineDistanceMatrix.calculateBeelineDistanceMatrix(zonesById, coordsPerZone, numberOfThreads);

        log.info("write beeline distance matrix to " + outputDirectory);
        MyFloatMatrixIO.writeAsCSV(beelineMatrix, outputDirectory + "/" + BEELINE_DISTANCE_FILENAME);
    }

    public final void calculateNetworkMatrices(String networkFilename, String eventsFilename, double[] times, Config config, String outputPrefix, Predicate<Link> xy2linksPredicate) throws IOException {
        String prefix = outputPrefix == null ? "" : outputPrefix;
        Scenario scenario = ScenarioUtils.createScenario(config);
        log.info("loading network from " + networkFilename);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);

        TravelTime tt;
        if (eventsFilename != null) {
            log.info("extracting actual travel times from " + eventsFilename);
            TravelTimeCalculator ttc = TravelTimeCalculator.create(scenario.getNetwork(), config.travelTimeCalculator());
            EventsManager events = EventsUtils.createEventsManager();
            events.addHandler(ttc);
            new MatsimEventsReader(events).readFile(eventsFilename);
            tt = ttc.getLinkTravelTimes();
        } else {
            tt = new FreeSpeedTravelTime();
            log.info("No events specified. Travel Times will be calculated with free speed travel times.");
        }

        TravelDisutility td = new OnlyTimeDependentTravelDisutility(tt);

        log.info("extracting car-only network");
        final Network carNetwork = NetworkUtils.createNetwork();
        new TransportModeNetworkFilter(scenario.getNetwork()).filter(carNetwork, Collections.singleton(TransportMode.car));

        log.info("filter car-only network for assigning links to locations");
        final Network xy2linksNetwork = extractXy2LinksNetwork(carNetwork, xy2linksPredicate);

        log.info("calc CAR matrix for " + Time.writeTime(times[0]));
        MyNetworkSkimMatrices.NetworkIndicators<String> netIndicators = MyNetworkSkimMatrices.calculateSkimMatrices(
                xy2linksNetwork, carNetwork, zonesById, coordsPerZone, times[0], tt, td, this.numberOfThreads);

        if (tt instanceof FreeSpeedTravelTime) {
            log.info("Do not calculate CAR matrices for other times as only freespeed is being used");
        } else {
            for (int i = 1; i < times.length; i++) {
                log.info("calc CAR matrices for " + Time.writeTime(times[i]));
                MyNetworkSkimMatrices.NetworkIndicators<String> indicators2 = MyNetworkSkimMatrices.calculateSkimMatrices(
                        xy2linksNetwork, carNetwork, zonesById, coordsPerZone, times[i], tt, td, this.numberOfThreads);
                log.info("merge CAR matrices for " + Time.writeTime(times[i]));
                combineMatrices(netIndicators.travelTimeMatrix, indicators2.travelTimeMatrix);
                combineMatrices(netIndicators.distanceMatrix, indicators2.distanceMatrix);
            }
            log.info("re-scale CAR matrices after all data is merged.");
            netIndicators.travelTimeMatrix.multiply((float) (1.0 / times.length));
            netIndicators.distanceMatrix.multiply((float) (1.0 / times.length));
        }

        log.info("write CAR matrices to " + outputDirectory + (prefix.isEmpty() ? "" : (" with prefix " + prefix)));
        MyFloatMatrixIO.writeAsCSV(netIndicators.travelTimeMatrix, outputDirectory + "/" + prefix + CAR_TRAVELTIMES_FILENAME);
        MyFloatMatrixIO.writeAsCSV(netIndicators.distanceMatrix, outputDirectory + "/" + prefix + CAR_DISTANCES_FILENAME);
    }

    public final void calculateNetworkMatricesWithTolls(String networkFilename, String eventsFilename, double[] times, Config config, String outputPrefix, Predicate<Link> xy2linksPredicate) throws IOException {
        String prefix = outputPrefix == null ? "" : outputPrefix;
        Scenario scenario = ScenarioUtils.createScenario(config);
        log.info("loading network from " + networkFilename);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);

        TravelTime tt;
        if (eventsFilename != null) {
            log.info("extracting actual travel times from " + eventsFilename);
            TravelTimeCalculator ttc = TravelTimeCalculator.create(scenario.getNetwork(), config.travelTimeCalculator());
            EventsManager events = EventsUtils.createEventsManager();
            events.addHandler(ttc);
            new MatsimEventsReader(events).readFile(eventsFilename);
            tt = ttc.getLinkTravelTimes();
        } else {
            tt = new FreeSpeedTravelTime();
            log.info("No events specified. Travel Times will be calculated with free speed travel times.");
        }

        //change here to avoid tolls or not
        TravelDisutility td = TollUtils.getTravelDisutilityToAvoidTolls(tt);

        log.info("extracting car-only network");
        final Network carNetwork = NetworkUtils.createNetwork();
        new TransportModeNetworkFilter(scenario.getNetwork()).filter(carNetwork, Collections.singleton(TransportMode.car));

        log.info("filter car-only network for assigning links to locations");
        final Network xy2linksNetwork = extractXy2LinksNetwork(carNetwork, xy2linksPredicate);

        log.info("calc CAR matrix for " + Time.writeTime(times[0]));
        MyNetworkSkimMatrices.NetworkIndicators<String> netIndicators = MyNetworkSkimMatrices.calculateSkimMatrices(
                xy2linksNetwork, carNetwork, zonesById, coordsPerZone, times[0], tt, td, this.numberOfThreads);

        if (tt instanceof FreeSpeedTravelTime) {
            log.info("Do not calculate CAR matrices for other times as only freespeed is being used");
        } else {
            for (int i = 1; i < times.length; i++) {
                log.info("calc CAR matrices for " + Time.writeTime(times[i]));
                MyNetworkSkimMatrices.NetworkIndicators<String> indicators2 = MyNetworkSkimMatrices.calculateSkimMatrices(
                        xy2linksNetwork, carNetwork, zonesById, coordsPerZone, times[i], tt, td, this.numberOfThreads);
                log.info("merge CAR matrices for " + Time.writeTime(times[i]));
                combineMatrices(netIndicators.travelTimeMatrix, indicators2.travelTimeMatrix);
                combineMatrices(netIndicators.distanceMatrix, indicators2.distanceMatrix);
                combineMatrices(netIndicators.distanceMatrix, indicators2.tollDistanceMatrix);
            }
            log.info("re-scale CAR matrices after all data is merged.");
            netIndicators.travelTimeMatrix.multiply((float) (1.0 / times.length));
            netIndicators.distanceMatrix.multiply((float) (1.0 / times.length));
            netIndicators.tollDistanceMatrix.multiply((float) (1.0 / times.length));
        }

        log.info("write CAR matrices to " + outputDirectory + (prefix.isEmpty() ? "" : (" with prefix " + prefix)));
        MyFloatMatrixIO.writeAsCSV(netIndicators.travelTimeMatrix, outputDirectory + "/" + prefix + CAR_TRAVELTIMES_FILENAME);
        MyFloatMatrixIO.writeAsCSV(netIndicators.distanceMatrix, outputDirectory + "/" + prefix + CAR_DISTANCES_FILENAME);
        MyFloatMatrixIO.writeAsCSV(netIndicators.tollDistanceMatrix, outputDirectory + "/" + prefix + "toll_distances.csv.gz");

        //String omxFilePath = outputDirectory + "/" + "car_matrix.omx";
        //OmxMatrixWriter.createOmxFile(omxFilePath, zones.size());
        //createFloatOmxSkimMatrixFromFloatMatrix(netIndicators.tollDistanceMatrix, zones, omxFilePath, "tollDistance_m");
        //createFloatOmxSkimMatrixFromFloatMatrix(netIndicators.distanceMatrix, zones, omxFilePath, "distance_m");
        //createFloatOmxSkimMatrixFromFloatMatrix(netIndicators.travelTimeMatrix, zones, omxFilePath, "time_s");
    }

    public static Network extractXy2LinksNetwork(Network network, Predicate<Link> xy2linksPredicate) {
        Network xy2lNetwork = NetworkUtils.createNetwork();
        NetworkFactory nf = xy2lNetwork.getFactory();
        for (Link link : network.getLinks().values()) {
            if (xy2linksPredicate.test(link)) {
                // okay, we need that link
                Node fromNode = link.getFromNode();
                Node xy2lFromNode = xy2lNetwork.getNodes().get(fromNode.getId());
                if (xy2lFromNode == null) {
                    xy2lFromNode = nf.createNode(fromNode.getId(), fromNode.getCoord());
                    xy2lNetwork.addNode(xy2lFromNode);
                }
                Node toNode = link.getToNode();
                Node xy2lToNode = xy2lNetwork.getNodes().get(toNode.getId());
                if (xy2lToNode == null) {
                    xy2lToNode = nf.createNode(toNode.getId(), toNode.getCoord());
                    xy2lNetwork.addNode(xy2lToNode);
                }
                Link xy2lLink = nf.createLink(link.getId(), xy2lFromNode, xy2lToNode);
                xy2lLink.setAllowedModes(link.getAllowedModes());
                xy2lLink.setCapacity(link.getCapacity());
                xy2lLink.setFreespeed(link.getFreespeed());
                xy2lLink.setLength(link.getLength());
                xy2lLink.setNumberOfLanes(link.getNumberOfLanes());
                xy2lNetwork.addLink(xy2lLink);
            }
        }
        return xy2lNetwork;
    }

    public final void calculatePTMatrices(String networkFilename, String transitScheduleFilename, double startTime, double endTime, Config config, String outputPrefix, BiPredicate<TransitLine, TransitRoute> trainDetector) throws IOException {
        String prefix = outputPrefix == null ? "" : outputPrefix;
        Scenario scenario = ScenarioUtils.createScenario(config);
        log.info("loading schedule from " + transitScheduleFilename);
        new TransitScheduleReader(scenario).readFile(transitScheduleFilename);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);

        log.info("prepare PT Matrix calculation");
        RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(config);
        raptorConfig.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
        SwissRailRaptorData raptorData = SwissRailRaptorData.create(scenario.getTransitSchedule(), raptorConfig, scenario.getNetwork());
        RaptorParameters raptorParameters = RaptorUtils.createParameters(config);

        log.info("calc PT matrices for " + Time.writeTime(startTime) + " - " + Time.writeTime(endTime));
        MyPtSkimMatricesWithAccessMode.PtIndicators<String> matrices = MyPtSkimMatricesWithAccessMode.calculateSkimMatrices(
                raptorData, this.zonesById, this.coordsPerZone, startTime, endTime, 3600,
                raptorParameters, this.numberOfThreads, trainDetector,
                config, networkFilename, link -> true, transportMode);

        log.info("write PT matrices to " + outputDirectory + (prefix.isEmpty() ? "" : (" with prefix " + prefix)));
            MyFloatMatrixIO.writeAsCSV(matrices.adaptionTimeMatrix, outputDirectory + "/" + prefix + PT_ADAPTIONTIMES_FILENAME);
            MyFloatMatrixIO.writeAsCSV(matrices.frequencyMatrix, outputDirectory + "/" + prefix + PT_FREQUENCIES_FILENAME);
            MyFloatMatrixIO.writeAsCSV(matrices.distanceMatrix, outputDirectory + "/" + prefix + PT_DISTANCES_FILENAME);
            MyFloatMatrixIO.writeAsCSV(matrices.travelTimeMatrix, outputDirectory + "/" + prefix + PT_TRAVELTIMES_FILENAME);
            MyFloatMatrixIO.writeAsCSV(matrices.accessTimeMatrix, outputDirectory + "/" + prefix + PT_ACCESSTIMES_FILENAME);
            MyFloatMatrixIO.writeAsCSV(matrices.egressTimeMatrix, outputDirectory + "/" + prefix + PT_EGRESSTIMES_FILENAME);
            MyFloatMatrixIO.writeAsCSV(matrices.transferCountMatrix, outputDirectory + "/" + prefix + PT_TRANSFERCOUNTS_FILENAME);
            MyFloatMatrixIO.writeAsCSV(matrices.trainTravelTimeShareMatrix, outputDirectory + "/" + prefix + PT_TRAINSHARE_BYTIME_FILENAME);
            MyFloatMatrixIO.writeAsCSV(matrices.trainDistanceShareMatrix, outputDirectory + "/" + prefix + PT_TRAINSHARE_BYDISTANCE_FILENAME);

        //String omxFilePath = outputDirectory + "/" + prefix + "matrices.omx";
        //OmxMatrixWriter.createOmxFile(omxFilePath, zones.size());

        //createFloatOmxSkimMatrixFromFloatMatrix(matrices.adaptionTimeMatrix, zones, omxFilePath, "adaption_time_s");
        //createFloatOmxSkimMatrixFromFloatMatrix(matrices.frequencyMatrix, zones, omxFilePath, "frequency");
        //createFloatOmxSkimMatrixFromFloatMatrix(matrices.distanceMatrix, zones, omxFilePath, "distance_m");
        //createFloatOmxSkimMatrixFromFloatMatrix(matrices.travelTimeMatrix, zones, omxFilePath, "travel_time_s");
        //createFloatOmxSkimMatrixFromFloatMatrix(matrices.accessTimeMatrix, zones, omxFilePath, "access_time_s");
        //createFloatOmxSkimMatrixFromFloatMatrix(matrices.egressTimeMatrix, zones, omxFilePath, "egress_time_s");
        //createFloatOmxSkimMatrixFromFloatMatrix(matrices.transferCountMatrix, zones, omxFilePath, "transfer_count");
        //createFloatOmxSkimMatrixFromFloatMatrix(matrices.trainTravelTimeShareMatrix, zones, omxFilePath, "train_time_share");
        //createFloatOmxSkimMatrixFromFloatMatrix(matrices.trainDistanceShareMatrix, zones, omxFilePath, "train_distance_share");
        //createFloatOmxSkimMatrixFromFloatMatrix(matrices.inVehicleTimeMatrix, zones, omxFilePath, "in_vehicle_time_s");
        //createFloatOmxSkimMatrixFromFloatMatrix(matrices.accessDistanceMatrix, zones, omxFilePath, "access_distance_m");
        //createFloatOmxSkimMatrixFromFloatMatrix(matrices.egressDistanceMatrix, zones, omxFilePath, "egress_distance_m");

        writeCoordinatesOfAccess(matrices.coordinatesOfAccessStation, outputDirectory + "/" + prefix + PT_ACCESS_STATION_COORDINATES);

    }

    private static void writeCoordinatesOfAccess(Map<String, Map<String, Coord>> coordinatesOfAccessStation, String filename) {
        String NL = "\n";
        String HEADER = "FROM,TO,X_ACCESS,Y_ACCESS";
        String SEP = ",";

        try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
            writer.write(HEADER);
            writer.write(NL);
            String[] zoneIds = coordinatesOfAccessStation.keySet().toArray(new String[0]);
            for (String fromZoneId : zoneIds) {
                for (String toZoneId : zoneIds) {
                    writer.write(fromZoneId);
                    writer.append(SEP);
                    writer.write(toZoneId);
                    writer.append(SEP);
                    Coord coord = coordinatesOfAccessStation.get(fromZoneId).get(toZoneId);
                    if (coord != null) {
                        writer.write(String.valueOf(Math.round(coord.getX())));
                        writer.append(SEP);
                        writer.write(String.valueOf(Math.round(coord.getY())));
                        writer.append(NL);
                    } else {
                        writer.write("-1");
                        writer.append(SEP);
                        writer.write("-1");
                        writer.append(NL);
                    }
                }
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void createIntOmxSkimMatrixFromFloatMatrix(MyFloatMatrix<String> matrix, Collection<SimpleFeature> zones, String omxFilePath, String name) {
        try (OmxFile omxFile = new OmxFile(omxFilePath)) {
            omxFile.openReadWrite();
            int mat1NA = -1;

            int dimension = zones.size();

            int[][] array = new int[dimension][dimension];
            int[] indices = new int[dimension];
            Map<String, Integer> id2index = matrix.id2index;

            for (String origin : id2index.keySet()) {
                try {
                    indices[id2index.get(origin)] = Integer.parseInt(origin);
                } catch (NumberFormatException e) {
                    System.out.println("Conversion to omx only works with zone integer IDs");
                }
                for (String destination : id2index.keySet()) {
                    array[id2index.get(origin)][id2index.get(destination)] = (int) matrix.get(origin, destination);
                }
            }

            OmxLookup lookup = new OmxLookup.OmxIntLookup("zone", indices, -1);
            OmxMatrix.OmxIntMatrix mat1 = new OmxMatrix.OmxIntMatrix(name, array, mat1NA);
            mat1.setAttribute(OmxConstants.OmxNames.OMX_DATASET_TITLE_KEY.getKey(), "skim_matrix");
            omxFile.addMatrix(mat1);
            omxFile.addLookup(lookup);
            omxFile.save();
            System.out.println(omxFile.summary());
            omxFile.close();
            System.out.println(name + "matrix written");
        }
    }

    private static void createFloatOmxSkimMatrixFromFloatMatrix(MyFloatMatrix<String> matrix, Collection<SimpleFeature> zones, String omxFilePath, String name) {
        try (OmxFile omxFile = new OmxFile(omxFilePath)) {
            omxFile.openReadWrite();
            float mat1NA = -1;

            int dimension = zones.size();

            float[][] array = new float[dimension][dimension];
            int[] indices = new int[dimension];
            Map<String, Integer> id2index = matrix.id2index;

            for (String origin : id2index.keySet()) {
                try {
                    indices[id2index.get(origin)] = Integer.parseInt(origin);
                } catch (NumberFormatException e) {
                    System.out.println("Conversion to omx only works with zone integer IDs");
                }
                for (String destination : id2index.keySet()) {
                    array[id2index.get(origin)][id2index.get(destination)] = matrix.get(origin, destination);
                }
            }

            OmxLookup lookup = new OmxLookup.OmxIntLookup("zone", indices, -1);
            OmxMatrix.OmxFloatMatrix mat1 = new OmxMatrix.OmxFloatMatrix(name, array, mat1NA);
            mat1.setAttribute(OmxConstants.OmxNames.OMX_DATASET_TITLE_KEY.getKey(), "skim_matrix");
            omxFile.addMatrix(mat1);
            omxFile.addLookup(lookup);
            omxFile.save();
            System.out.println(omxFile.summary());
            omxFile.close();
            System.out.println(name + "matrix written");
        }
    }


    private static <T> void combineMatrices(MyFloatMatrix<T> matrix1, MyFloatMatrix<T> matrix2) {
        Set<T> ids = matrix2.id2index.keySet();
        for (T fromId : ids) {
            for (T toId : ids) {
                float value2 = matrix2.get(fromId, toId);
                matrix1.add(fromId, toId, value2);
            }
        }
    }

    private String findZone(Coord coord) {
        Point pt = GEOMETRY_FACTORY.createPoint(new Coordinate(coord.getX(), coord.getY()));
        List elements = this.zonesQt.query(pt.getEnvelopeInternal());
        for (Object o : elements) {
            SimpleFeature z = (SimpleFeature) o;
            if (((Geometry) z.getDefaultGeometry()).intersects(pt)) {
                return z.getAttribute(this.zonesIdAttributeName).toString();
            }
        }
        return null;
    }

    private static class WeightedCoord {
        Coord coord;
        double weight;

        private WeightedCoord(Coord coord, double weight) {
            this.coord = coord;
            this.weight = weight;
        }
    }

    public static void main(String[] args) throws IOException {
        String zonesShapeFilename = args[0];
        String zonesIdAttributeName = args[1];
        String facilitiesFilename = args[2];
        String networkFilename = args[3];
        String transitScheduleFilename = args[4];
        String eventsFilename = args[5];
        String outputDirectory = args[6];
        int numberOfPointsPerZone = Integer.parseInt(args[7]);
        int numberOfThreads = Integer.parseInt(args[8]);
        String[] timesCarStr = args[9].split(";");
        String[] timesPtStr = args[10].split(";");
        Set<String> modes = CollectionUtils.stringToSet(args[11]);

        double[] timesCar = new double[timesCarStr.length];
        for (int i = 0; i < timesCarStr.length; i++)
            timesCar[i] = Time.parseTime(timesCarStr[i]);

        double[] timesPt = new double[timesPtStr.length];
        for (int i = 0; i < timesPtStr.length; i++)
            timesPt[i] = Time.parseTime(timesPtStr[i]);

        Config config = ConfigUtils.createConfig();
        Random r = new Random(4711);

        MyCalculateSkimMatricesWithAccessMode skims = new MyCalculateSkimMatricesWithAccessMode(zonesShapeFilename, zonesIdAttributeName, outputDirectory, numberOfThreads, TransportMode.car);
        skims.calculateSamplingPointsPerZoneFromFacilities(facilitiesFilename, numberOfPointsPerZone, r, f -> 1);
        // alternative if you don't have facilities, use the network:
        // skims.calculateSamplingPointsPerZoneFromNetwork(networkFilename, numberOfPointsPerZone, r);

        if (modes.contains(TransportMode.car)) {
            skims.calculateNetworkMatrices(networkFilename, eventsFilename, timesCar, config, null, l -> true);
        }

        if (modes.contains(TransportMode.pt)) {
            skims.calculatePTMatrices(networkFilename, transitScheduleFilename, timesPt[0], timesPt[1], config, null, (line, route) -> route.getTransportMode().equals("train"));
        }

        skims.calculateBeelineMatrix();
    }

}
