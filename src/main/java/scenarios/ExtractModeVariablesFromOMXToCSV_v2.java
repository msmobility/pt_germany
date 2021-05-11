package scenarios;

import de.tum.bgu.msm.io.input.readers.OmxSkimsReader;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

public class ExtractModeVariablesFromOMXToCSV_v2 {


    String inputMatrixTrain1 = "c:/models/transit_germany/output/skims/ld_train_v3/ld_train_v3_matrices.omx";
    String inputMatrixTrain2 = "c:/models/transit_germany/output/skims/ld_train_with_auto_access_2/ld_train_with_auto_access_2matrices.omx";

    private IndexedDoubleMatrix2D trainAccess1;
    private IndexedDoubleMatrix2D trainAccess2;
    private IndexedDoubleMatrix2D trainTime1;
    private IndexedDoubleMatrix2D trainTime2;
    private IndexedDoubleMatrix2D trainEgres1;
    private IndexedDoubleMatrix2D trainEgres2;


    public static void main(String[] args) throws FileNotFoundException {

        ExtractModeVariablesFromOMXToCSV_v2 ebn = new ExtractModeVariablesFromOMXToCSV_v2();
        ebn.readInput();
        ebn.runAnalysis(0.01, "c:/projects/bast_entlastung/analysis/ld_model_analysis/scenarios/results_scen_1.csv");

    }


    private void readInput() {
        trainAccess1 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrain1, "access_time_s", 1.0);
        trainAccess2 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrain2, "access_time_s", 1.0);
        trainTime1 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrain1, "travel_time_s", 1.0);
        trainTime2 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrain2, "travel_time_s", 1.0);
        trainEgres1 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrain1, "egress_time_s", 1.0);
        trainEgres2 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrain2, "egress_time_s", 1.0);

        System.out.println("Matrices were read");
    }

    private void runAnalysis(double shareOfOriginZonesForAnalysis, String outputFileName) throws FileNotFoundException {
        Random random = new Random(0);
        PrintWriter pw = new PrintWriter(new File(outputFileName));
        pw.println("o,d,time_1,time_2,access_time_1,access_time_2,egress_time_1,egress_time_2");
        for (int origin : trainAccess1.getRowLookupArray()) {
            if (random.nextDouble() < shareOfOriginZonesForAnalysis) {
                for (int destination : trainAccess1.getColumnLookupArray()) {
                    pw.println(origin + "," +
                            destination + "," +
                            trainTime1.getIndexed(origin,destination) + "," +
                            trainTime2.getIndexed(origin,destination) + "," +
                            trainAccess1.getIndexed(origin,destination) + "," +
                            trainAccess2.getIndexed(origin,destination) + "," +
                            trainEgres1.getIndexed(origin,destination) + "," +
                            trainEgres2.getIndexed(origin,destination));



                }
            }
        }
        pw.close();
        System.out.println("Speed analysis is done");
    }

}
