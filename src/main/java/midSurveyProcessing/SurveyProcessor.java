package midSurveyProcessing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.util.Map;

public class SurveyProcessor {


    public static void main(String[] args) throws IOException {

        String fileName = "Z:\\projects\\2019\\BASt\\data\\MiD_analysis\\trip_data2017_complete_case_coord_v1.csv";
        String outputFileName = "Z:\\projects\\2019\\BASt\\data\\MiD_analysis\\output_carlos.csv";

        Map<Integer, SurveyTrip> tripMap = new SurveyReader().readSurvey(fileName);

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("Z:\\projects\\2019\\BASt\\data\\MiD_analysis\\germany_w_tertiary.xml.gz");

        Scenario scenario = ScenarioUtils.loadScenario(config);

        CarTimeCalculator carTimeCalculator = new CarTimeCalculator(scenario.getNetwork());
        carTimeCalculator.assignCarTravelTimes(tripMap);

        new SurveyTripPrinter().printOutResults(outputFileName, tripMap);

    }
}
