package skimCalculator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

public class TollUtils {

    private static final Logger log = Logger.getLogger(TollUtils.class);

    public static boolean hasToll(Link l) {
        if (l.getAttributes().getAttribute("type").toString().equals("motorway")){
            return true;
        } else {
            return false;
        }

    }

    static TravelDisutility getTravelDisutilityToAvoidTolls(TravelTime travelTime){
        TravelTime thisTravelTime;
        if (travelTime == null) {
            log.warn("TimeCalculator is null so FreeSpeedTravelTimes will be calculated!");
            thisTravelTime = new FreeSpeedTravelTime();
        } else thisTravelTime = travelTime;

        TravelDisutility td = new TravelDisutility() {
            @Override
            public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
                if (hasToll(link)){
                    return link.getLength()/0.0001;
                } else {
                    return  thisTravelTime.getLinkTravelTime(link, time, person, vehicle);
                }
            }

            @Override
            public double getLinkMinimumTravelDisutility(Link link) {
                return thisTravelTime.getLinkTravelTime(link, Time.UNDEFINED_TIME, null, null);
            }
        };
        return td;
    }




}
