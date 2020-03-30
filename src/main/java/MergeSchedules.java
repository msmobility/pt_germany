
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.pt2matsim.tools.ScheduleTools;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.opengis.feature.simple.SimpleFeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MergeSchedules {

    public static void main(String[] args) {

        Scenario scenarioOne = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Scenario scenarioTwo = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new TransitScheduleReader(scenarioOne).readFile("./output/db/scheduleMuc.xml");
        new TransitScheduleReader(scenarioTwo).readFile("./output/rb/scheduleMuc.xml");
        TransitSchedule scheduleOne = scenarioOne.getTransitSchedule();
        TransitSchedule scheduleTwo = scenarioTwo.getTransitSchedule();
        final TransitSchedule mergedSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();

        scheduleOne = addSuffixToScheduleElements(scheduleOne, "db", TransportMode.train);
        scheduleTwo = addSuffixToScheduleElements(scheduleTwo, "rb", TransportMode.train);


        //add all stops from first schedule if within study area
        for (TransitStopFacility stop : scheduleOne.getFacilities().values()) {
            mergedSchedule.addStopFacility(stop);
        }
        //add all stops from second schedule if within study area
        for (TransitStopFacility stop : scheduleTwo.getFacilities().values()) {
            mergedSchedule.addStopFacility(stop);
        }
        for (TransitLine line : scheduleOne.getTransitLines().values()) {
            mergedSchedule.addTransitLine(line);
        }

        for (TransitLine line : scheduleTwo.getTransitLines().values()) {
            mergedSchedule.addTransitLine(line);
        }

        new TransitScheduleWriter(mergedSchedule).writeFile("./output/db_rb/scheduleMuc.xml");

        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
        new CreateVehiclesForSchedule(mergedSchedule, vehicles).run();
        new VehicleWriterV1(vehicles).writeFile("./output/db_rb/vehicleMuc.xml");


    }

    private static TransitSchedule addSuffixToScheduleElements(TransitSchedule schedule, String suffix, String mode) {
        TransitScheduleFactory factory = schedule.getFactory();
        TransitSchedule newSchedule = factory.createTransitSchedule();
        for (TransitStopFacility stop : schedule.getFacilities().values()) {
            Id<TransitStopFacility> newStopId = Id.create(stop.getId() + suffix, TransitStopFacility.class);
            TransitStopFacility newStop = factory.createTransitStopFacility(newStopId, stop.getCoord(), stop.getIsBlockingLane());
            newSchedule.addStopFacility(newStop);
        }

        for (TransitLine line : schedule.getTransitLines().values()) {
            Id<TransitLine> newLineId = Id.create(line.getId() + suffix, TransitLine.class);
            TransitLine newLine = factory.createTransitLine(newLineId);

            for (TransitRoute route : line.getRoutes().values()) {

                List<TransitRouteStop> newStops = new ArrayList<>();

                for (TransitRouteStop routeStop : route.getStops()) {
                    Id<TransitStopFacility> newStopFacilityId = Id.create(routeStop.getStopFacility().getId() + suffix, TransitStopFacility.class);
                    TransitStopFacility newStopFacility = newSchedule.getFacilities().get(newStopFacilityId);
                    TransitRouteStop newRouteStop = factory.createTransitRouteStop(newStopFacility, routeStop.getArrivalOffset(), routeStop.getDepartureOffset());
                    newStops.add(newRouteStop);
                }


                Id<TransitRoute> newRouteId = Id.create(route.getId() + suffix, TransitRoute.class);
                TransitRoute newRoute = factory.createTransitRoute(newRouteId, null, newStops, mode);

                for (Departure departure : route.getDepartures().values()) {
                    departure.getDepartureTime();
                    Id<Departure> newDepartureId = Id.create(departure.getId() + suffix, Departure.class);
                    Id<Vehicle> newVehicleId = Id.create(departure.getVehicleId() + suffix, Vehicle.class);
                    Departure newDeparture = factory.createDeparture(newDepartureId, departure.getDepartureTime());
                    newDeparture.setVehicleId(newVehicleId);
                    newRoute.addDeparture(newDeparture);
                }

                newLine.addRoute(newRoute);
            }
            newSchedule.addTransitLine(newLine);
        }
        return newSchedule;
    }


}