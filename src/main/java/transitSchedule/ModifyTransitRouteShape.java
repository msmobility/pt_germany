package transitSchedule;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.*;
import skimCalculator.MyCalculateSkimMatrices;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyTransitRouteShape {


    private static final Logger log = Logger.getLogger(MyCalculateSkimMatrices.class);
    private static final Config config = ConfigUtils.createConfig();
    private static final Scenario scenario = ScenarioUtils.createScenario(config);

    private static final String transitScheduleFile = "./input/transitSchedule/mapped_schedule.xml";

    private static final String repairListPath = "./input/transitSchedule/repairList.csv";
    private static final String outputTransitScheduleFile = "./input/transitSchedule/unmapped_schedule_modified_v1.xml";

    private static int numModifiedStops = 0;
    private static int numScannedStops = 0;
    private static int numScannedLines = 0;
    private static int numScannedRoutes = 0;
    private static int newStopId = 1;

    private static TransitSchedule transitSchedule;
    private static TransitSchedule newTransitSchedule;

    private static Map<Id<TransitStopFacility>, Map<Boolean, Integer>> repairList = new HashMap<>();

    public static void main(String[] args) throws FileNotFoundException {
        readTransitSchedule();
        scanTransitStopFacilities();
        writeRepairList();
        repairTransitSchedule();
        generateUnmappedTransitSchedule();
        writeTransitSchedule();
    }

    private static void generateUnmappedTransitSchedule() {

        TransitScheduleFactory transitScheduleFactory = new TransitScheduleFactoryImpl();
        newTransitSchedule = transitScheduleFactory.createTransitSchedule();

        Map<Id<TransitStopFacility>, TransitStopFacility> transitStopMap = transitSchedule.getFacilities();

        for (Map.Entry<Id<TransitStopFacility>, TransitStopFacility> entryStop : transitStopMap.entrySet()) {
            String originalStopId = entryStop.getValue().getId().toString().split("link")[0];
            String cleanedStopId = originalStopId.substring(0, originalStopId.length() - 1);
            Id<TransitStopFacility> newStopId = Id.create(cleanedStopId, TransitStopFacility.class);
            Coord stopCoord = entryStop.getValue().getCoord();
            TransitStopFacility newStop = transitScheduleFactory.createTransitStopFacility(newStopId, stopCoord, false);
            newTransitSchedule.addStopFacility(newStop);
        }

        Map<Id<TransitLine>, TransitLine> transitLineMap = transitSchedule.getTransitLines();

        for (Map.Entry<Id<TransitLine>, TransitLine> entryLine : transitLineMap.entrySet()) {

            String originalLineId = entryLine.getValue().getId().toString();
            Id<TransitLine> newLineId = Id.create(originalLineId, TransitLine.class);
            TransitLine newLine = transitScheduleFactory.createTransitLine(newLineId);

            Map<Id<TransitRoute>, TransitRoute> transitRouteMap = entryLine.getValue().getRoutes();

            for (Map.Entry<Id<TransitRoute>, TransitRoute> entryRoute : transitRouteMap.entrySet()) {
                String originalRouteId = entryRoute.getValue().getId().toString();
                Id<TransitRoute> newRouteId = Id.create(originalRouteId, TransitRoute.class);

                List<TransitRouteStop> transitRouteStopList = entryRoute.getValue().getStops();
                List<TransitRouteStop> newTransitRouteStopList = new ArrayList<>();
                for (int i = 0; i < transitRouteStopList.size(); i++) {

                    String originalRouteStopId = transitRouteStopList.get(i).getStopFacility().getId().toString().split("link")[0];
                    String cleanedRouteStopId = originalRouteStopId.substring(0, originalRouteStopId.length() - 1);
                    Id<TransitStopFacility> newRouteStopId = Id.create(cleanedRouteStopId, TransitStopFacility.class);
                    double arrivalTime = transitRouteStopList.get(i).getArrivalOffset();
                    double departureTime = transitRouteStopList.get(i).getDepartureOffset();
                    TransitStopFacility stopFacility = newTransitSchedule.getFacilities().get(newRouteStopId);
                    TransitRouteStop newRouteStop = transitScheduleFactory.createTransitRouteStop(stopFacility, arrivalTime, departureTime);
                    newTransitRouteStopList.add(i, newRouteStop);
                }

                TransitRoute newRoute = transitScheduleFactory.createTransitRoute(newRouteId, null, newTransitRouteStopList, newRouteId.toString());
                newRoute.setTransportMode(entryRoute.getValue().getTransportMode());

                Map<Id<Departure>, Departure> departuresMap = entryRoute.getValue().getDepartures();
                for (Map.Entry<Id<Departure>, Departure> entryDeparture : departuresMap.entrySet()) {
                    newRoute.addDeparture(entryDeparture.getValue());
                }
                newLine.addRoute(newRoute);
            }
            newTransitSchedule.addTransitLine(newLine);
        }
    }

    private static void repairTransitSchedule() {

        log.info("Start repairing transit stop facilities.");

        Map<Id<TransitLine>, TransitLine> transitLineMap = transitSchedule.getTransitLines();

        for (Map.Entry<Id<TransitLine>, TransitLine> entry : transitLineMap.entrySet()) {

            Map<Id<TransitRoute>, TransitRoute> transitRouteMap = entry.getValue().getRoutes();

            for (Map.Entry<Id<TransitRoute>, TransitRoute> entryRoute : transitRouteMap.entrySet()) {

                List<TransitRouteStop> transitStopFacilityMap = entryRoute.getValue().getStops();

                String mode = entryRoute.getValue().getTransportMode();

                if (mode.equals("bus") || mode.equals("tram")) {

                    for (int i = 1; i < transitStopFacilityMap.size() - 1; i++) {

                        TransitStopFacility currentStop = transitStopFacilityMap.get(i).getStopFacility();
                        TransitStopFacility previousStop = transitStopFacilityMap.get(i - 1).getStopFacility();
                        TransitStopFacility nextStop = transitStopFacilityMap.get(i + 1).getStopFacility();

                        Coord coordCurrentStop = currentStop.getCoord();
                        Coord coordPreviousStop = previousStop.getCoord();
                        Coord coordNextStop = nextStop.getCoord();

                        double distPrevious2Current_m = CoordUtils.calcEuclideanDistance(coordPreviousStop, coordCurrentStop);
                        double distPrevious2Next_m = CoordUtils.calcEuclideanDistance(coordPreviousStop, coordNextStop);

                        double timeCurrentStop_sec = transitStopFacilityMap.get(i).getDepartureOffset();
                        double timePreviousStop_sec = transitStopFacilityMap.get(i - 1).getDepartureOffset();
                        double timeNextStop_sec = transitStopFacilityMap.get(i + 1).getArrivalOffset();

                        double speedPrevious2Current = distPrevious2Current_m / (timeCurrentStop_sec - timePreviousStop_sec);
                        double speedPrevious2Next = distPrevious2Next_m / (timeNextStop_sec - timePreviousStop_sec);

                        boolean suspiciousStops = (((distPrevious2Current_m - distPrevious2Next_m) >= (10 * 1000)) &&
                                (distPrevious2Current_m / distPrevious2Next_m >= 3));

                        if (suspiciousStops) {

                            double newX = (coordPreviousStop.getX() + coordNextStop.getX()) / 2;
                            double newY = (coordPreviousStop.getY() + coordNextStop.getY()) / 2;
                            Coord newCoord = new Coord(newX, newY);

                            if (repairList.get(currentStop.getId()).get(Boolean.TRUE) > 0 &&
                                    repairList.get(currentStop.getId()).get(Boolean.FALSE) == 0) {

                                //Stop facality is wrong for all routes
                                currentStop.setCoord(newCoord);

                                log.info("Line: " + entry +
                                        "\n Route: " + entryRoute +
                                        "\n Stop id: " + currentStop.getId().toString() + " has been modified.");

                                numModifiedStops = numModifiedStops + 1;

                            } else if (repairList.get(currentStop.getId()).get(Boolean.TRUE) > 0 &&
                                    repairList.get(currentStop.getId()).get(Boolean.FALSE) > 0) {

                                Id<TransitStopFacility> newTransitStopFacilityId = Id.create("newStop_" + newStopId + ".link:pt_" + "newStop_" + newStopId, TransitStopFacility.class);
                                TransitScheduleFactory transitScheduleFactory = new TransitScheduleFactoryImpl();
                                TransitStopFacility newTransitStopFacility = transitScheduleFactory.createTransitStopFacility(newTransitStopFacilityId, newCoord, false);
                                transitSchedule.addStopFacility(newTransitStopFacility);

                                entryRoute.getValue().getStops().get(i).setStopFacility(newTransitStopFacility);

                                //Todo also added for stops on the same line/route, think on how to do this

                                newStopId += 1;
                            }
                        }
                    }
                }
            }
        }
        log.info("Modification completed: " + numModifiedStops + " stops are modified after this run.");
        log.info("New transit stop facility added: " + newStopId + " stops are added after this run.");
    }


    private static void scanTransitStopFacilities() {

        log.info("Start scanning transit stop facilities.");

        Map<Id<TransitLine>, TransitLine> transitLineMap = transitSchedule.getTransitLines();

        for (Map.Entry<Id<TransitLine>, TransitLine> entry : transitLineMap.entrySet()) {

            Map<Id<TransitRoute>, TransitRoute> transitRouteMap = entry.getValue().getRoutes();

            for (Map.Entry<Id<TransitRoute>, TransitRoute> entryRoute : transitRouteMap.entrySet()) {

                List<TransitRouteStop> transitStopFacilityMap = entryRoute.getValue().getStops();

                for (int i = 0; i < transitStopFacilityMap.size(); i++) {

                    TransitStopFacility currentStop = transitStopFacilityMap.get(i).getStopFacility();
                    Coord coordCurrentStop = currentStop.getCoord();
                    boolean suspiciousStops = false;
                    if (i != 0 && i != transitStopFacilityMap.size() - 1) {
                        TransitStopFacility previousStop = transitStopFacilityMap.get(i - 1).getStopFacility();
                        TransitStopFacility nextStop = transitStopFacilityMap.get(i + 1).getStopFacility();

                        Coord coordPreviousStop = previousStop.getCoord();
                        Coord coordNextStop = nextStop.getCoord();

                        double distPrevious2Current_m = CoordUtils.calcEuclideanDistance(coordPreviousStop, coordCurrentStop);
                        double distPrevious2Next_m = CoordUtils.calcEuclideanDistance(coordPreviousStop, coordNextStop);

                        double timeCurrentStop_sec = transitStopFacilityMap.get(i).getDepartureOffset();
                        double timePreviousStop_sec = transitStopFacilityMap.get(i - 1).getDepartureOffset();
                        double timeNextStop_sec = transitStopFacilityMap.get(i + 1).getArrivalOffset();

                        double speedPrevious2Current = distPrevious2Current_m / (timeCurrentStop_sec - timePreviousStop_sec);
                        double speedPrevious2Next = distPrevious2Next_m / (timeNextStop_sec - timePreviousStop_sec);
                        suspiciousStops = (((distPrevious2Current_m - distPrevious2Next_m) >= (10 * 1000)) &&
                                (distPrevious2Current_m / distPrevious2Next_m >= 3));

                    }

                    repairList.putIfAbsent(currentStop.getId(), new HashMap<>());
                    repairList.get(currentStop.getId()).putIfAbsent(Boolean.TRUE, 0);
                    repairList.get(currentStop.getId()).putIfAbsent(Boolean.FALSE, 0);

                    if (suspiciousStops) {
                        int originalCount = repairList.get(currentStop.getId()).get(Boolean.TRUE);
                        repairList.get(currentStop.getId()).put(Boolean.TRUE, originalCount + 1);
                    } else {
                        int originalCount = repairList.get(currentStop.getId()).get(Boolean.FALSE);
                        repairList.get(currentStop.getId()).put(Boolean.FALSE, originalCount + 1);
                    }
                    numScannedStops += 1;
                }
                numScannedRoutes += 1;
            }
            numScannedLines += 1;
        }
        log.info(numScannedLines + " lines are scanned.");
        log.info(numScannedRoutes + " routes are scanned.");
        log.info(numScannedStops + " stops are scanned.");
    }

    private static void readTransitSchedule() {
        log.info("Start reading transit schedule.");
        TransitScheduleReaderV1 transitScheduleReader = new TransitScheduleReaderV1(scenario);
        transitScheduleReader.readFile(transitScheduleFile);
        log.info("Transit schedule is read completely.");
        transitSchedule = scenario.getTransitSchedule();
    }

    private static void writeTransitSchedule() {
        log.info("start writing transit schedule.");
        TransitScheduleWriterV1 transitScheduleWriter = new TransitScheduleWriterV1(newTransitSchedule);
        transitScheduleWriter.write(outputTransitScheduleFile);
        log.info("Transit schedule is written completely.");
    }

    private static void writeRepairList() throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(repairListPath);
        pw.println("stopId,misplacedStop,okayStop");
        for (Map.Entry<Id<TransitStopFacility>, Map<Boolean, Integer>> stop : repairList.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(stop.getKey().toString()).append(",");
            sb.append(stop.getValue().get(Boolean.TRUE)).append(",");
            sb.append(stop.getValue().get(Boolean.FALSE));
            pw.println(sb);
        }
        pw.close();
    }

}
