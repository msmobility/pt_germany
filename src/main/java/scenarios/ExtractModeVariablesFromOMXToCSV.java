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
    private IndexedDoubleMatrix2D matrixTrainDistance;
    private IndexedDoubleMatrix2D matrixBus;
    private IndexedDoubleMatrix2D matrixBusTransfers;
    private IndexedDoubleMatrix2D matrixBusDistanceShare;
    private IndexedDoubleMatrix2D matrixBusDistance;
    private IndexedDoubleMatrix2D matrixBusTimeShare;
    private IndexedDoubleMatrix2D matrixTrainTimeShare;
    private IndexedDoubleMatrix2D matrixTrainDistanceShare;

    public static void main(String[] args) throws FileNotFoundException {

        ExtractModeVariablesFromOMXToCSV ebn = new ExtractModeVariablesFromOMXToCSV();
        ebn.readInput();
        ebn.runAnalysis(0.01, "c:/projects/bast_entlastung/analysis/ld_model_analysis/scenarios/extendedBusNetworkCurrentSpeeds.csv");

    }


    private void readInput() {
        matrixCarDistance = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixCarDistance, "mat1", 1.0);
        matrixCar = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixCarTime, "mat1", 1.0);

        matrixTrain = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrain, "travel_time_s", 1.0);
        matrixTrainDistance = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrain, "distance_m", 1.0);
        matrixTrainDistanceShare = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrain, "train_distance_share", 1.0);
        matrixTrainTimeShare = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrain, "train_time_share", 1.0);

        matrixBus = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBus, "travel_time_s", 1.0);
        matrixBusTransfers = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBus, "transfer_count", 1.0);
        matrixBusDistanceShare = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBus, "train_distance_share", 1.0);
        matrixBusTimeShare = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBus, "train_time_share", 1.0);
        matrixBusDistance = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixBus, "distance_m", 1.0);

        System.out.println("Matrices were read");
    }

    private void runAnalysis(double shareOfOriginZonesForAnalysis, String outputFileName) throws FileNotFoundException {
        Random random = new Random(0);
        PrintWriter pw = new PrintWriter(new File(outputFileName));
        pw.println("o,d,dist,mode,time,speed,transfers,share_distance,share_time");
        for (int origin : matrixCarDistance.getRowLookupArray()) {
                for (int destination : matrixCarDistance.getColumnLookupArray()) {
                    if (random.nextDouble() < shareOfOriginZonesForAnalysis) {
                    double distanceCar = matrixCarDistance.getIndexed(origin, destination);
                    double timeCar = matrixCar.getIndexed(origin, destination);
                    double timeTrain = matrixTrain.getIndexed(origin, destination);
                    double distanceTrain= matrixTrainDistance.getIndexed(origin, destination);
                    double timeBus = matrixBus.getIndexed(origin, destination);
                    double distanceBus = matrixBusDistance.getIndexed(origin, destination);

                    double speedCar = timeCar > 0 ? distanceCar / timeCar : -1;
                    double speedTrain = timeTrain > 0 ? distanceTrain / timeTrain : -1;
                    double speedBus = timeBus > 0 ? distanceBus / timeBus : -1;

                    pw.println(origin + "," +
                            destination + "," +
                            distanceCar + "," +
                            "car" + "," +
                            timeCar + "," +
                            speedCar + "," +
                            "0,1,1");

                    double shareTrainDistance = matrixTrainDistanceShare.getIndexed(origin, destination);
                    double shareTrainTime = matrixTrainTimeShare.getIndexed(origin, destination);
                    pw.println(origin + "," +
                            destination + "," +
                            distanceTrain + "," +
                            "train" + "," +
                            timeTrain + "," +
                            speedTrain + "," +
                            "0" + "," +
                            shareTrainDistance + "," +
                            shareTrainTime);
                    double transfersBus = matrixBusTransfers.getIndexed(origin, destination);
                    double shareBusDistance = matrixBusDistanceShare.getIndexed(origin, destination);
                    double shareBusTime = matrixBusTimeShare.getIndexed(origin, destination);
                    pw.println(origin + "," +
                            destination + "," +
                            distanceBus + "," +
                            "bus" + "," +
                            timeBus + "," +
                            speedBus + "," +
                            transfersBus + "," +
                            shareBusDistance + "," +
                            shareBusTime);

                }
            }
        }
        pw.close();
        System.out.println("Speed analysis is done");
    }

}
