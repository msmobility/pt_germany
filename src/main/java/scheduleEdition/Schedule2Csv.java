package scheduleEdition;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV2;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt2matsim.plausibility.PlausibilityCheck;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

public class Schedule2Csv {

    public static void main(String[] args) throws FileNotFoundException {


        Scenario scenarioOne = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new TransitScheduleReader(scenarioOne).readFile("./output/ld_train_2/mapped_schedule.xml");
        String csvFile = "./output/ld_train_2/lines.csv" ;

        PrintWriter pw = new PrintWriter(new File(csvFile));
        pw.println("line,line_name,route,mode,seq,stop_id,stop_name,stop_x,stop_y");

        TransitSchedule transitSchedule = scenarioOne.getTransitSchedule();



        for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
            for (TransitRoute transitRoute : transitLine.getRoutes().values()){
                List<TransitRouteStop> stops = transitRoute.getStops();
                int seq = 0;
                for (TransitRouteStop stop : stops){
                    StringBuilder sb = new StringBuilder();
                    sb.append(transitLine.getId()).append(",");
                    String name = transitLine.getName();
                    if (name != null){
                        name  = name.replace(",", "-");
                    }
                    sb.append(name).append(",");
                    sb.append(transitRoute.getId()).append(",");
                    sb.append(transitRoute.getTransportMode()).append(",");
                    sb.append(seq).append(",");
                    sb.append(stop.getStopFacility().getId()).append(",");
                    String name1 = stop.getStopFacility().getName();
                    if (name1 != null){
                        name1  = name1.replace(",", "-");
                    }
                    sb.append(name1).append(",");
                    sb.append(stop.getStopFacility().getCoord().getX()).append(",");
                    sb.append(stop.getStopFacility().getCoord().getY()).append(",");

                    pw.println(sb);

                    seq++;
                }
            }
        }

        pw.close();





    }
}
