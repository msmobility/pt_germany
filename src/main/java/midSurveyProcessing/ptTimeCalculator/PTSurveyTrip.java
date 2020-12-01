package midSurveyProcessing.ptTimeCalculator;

import org.matsim.api.core.v01.Coord;

public class PTSurveyTrip {

    int id;
    //boolean served;
    //double distanceToClosest;
    int personId;
    int tripId;
    int departureTime_s;
    Coord origCoord;
    Coord destCoord;

    double transitTotalTime;
    double transitInTime;
    double transitAccessTt;
    double transitEgressTt;
    int transitTransfers;
    double inVehicleTime;
    double transitDistance;
    double accessDistance;
    double egressDistance;
    String linesIds = "";
    String routesIds = "";
    double beelineDist;


    public PTSurveyTrip(int id, int personId, int tripId, int departureTime_s, Coord origCoord, Coord destCoord) {
        this.id = id;
        this.personId = personId;
        this.tripId = tripId;
        this.departureTime_s = departureTime_s;
        this.origCoord = origCoord;
        this.destCoord = destCoord;
    }

    public void setTransitTotalTime(double transitTotalTime) {
        this.transitTotalTime = transitTotalTime;
    }
    public void setTransitInTime(double transitInTime) {
        this.transitInTime = transitInTime;
    }
    public void setTransitAccessTt(double transitAccessTt) {
        this.transitAccessTt = transitAccessTt;
    }
    public void setTransitEgressTt(double transitEgressTt) {
        this.transitEgressTt = transitEgressTt;
    }
    public void setTransitTransfers(int transitTransfers) {
        this.transitTransfers = transitTransfers;
    }
    public void setInVehicleTime(double inVehicleTime) {
        this.inVehicleTime = inVehicleTime;
    }
    public void setDistance(double transitDistance) {
        this.transitDistance = transitDistance;
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

    public double getTransitTotalTime() {
        return transitTotalTime;
    }

    public double getTransitInTime() {
        return transitInTime;
    }

    public double getTransitAccessTt() {
        return transitAccessTt;
    }

    public double getTransitEgressTt() { return transitEgressTt; }

    public int getTransitTransfers() {
        return transitTransfers;
    }

    public double getInVehicleTime() {
        return inVehicleTime;
    }

    public double getTransitDistance() {
        return transitDistance;
    }

    public double getAccessDistance() {
        return accessDistance;
    }

    public void setAccessDistance(double accessDistance) {
        this.accessDistance = accessDistance;
    }

    public double getEgressDistance() {
        return egressDistance;
    }

    public void setEgressDistance(double egressDistance) {
        this.egressDistance = egressDistance;
    }

    public String getLinesIds() {
        return linesIds;
    }

    public void setLinesIds(String linesIds) {
        this.linesIds = linesIds;
    }

    public String getRoutesIds() {
        return routesIds;
    }

    public void setRoutesIds(String routesIds) {
        this.routesIds = routesIds;
    }
    public double getBeelineDist() {
        return beelineDist;
    }

    public void setBeelineDist(double beelineDist) {
        this.beelineDist = beelineDist;
    }


    @Override
    public String toString() {
        //pw.println("uniqueId,p.id,t.id,time_car,distance_car");
        return id + "," + personId + "," + tripId + "," + transitTotalTime + "," + transitInTime + "," +
                transitAccessTt + "," + transitEgressTt + "," + transitTransfers + "," + inVehicleTime + ","  + transitDistance +
                "," + accessDistance + "," + egressDistance + "," + linesIds + "," + routesIds + "," + beelineDist;
    }

}
