package skimCalculator;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.io.IOException;
import java.util.Random;


public class SkimCalculatorPtWithAccessMode {

    private static Logger log = Logger.getLogger(SkimCalculatorPtWithAccessMode.class);

    public static void main(String[] args) throws IOException {

        String zonesShapeFilename = "./input/zones/TAZs_completed_11879_skimCalculation.shp";
        String zonesIdAttributeName = "TAZ_id";

        Config config = ConfigUtils.loadConfig("./sbbConfigTest.xml");

        String mode = "ld_train_with_auto_access_2";


        String outputDirectory = "./output/skims/ld_train_with_auto_access_2";
        String networkFilename = "./output/ld_train_2/network_merged.xml.gz";
        String transitScheduleFilename = "./output/ld_train_2/mapped_schedule.xml";

        MyCalculateSkimMatricesWithAccessMode skims = new MyCalculateSkimMatricesWithAccessMode(zonesShapeFilename, zonesIdAttributeName, outputDirectory, 8);
        skims.loadSamplingPointsFromFile("output/skims/ld_train_v3/zone_coordinates.csv");
        skims.calculatePTMatrices(networkFilename, transitScheduleFilename, 8 * 60 * 60, 8.1 * 60 * 60, config,
                mode, (line, route) -> isRail(route));

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
