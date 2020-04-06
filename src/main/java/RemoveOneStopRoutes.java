import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt2matsim.plausibility.PlausibilityCheck;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

public class RemoveOneStopRoutes {

    public static void main(String[] args) {


        Scenario scenarioOne = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new TransitScheduleReader(scenarioOne).readFile("./output/opnv/schedule.xml");
        String newScheduleFile = "./output/opnv/schedule2.xml" ;
        String vehFileName = "./output/opnv/vehicles.xml";


//        String networkFile = "./output/opnv/network_merged.xml.gz";
//        Network network = NetworkUtils.readNetwork(networkFile);
//        new PlausibilityCheck(scenarioOne.getTransitSchedule(), network, "EPSG:31468").runCheck();

        TransitSchedule transitSchedule = scenarioOne.getTransitSchedule();
        TransitSchedule newTransitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();

        for (TransitStopFacility transitStopFacility : transitSchedule.getFacilities().values()){
            newTransitSchedule.addStopFacility(transitStopFacility);
        }

        for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
            boolean add = true;

            for (TransitRoute transitRoute : transitLine.getRoutes().values()){
                if (transitRoute.getStops().size() < 2){
                    System.out.println("Unplausible line " + transitLine.getId());
                    add = false;
                }
            }

            if (add){
                newTransitSchedule.addTransitLine(transitLine);
            }

        }

        new TransitScheduleWriterV2(newTransitSchedule).write(newScheduleFile);

        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
        MergeSchedules.createVehiclesForSchedule(newTransitSchedule, vehicles);
        new VehicleWriterV1(vehicles).writeFile(vehFileName);


    }
}
