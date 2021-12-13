package transitSchedule;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.*;
import skimCalculator.MyCalculateSkimMatrices;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class CheckTransitRouteShape {

    private static final Logger log = Logger.getLogger(MyCalculateSkimMatrices.class);
    private static final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
    private static TransitScheduleReaderV1 transitScheduleReader;
    private static final String transitScheduleFile = "./input/transitSchedule/unmapped_schedule_modified_v1.xml";
    private static final String outputCsvFile = "./input/transitSchedule/linesInfoWithDist_new_v2.csv";
    private static int numScannedStops = 0;
    private static int numScannedLines = 0;
    private static int numScannedRoutes = 0;

    public static void main(String[] args) throws FileNotFoundException {

        readTransitSchedule();
        analyzeTransitRoute();

    }

    private static void analyzeTransitRoute() throws FileNotFoundException {

        TransitSchedule transitSchedule = scenario.getTransitSchedule();

        PrintWriter pw = new PrintWriter(outputCsvFile);
        pw.println("line,route,mode,seq,stop_id,stop_x,stop_y," +
                "distPrevious2Current,distPrevious2Next,speedPrevious2Current,speedPrevious2Next," +
                "suspicious,inspected,offRatio,offDiff,speedRatio");

        log.info("Starting calculating.");

        Map<Id<TransitLine>, TransitLine> transitLineMap = transitSchedule.getTransitLines();

        for (Map.Entry<Id<TransitLine>, TransitLine> transitLines : transitLineMap.entrySet()) {

            Map<Id<TransitRoute>, TransitRoute> transitRouteMap = transitSchedule.getTransitLines().get(transitLines.getKey()).getRoutes();

            for (Map.Entry<Id<TransitRoute>, TransitRoute> transitRoutes : transitRouteMap.entrySet()) {

                List<TransitRouteStop> transitStopFacilityMap = transitRouteMap.get(transitRoutes.getKey()).getStops();

                String mode = transitRouteMap.get(transitRoutes.getKey()).getTransportMode();
                if (transitStopFacilityMap.size() < 2) {
                    log.info("Line:" + transitLines);
                    log.info("Route: " + transitRoutes);
                    log.info("has " + transitStopFacilityMap.size() + " stops.");
                }

                for (int i = 0; i <= transitStopFacilityMap.size() - 1; i++) {

                    TransitStopFacility currentStop = transitStopFacilityMap.get(i).getStopFacility();
                    TransitStopFacility previousStop;
                    TransitStopFacility nextStop;

                    Coord coordCurrentStop = currentStop.getCoord();
                    Coord coordPreviousStop;
                    Coord coordNextStop;

                    double distPrevious2Current_m = 0.01;
                    double distPrevious2Next_m = 0.01;

                    double timeCurrentStop_sec = transitStopFacilityMap.get(i).getDepartureOffset();
                    double timePreviousStop_sec;
                    double timeNextStop_sec;

                    double speedPrevious2Current = 0.01;
                    double speedPrevious2Next = 0.01;

                    boolean isSuspiciousStop = false;
                    boolean isInspectedStop = false;

                    if (i > 0 && i < transitStopFacilityMap.size() - 1) {
                        previousStop = transitStopFacilityMap.get(i - 1).getStopFacility();
                        nextStop = transitStopFacilityMap.get(i + 1).getStopFacility();
                        coordPreviousStop = previousStop.getCoord();
                        coordNextStop = nextStop.getCoord();
                        distPrevious2Current_m = CoordUtils.calcEuclideanDistance(coordPreviousStop, coordCurrentStop);
                        distPrevious2Next_m = CoordUtils.calcEuclideanDistance(coordPreviousStop, coordNextStop);
                        timePreviousStop_sec = transitStopFacilityMap.get(i - 1).getDepartureOffset();
                        timeNextStop_sec = transitStopFacilityMap.get(i + 1).getArrivalOffset();
                        speedPrevious2Current = (distPrevious2Current_m / (timeCurrentStop_sec - timePreviousStop_sec)) * 3.6;
                        speedPrevious2Next = (distPrevious2Next_m / (timeNextStop_sec - timePreviousStop_sec)) * 3.6;

                        //Todo Check this one first ansd see whether the threshold for speed is reasonable or not
                        isSuspiciousStop = (((distPrevious2Current_m - distPrevious2Next_m) >= (10 * 1000)) &&
                                (distPrevious2Current_m / distPrevious2Next_m >= 3));

                        //Todo Check this one second
//                        isSuspiciousStop = (((distPrevious2Current_m - distPrevious2Next_m) >= (10 * 1000)) &&
//                                (distPrevious2Current_m / distPrevious2Next_m >= 3)) &&
//                                (speedPrevious2Current / speedPrevious2Next >= 1.5);
                        isInspectedStop = true;
                    } else if (i == transitStopFacilityMap.size() - 1) {
                        previousStop = transitStopFacilityMap.get(i - 1).getStopFacility();
                        coordPreviousStop = previousStop.getCoord();
                        distPrevious2Current_m = CoordUtils.calcEuclideanDistance(coordPreviousStop, coordCurrentStop);
                        timePreviousStop_sec = transitStopFacilityMap.get(i - 1).getDepartureOffset();
                        speedPrevious2Current = (distPrevious2Current_m / (timeCurrentStop_sec - timePreviousStop_sec)) * 3.6;
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("\"").append(transitLines.getKey().toString()).append("\"").append(",");
                    sb.append("\"").append(transitRoutes.getKey().toString()).append("\"").append(",");
                    sb.append(mode).append(",");
                    sb.append(i + 1).append(",");
                    sb.append("\"").append(transitStopFacilityMap.get(i).getStopFacility().getId().toString()).append("\"").append(",");
                    sb.append(transitStopFacilityMap.get(i).getStopFacility().getCoord().getX()).append(",");
                    sb.append(transitStopFacilityMap.get(i).getStopFacility().getCoord().getY()).append(",");
                    sb.append(distPrevious2Current_m).append(",");
                    sb.append(distPrevious2Next_m).append(",");
                    sb.append(speedPrevious2Current).append(",");
                    sb.append(speedPrevious2Next).append(",");
                    sb.append(isSuspiciousStop).append(",");
                    sb.append(isInspectedStop).append(",");
                    sb.append(distPrevious2Current_m / distPrevious2Next_m).append(",");
                    sb.append(distPrevious2Current_m - distPrevious2Next_m).append(",");
                    sb.append(speedPrevious2Current / speedPrevious2Next);
                    pw.println(sb);

                }
                numScannedStops += transitStopFacilityMap.size();
            }
            numScannedRoutes += transitRouteMap.size();
        }
        numScannedLines += transitLineMap.size();
        pw.close();
        log.info("Calculating completed.");
        log.info(numScannedLines + " lines are scanned.");
        log.info(numScannedRoutes + " routes are scanned.");
        log.info(numScannedStops + " stops are scanned.");
    }

    private static void readTransitSchedule() {
        log.info("Starting reading transit schedule.");
        transitScheduleReader = new TransitScheduleReaderV1(scenario);
        transitScheduleReader.readFile(transitScheduleFile);
        log.info("Transit schedule read completely: " + transitScheduleFile);
    }
}
