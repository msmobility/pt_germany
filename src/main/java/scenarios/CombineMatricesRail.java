package scenarios;

import de.tum.bgu.msm.io.input.readers.OmxSkimsReader;
import de.tum.bgu.msm.io.output.OmxMatrixWriter;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import skimCalculator.OmxMatrixNames;

import java.io.FileNotFoundException;

public class CombineMatricesRail {

    String inputMatrixTrainWalk = "c:/models/transit_germany/output/skims/ld_train_with_walk_2/ld_train_with_walk_matrices.omx";
    String inputMatrixTrainAuto = "c:/models/transit_germany/output/skims/ld_train_with_auto_2/ld_train_with_auto_matrices.omx";
    String inputMatrixTrain3 = "c:/models/transit_germany/output/skims/ld_train_with_auto_2/ld_train_with_auto_matrices_v2.omx";

    private IndexedDoubleMatrix2D trainAccess1;
    private IndexedDoubleMatrix2D trainTime1;
    private IndexedDoubleMatrix2D trainEgress1;
    private IndexedDoubleMatrix2D trainShare1;
    private IndexedDoubleMatrix2D distance1;
    private IndexedDoubleMatrix2D trainAccess2;
    private IndexedDoubleMatrix2D trainTime2;
    private IndexedDoubleMatrix2D trainEgress2;
    private IndexedDoubleMatrix2D trainShare2;
    private IndexedDoubleMatrix2D distance2;


    public static void main(String[] args) throws FileNotFoundException {

        CombineMatricesRail dataExtractor = new CombineMatricesRail();
        dataExtractor.readInput();
        dataExtractor.runAnalysis();

    }


    private void readInput() {
        trainAccess1 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrainWalk, "access_time_s", 1.0);
        trainAccess2 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrainAuto, "access_time_s", 1.0);
        System.out.println("Access time matrices were read");
        trainTime1 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrainWalk, "travel_time_s", 1.0);
        trainTime2 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrainAuto, "travel_time_s", 1.0);
        System.out.println("Time matrices were read");
        trainShare1 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrainWalk, "train_time_share", 1.0);
        trainShare2 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrainAuto, "train_time_share", 1.0);
        System.out.println("Share matrices were read");
        distance1 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrainWalk, "distance_m", 1.0);
        distance2 = OmxSkimsReader.readAndConvertToDoubleMatrix(inputMatrixTrainAuto, "distance_m", 1.0);
        System.out.println("Distance matrices were read");
    }

    private void runAnalysis() {

        IndexedDoubleMatrix2D trainTime3 = new IndexedDoubleMatrix2D(trainTime1.getRowLookupArray());
        IndexedDoubleMatrix2D trainAccess3 = new IndexedDoubleMatrix2D(trainTime1.getRowLookupArray());
        IndexedDoubleMatrix2D trainEgress3 = new IndexedDoubleMatrix2D(trainTime1.getRowLookupArray());
        IndexedDoubleMatrix2D distance3 = new IndexedDoubleMatrix2D(trainTime1.getRowLookupArray());
        IndexedDoubleMatrix2D trainShare3 = new IndexedDoubleMatrix2D(trainTime1.getRowLookupArray());


        for (int origin : trainTime1.getRowLookupArray()) {
            for (int destination : trainTime1.getColumnLookupArray()) {
                if (trainTime1.getIndexed(origin, destination) > trainTime2.getIndexed(origin, destination)){
                    //the normal case
                    trainTime3.setIndexed(origin, destination, trainTime2.getIndexed(origin, destination));
                    trainAccess3.setIndexed(origin, destination, trainAccess2.getIndexed(origin, destination));
                    //trainEgress3.setIndexed(origin, destination, trainEgress2.getIndexed(origin, destination));
                    distance3.setIndexed(origin, destination, distance2.getIndexed(origin, destination));
                    trainShare3.setIndexed(origin, destination, trainShare2.getIndexed(origin, destination));


                } else {
                    //unexpected case
                    trainTime3.setIndexed(origin, destination, trainTime1.getIndexed(origin, destination));
                    trainAccess3.setIndexed(origin, destination, trainAccess1.getIndexed(origin, destination));
                    //trainEgress3.setIndexed(origin, destination, trainEgress1.getIndexed(origin, destination));
                    distance3.setIndexed(origin, destination, distance1.getIndexed(origin, destination));
                    trainShare3.setIndexed(origin, destination, trainShare1.getIndexed(origin, destination));
                }
            }
        }
        System.out.println("Processing done");

        OmxMatrixWriter.createOmxFile(inputMatrixTrain3, trainTime3.getRowLookupArray().length);
        OmxMatrixWriter.createOmxSkimMatrix(trainTime3, inputMatrixTrain3, OmxMatrixNames.TT_MATRIX_NAME);
        OmxMatrixWriter.createOmxSkimMatrix(distance3, inputMatrixTrain3, OmxMatrixNames.DISTANCE_MATRIX_NAME);
        OmxMatrixWriter.createOmxSkimMatrix(trainAccess3, inputMatrixTrain3, OmxMatrixNames.ACCESS_TIME_MATRIX_NAME);
    }
}
