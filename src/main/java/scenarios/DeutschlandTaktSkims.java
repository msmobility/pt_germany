package scenarios;

import de.tum.bgu.msm.io.input.readers.OmxSkimsReader;
import de.tum.bgu.msm.io.output.OmxMatrixWriter;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import skimCalculator.OmxMatrixNames;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

public class DeutschlandTaktSkims {

    String originalOmxFile1 = "c:/models/transit_germany/output/skims/ld_train_with_walk_2/ld_train_with_walk_matrices.omx";
    String newOmxFile2 = "c:/models/transit_germany/output/skims/ld_train_with_walk_2/ld_train_with_walk_matrices_deutschland_takt.omx";
    String fileName = "c:/models/transit_germany/output/skims/ld_train_with_walk_2/deutschland_takt_summary.csv";

    private IndexedDoubleMatrix2D trainAccess1;
    //private IndexedDoubleMatrix2D trainAccess2;
    private IndexedDoubleMatrix2D trainTime1;
    private IndexedDoubleMatrix2D trainTime2;
    private IndexedDoubleMatrix2D trainEgress1;
    //private IndexedDoubleMatrix2D trainEgress2;
    private IndexedDoubleMatrix2D trainShare1;
    private IndexedDoubleMatrix2D trainShare2;
    private IndexedDoubleMatrix2D trainInVehicle1;
    private IndexedDoubleMatrix2D trainInVehicle2;
    private IndexedDoubleMatrix2D distance1;
    //private IndexedDoubleMatrix2D distance2;
    private IndexedDoubleMatrix2D transfers1;
    // private IndexedDoubleMatrix2D transfers2;


    public static void main(String[] args) throws FileNotFoundException {

        DeutschlandTaktSkims dataExtractor = new DeutschlandTaktSkims();
        dataExtractor.readInput();
        dataExtractor.createMatrices();


    }


    private void readInput() {
        trainAccess1 = OmxSkimsReader.readAndConvertToDoubleMatrix(originalOmxFile1, OmxMatrixNames.ACCESS_TIME_MATRIX_NAME, 1.0);
        trainTime1 = OmxSkimsReader.readAndConvertToDoubleMatrix(originalOmxFile1, OmxMatrixNames.TT_MATRIX_NAME, 1.0);
        trainEgress1 = OmxSkimsReader.readAndConvertToDoubleMatrix(originalOmxFile1, OmxMatrixNames.EGRESS_TIME_MATRIX_NAME, 1.0);
        trainShare1 = OmxSkimsReader.readAndConvertToDoubleMatrix(originalOmxFile1, OmxMatrixNames.TIME_SHARE_MATRIX_NAME, 1.0);
        trainInVehicle1 = OmxSkimsReader.readAndConvertToDoubleMatrix(originalOmxFile1, OmxMatrixNames.IN_VEH_TIME_MATRIX_NAME, 1.0);
        distance1 = OmxSkimsReader.readAndConvertToDoubleMatrix(originalOmxFile1, OmxMatrixNames.DISTANCE_MATRIX_NAME, 1.0);
        transfers1 = OmxSkimsReader.readAndConvertToDoubleMatrix(originalOmxFile1, OmxMatrixNames.TRANSFERS_MATRIX_NAME, 1.0);
        System.out.println("Matrices were read");
    }

    private void createMatrices() throws FileNotFoundException {

        double inVehicleTimeReductionFactor = 0.10;
        double timeReductionPerTransferFactor = 5. * 60.;
        Random random = new Random(-1);
        double scaleForSumamry = 0.005;
        PrintWriter pw = new PrintWriter(fileName);
        pw.println("origin,destination,access_s,egress_s,distance_m,transfers," +
                "waiting_before_s,in_veh_before_s,time_before_s,share_time_before," +
                "in_veh_after,time_after_s,share_time_after");


        trainShare2 = new IndexedDoubleMatrix2D(trainAccess1.getRowLookupArray());
        trainInVehicle2 = new IndexedDoubleMatrix2D(trainAccess1.getRowLookupArray());
        trainTime2 = new IndexedDoubleMatrix2D(trainAccess1.getRowLookupArray());

        for (int origin : trainAccess1.getRowLookupArray()) {
            for (int destination : trainAccess1.getColumnLookupArray()) {
                double totalTime = trainTime1.getIndexed(origin, destination);
                double nTransfers = transfers1.getIndexed(origin, destination);
                double inVehicleTime = trainInVehicle1.getIndexed(origin, destination);
                double accessTime = trainAccess1.getIndexed(origin, destination);
                double egressTime = trainEgress1.getIndexed(origin, destination);
                double waitingTime = totalTime - inVehicleTime - accessTime - egressTime;

                double reductionInVehicleTime = inVehicleTimeReductionFactor * inVehicleTime;
                double reductionTransfers = timeReductionPerTransferFactor * nTransfers;

                double newInvehicleTime;
                double newTotalTime;
                if (inVehicleTime > reductionInVehicleTime) {
                    newInvehicleTime = inVehicleTime - reductionInVehicleTime;
                    newTotalTime = totalTime - reductionInVehicleTime;
                } else {
                    newInvehicleTime = inVehicleTime;
                    newTotalTime = totalTime;
                }

                if (newTotalTime > reductionTransfers) {
                    newTotalTime = newTotalTime - reductionTransfers;
                }

                trainInVehicle2.setIndexed(origin, destination, newInvehicleTime);
                trainTime2.setIndexed(origin, destination, newTotalTime);

                final double shareTime = trainShare1.getIndexed(origin, destination);
                double newShare = (shareTime * inVehicleTime) / newInvehicleTime;
                trainShare2.setIndexed(origin, destination, newShare);

                double distance = distance1.getIndexed(origin, destination);

                if (random.nextDouble() < scaleForSumamry) {
                    pw.println(origin + "," +
                            destination + "," +
                            accessTime + "," +
                            egressTime + "," +
                            distance + "," +
                            nTransfers + "," +
                            waitingTime + "," +
                            inVehicleTime + "," +
                            totalTime + "," +
                            shareTime + "," +
                            newInvehicleTime + "," +
                            newTotalTime + "," +
                            newShare);
                }


            }
        }

        pw.close();

        OmxMatrixWriter.createOmxFile(newOmxFile2, trainAccess1.getRowLookupArray().length);
        OmxMatrixWriter.createOmxSkimMatrix(trainAccess1, newOmxFile2, OmxMatrixNames.ACCESS_TIME_MATRIX_NAME);
        OmxMatrixWriter.createOmxSkimMatrix(trainEgress1, newOmxFile2, OmxMatrixNames.EGRESS_TIME_MATRIX_NAME);
        OmxMatrixWriter.createOmxSkimMatrix(distance1, newOmxFile2, OmxMatrixNames.DISTANCE_MATRIX_NAME);
        OmxMatrixWriter.createOmxSkimMatrix(transfers1, newOmxFile2, OmxMatrixNames.TRANSFERS_MATRIX_NAME);

        OmxMatrixWriter.createOmxSkimMatrix(trainShare2, newOmxFile2, OmxMatrixNames.TIME_SHARE_MATRIX_NAME);
        OmxMatrixWriter.createOmxSkimMatrix(trainInVehicle2, newOmxFile2, OmxMatrixNames.IN_VEH_TIME_MATRIX_NAME);
        OmxMatrixWriter.createOmxSkimMatrix(trainTime2, newOmxFile2, OmxMatrixNames.TT_MATRIX_NAME);


    }
}
