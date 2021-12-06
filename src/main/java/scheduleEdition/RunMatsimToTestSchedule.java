package scheduleEdition;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunMatsimToTestSchedule {


    public static void main(String[] args) {

        String configFileName = args[0];
        Config config = ConfigUtils.loadConfig(configFileName);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.run();

    }

}
