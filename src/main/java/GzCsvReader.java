import java.io.*;
import java.util.zip.GZIPInputStream;

public class GzCsvReader {


    public static void main(String[] args) {

    }

    void readFile() throws IOException {
        String infile = "file.gzip";
        GZIPInputStream in = new GZIPInputStream(new FileInputStream(infile));
        Reader decoder = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(decoder);
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }

    }






}
