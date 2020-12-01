package midSurveyProcessing.ptTimeCalculator;

import de.tum.bgu.msm.utils.SiloUtil;
import midSurveyProcessing.SurveyTrip;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
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
import java.util.Map;

public class PTTimeCalculator {

    private RoutingModule routingModule;

    public PTTimeCalculator(Network ptNetwork) {

        AtomicInteger counter = new AtomicInteger(1);
        long startTime_s = System.currentTimeMillis() / 1000;
        ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();

        //public void assignPTTravelTimes(Map<Integer, PTSurveyTrip > tripMap){

        //for (PTSurveyTrip trip : tripMap.values()) {

    }
}
