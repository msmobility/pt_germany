package midSurveyProcessing.ptTimeCalculator;

import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import java.io.BufferedReader;
import java.io.FileReader;

public class PT_TimeCalculator {


    public static Logger logger = Logger.getLogger(PT_TimeCalculator.class);

    private static double stopThresholdRadius_m = 1000;

    private static String networkFilename = "./input/network_merged_germany_bus.xml.gz";
    private static String transitScheduleFilename = "./input/schedule_germany_bus_mapped.xml";
    private static String transitVehiclesFilename = "./input/vehicle_germany_bus.xml";;
    private static String outputFolder =  "./output/bus/";
    //private static String zoneFile = "./output/skims/germany_opnv_db_rb/zone_coordinates.csv"; //?
    private static String tripFileName = "input/MiD_trips_with_coordinates.csv";

    public static void main(String[] args) throws FileNotFoundException {
        Config config = configureMATSim();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        TransitRouterConfig transitConfig = new TransitRouterConfig(config);
        transitConfig.setAdditionalTransferTime(60);
        transitConfig.setBeelineWalkSpeed(3/3.6);

        Map<Integer, ModelTAZ> zoneMap = readCSVOfZones();


        long startTime_s = System.currentTimeMillis() / 1000;
        AtomicInteger counter = new AtomicInteger(1);

        ActivityFacilitiesFactoryImpl activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();

        for (ModelTAZ trip : zoneMap.values()) {

            Facility fromFacility = ((ActivityFacilitiesFactory) activityFacilitiesFactory).createActivityFacility(Id.create(1, ActivityFacility.class),
                    trip.getOrigCoord());
            Facility toFacility = ((ActivityFacilitiesFactory) activityFacilitiesFactory).createActivityFacility(Id.create(2, ActivityFacility.class),
                    trip.getDestCoord());
            double transitTotalTime = trip.getTransitTotalTime();
            double transitInTime = trip.getTransitInTime();
            int transitTransfers = trip.getTransitTransfers();
            Coord origCoord = trip.getOrigCoord();
            Coord destCoord = trip.getDestCoord();
            double transitAccessTt = trip.getTransitAccessTt();
            double transitEgressTt = trip.transitEgressTt;
            double inVehicleTime = trip.getInVehicleTime();
            double transitDistance = trip.getTransitDistance();

            TransitRouter transitRouter = new TransitRouterImpl(transitConfig, scenario.getTransitSchedule());
            List<? extends PlanElement> route = transitRouter.calcRoute(fromFacility, toFacility, 10 * 60 * 60, null);
            float sumTravelTime_min = 0;
            int sequence = 0;
            float access_min = 0;
            float egress_min = 0;
            float inVehicle = 0;
            float distance = 0;
            int pt_legs = 0;
            for (PlanElement pe : route) {
                double this_leg_time = (((Leg) pe).getRoute().getTravelTime() / 60.);
                double this_leg_distance = (((Leg) pe).getRoute().getDistance());
                sumTravelTime_min += this_leg_time;
                if (((Leg) pe).getMode().equals("transit_walk") && sequence == 0) {
                    access_min += this_leg_time;
                } else if (((Leg) pe).getMode().equals("transit_walk") && sequence == route.size() - 1) {
                    egress_min += this_leg_time;
                } else if (((Leg) pe).getMode().equals("pt")) {
                    inVehicle += this_leg_time;
                    distance += this_leg_distance;
                    pt_legs++;
                }
                sequence++;
            }
            float inTransitTime = sumTravelTime_min - access_min - egress_min;

            counter.incrementAndGet();

            if (pt_legs == 0){
                //this trips are not made by transit
                if (originCoord.distanceToClosest < stopThresholdRadius_m && destinationTAZ.distanceToClosest < stopThresholdRadius_m){
                    //there are stops in the areas and probably it is possible to go by transit
                    //assume the same time as by walk
                    inVehicle = access_min;
                    inTransitTime = inVehicle;
                    //but add new access and egress
                    access_min = (float) (originTAZ.distanceToClosest / transitConfig.getBeelineWalkSpeed());
                    egress_min = (float) (destinationTAZ.distanceToClosest / transitConfig.getBeelineWalkSpeed());
                    sumTravelTime_min = access_min + egress_min + inVehicle;
                } else {
                    //there are no transit stops and the trips by transit are not reasonable
                    //stored as -1
                    sumTravelTime_min = -1;
                    inVehicle = -1;
                    inTransitTime = -1;
                    access_min = -1;
                    egress_min = -1;
                }
            }

           /* int size = zoneMap.keySet().stream().max(Integer::compareTo).get();
            Matrix transitTotalTime = new Matrix(size, size);
            transitTotalTime.fill(-1F);
            Matrix transitInTime = new Matrix(size, size);
            transitInTime.fill(-1F);
            Matrix transitTransfers = new Matrix(size, size);
            transitTransfers.fill(-1F);
            Matrix inVehicleTime = new Matrix(size, size);
            inVehicleTime.fill(-1F);
            Matrix transitAccessTt = new Matrix(size, size);
            transitAccessTt.fill(-1F);
            Matrix transitEgressTt = new Matrix(size, size);
            transitEgressTt.fill(-1F);
            Matrix transitDistance = new Matrix(size, size);
            transitDistance.fill(-1F);*/

           /*Map<ModelTAZ, ActivityFacility> facilitiesByTaz = new HashMap<>();
            zoneMap.values().parallelStream().forEach(taz -> {
                Node node = NetworkUtils.getNearestNode(scenario.getNetwork(), taz.coord);
                Id<Link> link = node.getInLinks().values().iterator().next().getId();
                ActivityFacility facility = activityFacilitiesFactory.createActivityFacility(null, node.getCoord(), link);
                if (facility != null && facility.getCoord() != null) {
                    facilitiesByTaz.put(taz, facility);
                }
            });*/
        }

        logger.warn("Assign facilities to taz");

        facilitiesByTaz.keySet().parallelStream().forEach(originTAZ -> {
            TransitRouter transitRouter = new TransitRouterImpl(transitConfig, scenario.getTransitSchedule());
            ActivityFacility originFacility = facilitiesByTaz.get(originTAZ);
            for (ModelTAZ destinationTAZ : facilitiesByTaz.keySet()) {
                if (originTAZ.id < destinationTAZ.id) {
                    ActivityFacility destinationFacility = facilitiesByTaz.get(destinationTAZ);
                    List<? extends PlanElement> route = transitRouter.calcRoute(originFacility, destinationFacility, 10 * 60 * 60, null);
                    float sumTravelTime_min = 0;
                    int sequence = 0;
                    float access_min = 0;
                    float egress_min = 0;
                    float inVehicle = 0;
                    float distance = 0;
                    int pt_legs = 0;
                    for (PlanElement pe : route) {
                        double this_leg_time = (((Leg) pe).getRoute().getTravelTime() / 60.);
                        double this_leg_distance = (((Leg) pe).getRoute().getDistance());
                        sumTravelTime_min += this_leg_time;
                        if (((Leg) pe).getMode().equals("transit_walk") && sequence == 0) {
                            access_min += this_leg_time;
                        } else if (((Leg) pe).getMode().equals("transit_walk") && sequence == route.size() - 1) {
                            egress_min += this_leg_time;
                        } else if (((Leg) pe).getMode().equals("pt")) {
                            inVehicle += this_leg_time;
                            distance += this_leg_distance;
                            pt_legs++;
                        }
                        sequence++;
                    }

                    float inTransitTime = sumTravelTime_min - access_min - egress_min;

                    counter.incrementAndGet();

                    if (pt_legs == 0){

                        //this trips are not made by transit

                        if (originTAZ.distanceToClosest < stopThresholdRadius_m && destinationTAZ.distanceToClosest < stopThresholdRadius_m){

                            //there are stops in the areas and probably it is possible to go by transit

                            //assume the same time as by walk

                            inVehicle = access_min;
                            inTransitTime = inVehicle;

                            //but add new access and egress
                            access_min = (float) (originTAZ.distanceToClosest / transitConfig.getBeelineWalkSpeed());
                            egress_min = (float) (destinationTAZ.distanceToClosest / transitConfig.getBeelineWalkSpeed());
                            sumTravelTime_min = access_min + egress_min + inVehicle;
                        } else {
                            //there are no transit stops and the trips by transit are not reasonable
                            //stored as -1
                            sumTravelTime_min = -1;
                            inVehicle = -1;
                            inTransitTime = -1;
                            access_min = -1;
                            egress_min = -1;
                        }
                    }

                    /*transitTotalTime.setValueAt(originTAZ.id, destinationTAZ.id, sumTravelTime_min);
                    transitInTime.setValueAt(originTAZ.id, destinationTAZ.id,inTransitTime);
                    transitAccessTt.setValueAt(originTAZ.id, destinationTAZ.id, access_min);
                    transitEgressTt.setValueAt(originTAZ.id, destinationTAZ.id, egress_min);
                    transitTransfers.setValueAt(originTAZ.id, destinationTAZ.id, pt_legs - 1);
                    inVehicleTime.setValueAt(originTAZ.id, destinationTAZ.id, inVehicle);
                    transitDistance.setValueAt(originTAZ.id, destinationTAZ.id, distance);*/

                    //and the other half matrix
                    /*transitTotalTime.setValueAt(destinationTAZ.id, originTAZ.id, sumTravelTime_min);
                    transitInTime.setValueAt(destinationTAZ.id, originTAZ.id, inTransitTime);
                    transitAccessTt.setValueAt(destinationTAZ.id, originTAZ.id, access_min);
                    transitEgressTt.setValueAt(destinationTAZ.id, originTAZ.id, egress_min);
                    transitTransfers.setValueAt(destinationTAZ.id, originTAZ.id, pt_legs - 1);
                    inVehicleTime.setValueAt(destinationTAZ.id, originTAZ.id, inVehicle);
                    transitDistance.setValueAt(destinationTAZ.id, originTAZ.id, distance);*/

                }

                if (counter.get() % 10000 == 0) {
                    long duration = System.currentTimeMillis() / 1000 - startTime_s;
                    logger.warn(counter + " completed in " + duration + " seconds");
                }

            }

        });



        PrintWriter pw = new PrintWriter(new File(outputFolder + "skim.csv"));
        pw.println(("from,to,total_t,in_transit_t,acces_t,egress_t,transfers,in_veh_t,dist"));
        //for(int orig : zoneMap.keySet()){
            //for (int dest : zoneMap.keySet()){
            for(ModelTAZ trip : zoneMap.values()){
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
                pw.print(",");inT
                pw.print(inVehicleTime.getValueAt(orig , dest));
                pw.print(",");
                pw.print(transitDistance.getValueAt(orig , dest));
                pw.println();*/
            //}
        }
            pw.close();
    }


