import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class RunScheduleValidator {

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {


        String service = args[0];
        String scheduleFile = "./output/" + service + "/mapped_schedule.xml";
        String networkFile = "./output/" + service + "/network_merged.xml.gz";

        TransitScheduleValidator.main(new String[]{scheduleFile, networkFile});
        
        
    }
    
}
