package scheduleEdition.sf;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import scheduleEdition.CutScheduleByShape;
import scheduleEdition.MergeSchedules;

import java.util.ArrayList;
import java.util.List;

public class FilterScheduleByModeSf {


    public static void main(String[] args) {

        /**
         * original schedule file
         */
        String scheduleFile = "./matsim/mapped_schedule.xml";
        /**
         * new schedule file
         */
        String newScheduleFile = "./matsim/all_metro/mapped_schedule.xml";

        /**
         * new vehicle file
         */
        String vehFileName = "./matsim/all_metro/vehicle.xml";


        /**
         * subset of modes to keep (according to the gtfs codes, i.e. "subway" instead of "metro"
         */
        List<String> modes = new ArrayList<>();
        modes.add("bus");
        modes.add("tram");
        modes.add("subway");

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new TransitScheduleReader(scenario).readFile(scheduleFile);
        final TransitSchedule originalSchedule = scenario.getTransitSchedule();
        final TransitSchedule newSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();

        int total = 0;
        int filtered = 0;

        for (TransitLine line : originalSchedule.getTransitLines().values()) {
            TransitLine lineCopy = newSchedule.getFactory().createTransitLine(line.getId());
            for (TransitRoute route : line.getRoutes().values()) {
                total++;
                if (modes.contains(route.getTransportMode())) {
                    filtered++;
                    lineCopy.addRoute(route);
                    route.getStops().stream().map(TransitRouteStop::getStopFacility).forEach(stop -> CutScheduleByShape.addStofIfNonExistent(stop, newSchedule));
                }
            }
            if(!lineCopy.getRoutes().isEmpty()){
                newSchedule.addTransitLine(lineCopy);
            }
        }

        System.out.println("Transit lines = " + total + " Filtered transit lines = " + filtered);

        new TransitScheduleWriterV2(newSchedule).write(newScheduleFile);

        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
        MergeSchedules.createVehiclesForSchedule(newSchedule, vehicles);
        new VehicleWriterV1(vehicles).writeFile(vehFileName);


    }


}