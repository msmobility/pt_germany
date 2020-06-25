package midSurveyProcessing;

import de.tum.bgu.msm.data.Zone;
import de.tum.bgu.msm.matsim.SiloMatsimUtils;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.util.concurrent.ConcurrentExecutor;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstraFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CarTimeCalculator {


    private RoutingModule routingModule;

    public CarTimeCalculator(Network carNetwork) {
        LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = new FastAStarLandmarksFactory(1);
        TravelTime myTravelTime = getAnEmptyNetworkTravelTime();
        TravelDisutility myTravelDisutility = getAnEmptyNetworkTravelDisutility();


        routingModule = DefaultRoutingModules.createPureNetworkRouter(
                TransportMode.car, PopulationUtils.getFactory(), carNetwork, leastCostPathCalculatorFactory.createPathCalculator(carNetwork, myTravelDisutility, myTravelTime));


    }

    public void assignCarTravelTimes(Map<Integer, SurveyTrip> tripMap){

        for (SurveyTrip trip : tripMap.values()){


            //Node originNode = NetworkUtils.getNearestNode(carNetwork, trip.getOrigCoord());
            //Node destNode = NetworkUtils.getNearestNode(carNetwork, trip.getDestCoord());

            ActivityFacilitiesFactoryImpl activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
            Facility fromFacility = ((ActivityFacilitiesFactory) activityFacilitiesFactory).createActivityFacility(Id.create(1, ActivityFacility.class),
                    trip.getOrigCoord());
            Facility toFacility = ((ActivityFacilitiesFactory) activityFacilitiesFactory).createActivityFacility(Id.create(2, ActivityFacility.class),
                    trip.getDestCoord());

            double departureTime_s = trip.getDepartureTime_s();
            List<? extends PlanElement> planElements = routingModule.calcRoute(fromFacility, toFacility, departureTime_s, null);

            double arrivalTime = departureTime_s;
            double distance = 0;

            if (!planElements.isEmpty()) {
                final Leg lastLeg = (Leg) planElements.get(planElements.size() - 1);
                arrivalTime = lastLeg.getDepartureTime() + lastLeg.getTravelTime();
                distance += lastLeg.getRoute().getDistance();
            }

            double time = arrivalTime - departureTime_s;

            trip.setCarTravelTime(time);
            trip.setCarDistance(distance);

            if(trip.getId() % 10000 == 0){
                System.out.println("Completed " + trip.getId() + " trips.");
            }

        }








//                            toNodes.add(new InitialNode(originNode, 0., 0.));
//                        }
//                    }
//
//                    ImaginaryNode aggregatedToNodes = MultiNodeDijkstra.createImaginaryNode(toNodes);
//
//                    for (Zone origin : partition) {
//                        Node originNode = NetworkUtils.getNearestNode(carNetwork, matsimData.getZoneConnectorManager().getCoordsForZone(origin).get(0));
//                        calculator.calcLeastCostPath(originNode, aggregatedToNodes, Properties.get().transportModel.peakHour_s, null, null);
//                        for (Zone destination : zones) {
//                            Node destinationNode = NetworkUtils.getNearestNode(carNetwork, matsimData.getZoneConnectorManager().getCoordsForZone(destination).get(0));
//                            double travelTime = calculator.constructPath(originNode, destinationNode, Properties.get().transportModel.peakHour_s).travelTime;
//
//                            //convert to minutes
//                            travelTime /= 60.;
//
//                            skim.setIndexed(origin.getZoneId(), destination.getZoneId(), travelTime);
//                        }
//                    }
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//                return null;
//            });
//        }
//        executor.execute();
//


    }


    private TravelTime getAnEmptyNetworkTravelTime(){
        return new TravelTime() {
            @Override
            public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
                return link.getLength() / link.getFreespeed();
            }
        };
    }

    private TravelDisutility getAnEmptyNetworkTravelDisutility(){
        return new TravelDisutility() {
            @Override
            public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
                return link.getLength() / link.getFreespeed();
            }

            @Override
            public double getLinkMinimumTravelDisutility(Link link) {
                return link.getLength() / link.getFreespeed();
            }
        };

    }



}
