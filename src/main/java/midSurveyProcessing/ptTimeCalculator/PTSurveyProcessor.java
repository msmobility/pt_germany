package midSurveyProcessing.ptTimeCalculator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;

import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PTSurveyProcessor {


    public static Logger logger = Logger.getLogger(PTSurveyProcessor.class);

    public static void main(String[] args) throws IOException{
        String networkFilename = "./input/network_merged.xml.gz"; // "./input/network_merged_germany_bus.xml.gz";
        String transitScheduleFilename = "./input/mapped_schedule.xml"; //"./input/schedule_germany_bus_mapped.xml";
        String transitVehiclesFilename = "./input/vehicles.xml"; //"./input/vehicle_germany_bus.xml";;
        String outputFolder =  "./output/train/";
        String outputFileName = "added_bus_ld_times.csv";
        //String zoneFile = "./output/skims/germany_opnv_db_rb/zone_coordinates.csv"; //?
        String fileName = "./MATSim_LDTrips1.csv";

        Config config1 = configureMATSim();
        Scenario scenario = ScenarioUtils.loadScenario(config1);


        Map<Integer, PTSurveyTrip> tripMap = new PTSurveyReader().readCSVOfTrips(fileName);

        long startTime_s = System.currentTimeMillis() / 1000;
        AtomicInteger counter = new AtomicInteger(1);

        //PTTimeCalculator ptTimeCalculator = new PTTimeCalculator(scenario.getNetwork());
        //ptTimeCalculator.assignPTTravelTimes(tripMap);

        ActivityFacilitiesFactoryImpl activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();


        // Trip Origin
        /*Map<PTSurveyTrip, ActivityFacility> facilitiesByTripOrigin = new HashMap<>();
        tripMap.values().parallelStream().forEach(trip -> {
            Node node = NetworkUtils.getNearestNode(scenario.getNetwork(), trip.origCoord);
            Id<Link> link = node.getInLinks().values().iterator().next().getId();
            ActivityFacility facility = activityFacilitiesFactory.createActivityFacility(null, node.getCoord(), link);
            if (facility != null && facility.getCoord() != null) {
                facilitiesByTripOrigin.put(trip, facility);
            }
        });

        //Trip Destination
        Map<PTSurveyTrip, ActivityFacility> facilitiesByTripDestination = new HashMap<>();
        tripMap.values().parallelStream().forEach(trip -> {
            Node node = NetworkUtils.getNearestNode(scenario.getNetwork(), trip.destCoord);
            Id<Link> link = node.getInLinks().values().iterator().next().getId();
            ActivityFacility facility = activityFacilitiesFactory.createActivityFacility(null, node.getCoord(), link);
            if (facility != null && facility.getCoord() != null) {
                facilitiesByTripDestination.put(trip, facility);
            }
        });*/

        logger.warn("Assign facilities to trip origin");

        TransitRouterConfig transitConfig = new TransitRouterConfig(config1);
        transitConfig.setAdditionalTransferTime(60);
        transitConfig.setBeelineWalkSpeed(3/3.6);

        /*facilitiesByTripOrigin.keySet().parallelStream().forEach(tripOrigin -> {
            TransitRouter transitRouter = new TransitRouterImpl(transitConfig, scenario.getTransitSchedule());
            ActivityFacility originFacility = facilitiesByTripOrigin.get(tripOrigin);
            for (PTSurveyTrip tripOrigin : facilitiesByTripOrigin.keySet()){
            }

        });*/
        TransitRouter transitRouter = new TransitRouterImpl(transitConfig, scenario.getTransitSchedule());

       // for (PTSurveyTrip trip : tripMap.values()){

            for (int i =0; i< 50; i++){
                PTSurveyTrip trip = tripMap.get(i);
            //tripMap.values().parallelStream().forEach(trip ->{


            Facility fromFacility = ((ActivityFacilitiesFactory) activityFacilitiesFactory).createActivityFacility(Id.create(1, ActivityFacility.class),
                    trip.getOrigCoord());
            Facility toFacility = ((ActivityFacilitiesFactory) activityFacilitiesFactory).createActivityFacility(Id.create(2, ActivityFacility.class),
                    trip.getDestCoord());



            double departureTime_s = trip.getDepartureTime_s();
            List<? extends PlanElement> route = transitRouter.calcRoute(fromFacility, toFacility, departureTime_s, null);
            float sumTravelTime_min = 0;
            double transitInTime = 0;
            int sequence = 0;
            float access_min = 0;
            float egress_min = 0;
            float access_dist_m = 0;
            float egress_dist_m =0;
            float inVehicle = 0;
            int transitTransfers = 0;
            float distance = 0;
            int pt_legs = 0;

            String linesIds = "";
            String routesIds = "";
            //Network network = scenario.getNetwork();
            for (PlanElement pe : route) {
                double this_leg_time = (((Leg) pe).getRoute().getTravelTime() / 60.);
                double this_leg_distance = (((Leg) pe).getRoute().getDistance());
                sumTravelTime_min += this_leg_time;
                if (((Leg) pe).getMode().equals("transit_walk") && sequence == 0) {
                    access_min += this_leg_time;
                    access_dist_m += this_leg_distance;
                } else if (((Leg) pe).getMode().equals("transit_walk") && sequence == route.size() - 1) {
                    egress_min += this_leg_time;
                    egress_dist_m += this_leg_distance;
                } else if (((Leg) pe).getMode().equals("pt")) {
                    Route route1 = ((Leg) pe).getRoute();
                    //commented to make it faster?
                    ExperimentalTransitRoute experimentalTransitRoute = (ExperimentalTransitRoute) route1;
                    String lineId = experimentalTransitRoute.getLineId().toString();
                    String routeId = experimentalTransitRoute.getRouteId().toString();

                    linesIds += "===" + lineId;
                    routesIds += "===" + routeId;
                    inVehicle += this_leg_time;

                    //distance += network.getLinks().get(Id.create(lineId, Link.class)).getLength();;
                    distance += this_leg_distance;
                    pt_legs++;
                    transitTransfers++;
                }
                sequence++;


            }

            if(trip.getId() % 10 == 0){
                logger.info("Completed " + trip.getId() + " trips.");
            }

            float inTransitTime = sumTravelTime_min - access_min - egress_min;
            counter.incrementAndGet();

            trip.setInVehicleTime(sumTravelTime_min);
            trip.setTransitInTime(transitInTime);
            trip.setTransitAccessTt(access_min);
            trip.setAccessDistance(access_dist_m);
            trip.setEgressDistance(egress_dist_m);
            trip.setTransitEgressTt(egress_min);
            trip.setTransitTransfers(transitTransfers);
            trip.setInVehicleTime(inVehicle);
            trip.setDistance(distance);
            trip.setLinesIds(linesIds);
            trip.setRoutesIds(routesIds);
            trip.setBeelineDist(NetworkUtils.getEuclideanDistance(trip.getOrigCoord(), trip.getDestCoord()));
        }
        //});

        //PrintWriter pw = new PrintWriter(new File(outputFolder + "skim.csv"));
        PrintWriter pw = new PrintWriter(new File(outputFileName));
        pw.println(("uniqueId,p.id,t.id,total_t,in_transit_t,access_t,egress_t,transfers,in_veh_t,dist_transit,access_dist_m,egress_dist_m,lineIDs,routeIDs,beelineDist"));
        //for(int orig : zoneMap.keySet()){
        //for (int dest : zoneMap.keySet()){
        for(PTSurveyTrip trip : tripMap.values()){
            pw.println(trip.toString());
                /*pw.print(orig);
                pw.print(",");
                pw.print(dest);
                pw.print(",");
                pw.print(transitTotalTime.getValueAt(orig , dest));
                pw.print(",");
                pw.print(transitInTime.getValueAt(orig , dest));
                pw.print(",");
                pw.print(transitAccessTt.getValueAt(orig , dest));
                pw.print(",");
                pw.print(transitEgressTt.getValueAt(orig , dest));
                pw.print(",");
                pw.print(transitTransfers.getValueAt(orig , dest));
                pw.print(",");
                pw.print(inVehicleTime.getValueAt(orig , dest));
                pw.print(",");
                pw.print(transitDistance.getValueAt(orig , dest));
                pw.println();*/
            //}
        }
        pw.close();

    }



    private static Config configureMATSim() {

        //String networkFilename = "./input/network_merged_germany_bus.xml.gz";
        String networkFilename = "C:\\models\\mito\\germanymodel\\network\\longDistanceBus/network_merged.xml.gz";
        //String transitScheduleFilename = "./input/schedule_germany_bus_mapped.xml";
        String transitScheduleFilename = "C:\\models\\mito\\germanymodel\\network\\longDistanceBus/mapped_schedule.xml";
        //String transitVehiclesFilename = "./input/vehicle_germany_bus.xml";
        String transitVehiclesFilename = "C:\\models\\mito\\germanymodel\\network\\longDistanceBus/vehicles.xml";
        String outputFolder =  "";
        //String zoneFile = "./output/skims/germany_opnv_db_rb/zone_coordinates.csv"; //?
        //String fileName = "input/MiD_trips_with_coordinates.csv";

        Config config = ConfigUtils.createConfig();

        String networkFile = networkFilename;
        String scheduleFile = transitScheduleFilename;
        String vehicleFile = transitVehiclesFilename;

        config.global().setCoordinateSystem(TransformationFactory.DHDN_GK4);

        // Network
        config.network().setInputFile(networkFile);

        //public transport
        config.transit().setUseTransit(true);
        config.transit().setTransitScheduleFile(scheduleFile);
        config.transit().setVehiclesFile(vehicleFile);
        Set<String> transitModes = new TreeSet<>();
        transitModes.add("pt");
        config.transit().setTransitModes(transitModes);
        config.controler().setOutputDirectory(outputFolder);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setFirstIteration(1);
        config.controler().setLastIteration(1);
        config.controler().setWriteEventsUntilIteration(0);
        return config;
    }


}
