package skimCalculator;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.io.IOException;
import java.util.Random;

public class SkimCalculatorCarWithTolls {

    private static Logger log = Logger.getLogger(SkimCalculatorCarWithTolls.class);

    public static void main(String[] args) throws IOException {

        String zonesShapeFilename = "./input/zones/TAZs_completed_11879_skimCalculation.shp";
        String zonesIdAttributeName = "TAZ_id";
        String networkFilename = "./input/road_networks/final_network.xml.gz";
        Config config = ConfigUtils.loadConfig("./input/sbbConfig.xml");

        //Todo seeting for each run
        boolean avoidToll = false; //true: auto_noToll ; false: auto
        boolean appliedTollOnBundesstrasse = false;
        //String eventsFilename = "./input/2011_base_output_events.xml.gz";
        String outputDirectory = "./output/skims/freeflow_3points_grid";

        Random rmd = new Random(10);

//        MyCalculateSkimMatrices skims = new MyCalculateSkimMatrices(zonesShapeFilename,
//                zonesIdAttributeName, outputDirectory, 16, false);

        MyCalculateSkimMatrices skims = new MyCalculateSkimMatrices(zonesShapeFilename,
                zonesIdAttributeName, outputDirectory, 16, avoidToll, appliedTollOnBundesstrasse);

        //Resampling points in zones
        //skims.calculateSamplingPointsPerZoneFromNetwork(networkFilename, 3, rmd);

        skims.loadSamplingPointsFromFile("./output/zone_coordinates_3_kMeans_grid.csv");

        double[] timesCar = new double[]{8 * 3600};
        //freeflow
        skims.calculateNetworkMatrices(networkFilename, null, timesCar, config, null, link -> true);
        //congested
        //skims.calculateNetworkMatrices(networkFilename, eventsFilename, timesCar, config, null, link -> true);

    }

}
