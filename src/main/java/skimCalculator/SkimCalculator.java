package skimCalculator;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.io.IOException;
import java.util.Random;


public class SkimCalculator {

    private static Logger log = Logger.getLogger(SkimCalculator.class);

    public static void main(String[] args) throws IOException {
        String zonesShapeFilename = "./input/zones/TAZs_completed_11879_skimCalculation.shp";
        String zonesIdAttributeName = "TAZ_id";

        Config config = ConfigUtils.loadConfig("./sbbConfigTest.xml");
        //RaptorUtils.createStaticConfig(config);
        //RaptorUtils.createParameters(config);

        String mode = "ld_train_v4_";

        //for the v2
        String outputDirectory = "./output/skims/ld_train_v4";
        String networkFilename = "./output/ld_train_2/network_merged.xml.gz";
        String transitScheduleFilename = "./output/ld_train_2/mapped_schedule.xml";

//        String outputDirectory = "./output/skims/germany_all_v1/";
//        String networkFilename = "./output/opnv/network_merged_germany_bus.xml.gz";
//        String transitScheduleFilename = "./output/opnv/schedule_germany_" + mode + "_mapped.xml";

        MyCalculateSkimMatrices skims = new MyCalculateSkimMatrices(zonesShapeFilename, zonesIdAttributeName, outputDirectory, 8);
        //skims.loadSamplingPointsFromFile("./input/centroids/meanStopsCSVModified.csv");

        //skims.calculateSamplingPointsPerZoneFromFacilities(facilitiesFilename, numberOfPointsPerZone, r, facility -> 1.0);
        // alternative if you don't have facilities:
        skims.calculateSamplingPointsPerZoneFromNetwork(networkFilename, 1, new Random(0));
        //double[] timesCar = new double[]{8 * 3600};
        //skims.calculateNetworkMatrices(networkFilename, null, timesCar, config, null, link -> true);
        skims.calculatePTMatrices(networkFilename, transitScheduleFilename, 8 * 60 * 60, 8.1 * 60 * 60, config,
                mode, (line, route) -> isCoach(route));

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



}
