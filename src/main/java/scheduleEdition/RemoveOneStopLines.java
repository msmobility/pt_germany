package scheduleEdition;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitRouteStopImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

public class RemoveOneStopLines {

    public static void main(String[] args) {

        String suffix = "_v2";
        for (String arg : args){

            Scenario scenarioOne = ScenarioUtils.createScenario(ConfigUtils.createConfig());

            new TransitScheduleReader(scenarioOne).readFile(arg);
            String newScheduleFile = arg.replace(".xml", "") + suffix + ".xml";
            //todo careful with the output file - check every case!
            System.out.println(newScheduleFile);

            //String vehFileName = "./output/opnv/Schedule2_step2/vehicles.xml";


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
                if(transitLine.getRoutes().isEmpty()){
                    System.out.println("Line without route");
                    add = false;
                }

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

            //Vehicles vehicles = VehicleUtils.createVehiclesContainer();
            //scheduleEdition.MergeSchedulesOSM.createVehiclesForSchedule(newTransitSchedule, vehicles);
            //new VehicleWriterV1(vehicles).writeFile(vehFileName);
        }



    }
}
