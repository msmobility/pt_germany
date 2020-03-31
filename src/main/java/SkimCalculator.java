import ch.sbb.matsim.analysis.skims.CalculateSkimMatrices;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.io.IOException;
import java.util.Random;

public class SkimCalculator {


    public static void main(String[] args) throws IOException {

        String zonesShapeFilename = "./input/zones/zones.shp";
        String zonesIdAttributeName = "id";
        String outputDirectory = "./output/skims/subway_tram";
        Config config = ConfigUtils.loadConfig("./sbbConfig.xml");
        String networkFilename = "./output/all/network_merged_muc.xml.gz";
        String transitScheduleFilename = "./output/all/mapped_schedule_muc_subway_tram.xml";


        CalculateSkimMatrices skims = new CalculateSkimMatrices(zonesShapeFilename, zonesIdAttributeName, outputDirectory, 16);
        //skims.calculateSamplingPointsPerZoneFromFacilities(facilitiesFilename, numberOfPointsPerZone, r, facility -> 1.0);
        // alternative if you don't have facilities:
        skims.calculateSamplingPointsPerZoneFromNetwork(networkFilename, 1, new Random(0));
        //skims.calculateNetworkMatrices(networkFilename, eventsFilename, timesCar, config, null, link -> true);
        skims.calculatePTMatrices(networkFilename, transitScheduleFilename, 8*60*60, 9*60*60, config, null, (line, route) -> isRailTramOrSubway(route));

        //skims.calculateBeelineMatrix();


    }

    /*
    Use this for tram, metro, to find when these modes are not used.
     */
    private static boolean isRailTramOrSubway(TransitRoute route) {
        boolean returnValue = false;
        if (route.getTransportMode().equalsIgnoreCase("rail") ||
                route.getTransportMode().equalsIgnoreCase("tram") ||
                route.getTransportMode().equalsIgnoreCase("subway")) {
            return true;
        } else {
            return false;
        }
    }

    /*
    Use this for all modes, to find when train is not used
     */
    private static boolean isRail(TransitRoute route) {
        boolean returnValue = false;
        if (route.getTransportMode().equalsIgnoreCase("rail")) {
            return true;
        } else {
            return false;
        }
    }


}
