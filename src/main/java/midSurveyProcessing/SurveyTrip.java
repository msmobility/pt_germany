package midSurveyProcessing;

import org.matsim.api.core.v01.Coord;

public class SurveyTrip {


    private int id;
    private int personId;
    private int tripId;
    private int departureTime_s;

    private Coord origCoord;
    private Coord destCoord;

    private double carTravelTime;

    private double carDistance;

    public SurveyTrip(int id, int personId, int tripId, int departureTime_s, Coord origCoord, Coord destCoord) {
        this.id = id;
        this.personId = personId;
        this.tripId = tripId;
        this.departureTime_s = departureTime_s;
        this.origCoord = origCoord;
        this.destCoord = destCoord;
    }

    public void setCarTravelTime(double carTravelTime) {
        this.carTravelTime = carTravelTime;
    }


    public int getId() {
        return id;
    }

    public int getPersonId() {
        return personId;
    }

    public int getTripId() {
        return tripId;
    }

    public int getDepartureTime_s() {
        return departureTime_s;
    }

    public Coord getOrigCoord() {
        return origCoord;
    }

    public Coord getDestCoord() {
        return destCoord;
    }

    public double getCarTravelTime() {
        return carTravelTime;
    }

    public void setCarDistance(double distance) {
        this.carDistance = distance;
    }

    public double getCarDistance() {
        return carDistance;
    }

    @Override
    public String toString() {
        //pw.println("uniqueId,p.id,t.id,time_car,distance_car");
        return id + "," + personId + "," + tripId + "," + carTravelTime + "," + carDistance;
    }
}
