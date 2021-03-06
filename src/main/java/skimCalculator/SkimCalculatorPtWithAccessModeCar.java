package skimCalculator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.io.IOException;


public class SkimCalculatorPtWithAccessModeCar {

    private static Logger log = Logger.getLogger(SkimCalculatorPtWithAccessModeCar.class);

    public static void main(String[] args) throws IOException {

        String zonesShapeFilename = "./input/zones/TAZs_completed_11879_skimCalculation.shp";
        String zonesIdAttributeName = "TAZ_id";

        Config config = ConfigUtils.loadConfig("./sbbConfigTest.xml");

        String mode = "ld_rb_sb_train_with_auto_";


        String outputDirectory = "./output/skims/ld_rb_sb_train_with_auto";
        String networkFilename = "./output/ld_rb/network_merged.xml.gz";
        String transitScheduleFilename = "./output/ld_rb/mapped_schedule.xml";

        MyCalculateSkimMatricesWithAccessMode skims = new MyCalculateSkimMatricesWithAccessMode(zonesShapeFilename,
                zonesIdAttributeName, outputDirectory, 8, TransportMode.car);
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
