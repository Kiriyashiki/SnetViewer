package xyz.hainaut.snetviewer.service;

import xyz.hainaut.snetviewer.model.Station;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads station coordinates and tile metadata from sta-mapped.csv.
 */
public class StationCsvLoader {

    /**
     * Loads stations from either a file path or fallback to classpath resource.
     *
     * @param preferredFilePath the file path to try first (e.g. "sta-mapped.csv")
     * @return List of Station objects
     * @throws IOException if CSV cannot be loaded or parsed
     */
    public static List<Station> loadStations(String preferredFilePath) throws IOException {
        Path path = Paths.get(preferredFilePath);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                return parseCsv(in, path.toAbsolutePath().toString());
            }
        }

        // Try classpath resource
        String resourcePath = preferredFilePath.startsWith("/") ? preferredFilePath : "/" + preferredFilePath;
        InputStream resourceStream = StationCsvLoader.class.getResourceAsStream(resourcePath);
        if (resourceStream != null) {
            try (InputStream in = resourceStream) {
                return parseCsv(in, "classpath:" + resourcePath);
            }
        }

        throw new FileNotFoundException("Could not find CSV file at path: " + preferredFilePath +
                " or as classpath resource: " + resourcePath);
    }

    /**
     * Parses the CSV input stream with header: name;lat;lng;depth;tilex;tiley;pixelx;pixely
     */
    public static List<Station> parseCsv(InputStream inputStream, String sourceDescription) throws IOException {
        List<Station> stations = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV source is empty: " + sourceDescription);
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] tokens = line.split(";");
                if (tokens.length < 8) {
                    System.err.println("Warning: Skipping malformed line " + lineNumber + ": " + line);
                    continue;
                }

                try {
                    String name = tokens[0].trim();
                    double lat = Double.parseDouble(tokens[1].trim());
                    double lng = Double.parseDouble(tokens[2].trim());
                    double depth = Double.parseDouble(tokens[3].trim());
                    int tileX = Integer.parseInt(tokens[4].trim());
                    int tileY = Integer.parseInt(tokens[5].trim());
                    int pixelX = Integer.parseInt(tokens[6].trim());
                    int pixelY = Integer.parseInt(tokens[7].trim());

                    Station station = new Station(name, lat, lng, depth, tileX, tileY, pixelX, pixelY);
                    stations.add(station);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Number parse error on line " + lineNumber + ": " + line + " (" + e.getMessage() + ")");
                }
            }
        }
        return stations;
    }
}