    private static Config configureMATSim() {
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



    static class ModelTAZ {
        boolean served;
        int id;
        double distanceToClosest;
        int personId;
        int tripId;
        int departureTime_s;
        Coord origCoord;
        Coord destCoord;
        double transitTotalTime;
        double transitInTime;
        double transitAccessTt;
        double transitEgressTt;
        int transitTransfers;
        double inVehicleTime;
        double transitDistance;

        public ModelTAZ(int id, boolean served, double distanceToClosest, int personId, int tripId, int departureTime_s, Coord origCoord, Coord destCoord) {
            this.served = served;
            this.id = id;
            this.distanceToClosest = distanceToClosest;
            this.personId = personId;
            this.tripId = tripId;
            this.departureTime_s = departureTime_s;
            this.origCoord = origCoord;
            this.destCoord = destCoord;
            this.transitTotalTime = transitTotalTime;
            this.transitInTime = transitInTime;
            this.transitAccessTt = transitAccessTt;
            this.transitEgressTt = transitEgressTt;
            this.transitTransfers = transitTransfers;
            this.inVehicleTime = inVehicleTime;
            this.transitDistance = transitDistance;
        }
        public int getId() {
            return id;
        }

        public int getPersonId() {
            return personId;
        }

        public int getTripId() {
            return tripId;
        }

        public Coord getOrigCoord() {
            return origCoord;
        }

        public Coord getDestCoord() {
            return destCoord;
        }

        public double getTransitTotalTime() {
            return transitTotalTime;
        }

        public double getTransitInTime() {
            return transitInTime;
        }

        public double getTransitAccessTt() {
            return transitAccessTt;
        }

        public double getTransitEgressTt() {
            return transitEgressTt;
        }

        public int getTransitTransfers() {
            return transitTransfers;
        }

        public double getInVehicleTime() {
            return inVehicleTime;
        }

        public double getTransitDistance() {
            return transitDistance;
        }

    }



    private static Map<Integer, ModelTAZ> readCSVOfZones() {
        Random random = new Random();

        Map<Integer, ModelTAZ> map = new HashMap<>();
        String line;
        try {

            BufferedReader bufferReader = new BufferedReader(new FileReader(tripFileName));
            String headerLine = bufferReader.readLine();
            String[] header = headerLine.split(",");
            /*int posId = SiloUtil.findPositionInArray("ZONE", header);
            int posX = SiloUtil.findPositionInArray("X", header);
            int posY = SiloUtil.findPositionInArray("Y", header);*/

            //int posServed = SiloUtil.findPositionInArray("served", header);

            //int posDistToClosest = SiloUtil.findPositionInArray("dist", header);
            int indexPerson = SiloUtil.findPositionInArray("p.id", header);
            int indexTrip = SiloUtil.findPositionInArray("t.id", header);
            int indexH = SiloUtil.findPositionInArray("t.origin_time_hr", header);
            int indexM = SiloUtil.findPositionInArray("t.origin_time_min", header);
            int indexOrigX = SiloUtil.findPositionInArray("origin_coordX", header);
            int indexOrigY = SiloUtil.findPositionInArray("origin_coordY", header);
            int indexDestX = SiloUtil.findPositionInArray("destination_coordX", header);
            int indexDestY = SiloUtil.findPositionInArray("destination_coordY", header);
            int indexOrigCellSize = SiloUtil.findPositionInArray("t.origin_cell_size", header);
            int indexDestCellSize = SiloUtil.findPositionInArray("t.destination_cell_size", header);

            int id = 0;

            while ((line = bufferReader.readLine()) != null) {
                String[] splitLine = line.split(",");
                /*int id = Integer.parseInt(splitLine[posId]);
                double x = Double.parseDouble(splitLine[posX]);
                double y = Double.parseDouble(splitLine[posY]);*/
                // boolean served = Boolean.parseBoolean(splitLine[posServed]);
                //double distanceToClosest = Double.parseDouble(splitLine[posDistToClosest]);
                int personId = Integer.parseInt(splitLine[indexPerson]);
                int tripId = Integer.parseInt(splitLine[indexTrip]);
                int hour = Integer.parseInt(splitLine[indexH]);
                int minute = Integer.parseInt(splitLine[indexM]);
                double origX = Double.parseDouble(splitLine[indexOrigX]);
                double origY = Double.parseDouble(splitLine[indexOrigY]);
                double destX = Double.parseDouble(splitLine[indexDestX]);
                double destY = Double.parseDouble(splitLine[indexDestY]);
                int origCellSize = Integer.parseInt(splitLine[indexOrigCellSize]);
                int destCellSize = Integer.parseInt(splitLine[indexDestCellSize]);

                Coord origCoord = new Coord(origX + (random.nextDouble() - 0.5) * origCellSize, origY + (random.nextDouble() - 0.5) * origCellSize);
                Coord destCoord = new Coord(destX + (random.nextDouble() - 0.5) * destCellSize, destY + (random.nextDouble() - 0.5) * destCellSize);
                map.put(id, new ModelTAZ(id, true,0.0, personId, tripId, hour * 3600 + minute * 60, origCoord, destCoord));
                id++;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.warn("Read " + map.size() + " TAZs");
        return map;
    }

}