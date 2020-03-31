
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
import org.matsim.vehicles.*;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;

public class MergeSchedules {

    public static void main(String[] args) {

        Scenario scenarioOne = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Scenario scenarioTwo = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Scenario scenarioThree = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new TransitScheduleReader(scenarioOne).readFile("./output/db/scheduleMuc.xml");
        new TransitScheduleReader(scenarioTwo).readFile("./output/rb/scheduleMuc.xml");
        new TransitScheduleReader(scenarioThree).readFile("./output/opnv/scheduleMuc.xml");
        TransitSchedule scheduleOne = scenarioOne.getTransitSchedule();
        TransitSchedule scheduleTwo = scenarioTwo.getTransitSchedule();
        TransitSchedule scheduleThree = scenarioThree.getTransitSchedule();

        final TransitSchedule mergedSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();

        scheduleOne = addSuffixToScheduleElements(scheduleOne, "_db");
        scheduleTwo = addSuffixToScheduleElements(scheduleTwo, "_rb");
        scheduleThree = addSuffixToScheduleElements(scheduleThree, "_opnv");

        List<TransitSchedule> transitSchedules = new ArrayList<>();

        transitSchedules.add(scheduleOne);
        transitSchedules.add(scheduleTwo);
        transitSchedules.add(scheduleThree);

        for (TransitSchedule schedule : transitSchedules){
            for (TransitStopFacility stop : schedule.getFacilities().values()) {
                mergedSchedule.addStopFacility(stop);
            }

            for (TransitLine line : schedule.getTransitLines().values()) {
                mergedSchedule.addTransitLine(line);
            }
        }

        new TransitScheduleWriter(mergedSchedule).writeFile("./output/all/scheduleMuc.xml");

        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
        createVehiclesForSchedule(mergedSchedule, vehicles);
        new VehicleWriterV1(vehicles).writeFile("./output/all/vehicleMuc.xml");


    }

    public static void createVehiclesForSchedule(TransitSchedule schedule, Vehicles vehicles) {
        VehiclesFactory vb = vehicles.getFactory();
        Map<String, VehicleType> vehicleTypeMap = new HashMap<>();

        //todo read from vehicle xml file can be better
        {
            VehicleType vehicleType = vb.createVehicleType(Id.create("default", VehicleType.class));
            VehicleCapacity capacity = new VehicleCapacityImpl();
            capacity.setSeats(Integer.valueOf(50));
            capacity.setStandingRoom(Integer.valueOf(50));
            vehicleType.setCapacity(capacity);
            vehicles.addVehicleType(vehicleType);
            vehicleTypeMap.put(vehicleType.getId().toString(), vehicleType);
        }

        {
            VehicleType vehicleType = vb.createVehicleType(Id.create("bus", VehicleType.class));
            VehicleCapacity capacity = new VehicleCapacityImpl();
            capacity.setSeats(Integer.valueOf(101));
            capacity.setStandingRoom(Integer.valueOf(0));
            vehicleType.setCapacity(capacity);
            vehicles.addVehicleType(vehicleType);
            vehicleTypeMap.put(vehicleType.getId().toString(), vehicleType);
        }

        {
            VehicleType vehicleType = vb.createVehicleType(Id.create("tram", VehicleType.class));
            VehicleCapacity capacity = new VehicleCapacityImpl();
            capacity.setSeats(Integer.valueOf(100));
            capacity.setStandingRoom(Integer.valueOf(100));
            vehicleType.setCapacity(capacity);
            vehicles.addVehicleType(vehicleType);
            vehicleTypeMap.put(vehicleType.getId().toString(), vehicleType);
        }


        {
            VehicleType vehicleType = vb.createVehicleType(Id.create("subway", VehicleType.class));
            VehicleCapacity capacity = new VehicleCapacityImpl();
            capacity.setSeats(Integer.valueOf(300));
            capacity.setStandingRoom(Integer.valueOf(300));
            vehicleType.setCapacity(capacity);
            vehicles.addVehicleType(vehicleType);
            vehicleTypeMap.put(vehicleType.getId().toString(), vehicleType);
        }

        {
            VehicleType vehicleType = vb.createVehicleType(Id.create("rail", VehicleType.class));
            VehicleCapacity capacity = new VehicleCapacityImpl();
            capacity.setSeats(Integer.valueOf(300));
            capacity.setStandingRoom(Integer.valueOf(0));
            vehicleType.setCapacity(capacity);
            vehicles.addVehicleType(vehicleType);
            vehicleTypeMap.put(vehicleType.getId().toString(), vehicleType);
        }

        for (TransitLine line : schedule.getTransitLines().values()) {
            for (TransitRoute route : line.getRoutes().values()) {
                for (Departure departure : route.getDepartures().values()) {
                    Id<Vehicle> vehicleId = departure.getVehicleId();
                    VehicleType vehicleType = getVehicleTypeFromId(vehicleId, vehicleTypeMap);
                    Vehicle veh = vb.createVehicle(vehicleId, vehicleType);
                    vehicles.addVehicle(veh);
                    departure.setVehicleId(veh.getId());
                }
            }
        }



    }

    private static VehicleType getVehicleTypeFromId(Id<Vehicle> vehicleId, Map<String, VehicleType> vehicleTypeMap) {
        String string = vehicleId.toString().toLowerCase();
        if (string.contains("bus")){
            return vehicleTypeMap.get("bus");
        } else if (string.contains("tram")){
            return vehicleTypeMap.get("tram");
        } else if (string.contains("subway")){
            return vehicleTypeMap.get("subway");
        } else if (string.contains("rail")){
            return vehicleTypeMap.get("rail");
        } else {
            System.out.println("A default vehicle type is selected!");
            return vehicleTypeMap.get("default");
        }
    }

    private static TransitSchedule addSuffixToScheduleElements(TransitSchedule schedule, String suffix) {
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
                TransitRoute newRoute = factory.createTransitRoute(newRouteId, null, newStops, route.getTransportMode());

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