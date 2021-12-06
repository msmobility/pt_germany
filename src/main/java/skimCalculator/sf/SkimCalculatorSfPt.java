package skimCalculator.sf;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import skimCalculator.MyCalculateSkimMatrices;

import java.io.IOException;
import java.util.Random;


public class SkimCalculatorSfPt {

    private static Logger log = Logger.getLogger(SkimCalculatorSfPt.class);

    public static void main(String[] args) throws IOException {

        String zonesShapeFilename = "./shp/zones_7131.shp";
        String zonesIdAttributeName = "Id";

        Config config = ConfigUtils.loadConfig("./sbbConfigTest.xml");
        //RaptorUtils.createStaticConfig(config);
        //RaptorUtils.createParameters(config);



        //for the v2
        String outputDirectory = "C:/models/sf_skims/metro/";
        String networkFilename = "./matsim/pt_road_network.xml.gz";
//        String eventFilename = "F:/matsim_germany/output/run_210706/all.output_events.xml.gz";
//        String outputDirectory = "./output/skims/germany_all_v1/";
//        String networkFilename = "./output/opnv/network_merged_germany_bus.xml.gz";
        String transitScheduleFilename = "./matsim/all_metro/mapped_schedule.xml";

        MyCalculateSkimMatrices skims = new MyCalculateSkimMatrices(zonesShapeFilename, zonesIdAttributeName, outputDirectory, 8, false);
        skims.loadSamplingPointsFromFile("./matrices/rail/zone_coordinates.csv");

        //skims.calculateSamplingPointsPerZoneFromFacilities(facilitiesFilename, numberOfPointsPerZone, r, facility -> 1.0);
        // alternative if you don't have facilities:
        //skims.calculateSamplingPointsPerZoneFromNetwork(networkFilename, 1, new Random(0));
        double[] timesCar = new double[]{17 * 3600};
        //skims.calculateNetworkMatrices(networkFilename, null, timesCar, config, null, link -> true);
        skims.calculatePTMatrices(networkFilename, transitScheduleFilename, 8 * 60 * 60, 12 * 60 * 60, config, "", (line, route) -> tramMetroTrainDetector(route));

        //skims.calculateBeelineMatrix();
    }

    private static boolean trainDetector(TransitRoute route) {
        if (route.getTransportMode().equalsIgnoreCase("bus") ||
                route.getTransportMode().equalsIgnoreCase("tram") ||
                route.getTransportMode().equalsIgnoreCase("subway")
        ) {
            return false;
        } else {
            //will return true for rail and ferries, etc.
            return true;
        }
    }

    private static boolean tramMetroTrainDetector(TransitRoute route) {
        if (route.getTransportMode().equalsIgnoreCase("bus")) {
            return false;
        } else {
            //will return true for rail and ferries, etc.
            return true;
        }
    }





}
