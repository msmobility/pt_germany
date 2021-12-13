package skimCalculator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.io.IOException;


public class SkimCalculatorPtWithAccessModeWalk {

    private static Logger log = Logger.getLogger(SkimCalculatorPtWithAccessModeWalk.class);

    public static void main(String[] args) throws IOException {

        String zonesShapeFilename = "./input/zones/TAZs_completed_11879_skimCalculation.shp";
        String zonesIdAttributeName = "TAZ_id";

        Config config = ConfigUtils.loadConfig("./input/sbbConfig.xml");

        String mode = "ld_rail_with_walk_";


        String outputDirectory = "./output/skims/ld_rail_with_walk";
        String networkFilename = "./input/road_networks/network_merged_modified_v1.xml.gz";
        String transitScheduleFilename = "./input/road_networks/mapped_schedule_modified_v1.xml";

        MyCalculateSkimMatricesWithAccessMode skims = new MyCalculateSkimMatricesWithAccessMode(zonesShapeFilename,
                zonesIdAttributeName, outputDirectory, 16, TransportMode.walk);
        skims.loadSamplingPointsFromFile("./output/zone_coordinates_3_kMeans_pop.csv");
        skims.calculatePTMatrices(networkFilename, transitScheduleFilename, 8 * 60 * 60, 8.1 * 60 * 60, config,
                mode, (line, route) -> isRailTramOrSubway(route));

        //skims.calculateBeelineMatrix();


    }

    /*
    Use this for mito mode == tramMetro, to find when these modes are not used.
     */
    private static boolean isRailTramOrSubway(TransitRoute route) {
        return route.getTransportMode().equalsIgnoreCase("rail") ||
                route.getTransportMode().equalsIgnoreCase("tram") ||
                route.getTransportMode().equalsIgnoreCase("subway");
    }

    /*
       Use this for mito mode == tramMetro, to find when these modes are not used.
        */
    private static boolean isCoach(TransitRoute route) {
        return route.getTransportMode().equalsIgnoreCase("coach");
    }

    /*
    Use this for mito mode == train, to find when train is not used
     */
    private static boolean isRail(TransitRoute route) {
        return route.getTransportMode().equalsIgnoreCase("rail");
    }



}
