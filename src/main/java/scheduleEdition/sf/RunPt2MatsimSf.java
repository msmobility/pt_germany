package scheduleEdition.sf;

import org.matsim.pt2matsim.run.Gtfs2TransitSchedule;

import java.io.File;


public class RunPt2MatsimSf {


    public static void main(String[] args) {

        for (String service : args){
            String folderToGtfs = "./gtfs/" + service + "/";
            String sampleDayParam = "dayWithMostTrips";
            String outputCoordSystem = epsg7131;

            String scheduleFile = "./matsim/" + service + "/schedule.xml";
            String vehicleFile =  "./matsim/" + service + "/vehicles.xml";

            File file = new File(scheduleFile);
            file.getParentFile().mkdirs();

            Gtfs2TransitSchedule.run(folderToGtfs, sampleDayParam, outputCoordSystem, scheduleFile, vehicleFile);
        }


    }


    private static String epsg7131 = "PROJCS[\"unnamed\",GEOGCS[\"GRS 1980(IUGG, 1980)\",DATUM[\"unknown\",SPHEROID[\"GRS80\",6378137,298.257222101]],PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",37.75],PARAMETER[\"central_meridian\",-122.45],PARAMETER[\"scale_factor\",1.000007],PARAMETER[\"false_easting\",48000],PARAMETER[\"false_northing\",24000],UNIT[\"Meter\",1],AUTHORITY[\"epsg\",\"7131\"]]";
}
