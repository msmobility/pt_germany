package midSurveyProcessing;

import de.tum.bgu.msm.utils.SiloUtil;
import org.matsim.api.core.v01.Coord;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SurveyReader {

    private Random random = new Random();


    Map<Integer, SurveyTrip> readSurvey(String fileName) throws IOException {

        Map<Integer, SurveyTrip> tripMap = new HashMap<>();

        BufferedReader reader = new BufferedReader(new FileReader(fileName));

        String[] header = reader.readLine().split(",");

        int indexPerson = SiloUtil.findPositionInArray("p.id", header);
        int indexTrip = SiloUtil.findPositionInArray("t.id", header);
        int indexH = SiloUtil.findPositionInArray("t.origin_time_hr", header);
        int indexM = SiloUtil.findPositionInArray("t.origin_time_min", header);
        int indexOrigX = SiloUtil.findPositionInArray("origin_coordX", header);
        int indexOrigY = SiloUtil.findPositionInArray("origin_coordY", header);
        int indexDestX = SiloUtil.findPositionInArray("destination_coordX", header);
        int indexDestY = SiloUtil.findPositionInArray("destination_coordY", header);
        int indexOrigCellSize = SiloUtil.findPositionInArray("t.origin_cell_size", header);
        int indexDestCellSize = SiloUtil.findPositionInArray("t.destination_cell_size", header);


        int id = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            String[] splitLine = line.split(",");

            int personId = Integer.parseInt(splitLine[indexPerson]);
            int tripId = Integer.parseInt(splitLine[indexTrip]);
            int hour = Integer.parseInt(splitLine[indexH]);
            int minute = Integer.parseInt(splitLine[indexM]);
            double origX = Double.parseDouble(splitLine[indexOrigX]);
            double origY = Double.parseDouble(splitLine[indexOrigY]);
            double destX = Double.parseDouble(splitLine[indexDestX]);
            double destY = Double.parseDouble(splitLine[indexDestY]);
            int origCellSize = Integer.parseInt(splitLine[indexOrigCellSize]);
            int destCellSize = Integer.parseInt(splitLine[indexDestCellSize]);


            Coord origCoord = new Coord(origX + (random.nextDouble() - 0.5) * origCellSize, origY + (random.nextDouble() - 0.5) * origCellSize);
            Coord destCoord = new Coord(destX + (random.nextDouble() - 0.5) * destCellSize, destY + (random.nextDouble() - 0.5) * destCellSize);
            SurveyTrip trip = new SurveyTrip(id, personId, tripId, hour * 3600 + minute * 60, origCoord, destCoord);
            tripMap.put(id, trip);
            id++;
        }

        System.out.println("Read " + id++ + " trips");

        return tripMap;


    }

}
