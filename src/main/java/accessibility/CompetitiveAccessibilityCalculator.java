package accessibility;

import de.tum.bgu.msm.util.MitoUtil;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CompetitiveAccessibilityCalculator {

    String zoneFile = "D:/data/cape/zoneSystem/TAZs_completed_11879.csv";
    Map<Integer, ZoneForSkim> zoneMap = new HashMap<>();
    Map<String, Map<Double, Map<Double, Map<Integer, Double>>>> competition = new HashMap<>();
    Map<String, Map<Double, Map<Double, Map<Integer, Double>>>> accessibility = new HashMap<>();
    Map<String, Map<Double, Map<Double, Map<Integer, Double>>>> accessibilityNormalized = new HashMap<>();

    public static void main(String[] args) throws IOException {
        CompetitiveAccessibilityCalculator accessibilityCalculator = new CompetitiveAccessibilityCalculator();
        accessibilityCalculator.readZoneData();
        accessibilityCalculator.run();
        accessibilityCalculator.printResultWide();
        accessibilityCalculator.printResult();
    }

    private void readZoneData() throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(zoneFile));
        String[] header = br.readLine().split(",");
        int posId = MitoUtil.findPositionInArray("TAZ_id", header);
        int posPop = MitoUtil.findPositionInArray("population", header);
        int posEmp = MitoUtil.findPositionInArray("emp", header);
        int posUniStudent = MitoUtil.findPositionInArray("numUniStudents", header);
        int posEmpResearch = MitoUtil.findPositionInArray("empResearch", header);
        int posZoneType = MitoUtil.findPositionInArray("Zone_Type", header);

        String line;

        while ((line = br.readLine()) != null) {
            int id = Integer.parseInt(line.split(",")[posId]);
            double pop = Double.parseDouble(line.split(",")[posPop]);
            double emp = Double.parseDouble(line.split(",")[posEmp]);
            double uniStudents = Double.parseDouble(line.split(",")[posUniStudent]);
            double empResearch = Double.parseDouble(line.split(",")[posEmpResearch]);
            String zoneType = line.split(",")[posZoneType];

            pop = pop <= 0 ? 0 : pop;
            emp = emp <= 0 ? 0 : emp;
            uniStudents = uniStudents <= 0 ? 0 : uniStudents;
            empResearch = empResearch <= 0 ? 0 : empResearch;
            boolean isDomestic = zoneType.equals("Domestic") || zoneType.equals("domestic");

            ZoneForSkim zone = new ZoneForSkim(id, pop);
            zone.setEmployment(emp);
            zone.setUniStudents(uniStudents);
            zone.setEmpResearch(empResearch);
            zone.setDomestic(isDomestic);

            zoneMap.put(id, zone);
        }
    }

    private void run() throws IOException {

        Map<String, String> matrices = new HashMap<>();
        matrices.put("carFreeFlow", "./output/skims/freeflow_3points_pop/car_traveltimes.csv.gz");
        //matrices.put("carCongested_0800", "./output/skims/congested_0800_3points_pop/car_traveltimes.csv.gz");
        matrices.put("carCongested_1700", "./output/skims/congested_1700_3points_pop/car_traveltimes.csv.gz");
        //matrices.put("ld_rail", "./output/skims/ld_rail_with_walk/ld_rail_with_walk_pt_traveltimes.csv.gz");
        //matrices.put("ld_bus", "./output/skims/ld_bus_carlos/ld_bus_v3_pt_traveltimes.csv.gz");

        double[] alphaSets = {0.8, 1.0};
        double[] betaSets = {-0.4, -0.6, -0.8, -1.0, -2.0};
        double alpha;
        double beta;

        for (String skim : matrices.keySet()) {
            competition.put(skim, new HashMap<>());
            accessibility.put(skim, new HashMap<>());
            accessibilityNormalized.put(skim, new HashMap<>());
            SkimReader reader = new SkimReader();
            IndexedDoubleMatrix2D mat = reader.readAndConvertToDoubleMatrix2D(matrices.get(skim), 1. / 3600., zoneMap.values());
            int[] lookupArray = mat.getRowLookupArray();

            //competition
            for (double alphaSet : alphaSets) {
                alpha = alphaSet;
                competition.get(skim).put(alpha, new HashMap<>());

                for (double betaSet : betaSets) {

                    IndexedDoubleMatrix2D matCalculation = new IndexedDoubleMatrix2D(lookupArray);
                    matCalculation.assign(mat);

                    beta = betaSet;
                    competition.get(skim).get(alpha).put(beta, new HashMap<>());

                    double currentBeta = beta;
                    double currentAlpha = alpha;

                    matCalculation.forEachNonZero((i, k, x) -> Math.exp(currentBeta * x));
                    int[] rowLookupArray = matCalculation.getRowLookupArray();
                    matCalculation.forEachNonZero((i, k, x) -> x *
                            Math.pow(zoneMap.get(rowLookupArray[i]).getPopulation(), currentAlpha));

                    for (int col : matCalculation.getColumnLookupArray()) {
                        double[] array = matCalculation.viewRow(col).toNonIndexedArray();
                        double competence = Arrays.stream(array).sum();
                        this.competition.get(skim).get(alpha).get(beta).put(col, competence);
                    }
                }
            }
            System.out.println("Done competence calculation.");

            //potential
            for (double alphaSet : alphaSets) {
                alpha = alphaSet;
                accessibility.get(skim).put(alpha, new HashMap<>());
                accessibilityNormalized.get(skim).put(alpha, new HashMap<>());

                for (double betaSet : betaSets) {

                    IndexedDoubleMatrix2D matCalculation = new IndexedDoubleMatrix2D(lookupArray);
                    matCalculation.assign(mat);

                    beta = betaSet;
                    accessibility.get(skim).get(alpha).put(beta, new HashMap<>());
                    accessibilityNormalized.get(skim).get(alpha).put(beta, new HashMap<>());

                    double currentBeta = beta;
                    double currentAlpha = alpha;

                    matCalculation.forEachNonZero((i, k, x) -> Math.exp(currentBeta * x));
                    int[] rowLookupArray = matCalculation.getRowLookupArray();
                    matCalculation.forEachNonZero((i, k, x) -> x *
                            Math.pow(zoneMap.get(rowLookupArray[k]).getEmployment(), currentAlpha) / competition.get(skim).get(currentAlpha).get(currentBeta).get(rowLookupArray[k]));

                    double maxAccessibility = 0;
                    double minAccessibility = 999999;

                    for (int row : matCalculation.getRowLookupArray()) {
                        double[] array = matCalculation.viewRow(row).toNonIndexedArray();
                        double access = Arrays.stream(array).sum();
                        boolean isDomestic = zoneMap.get(row).isDomestic();

                        if (minAccessibility > access && isDomestic) {
                            minAccessibility = access;
                        }

                        if (maxAccessibility < access && isDomestic) {
                            maxAccessibility = access;
                        }

                        accessibility.get(skim).get(alpha).get(beta).put(row, access);
                    }

                    for (int taz = 1; taz <= accessibility.get(skim).get(alpha).get(beta).size(); taz++) {
                        double oldValue = accessibility.get(skim).get(alpha).get(beta).get(taz);
                        double newValue = (oldValue - minAccessibility) / (maxAccessibility - minAccessibility);
                        accessibilityNormalized.get(skim).get(alpha).get(beta).put(taz, newValue);
                    }
                }
            }
        }
        System.out.println("Done accessibility calculation.");

    }

    private void printResultWide() throws FileNotFoundException {

        PrintWriter pw = new PrintWriter("./output/accessibility/competitive_emp_11879_wide_20220117_col.csv");

        StringBuilder header = new StringBuilder();
        header.append("zone").append(",");
        for (String test : accessibility.keySet()) {
            for (Double alpha : accessibility.get(test).keySet()) {
                for (Double beta : accessibility.get(test).get(alpha).keySet()) {

                    header.append(test).append("_");
                    header.append("alpha").append("_").append(alpha).append("_");
                    header.append("beta").append("_").append(beta).append(",");

                    header.append(test).append("_");
                    header.append("alpha").append("_").append(alpha).append("_");
                    header.append("beta").append("_").append(beta).append("_").append("scaled").append(",");

                }
            }
        }
        pw.print(header);
        pw.println();

        for (int zone = 1; zone <= 11879; zone++) {
            StringBuilder string = new StringBuilder();
            string.append(zone).append(",");

            for (String test : accessibility.keySet()) {
                for (Double alpha : accessibility.get(test).keySet()) {
                    for (Double beta : accessibility.get(test).get(alpha).keySet()) {
                        string.append(accessibility.get(test).get(alpha).get(beta).get(zone)).append(",");
                        string.append(accessibilityNormalized.get(test).get(alpha).get(beta).get(zone)).append(",");
                    }
                }
            }
            pw.print(string);
            pw.println();
        }
        pw.close();
    }

    private void printResult() throws FileNotFoundException {

        PrintWriter pw = new PrintWriter("./output/accessibility/competitive_emp_11879_long_20220117_col.csv");
        pw.print("zone,skim,alpha,beta,accessibility,scaledAccessibility");
        pw.println();

        for (String test : accessibility.keySet()) {
            for (Double alpha : accessibility.get(test).keySet()) {
                for (Double beta : accessibility.get(test).get(alpha).keySet()) {
                    for (Integer zone : accessibility.get(test).get(alpha).get(beta).keySet()) {

                        StringBuilder string = new StringBuilder();
                        string.append(zone);
                        string.append(",");
                        string.append(test);
                        string.append(",");
                        string.append(alpha);
                        string.append(",");
                        string.append(beta);
                        string.append(",");
                        string.append(accessibility.get(test).get(alpha).get(beta).get(zone));
                        string.append(",");
                        string.append(accessibilityNormalized.get(test).get(alpha).get(beta).get(zone));
                        pw.print(string);
                        pw.println();
                    }
                }
            }
        }
        pw.close();
    }


}



