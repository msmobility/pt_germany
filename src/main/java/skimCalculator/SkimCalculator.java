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
        String zonesShapeFilename = "C:\\projects\\trampa\\open_data\\mid\\zones\\zones_mid_5km_31468.shp";
        String zonesIdAttributeName = "id";

        Config config = ConfigUtils.loadConfig("./sbbConfigTest.xml");
        //RaptorUtils.createStaticConfig(config);
        //RaptorUtils.createParameters(config);

        String mode = "auto_congested";

        //for the v2
        String outputDirectory = "C:\\projects\\trampa\\open_data\\mid\\matrices";
        String networkFilename = "C:\\models\\mito\\muc\\mitoMunich\\input\\trafficAssignment\\studyNetworkDense.xml.gz";
        String eventFilename = null;
//        String outputDirectory = "./output/skims/germany_all_v1/";
//        String networkFilename = "./output/opnv/network_merged_germany_bus.xml.gz";
//        String transitScheduleFilename = "./output/opnv/schedule_germany_" + mode + "_mapped.xml";

        MyCalculateSkimMatrices skims = new MyCalculateSkimMatrices(zonesShapeFilename, zonesIdAttributeName, outputDirectory, 8, false);
        //skims.loadSamplingPointsFromFile("./output/skims/zone_coordinates_1_point.csv");

        //skims.calculateSamplingPointsPerZoneFromFacilities(facilitiesFilename, numberOfPointsPerZone, r, facility -> 1.0);
        // alternative if you don't have facilities:
        skims.calculateSamplingPointsPerZoneFromNetwork(networkFilename, 5, new Random(0));
        double[] timesCar = new double[]{8 * 3600};
        skims.calculateNetworkMatrices(networkFilename, eventFilename, timesCar, config, null, link -> true);
//        skims.calculatePTMatrices(networkFilename, transitScheduleFilename, 8 * 60 * 60, 8.1 * 60 * 60, config,
//                mode, (line, route) -> isCoach(route));

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
