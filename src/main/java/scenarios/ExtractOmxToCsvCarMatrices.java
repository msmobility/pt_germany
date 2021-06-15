package scenarios;

import de.tum.bgu.msm.io.input.readers.OmxSkimsReader;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import skimCalculator.OmxMatrixNames;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

public class ExtractOmxToCsvCarMatrices {


    String inputOmx1 = "c:/models/germanymodel/skims/car_matrices.omx";
    String inputOmx2 = "c:/models/transit_germany/output/skims/carWithToll/car_matrix_toll.omx";

    private IndexedDoubleMatrix2D carDistance1;
    private IndexedDoubleMatrix2D cartDistance2;
    private IndexedDoubleMatrix2D carTime1;
    private IndexedDoubleMatrix2D carTime2;


    public static void main(String[] args) throws FileNotFoundException {

        ExtractOmxToCsvCarMatrices dataExtractor = new ExtractOmxToCsvCarMatrices();
        dataExtractor.readInput();
        dataExtractor.runAnalysis(0.01, "c:/projects/bast_entlastung/analysis/ld_model_analysis/scenarios/car_matrix_comparison.csv");

    }


    private void readInput() {
        carDistance1 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputOmx1, "distance_m", 1.0);
        cartDistance2 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputOmx2, "distance_m", 1.0);
        carTime1 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputOmx1, "time_s", 1.0);
        carTime2 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputOmx2, "time_s", 1.0);

        System.out.println("Matrices were read");
    }

    private void runAnalysis(double shareOfOriginZonesForAnalysis, String outputFileName) throws FileNotFoundException {
        Random random = new Random(0);
        PrintWriter pw = new PrintWriter(new File(outputFileName));
        pw.println("o,d,d_1_m,d_2_m,t_1_s,t_2_s");
        for (int origin : carDistance1.getRowLookupArray()) {
                for (int destination : carDistance1.getColumnLookupArray()) {
                    if (random.nextDouble() < shareOfOriginZonesForAnalysis) {
                    pw.println(origin + "," +
                            destination + "," +
                            carDistance1.getIndexed(origin,destination) + "," +
                            cartDistance2.getIndexed(origin,destination) + "," +
                            carTime1.getIndexed(origin,destination) + "," +
                            carTime2.getIndexed(origin,destination));


                }
            }
        }
        pw.close();
        System.out.println("Speed analysis is done");
    }

}
