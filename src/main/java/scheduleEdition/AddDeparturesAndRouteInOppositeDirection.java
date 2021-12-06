package scheduleEdition;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import java.util.ArrayList;
import java.util.List;

public class AddDeparturesAndRouteInOppositeDirection {

    private static TransitScheduleFactoryImpl transitScheduleFactory;
    private static double STOPPING_TIME_S = 20;
    private static int idCounter = 0;

    public static void main(String[] args) {


        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new TransitScheduleReader(scenario).readFile("./output/opnv/FilteredAndRemoved/FilteredSchedule.xml");
        String newScheduleFile = "./output/opnv/ReadyForDeparture/schedule.xml" ;
        String newVehFile = "./output/opnv/ReadyForDeparture/vehicles.xml";

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
                List<TransitRouteStop> newOppositeStops = new ArrayList<>();
                List<TransitRouteStop> newStops = new ArrayList<>();
                List<TransitRouteStop> stops = transitRoute.getStops();
                int index = 0;
                int oppositeIndex = stops.size() - 1;
                TransitRouteStop previousOppositeStop = stops.get(oppositeIndex);
                TransitRouteStop previousStop = stops.get(index);
                previousStop.setAwaitDepartureTime(true);
                previousOppositeStop.setAwaitDepartureTime(true);
                newOppositeStops.add(previousOppositeStop);
                newStops.add(previousStop);
                double cumTime_s = 0;

                for (index = 1; index < stops.size(); index++){
                    TransitRouteStop nextStop = stops.get(index);
                    double distance_m = NetworkUtils.getEuclideanDistance(previousStop.getStopFacility().getCoord(), nextStop.getStopFacility().getCoord());
                    //duplicates of the stops are not taking into account
                    if (distance_m <= 100){
                        continue;
                    }
                    //average speed of 30 km/h if short distance between stops
                    else if (distance_m > 100 && distance_m < 800) {
                        cumTime_s += 1.3 * distance_m / 30 * 3.6;
                    }
                    //average speed of 50 km/h if longer distance between stops
                    else{
                        cumTime_s += 1.3 * distance_m / 50 * 3.6;
                    }
                    //create the new stop with the correct offsets
                    TransitRouteStop newNextStop = transitScheduleFactory.createTransitRouteStop(nextStop.getStopFacility(),cumTime_s, cumTime_s + STOPPING_TIME_S );
                    newNextStop.setAwaitDepartureTime(true);
                    newStops.add(newNextStop);
                    cumTime_s+=STOPPING_TIME_S;

                    previousStop = newNextStop;

                }

                //opposite direction
                cumTime_s = 0;

                //check if the line does not already include the opposite route
                if (NetworkUtils.getEuclideanDistance(previousOppositeStop.getStopFacility().getCoord(), stops.get(0).getStopFacility().getCoord()) > 50) {
                    //loop starting from the end of the stops list
                    for (oppositeIndex = stops.size() - 2; oppositeIndex >= 0; oppositeIndex--) {
                        TransitRouteStop nextStop = stops.get(oppositeIndex);
                        double distance_m = NetworkUtils.getEuclideanDistance(previousOppositeStop.getStopFacility().getCoord(), nextStop.getStopFacility().getCoord());
                        if (distance_m <= 100) {
                            continue;
                        } else if (distance_m > 100 && distance_m < 800) {
                            cumTime_s += 1.3 * distance_m / 30 * 3.6;
                        } else {
                            cumTime_s += 1.3 * distance_m / 50 * 3.6;
                        }
                        TransitRouteStop newNextStop = transitScheduleFactory.createTransitRouteStop(nextStop.getStopFacility(), cumTime_s, cumTime_s + STOPPING_TIME_S);
                        newNextStop.setAwaitDepartureTime(true);
                        newOppositeStops.add(newNextStop);
                        cumTime_s += STOPPING_TIME_S;

                        previousOppositeStop = newNextStop;

                    }
                }



                //adding routes to the schedule
                TransitRoute oldTransitRoute = transitScheduleFactory.createTransitRoute(transitRoute.getId(), newTransitNetworkRoute, newStops, transitRoute.getTransportMode());
                TransitRoute oppositeTransitRoute = transitScheduleFactory.createTransitRoute(Id.create(transitRoute.getId() + "_opposite", TransitRoute.class), newTransitNetworkRoute, newOppositeStops, transitRoute.getTransportMode());

                //adding departures: one departure every 20 minutes, from 6:00 to 21:40)
                for (int i = 6; i <= 21; i++){
                    for (int j = 0; j < 60; j =  j+20) {
                        Departure departure1 = newTransitSchedule.getFactory().createDeparture(Id.create(idCounter, Departure.class), i * 60 * 60 + j*60);
                        //idCounter guarantees a different id for every departure of the file
                        idCounter++;
                        oldTransitRoute.addDeparture(departure1);

                        Departure departure2 = newTransitSchedule.getFactory().createDeparture(Id.create(idCounter, Departure.class), i * 60 * 60 + j*60);
                        idCounter++;
                        oppositeTransitRoute.addDeparture(departure2);
                    }
                }

                //adding the routes to the corresponding line
                newTransitLine.addRoute(oldTransitRoute);
                newTransitLine.addRoute(oppositeTransitRoute);

            }

            newTransitSchedule.addTransitLine(newTransitLine);

        }

        new TransitScheduleWriterV2(newTransitSchedule).write(newScheduleFile);

        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
        MergeSchedulesOSM.createVehiclesForSchedule(newTransitSchedule, vehicles);
        new VehicleWriterV1(vehicles).writeFile(newVehFile);


    }
}