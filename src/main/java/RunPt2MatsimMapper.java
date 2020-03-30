
import org.matsim.pt2matsim.run.Gtfs2TransitSchedule;
import org.matsim.pt2matsim.run.PublicTransitMapper;


public class RunPt2MatsimMapper {


    public static void main(String[] args) {

        //service is rb, db, etc..
        String service = args[0];

        String configFile = "./" + service + "Config.xml";
        PublicTransitMapper.run(configFile);

    }

}
