package midSurveyProcessing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

public class SurveyTripPrinter {
    public void printOutResults(String outputFileName, Map<Integer, SurveyTrip> tripMap) throws FileNotFoundException {

        PrintWriter pw = new PrintWriter(new File(outputFileName));

        pw.println("uniqueId,p.id,t.id,time_car,distance_car");

        for(SurveyTrip trip : tripMap.values()){
            pw.println(trip.toString());
        }

        pw.close();

    }
}
