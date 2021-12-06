package scheduleEdition;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitLineImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import java.util.ArrayList;
import java.util.List;

public class AddDeparturesAndTravelTimes {

    private static TransitScheduleFactoryImpl transitScheduleFactory;
    private static double STOPPING_TIME_S = 20;

    public static void main(String[] args) {


        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new TransitScheduleReader(scenario).readFile("./output/opnv/Schedule2_step2/schedule.xml");
        String newScheduleFile = "./output/opnv/Schedule2_step2/vehicles.xml" ;
        //String newVehFile = "./output/opnv/vehicles.xml";

        TransitSchedule transitSchedule = scenario.getTransitSchedule();
        transitScheduleFactory = new TransitScheduleFactoryImpl();
        TransitSchedule newTransitSchedule = transitScheduleFactory.createTransitSchedule();

        for (TransitStopFacility transitStopFacility : transitSchedule.getFacilities().values()){
            newTransitSchedule.addStopFacility(transitStopFacility);
        }

        for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {

            TransitLine newTransitLine = transitScheduleFactory.createTransitLine(transitLine.getId());

            for (TransitRoute transitRoute : transitLine.getRoutes().values()){
                NetworkRoute newTransitNetworkRoute = null;
                List<TransitRouteStop> newStops = new ArrayList<>();
                List<TransitRouteStop> stops = transitRoute.getStops();
                int index = 0;
                TransitRouteStop previousStop = stops.get(index);
                newStops.add(previousStop);
                double cumTime_s = 0;

                for (index = 1; index < stops.size(); index++){
                    TransitRouteStop nextStop = stops.get(index);
                    double distance_m = NetworkUtils.getEuclideanDistance(previousStop.getStopFacility().getCoord(), nextStop.getStopFacility().getCoord());
                    if (distance_m <= 50){
                        continue;
                    }
                    else if (distance_m > 50 && distance_m < 800) {
                        cumTime_s += 1.3 * distance_m / 30 * 3.6;
                    }
                    else{
                        cumTime_s += 1.3 * distance_m / 50 * 3.6;
                    }
                    TransitRouteStop newNextStop = transitScheduleFactory.createTransitRouteStop(nextStop.getStopFacility(),cumTime_s, cumTime_s + STOPPING_TIME_S );
                    newNextStop.setAwaitDepartureTime(true);
                    newStops.add(newNextStop);
                    cumTime_s+=STOPPING_TIME_S;

                    previousStop = newNextStop;

                }


                TransitRoute newTransitRoute = transitScheduleFactory.createTransitRoute(transitRoute.getId(), newTransitNetworkRoute, newStops, transitRoute.getTransportMode());

                newTransitRoute.addDeparture(transitScheduleFactory.createDeparture(Id.create(1, Departure.class), 8*60*60));

                newTransitLine.addRoute(newTransitRoute);

            }

            newTransitSchedule.addTransitLine(newTransitLine);

        }

        new TransitScheduleWriterV2(newTransitSchedule).write(newScheduleFile);

        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
        // scheduleEdition.MergeSchedules.createVehiclesForSchedule(newTransitSchedule, vehicles);
        //new VehicleWriterV1(vehicles).writeFile(newVehFile);


    }
}