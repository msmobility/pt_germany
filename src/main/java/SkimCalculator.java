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
import skimCalculator.MyCalculateSkimMatrices;

import java.io.IOException;
import java.util.Random;
import java.util.function.BiPredicate;


public class SkimCalculator {

    private static Logger log = Logger.getLogger(SkimCalculator.class);

    public static void main(String[] args) throws IOException {
        String zonesShapeFilename = "./input/zones/zones_eu_skimCalculation.shp";
        String zonesIdAttributeName = "TAZ_id";

        Config config = ConfigUtils.loadConfig("./sbbConfigTest.xml");
        //RaptorUtils.createStaticConfig(config);
        //RaptorUtils.createParameters(config);

        String mode = "ld_train_2";

        //for the v2
        String outputDirectory = "./output/skims/germany_auto_2";
        String networkFilename = "./output/eu_germany_network_w_connector.xml";
        String transitScheduleFilename = "./output/ld_train_2/mapped_schedule.xml";

//        String outputDirectory = "./output/skims/germany_all_v1/";
//        String networkFilename = "./output/opnv/network_merged_germany_bus.xml.gz";
//        String transitScheduleFilename = "./output/opnv/schedule_germany_" + mode + "_mapped.xml";

        MyCalculateSkimMatrices skims = new MyCalculateSkimMatrices(zonesShapeFilename, zonesIdAttributeName, outputDirectory, 16);
        //skims.loadSamplingPointsFromFile("./input/centroids/meanStopsCSVModified.csv");

        //skims.calculateSamplingPointsPerZoneFromFacilities(facilitiesFilename, numberOfPointsPerZone, r, facility -> 1.0);
        // alternative if you don't have facilities:
        skims.calculateSamplingPointsPerZoneFromNetwork(networkFilename, 1, new Random(0));
        double[] timesCar = new double[]{8 * 3600};
        skims.calculateNetworkMatrices(networkFilename, null, timesCar, config, null, link -> true);
        //skims.calculatePTMatrices(networkFilename, transitScheduleFilename, 8 * 60 * 60, 14 * 60 * 60, config,
               // mode, (line, route) -> isRail(route));

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
       Use this for mito mode == tramMetro, to find when these modes are not used.
        */
    private static boolean isCoach(TransitRoute route) {
        if (route.getTransportMode().equalsIgnoreCase("coach")) {
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
