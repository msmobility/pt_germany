
import org.matsim.pt2matsim.run.Gtfs2TransitSchedule;
import org.matsim.pt2matsim.run.PublicTransitMapper;



public class RunPt2Matsim {


    public static void main(String[] args) {

        //service is rb, db, etc..
        String service = args[0];

        String folderToGtfs = "./input/gtfs/" + service + "/";
        String sampleDayParam = "dayWithMostTrips";
        String outputCoordSystem = "EPSG:31468";
        String scheduleFile = "./output/" + service + "/scheduleMostTrips.xml";
        String vehicleFile =  "./output/" + service + "/vehiclesMostTrips.xml";

        Gtfs2TransitSchedule.run(folderToGtfs, sampleDayParam, outputCoordSystem, scheduleFile, vehicleFile );

    }

}
