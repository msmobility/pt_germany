package skimCalculator;

import com.pb.common.matrix.Matrix;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SkimCalculator2 {


    public static Logger logger = Logger.getLogger(SkimCalculator2.class);
    private static double stopThresholdRadius_m = -1;

    private static String networkFilename = "./output/db/network_merged.xml.gz";
    private static String transitScheduleFilename = "./output/db/mapped_schedule.xml";
    private static String transitVehiclesFilename = "./output/db/vehicles.xml";
    ;
    private static String outputFolder = "./output/skims/germany_db_2/";
    private static String zoneFile = "./output/skims/germany_db_2/zone_coordinates_with_ld_stops_2.csv";


    public static void main(String[] args) throws FileNotFoundException {
        Config config = configureMATSim();
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Map<Integer, ModelTAZ> zoneMap = readCSVOfZones();
        int size = zoneMap.keySet().stream().max(Integer::compareTo).get();
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
        transitDistance.fill(-1F);


        AtomicInteger counter = new AtomicInteger(1);

        long startTime_s = System.currentTimeMillis() / 1000;

        ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();

        Map<ModelTAZ, ActivityFacility> facilitiesByTaz = new HashMap<>();
        zoneMap.values().parallelStream().forEach(taz -> {
            Node node = NetworkUtils.getNearestNode(scenario.getNetwork(), taz.coord);
            Id<Link> link = node.getInLinks().values().iterator().next().getId();
            ActivityFacility facility = activityFacilitiesFactory.createActivityFacility(null, node.getCoord(), link);
            if (facility != null && facility.getCoord() != null) {
                facilitiesByTaz.put(taz, facility);
            }
        });

        logger.warn("Assign facilities to taz");

        TransitRouterConfig transitConfig = new TransitRouterConfig(config);
        transitConfig.setAdditionalTransferTime(60 * 15);
        transitConfig.setBeelineWalkSpeed(3 / 3.6);
        facilitiesByTaz.keySet().parallelStream().forEach(originTAZ -> {            //if (originTAZ.id == 8605) {

            TransitRouter transitRouter = new TransitRouterImpl(transitConfig, scenario.getTransitSchedule());
            PlansCalcRouteConfigGroup.ModeRoutingParams params = new PlansCalcRouteConfigGroup.ModeRoutingParams();
            params.setBeelineDistanceFactor(1d);
            params.setMode("transit_walk");
            params.setTeleportedModeSpeed(0 / 3.6);
            TransitRouterWrapper transitRouterWrapper =
                    new TransitRouterWrapper(transitRouter, scenario.getTransitSchedule(), scenario.getNetwork(),
                            DefaultRoutingModules.createTeleportationRouter("transit_walk", scenario.getPopulation().getFactory(), params));

            ActivityFacility originFacility = facilitiesByTaz.get(originTAZ);
            for (ModelTAZ destinationTAZ : facilitiesByTaz.keySet()) {
                //if (destinationTAZ.id == 7698) {
                ActivityFacility destinationFacility = facilitiesByTaz.get(destinationTAZ);
                List<? extends PlanElement> route = transitRouterWrapper.calcRoute(originFacility, destinationFacility, 10 * 60 * 60, null);
                float sumTravelTime_min = 0;
                int sequence = 0;
                float access_min = 0;
                float egress_min = 0;
                float inVehicle = 0;
                float distance = 0;
                int pt_legs = 0;
                for (PlanElement pe : route) {
                    if (pe instanceof Leg) {
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
                }

                float inTransitTime = sumTravelTime_min - access_min - egress_min;

                counter.incrementAndGet();

                if (pt_legs == 0) {
                    //this trips are not made by transit
                    if (originTAZ.distanceToClosest < stopThresholdRadius_m && destinationTAZ.distanceToClosest < stopThresholdRadius_m) {
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

                transitTotalTime.setValueAt(originTAZ.id, destinationTAZ.id, sumTravelTime_min);
                transitInTime.setValueAt(originTAZ.id, destinationTAZ.id, inTransitTime);
                transitAccessTt.setValueAt(originTAZ.id, destinationTAZ.id, access_min);
                transitEgressTt.setValueAt(originTAZ.id, destinationTAZ.id, egress_min);
                transitTransfers.setValueAt(originTAZ.id, destinationTAZ.id, pt_legs - 1);
                inVehicleTime.setValueAt(originTAZ.id, destinationTAZ.id, inVehicle);
                transitDistance.setValueAt(originTAZ.id, destinationTAZ.id, distance);

                //and the other half matrix
                transitTotalTime.setValueAt(destinationTAZ.id, originTAZ.id, sumTravelTime_min);
                transitInTime.setValueAt(destinationTAZ.id, originTAZ.id, inTransitTime);
                transitAccessTt.setValueAt(destinationTAZ.id, originTAZ.id, access_min);
                transitEgressTt.setValueAt(destinationTAZ.id, originTAZ.id, egress_min);
                transitTransfers.setValueAt(destinationTAZ.id, originTAZ.id, pt_legs - 1);
                inVehicleTime.setValueAt(destinationTAZ.id, originTAZ.id, inVehicle);
                transitDistance.setValueAt(destinationTAZ.id, originTAZ.id, distance);


            }

            if (counter.get() % 10000 == 0) {
                long duration = System.currentTimeMillis() / 1000 - startTime_s;
                logger.warn(counter + " completed in " + duration + " seconds");
            }

        });
//            }
//        }

        PrintWriter pw = new PrintWriter(new File(outputFolder + "skim.csv"));
        pw.println(("from,to,total_t,in_transit_t,acces_t,egress_t,transfers,in_veh_t,dist"));
        for (int orig : zoneMap.keySet()) {
            for (int dest : zoneMap.keySet()) {
                pw.print(orig);
                pw.print(",");
                pw.print(dest);
                pw.print(",");
                pw.print(transitTotalTime.getValueAt(orig, dest));
                pw.print(",");
                pw.print(transitInTime.getValueAt(orig, dest));
                pw.print(",");
                pw.print(transitAccessTt.getValueAt(orig, dest));
                pw.print(",");
                pw.print(transitEgressTt.getValueAt(orig, dest));
                pw.print(",");
                pw.print(transitTransfers.getValueAt(orig, dest));
                pw.print(",");
                pw.print(inVehicleTime.getValueAt(orig, dest));
                pw.print(",");
                pw.print(transitDistance.getValueAt(orig, dest));
                pw.println();


            }
        }


    }

        private static PopulationFactory getMyPopFactory () {
            return null;
        }


        private static Config configureMATSim () {

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
            Coord coord;
            boolean served;
            int id;
            double distanceToClosest;

            public ModelTAZ(int id, boolean served, Coord coord, double distanceToClosest) {
                this.coord = coord;
                this.served = served;
                this.id = id;
                this.distanceToClosest = distanceToClosest;
            }

        }


        private static Map<Integer, ModelTAZ> readCSVOfZones () {

            Map<Integer, ModelTAZ> map = new HashMap<>();
            String line;
            try {
                BufferedReader bufferReader = new BufferedReader(new FileReader(zoneFile));

                String headerLine = bufferReader.readLine();
                String[] header = headerLine.split(",");

                int posId = SiloUtil.findPositionInArray("ZONE", header);
                int posX = SiloUtil.findPositionInArray("X", header);
                int posY = SiloUtil.findPositionInArray("Y", header);
                //int posServed = SiloUtil.findPositionInArray("served", header);
                //int posDistToClosest = SiloUtil.findPositionInArray("dist", header);

                while ((line = bufferReader.readLine()) != null) {
                    String[] splitLine = line.split(",");

                    int id = Integer.parseInt(splitLine[posId]);
                    double x = Double.parseDouble(splitLine[posX]);
                    double y = Double.parseDouble(splitLine[posY]);
                    // boolean served = Boolean.parseBoolean(splitLine[posServed]);
                    //double distanceToClosest = Double.parseDouble(splitLine[posDistToClosest]);

                    map.put(id, new ModelTAZ(id, true, new Coord(x, y), 0));
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
