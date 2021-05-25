package scenarios;

import com.google.common.math.LongMath;
import de.tum.bgu.msm.io.input.readers.OmxSkimsReader;
import de.tum.bgu.msm.io.output.OmxMatrixWriter;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import skimCalculator.OmxMatrixNames;

public class BusNetworkMatrices {


    private static String inputMatrixBusExisting = "c:/models/transit_germany/output/skims/ld_bus_with_walk/ld_bus_with_walk_matrices.omx";
    private static String inputMatrixBusNew = "c:/models/transit_germany/output/skims/ld_bus_with_walk/ld_bus_with_walk_matrices_scenario_2.omx";

    private static IndexedDoubleMatrix2D matrixBusTimesExisting;
    private static IndexedDoubleMatrix2D matrixBusTimesNew;
    //    private static IndexedDoubleMatrix2D matrixBusTransfers;
    private static IndexedDoubleMatrix2D matrixBusDistance;
    private static IndexedDoubleMatrix2D matrixBusDistanceShare;
//    private static IndexedDoubleMatrix2D matrixBusTimeShare;


    public static void main(String[] args) {


        matrixBusTimesExisting = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBusExisting, OmxMatrixNames.TT_MATRIX_NAME, 1.0);
        matrixBusTimesNew = new IndexedDoubleMatrix2D(matrixBusTimesExisting.getRowLookupArray());
//        matrixBusTransfers = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBus, "transfer_count", 1.0);

        matrixBusDistance = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBusExisting, OmxMatrixNames.DISTANCE_MATRIX_NAME, 1.0);

        matrixBusDistanceShare = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBusExisting, OmxMatrixNames.DISTANCE_SHARE_MATRIX_NAME, 1.0);
//        matrixBusTimeShare = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBus, "train_time_share", 1.0);

        double factorTime = 7.7 / 10.3;
        double minShare = 0.75;

        int counter = 0;

        double sumExisting = 0.;
        double sumNew = 0.;
        int counterValidNumbers = 0;

        for (int origin : matrixBusTimesExisting.getRowLookupArray()) {
            for (int destination : matrixBusTimesExisting.getColumnLookupArray()) {
                double currentTime_s = matrixBusTimesExisting.getIndexed(origin, destination);
                double newTime_s;
                if (matrixBusDistanceShare.getIndexed(origin, destination) < minShare){
                    newTime_s = currentTime_s * factorTime;
                    matrixBusTimesNew.setIndexed(origin, destination, newTime_s);
                } else {
                    newTime_s = currentTime_s;
                    matrixBusTimesNew.setIndexed(origin, destination, newTime_s);
                }
                if (currentTime_s != Double.NaN && currentTime_s < Double.MAX_VALUE){
                    sumExisting += currentTime_s;
                    sumNew += newTime_s;
                    counterValidNumbers++;
                }
                counter++;
                if (LongMath.isPowerOfTwo(counter)){
                    System.out.println("Completed " + counter + " origin/destination pairs");
                }
            }
        }

        System.out.println("Exisiting mean time " + sumExisting / counterValidNumbers / 3600);
        System.out.println("New mean time " + sumNew / counterValidNumbers / 3600);


        OmxMatrixWriter.createOmxFile(inputMatrixBusNew, matrixBusTimesExisting.getRowLookupArray().length);
        OmxMatrixWriter.createOmxSkimMatrix(matrixBusTimesNew, inputMatrixBusNew, OmxMatrixNames.TT_MATRIX_NAME);
        OmxMatrixWriter.createOmxSkimMatrix(matrixBusDistance, inputMatrixBusNew, OmxMatrixNames.DISTANCE_MATRIX_NAME);
        //the other matrices contained in the original omx file are not needed
    }

}
