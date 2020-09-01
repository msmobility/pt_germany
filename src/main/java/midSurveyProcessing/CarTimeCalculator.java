package midSurveyProcessing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

public class CarTimeCalculator {


    private LeastCostPathCalculator pathCalculator;
    private Network carNetwork;

    public CarTimeCalculator(Network carNetwork) {
        this.carNetwork = carNetwork;
        LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = new FastAStarLandmarksFactory(1);
        TravelTime myTravelTime = getAnEmptyNetworkTravelTime();
        TravelDisutility myTravelDisutility = getAnEmptyNetworkTravelDisutility();


        pathCalculator = leastCostPathCalculatorFactory.createPathCalculator(carNetwork, myTravelDisutility, myTravelTime);
        //routingModule = DefaultRoutingModules.createPureNetworkRouter(
        //        TransportMode.car, PopulationUtils.getFactory(), carNetwork, pathCalculator);


    }

    public void assignCarTravelTimes(Map<Integer, SurveyTrip> tripMap){

        for (SurveyTrip trip : tripMap.values()){


            Node fromNode = NetworkUtils.getNearestNode(carNetwork, trip.getOrigCoord());
            Node toNode = NetworkUtils.getNearestNode(carNetwork, trip.getDestCoord());

            double departureTime_s = trip.getDepartureTime_s();
            LeastCostPathCalculator.Path path = pathCalculator.calcLeastCostPath(fromNode, toNode, departureTime_s, null, null);

            double distance = 0;

            double time =  path.travelTime ;

            for (Link link : path.links) {
                distance += link.getLength();
            }

            double speed_kmh = distance / time * 3.6;
            if (speed_kmh > 121 || Double.isNaN(speed_kmh) || Double.isInfinite(speed_kmh)) {
                System.out.println("Trip with too high speed: V = " + speed_kmh + " id = " + trip.getId() );
            }

            if(trip.getId() % 10000 == 0){
                System.out.println("Completed " + trip.getId() + " trips.");
            }

            trip.setCarTravelTime(time);
            trip.setCarDistance(distance);

        }

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
