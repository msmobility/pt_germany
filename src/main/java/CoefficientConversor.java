import de.tum.bgu.msm.data.Mode;
import de.tum.bgu.msm.data.Purpose;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class CoefficientConversor {


    private final static double nestingCoefficient = 0.25;

    private final static double fuelCostEurosPerKm = 0.07;
    private final static double transitFareEurosPerKm = 0.12;

    //HBW    HBE,    HBS,    HBO,    NHBW,    NHBO
    //0     1       2       3       4          5
    private final static double[] VOT1500_autoD = {4.63 / 60., 4.63 / 60, 3.26 / 60, 3.26 / 60, 3.26 / 60, 3.26 / 60};
    private final static double[] VOT5600_autoD = {8.94 / 60, 8.94 / 60, 6.30 / 60, 6.30 / 60, 6.30 / 60, 6.30 / 60};
    private final static double[] VOT7000_autoD = {12.15 / 60, 12.15 / 60, 8.56 / 60, 8.56 / 60, 8.56 / 60, 8.56 / 60};

    private final static double[] VOT1500_autoP = {7.01 / 60, 7.01 / 60, 4.30 / 60, 4.30 / 60, 4.30 / 60, 4.30 / 60};
    private final static double[] VOT5600_autoP = {13.56 / 60, 13.56 / 60, 8.31 / 60, 8.31 / 60, 8.31 / 60, 8.31 / 60};
    private final static double[] VOT7000_autoP = {18.43 / 60, 18.43 / 60, 11.30 / 60, 11.30 / 60, 11.30 / 60, 11.30 / 60};

    private final static double[] VOT1500_transit = {8.94 / 60, 8.94 / 60, 5.06 / 60, 5.06 / 60, 5.06 / 60, 5.06 / 60};
    private final static double[] VOT5600_transit = {17.30 / 60, 17.30 / 60, 9.78 / 60, 9.78 / 60, 9.78 / 60, 9.78 / 60};
    private final static double[] VOT7000_transit = {23.50 / 60, 23.50 / 60, 13.29 / 60, 13.29 / 60, 13.29 / 60, 13.29 / 60};

    private final static double[][] intercepts = {
            //Auto driver, Auto passenger, bicyle, bus, train, tram or metro, walk
            //HBW
            {0.0, 0.64, 2.98, 2.95, 2.87, 3.03, 5.84},
            //HBE
            {0.0, 1.25, 2.82, 2.15, 1.73, 1.97, 5.14},
            //HBS
            {0.0, 1.27, 2.58, 1.80, 1.36, 1.76, 5.01},
            //HBO
            {0.0, 1.14, 1.38, 1.36, 1.08, 1.46, 3.74},
            //NHBW
            {0.0, 0.68, 2.02, 0.65, 1.21, 1.0, 4.74},
            //NHBO
            {0.0, 1.23, 1.08, 0.56, 0.41, 0.59, 2.89}
    };

    private final static double[][] betaAge = {
            //HBW
            {0.0, -0.0037, 0.0, -0.016, -0.017, -0.014, 0.0},
            //HBE
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBS
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBW
            {0.0, -0.0045, 0.0, 0.0, -0.0059, 0.0, -0.011},
            //NHBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };

    private final static double[][] betaMale = {
            //HBW
            {0.0, -0.16, 0.22, -0.28, -0.25, -0.18, 0.0},
            //HBE
            {0.0, -0.17, 0.0, -0.14, -0.15, -0.15, 0.0},
            //HBS
            {0.0, -0.47, -0.14, -0.62, -0.47, -0.53, -0.15},
            //HBO
            {0.0, -0.27, 0.17, -0.13, 0.0, -0.063, -0.13},
            //NHBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBO
            {0.0, -0.24, 0.0, -0.20, -0.23, -0.18, -0.073}
    };

    private final static double[][] betaDriversLicense = {
            //HBW
            {0.0, -1.03, -1.86, -2.25, -2.09, -2.14, -2.16},
            //HBE
            {0.0, -1.26, -0.43, -1.23, -0.75, -0.77, -0.55},
            //HBS
            {0.0, -1.43, -1.86, -2.43, -2.46, -2.39, -2.10},
            //HBO
            {0.0, -1.34, -1.51, -1.91, -1.66, -1.74, -1.30},
            //NHBW
            {0.0, -0.94, -1.56, -1.61, -1.67, -1.37, -1.43},
            //NHBO
            {0.0, -1.40, -1.49, -2.02, -1.74, -1.77, -1.44}
    };

    private final static double[][] betaHhSize = {
            //HBW
            {0.0, 0.063, 0.25, 0.17, 0.18, 0.15, 0.0},
            //HBE
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBS
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBO
            {0.0, 0.0, 0.0, -0.11, -0.11, -0.15, -0.190},
            //NHBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };

    private final static double[][] betaHhAutos = {
            //HBW
            {0.0, -0.16, -1.11, -1.27, -1.26, -1.29, -0.73},
            //HBE
            {0.0, -0.11, -0.56, -0.52, -0.56, -0.70, -0.68},
            //HBS
            {0.0, -0.03, -0.81, -1.88, -1.73, -1.88, -0.86},
            //HBO
            {0.0, -0.029, -0.57, -1.54, -1.56, -1.72, -0.300},
            //NHBW
            {0.0, -0.11, -1.12, -1.23, -1.44, -1.52, -0.47},
            //NHBO
            {0.0, -0.029, -0.73, -0.80, -0.85, -0.86, -0.40}
    };

    private final static double[][] betaDistToRailStop = {
            //HBW
            {0.0, 0.0, 0.0, -0.36, -0.39, -0.40, 0.0},
            //HBE
            {0.0, 0.0, 0.0, -0.28, -0.26, -0.46, 0.0},
            //HBS
            {0.0, 0.0, 0.0, -0.87, -0.68, -1.02, 0.0},
            //HBO
            {0.0, 0.0, 0.0, -0.61, -0.57, -0.58, -0.0650},
            //NHBW
            {0.0, 0.0, 0.0, -0.24, 0.0, -0.16, -0.37},
            //NHBO
            {0.0, 0.0, 0.0, -0.40, -0.44, -0.48, 0.0}
    };


    private final static double[][] betaCoreCitySG = {
            //HBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBE
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBS
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };

    private final static double[][] betaMediumSizedCitySG = {
            //HBHW
            {0.0, 0.0, -0.29, -0.70, -0.75, -1.05, -0.59},
            //HBE
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBS
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };

    private final static double[][] betaTownSG = {
            //HBW
            {0.0, 0.071, -0.39, -0.86, -0.88, -1.22, -0.89},
            //HBE
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBS
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };

    private final static double[][] betaRuralSG = {
            //HBW
            {0.0, 0.071, -0.39, -0.86, -0.88, -1.22, -0.890},
            //HBE
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBS
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };

    private final static double[][] betaAgglomerationUrbanR = {
            //HBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBE
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBS
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };

    private final static double[][] betaRuralR = {
            //HBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBE
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBS
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBO
            {0.0, 0.0, 0.0, -0.70, -0.91, -1.12, 0.0}
    };

    private final static double[][] betaGeneralizedCost = {
            //HBW
            {-0.0088, -0.0088, 0.0, -0.0088, -0.0088, -0.0088, 0.00},
            //HBE
            {-0.0025, -0.0025, 0.0, -0.0025, -0.0025, -0.0025, 0.0},
            //HBS
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBO
            {-0.0012, -0.0012, 0.0, -0.0012, -0.0012, -0.0012, 0.00},
            //NHBW
            {-0.0034, -0.0034, 0.0, -0.0034, -0.0034, -0.0034, 0.0},
            //NHBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };


    private final static double[][] betaHhChildren = {
            //HBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBE
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBS
            {0.0, -0.051, 0.0, 0.0, 0.0, 0.0, -0.17},
            //HBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };

    private final static double[][] betaGeneralizedCost_Squared = {
            //HBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBE
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBS
            {-0.0000068, -0.0000068, 0.0, -0.0000068, -0.0000068, -0.0000068, 0.0},
            //HBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBO
            {-0.000017, -0.000017, 0.0, -0.000017, -0.000017, -0.000017, 0.0}
    };

    private final static double[][] betaTripLength = {
            //HBW
            {0.0, 0.0, -0.32, 0.0, 0.0, 0.0, -2.02},
            //HBE
            {0.0, 0.0, -0.42, 0.0, 0.0, 0.0, -1.71},
            //HBS
            {0.0, 0.0, -0.42, 0.0, 0.0, 0.0, -1.46},
            //HBO
            {0.0, 0.0, -0.15, 0.0, 0.0, 0.0, -0.680},
            //NHBW
            {0.0, 0.0, -0.28, 0.0, 0.0, 0.0, -1.54},
            //NHBO
            {0.0, 0.0, -0.15, 0.0, 0.0, 0.0, -0.57}
    };

    private final static double[][] betaMunichTrip = {
            //HBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBE
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBS
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //HBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBW
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            //NHBO
            {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };

    public static void main(String[] args) throws FileNotFoundException {
        Purpose[] purposes = Purpose.values();
        PrintWriter pw = new PrintWriter("coefficients.csv");
        pw.print("purpose,variable");
        for (int j = 0; j < 7; j++){
            String mode = Mode.values()[j].toString();
            pw.print(",");
            pw.print(mode);
        }
        pw.println();

        Map<String, double[][]> variables = new HashMap<>();
        variables.put("intercept", intercepts);
        variables.put("age", betaAge);
        variables.put("isMale", betaMale);
        variables.put("hasLicense", betaDriversLicense);
        variables.put("hhSize", betaHhSize);
        variables.put("hhAutos", betaHhAutos);
        variables.put("distanceToNearestRailStop", betaDistToRailStop);
        variables.put("hhChildren", betaHhChildren);
        variables.put("isCoreCity", betaCoreCitySG);
        variables.put("isMediumCity", betaMediumSizedCitySG);
        variables.put("isTown", betaTownSG);
        variables.put("isRural", betaRuralSG);
        variables.put("isAgglomerationOrUrbanR", betaAgglomerationUrbanR);
        variables.put("isRuralR", betaRuralR);
        variables.put("gc", betaGeneralizedCost);
        variables.put("gc_squared", betaGeneralizedCost_Squared);
        variables.put("distance", betaTripLength);
        variables.put("isMunich", betaMunichTrip);



        for (int i = 0; i < 6; i++){
            String purpose = purposes[i].toString();
            for (String variable : variables.keySet()){
                pw.print(purpose + "," + variable);
                for (int j = 0; j < 7; j++){
                    //String mode = Mode.values()[j].toString();
                    pw.print(",");
                    pw.print(variables.get(variable)[i][j]);
                }
                pw.println();
            }
        }

        double[] zeros = new double[]{0,0,0,0,0,0};

        Map<String, double[][]> vots = new HashMap<>();
        vots.put("vot_under_1500", new double[][]{VOT1500_autoD,
                VOT1500_autoP,
                zeros,
                VOT1500_transit,
                VOT1500_transit,
                VOT1500_transit,
                zeros});

        vots.put("vot_1500_to_5600", new double[][]{VOT5600_autoD,
                VOT5600_autoP,
                zeros,
                VOT5600_transit,
                VOT5600_transit,
                VOT5600_transit,
                zeros});

        vots.put("vot_above_5600", new double[][]{VOT7000_autoD,
                VOT7000_autoP,
                zeros,
                VOT7000_transit,
                VOT7000_transit,
                VOT7000_transit,
                zeros});

        for (int i = 0; i < 6; i++){
            String purpose = purposes[i].toString();
            for (String variable : vots.keySet()){
                pw.print(purpose + "," + variable);
                for (int j = 0; j < 7; j++){
                    //String mode = Mode.values()[j].toString();
                    pw.print(",");
                    pw.print(vots.get(variable)[j][i]);
                }
                pw.println();
            }
        }

        for (int i = 0; i < 6; i++){
            String purpose = purposes[i].toString();
            pw.print(purpose + "," + "nestingCoefficient");
                for (int j = 0; j < 7; j++){
                    pw.print(",");
                    pw.print(0.25);
                }
            pw.println();

        }

        for (int i = 0; i < 6; i++){
            String purpose = purposes[i].toString();
                pw.print(purpose + "," + "costPerKm");
                pw.print(",");
                pw.print(0.07); //autoD
                pw.print(",");
                pw.print(0.07); //autoP
                pw.print(",");
                pw.print(0.0); //bike
                pw.print(",");
                pw.print(0.12); //pt
                pw.print(",");
                pw.print(0.12); //pt
                pw.print(",");
                pw.print(0.12); //pt
                pw.print(",");
                pw.print(0.0); //autoP
                pw.println();
        }
        pw.close();
    }


}



