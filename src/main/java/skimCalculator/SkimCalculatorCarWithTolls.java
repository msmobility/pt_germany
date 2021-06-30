package skimCalculator;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.io.IOException;
import java.util.Random;


public class SkimCalculatorCarWithTolls {

    private static Logger log = Logger.getLogger(SkimCalculatorCarWithTolls.class);

    public static void main(String[] args) throws IOException {
        String zonesShapeFilename = "./input/zones/TAZs_completed_11879_skimCalculation.shp";
        String zonesIdAttributeName = "TAZ_id";

        Config config = ConfigUtils.loadConfig("./sbbConfigTest.xml");

        String outputDirectory = "./output/skims/carWithToll_ab";
        String networkFilename = "./input/road_networks/eu_germany_network_w_connector_trucks_ab.xml.gz";

        MyCalculateSkimMatrices skims = new MyCalculateSkimMatrices(zonesShapeFilename,
                zonesIdAttributeName, outputDirectory, 16, false);
        skims.loadSamplingPointsFromFile("./output/skims/zone_coordinates_1_point.csv");

        double[] timesCar = new double[]{8 * 3600};
        skims.calculateNetworkMatrices(networkFilename, null, timesCar, config, null, link -> true);


    }

}
