package scenarios;

import de.tum.bgu.msm.io.input.readers.OmxSkimsReader;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import skimCalculator.OmxMatrixNames;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class OmxAnalyzer {


    static Map<String, String> fileNames = new HashMap<>();
    static List<String> matrixList = new ArrayList<>();
    static double scaleFactor;
    static boolean intrazonalsOnly;

    static Map<String, Map<String, IndexedDoubleMatrix2D>> matrixByFileAndMatrix = new HashMap<>();

    public static void main(String[] args) throws FileNotFoundException {

//        fileNames.put("base","c:/models/transit_germany/output/skims/ld_train_with_walk_2/ld_train_with_walk_matrices.omx");
//        fileNames.put("1","c:/models/transit_germany/output/skims/ld_train_with_auto_2/ld_train_with_auto_matrices.omx");
//        fileNames.put("2","c:/models/transit_germany/output/skims/ld_rb_sb_train_with_auto/ld_rb_sb_train_with_auto_matrices.omx");
//
//        matrixList.add(OmxMatrixNames.DISTANCE_MATRIX_NAME);
//        matrixList.add(OmxMatrixNames.TT_MATRIX_NAME);
//        matrixList.add(OmxMatrixNames.ACCESS_TIME_MATRIX_NAME);
//        matrixList.add(OmxMatrixNames.EGRESS_TIME_MATRIX_NAME);
//        matrixList.add(OmxMatrixNames.IN_VEH_TIME_MATRIX_NAME);
//        matrixList.add((OmxMatrixNames.TRANSFERS_MATRIX_NAME));
//        matrixList.add(OmxMatrixNames.ACCESS_DISTANCE_MATRIX_NAME);
//        matrixList.add(OmxMatrixNames.EGRESS_DISTANCE_MATRIX_NAME);
//        matrixList.add(OmxMatrixNames.DISTANCE_SHARE_MATRIX_NAME);
//        matrixList.add(OmxMatrixNames.TIME_SHARE_MATRIX_NAME);

        fileNames.put("base", "c:/models/transit_germany/output/skims/carWithToll/car_matrix_toll.omx");
        fileNames.put("congested", "c:/models/transit_germany/output/skims/auto_congested/car_matrix.omx");

        matrixList.add(OmxMatrixNames.CAR_DISTANCE);
        matrixList.add(OmxMatrixNames.CAR_TIME);

        scaleFactor = 0.005;
        intrazonalsOnly = false;

        OmxAnalyzer dataExtractor = new OmxAnalyzer();
        dataExtractor.readInput(fileNames, matrixList);
        final String outputFileName = "c:/projects/bast_entlastung/analysis/ld_model_analysis/scenarios/results_congestion.csv";
        dataExtractor.runAnalysis(scaleFactor, intrazonalsOnly, outputFileName);

    }


    private void readInput(Map<String, String> fileNames, List<String> matrixList) {


        for (String key : fileNames.keySet()){
            matrixByFileAndMatrix.put(key, new HashMap<>());
            for (String matrixName : matrixList){
                IndexedDoubleMatrix2D matrix;
                try {
                    matrix = OmxSkimsReader.readAndConvertToDoubleMatrix(fileNames.get(key), matrixName, 1.0);
                    matrixByFileAndMatrix.get(key).put(matrixName, matrix);
                    System.out.println("Read " + matrixName + " from file " + fileNames.get(key));
                } catch (Exception e) {
                    System.out.println("The matrix " + matrixName + " is not found in file " + fileNames.get(key));
                }

            }

        }

        System.out.println("Matrices were read");
    }

    private void runAnalysis(double scaleFactor, boolean intrazonalsOnly, String outputFileName) throws FileNotFoundException {
        Random random = new Random(0);
        PrintWriter pw = new PrintWriter(outputFileName);
        pw.print("o,d,alt");
        for (String name : matrixList){
            pw.print(",");
            pw.print(name);
        }
        pw.println();
        final String first = matrixByFileAndMatrix.keySet().stream().findFirst().get();
        final String key = matrixList.get(0);
        IndexedDoubleMatrix2D firstMatrix = matrixByFileAndMatrix.get(first).get(key);
        for (int origin : firstMatrix.getRowLookupArray()) {
                for (int destination : firstMatrix.getColumnLookupArray()) {
                    if (random.nextDouble() < scaleFactor) {
                    if (intrazonalsOnly && origin == destination){
                        printAttributesOfThisOdPair(origin, destination, pw);
                    }
                    if (!intrazonalsOnly){
                        printAttributesOfThisOdPair(origin, destination, pw);
                    }
                }
            }
        }
        pw.close();
        System.out.println("Matrix data extracted to CSV");
    }

    private void printAttributesOfThisOdPair(int origin, int destination, PrintWriter pw) {
        for (String key : matrixByFileAndMatrix.keySet()){
            pw.print(origin + "," + destination + "," + key);
            for (String matrixName : matrixList){
                IndexedDoubleMatrix2D matrix2D = matrixByFileAndMatrix.get(key).get(matrixName);
                if (matrix2D != null){
                    pw.print(",");
                    pw.print(matrix2D.getIndexed(origin, destination));
                }
            }
            pw.println();
        }
    }

}
