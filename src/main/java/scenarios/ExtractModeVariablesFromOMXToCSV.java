package scenarios;

import de.tum.bgu.msm.io.input.readers.OmxSkimsReader;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

public class ExtractModeVariablesFromOMXToCSV {

    String inputMatrixCarDistance = "c:/models/germanymodel/skims/car_11717_travelDistance_m_w_connect.omx";
    String inputMatrixCarTime = "c:/models/germanymodel/skims/car_11717_travelTime_sec.omx";
    String inputMatrixTrain = "c:/models/transit_germany/output/skims/ld_train_v3/ld_train_v3_matrices.omx";
    String inputMatrixBus = "c:/models/transit_germany/output/skims/ld_bus_v3/ld_bus_v3_matrices.omx";

    private IndexedDoubleMatrix2D matrixCarDistance;
    private IndexedDoubleMatrix2D matrixCar;
    private IndexedDoubleMatrix2D matrixTrain;
    private IndexedDoubleMatrix2D matrixBus;
    private IndexedDoubleMatrix2D matrixBusTransfers;
    private IndexedDoubleMatrix2D matrixBusShare;

    public static void main(String[] args) throws FileNotFoundException {

        ExtractModeVariablesFromOMXToCSV ebn = new ExtractModeVariablesFromOMXToCSV();
        ebn.readInput();
        ebn.runAnalysis(0.01, "c:/projects/bast_entlastung/analysis/ld_model_analysis/scenarios/extendedBusNetworkCurrentSpeeds.csv");

    }


    private void readInput() {
        matrixCarDistance = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixCarDistance, "mat1", 1.0);
        matrixCar = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixCarTime, "mat1", 1.0);
        matrixTrain = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrain, "travel_time_s", 1.0);
        matrixBus = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBus, "travel_time_s", 1.0);
        matrixBusTransfers = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBus, "transfer_count", 1.0);
        matrixBusShare = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBus, "train_distance_share", 1.0);

        System.out.println("Matrices were read");
    }

    private void runAnalysis(double shareOfOriginZonesForAnalysis, String outputFileName) throws FileNotFoundException {
        Random random = new Random(0);
        PrintWriter pw = new PrintWriter(new File(outputFileName));
        pw.println("o,d,dist,mode,time,speed,transfers,share");
        for (int origin : matrixCarDistance.getRowLookupArray()) {

            if (random.nextDouble() < shareOfOriginZonesForAnalysis) {
                for (int destination : matrixCarDistance.getColumnLookupArray()) {
                    double distance = matrixCarDistance.getIndexed(origin, destination);
                    double timeCar = matrixCar.getIndexed(origin, destination);
                    double timeTrain = matrixTrain.getIndexed(origin, destination);
                    double timeBus = matrixBus.getIndexed(origin, destination);

                    double speedCar = timeCar > 0 ? distance / timeCar : -1;
                    double speedTrain = timeTrain > 0 ? distance / timeTrain : -1;
                    double speedBus = timeBus > 0 ? distance / timeBus : -1;

                    pw.println(origin + "," +
                            destination + "," +
                            distance + "," +
                            "car" + "," +
                            timeCar + "," +
                            speedCar + "," +
                            "0,0");
                    pw.println(origin + "," +
                            destination + "," +
                            distance + "," +
                            "train" + "," +
                            timeTrain + "," +
                            speedTrain + "," +
                            "0,0");
                    double transfersBus = matrixBusTransfers.getIndexed(origin, destination);
                    double shareBus = matrixBusShare.getIndexed(origin, destination);
                    pw.println(origin + "," +
                            destination + "," +
                            distance + "," +
                            "bus" + "," +
                            timeBus + "," +
                            speedBus + "," +
                            transfersBus + "," +
                            shareBus);

                }
            }
        }
        pw.close();
        System.out.println("Speed analysis is done");
    }

}
