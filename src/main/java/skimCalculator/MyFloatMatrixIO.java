package skimCalculator;

import ch.sbb.matsim.analysis.skims.FloatMatrixIO;
import org.matsim.core.utils.io.IOUtils;

import java.io.*;
import java.util.Map;

public class MyFloatMatrixIO {
    
    private final static String SEP = ";";
    private final static String HEADER = "FROM" + SEP + "TO" + SEP + "VALUE";
    private final static String NL = "\n";

    public static <T> void writeAsCSV(MyFloatMatrix<T> matrix, String filename) throws IOException {
        try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
            writeCSV(matrix, writer);
        }
    }

    public static <T> void writeAsCSV(MyFloatMatrix<T> matrix, OutputStream stream) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
        writeCSV(matrix, writer);
    }

    private static <T> void writeCSV(MyFloatMatrix<T> matrix, BufferedWriter writer) throws IOException {
        writer.write(HEADER);
        writer.write(NL);
        T[] zoneIds = getSortedIds(matrix);
        for (T fromZoneId : zoneIds) {
            for (T toZoneId : zoneIds) {
                writer.write(fromZoneId.toString());
                writer.append(SEP);
                writer.write(toZoneId.toString());
                writer.append(SEP);
                writer.write(Float.toString(matrix.get(fromZoneId, toZoneId)));
                writer.append(NL);
            }
        }
        writer.flush();
    }


    private static <T> void writeOMX(MyFloatMatrix<T> matrix, BufferedWriter writer) throws IOException {

    }



    public static <T> void readAsCSV(MyFloatMatrix<T> matrix, String filename, FloatMatrixIO.IdConverter<T> idConverter) throws IOException {
        try (BufferedReader reader = IOUtils.getBufferedReader(filename)) {
            readCSV(matrix, reader, idConverter);
        }
    }

    public static <T> void readAsCSV(MyFloatMatrix<T> matrix, InputStream stream, FloatMatrixIO.IdConverter<T> idConverter) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        readCSV(matrix, reader, idConverter);
    }

    private static <T> void readCSV(MyFloatMatrix<T> matrix, BufferedReader reader, FloatMatrixIO.IdConverter<T> idConverter) throws IOException {
        String header = reader.readLine();
        if (!HEADER.equals(header)) {
            throw new IOException("Expected header '" + HEADER + "' but found '" + header + "'.");
        }
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(";");
            T fromZoneId = idConverter.parse(parts[0]);
            T toZoneId = idConverter.parse(parts[1]);
            float value = Float.parseFloat(parts[2]);
            matrix.set(fromZoneId, toZoneId, value);
        }
    }

    private static <T> T[] getSortedIds(MyFloatMatrix<T> matrix) {
        // the array-creation is only safe as long as the generated array is only within this class!
        @SuppressWarnings("unchecked")
        T[] ids = (T[]) (new Object[matrix.id2index.size()]);
        for (Map.Entry<T, Integer> e : matrix.id2index.entrySet()) {
            ids[e.getValue()] = e.getKey();
        }
        return ids;
    }

    @FunctionalInterface
    public interface IdConverter<T> {
        T parse(String id);
    }

}
