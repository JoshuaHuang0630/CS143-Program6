import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for reading participant names from a plain-text file.
 *
 * Expected file format:
 *   - One name per line
 *   - Blank lines and lines starting with '#' are ignored (treated as comments)
 */
public class NamesFileReader {

    /**
     * Reads names from the given file path and returns them in encounter order.
     *
     * @param filePath path to the names file
     * @return list of trimmed, non-empty names
     * @throws IOException if the file cannot be read
     */
    public static List<String> readNames(String filePath) throws IOException {
        List<String> names = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // Skip blank lines and comment lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                names.add(line);
            }
        }

        return names;
    }
}
