package accessibility;

import de.tum.bgu.msm.data.Id;
import de.tum.bgu.msm.io.input.readers.CsvGzSkimMatrixReader;
import de.tum.bgu.msm.util.MitoUtil;
import de.tum.bgu.msm.util.matrices.IndexedDoubleMatrix2D;
import de.tum.bgu.msm.utils.CSVFileReader2;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AccessibilityCalculator {

    public static void main(String[] args) throws IOException {
        new AccessibilityCalculator().run();
    }


    private void run() throws IOException {

        String populationFile = "C:/models/transit_germany/input/zones/pop_per_zone_TAZ_id.csv";

        BufferedReader br = new BufferedReader(new FileReader(populationFile));
        String[] header = br.readLine().split(",");
        int posId = MitoUtil.findPositionInArray("TAZ_id", header);
        int posPop = MitoUtil.findPositionInArray("pop", header);

        String line;
        Map<Integer, ZoneForSkim> popByZone = new HashMap<>();
        while ((line = br.readLine() )!= null){
            int id = Integer.parseInt(line.split(",")[posId]);
            int pop = Integer.parseInt(line.split(",")[posPop]);
            ZoneForSkim zone = new ZoneForSkim(id, pop);
            popByZone.put(id, zone);
        }

        Map<String, String> matrices = new HashMap<>();

        matrices.put("new", "c:/models/transit_germany/output/skims/germany_all_v2/buspt_traveltimes.csv.gz");
        matrices.put("old", "c:/models/transit_germany/output/skims/germany_all_v1/buspt_traveltimes.csv.gz");

        double beta = - 0.4;
        Map<String, Map<Integer, Double>> accessibility = new HashMap<>();

        for (String key : matrices.keySet()){
            SkimReader reader = new SkimReader();
            accessibility.put(key, new HashMap<>());
            IndexedDoubleMatrix2D mat = reader.readAndConvertToDoubleMatrix2D(matrices.get(key), 1./3600., popByZone.values());

            mat.forEachNonZero((i,k,x)-> Math.exp(beta * x));
            int[] rowLookupArray = mat.getRowLookupArray();
            mat.forEachNonZero((i,k,x)-> x * popByZone.get(rowLookupArray[k]).getPopulation());

            for (int row : mat.getRowLookupArray()){
                double[] array = mat.viewRow(row).toNonIndexedArray();
                double access = Arrays.stream(array).sum();
                accessibility.get(key).put(row, access);
            }
        }

        System.out.println("Done accessibility calculation.");

        PrintWriter pw = new PrintWriter("c:/models/transit_germany/output/access.csv");
        pw.print("zone,population");
        for (String key : matrices.keySet()){
            pw.print(",");
            pw.print(key);
        }
        pw.println();

        for (int i : popByZone.keySet()){
            pw.print(i + "," + popByZone.get(i).getPopulation());
            for (String key : matrices.keySet()){
                pw.print(",");
                pw.print(accessibility.get(key).get(i));
            }
            pw.println();
        }

        pw.close();






    }


    class ZoneForSkim implements Id {

        private int id;
        private int population;

        public ZoneForSkim(int id, int population) {
            this.id = id;
            this.population = population;
        }

        @Override
        public int getId() {
            return id;
        }

        public int getPopulation() {
            return population;
        }
    }


}



