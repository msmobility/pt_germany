import ch.sbb.matsim.analysis.skims.CalculateSkimMatrices;
import ch.sbb.matsim.analysis.skims.FloatMatrixIO;
import ch.sbb.matsim.analysis.skims.PTSkimMatrices;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import java.io.IOException;
import java.util.Random;
import java.util.function.BiPredicate;


public class SkimCalculator {

    private static Logger log = Logger.getLogger(SkimCalculator.class);

    public static void main(String[] args) throws IOException {
        String zonesShapeFilename = "./input/zones/zones.shp";
        String zonesIdAttributeName = "id";
        String outputDirectory = "./output/skims/db";
        Config config = ConfigUtils.loadConfig("./sbbConfig.xml");
        String networkFilename = "./output/db/network_muc_merged.xml.gz";
        String transitScheduleFilename = "./output/db/mapped_muc_schedule.xml";


        CalculateSkimMatrices skims = new CalculateSkimMatrices(zonesShapeFilename, zonesIdAttributeName, outputDirectory, 16);
        //skims.calculateSamplingPointsPerZoneFromFacilities(facilitiesFilename, numberOfPointsPerZone, r, facility -> 1.0);
        // alternative if you don't have facilities:
        skims.calculateSamplingPointsPerZoneFromNetwork(networkFilename, 1, new Random(0));
        //skims.calculateNetworkMatrices(networkFilename, eventsFilename, timesCar, config, null, link -> true);
        skims.calculatePTMatrices(networkFilename, transitScheduleFilename, 8*60*60, 9*60*60, config, null, (line, route) -> isRailTramOrSubway(route));

        //skims.calculateBeelineMatrix();


    }

    /*
    Use this for mito mode == tramMetro, to find when these modes are not used.
     */
    private static boolean isRailTramOrSubway(TransitRoute route) {
        if (route.getTransportMode().equalsIgnoreCase("rail") ||
                route.getTransportMode().equalsIgnoreCase("tram") ||
                route.getTransportMode().equalsIgnoreCase("subway")) {
            return true;
        } else {
            return false;
        }
    }

    /*
    Use this for mito mode == train, to find when train is not used
     */
    private static boolean isRail(TransitRoute route) {
        if (route.getTransportMode().equalsIgnoreCase("rail")) {
            return true;
        } else {
            return false;
        }
    }


//    void calculateMyPtMAtrices (String networkFilename, String transitScheduleFilename, double startTime, double endTime, Config config, String outputPrefix, BiPredicate< TransitLine, TransitRoute> trainDetector) throws IOException {
//            String prefix = outputPrefix == null ? "" : outputPrefix;
//            Scenario scenario = ScenarioUtils.createScenario(config);
//            log.info("loading schedule from " + transitScheduleFilename);
//            new TransitScheduleReader(scenario).readFile(transitScheduleFilename);
//            new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
//
//            log.info("prepare PT Matrix calculation");
//            RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(config);
//            raptorConfig.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
//            SwissRailRaptorData raptorData = SwissRailRaptorData.create(scenario.getTransitSchedule(), raptorConfig, scenario.getNetwork());
//            RaptorParameters raptorParameters = RaptorUtils.createParameters(config);
//
//
//            log.info("calc PT matrices for " + Time.writeTime(startTime) + " - " + Time.writeTime(endTime));
//            PTSkimMatrices.PtIndicators<String> matrices = PTSkimMatrices.calculateSkimMatrices(
//                    raptorData, skims.zonesById, this.coordsPerZone, startTime, endTime, 120, raptorParameters, this.numberOfThreads, trainDetector);
////
////            log.info("write PT matrices to " + outputDirectory + (prefix.isEmpty() ? "" : (" with prefix " + prefix)));
////            FloatMatrixIO.writeAsCSV(matrices.adaptionTimeMatrix, outputDirectory + "/" + prefix + PT_ADAPTIONTIMES_FILENAME);
////            FloatMatrixIO.writeAsCSV(matrices.frequencyMatrix, outputDirectory + "/" + prefix + PT_FREQUENCIES_FILENAME);
////            FloatMatrixIO.writeAsCSV(matrices.distanceMatrix, outputDirectory + "/" + prefix + PT_DISTANCES_FILENAME);
////            FloatMatrixIO.writeAsCSV(matrices.travelTimeMatrix, outputDirectory + "/" + prefix + PT_TRAVELTIMES_FILENAME);
////            FloatMatrixIO.writeAsCSV(matrices.accessTimeMatrix, outputDirectory + "/" + prefix + PT_ACCESSTIMES_FILENAME);
////            FloatMatrixIO.writeAsCSV(matrices.egressTimeMatrix, outputDirectory + "/" + prefix + PT_EGRESSTIMES_FILENAME);
////            FloatMatrixIO.writeAsCSV(matrices.transferCountMatrix, outputDirectory + "/" + prefix + PT_TRANSFERCOUNTS_FILENAME);
////            FloatMatrixIO.writeAsCSV(matrices.trainTravelTimeShareMatrix, outputDirectory + "/" + prefix + PT_TRAINSHARE_BYTIME_FILENAME);
////            FloatMatrixIO.writeAsCSV(matrices.trainDistanceShareMatrix, outputDirectory + "/" + prefix + PT_TRAINSHARE_BYDISTANCE_FILENAME);
//        }
//    }


}
