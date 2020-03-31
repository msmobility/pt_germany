import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import java.util.ArrayList;
import java.util.List;

public class FilterScheduleByMode {


    public static void main(String[] args) {

        String service = "all";

        String scheduleFile = "./output/" + service + "/mapped_schedule_muc.xml";
        String newScheduleFile = "./output/" + service + "/mapped_schedule_muc_bus.xml";

        String vehFileName = "./output/all/vehicleMuc_bus.xml";

        List<String> modes = new ArrayList<>();
        modes.add("bus");
//        modes.add("tram");
//        modes.add("subway");

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
