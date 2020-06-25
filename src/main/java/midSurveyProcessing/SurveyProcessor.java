package midSurveyProcessing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.util.Map;

public class SurveyProcessor {


    public static void main(String[] args) throws IOException {

        String fileName = "MiD_10_trips_with_coordinates.csv";
        String outputFileName = "MiD_10_trips_with_trip_data.csv";

        Map<Integer, SurveyTrip> tripMap = new SurveyReader().readSurvey(fileName);

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("germany_w_tertiary.xml.gz");

        Scenario scenario = ScenarioUtils.loadScenario(config);

        CarTimeCalculator carTimeCalculator = new CarTimeCalculator(scenario.getNetwork());
        carTimeCalculator.assignCarTravelTimes(tripMap);

        new SurveyTripPrinter().printOutResults(outputFileName, tripMap);

    }
}
