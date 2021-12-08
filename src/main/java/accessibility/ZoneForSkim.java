package accessibility;

import de.tum.bgu.msm.data.Id;

public class ZoneForSkim implements Id {

    private int id;
    private double population;
    private double employment;
    private double uniStudents;
    private double empResearch;

    public double getEmpResearch() {
        return empResearch;
    }

    public void setEmpResearch(double empResearch) {
        this.empResearch = empResearch;
    }

    public boolean isDomestic() {
        return isDomestic;
    }

    public void setDomestic(boolean domestic) {
        isDomestic = domestic;
    }

    private boolean isDomestic;

    public ZoneForSkim(int id, double population) {
        this.id = id;
        this.population = population;
    }

    public ZoneForSkim(int id, double population, double employment, double uniStudents) {
        this.id = id;
        this.population = population;
        this.employment = employment;
        this.uniStudents = uniStudents;
    }

    @Override
    public int getId() {
        return id;
    }

    public double getPopulation() {
        return population;
    }

    public void setPopulation(double population) {
        this.population = population;
    }

    public double getEmployment() {
        return employment;
    }

    public void setEmployment(double employment) {
        this.employment = employment;
    }

    public double getUniStudents() {
        return uniStudents;
    }

    public void setUniStudents(double uniStudents) {
        this.uniStudents = uniStudents;
    }

}
