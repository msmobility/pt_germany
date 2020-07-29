package osm2Matsim;

import org.matsim.pt2matsim.run.Osm2MultimodalNetwork;
import org.matsim.pt2matsim.run.Osm2TransitSchedule;

public class convertOsmToMatsim {


    public static void main(String[] args) {




        //Osm2MultimodalNetwork.run(args[0]);
        String osmFile = "./OSMzonesWithLineDuplicatesProblem/AschaffenburgOutput.osm";
        String scheduleFile = "./RawSchedulesXML/Aschaffenburg_schedule.xml.gz";
        Osm2TransitSchedule.run(osmFile, scheduleFile, "EPSG:31468");
        

    }

}
