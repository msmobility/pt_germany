package scheduleEdition.sf;

import org.matsim.pt2matsim.run.PublicTransitMapper;


public class RunPt2MatsimMapperSf {


    public static void main(String[] args) {

        //service is rb, db, etc. It is the prefix of the config file
        String service = "ld_rb";

        /**
         * The config file must include the paths to schedule files, road network and other settings
         * regarding which modes are routed on the road network and which ones are not.
         */
        String configFile = "./mapperConfig.xml";
        PublicTransitMapper.run(configFile);

    }

}
