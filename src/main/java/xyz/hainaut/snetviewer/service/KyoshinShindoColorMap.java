package xyz.hainaut.snetviewer.service;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Service for loading KyoshinShindoColorMap.csv and mapping sampled station colors
 * to measured instrumental seismic intensity (計測震度).
 */
public class KyoshinShindoColorMap {
    public static final String DEFAULT_FILE_NAME = "KyoshinShindoColorMap.csv";

    // Maximum Euclidean distance squared in RGB space to consider a color a valid match.
    // Consecutive points on the 101-step scale have a max distance squared of ~317 (distance ~17.8).
    // Placeholder gray (128, 128, 128) has distance squared of ~19,840.
    // 35^2 = 1225 provides generous margin for compression while rejecting placeholder/UI colors.
    private static final double MAX_DISTANCE_SQ = 35.0 * 35.0;

    public static class Entry {
        private final double intensity;
        private final int r;
        private final int g;
        private final int b;

        public Entry(double intensity, int r, int g, int b) {
            this.intensity = intensity;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public double getIntensity() {
            return intensity;
        }

        public int getR() {
            return r;
        }

        public int getG() {
            return g;
        }

        public int getB() {
            return b;
        }

        public double distanceSq(int red, int green, int blue) {
            int dr = red - r;
            int dg = green - g;
            int db = blue - b;
            return dr * dr + dg * dg + db * db;
        }
    }

    private static final KyoshinShindoColorMap INSTANCE = new KyoshinShindoColorMap();

    public static KyoshinShindoColorMap getInstance() {
        return INSTANCE;
    }

    private final List<Entry> entries;

    public KyoshinShindoColorMap() {
        this(DEFAULT_FILE_NAME);
    }

    public KyoshinShindoColorMap(String resourceOrFilePath) {
        List<Entry> loaded = new ArrayList<>();
        try {
            loaded = loadEntries(resourceOrFilePath);
        } catch (Exception e) {
            System.err.println("Warning: Could not load " + resourceOrFilePath + ": " + e.getMessage());
        }
        this.entries = Collections.unmodifiableList(loaded);
    }

    private List<Entry> loadEntries(String preferredPath) throws IOException {
        // 1. Try working directory path
        Path path = Paths.get(preferredPath);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                return parseStream(in);
            }
        }

        // 2. Try src/main/resources/ path
        Path srcPath = Paths.get("src", "main", "resources", preferredPath);
        if (Files.exists(srcPath)) {
            try (InputStream in = Files.newInputStream(srcPath)) {
                return parseStream(in);
            }
        }

        // 3. Try classpath resource
        String resPath = preferredPath.startsWith("/") ? preferredPath : "/" + preferredPath;
        InputStream in = getClass().getResourceAsStream(resPath);
        if (in == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                in = cl.getResourceAsStream(preferredPath.startsWith("/") ? preferredPath.substring(1) : preferredPath);
            }
        }
        if (in != null) {
            try (InputStream is = in) {
                return parseStream(is);
            }
        }

        throw new IOException("Color map resource not found: " + preferredPath);
    }

    private List<Entry> parseStream(InputStream in) throws IOException {
        List<Entry> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    try {
                        double intensity = Double.parseDouble(parts[0].trim());
                        int r = Integer.parseInt(parts[1].trim());
                        int g = Integer.parseInt(parts[2].trim());
                        int b = Integer.parseInt(parts[3].trim());
                        list.add(new Entry(intensity, r, g, b));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return list;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    /**
     * Finds the measured instrumental intensity for the given color.
     * Returns null if color is null, transparent (alpha < 128), or too far from the color scale.
     */
    public Double getIntensity(Color color) {
        if (color == null || color.getAlpha() < 128 || entries.isEmpty()) {
            return null;
        }

        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();

        Entry best = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (Entry e : entries) {
            double dSq = e.distanceSq(red, green, blue);
            if (dSq < minDistanceSq) {
                minDistanceSq = dSq;
                best = e;
                if (dSq == 0) {
                    break; // Exact match found
                }
            }
        }

        if (best != null && minDistanceSq <= MAX_DISTANCE_SQ) {
            double intensity = best.getIntensity();
            if (Math.abs(intensity) < 1e-6) {
                intensity = 0.0;
            }
            return intensity;
        }

        return null;
    }

    /**
     * Returns a formatted intensity string (e.g. "0.2" or "-2.7"), or "--" if unavailable.
     */
    public String getFormattedIntensity(Color color) {
        Double intensity = getIntensity(color);
        if (intensity == null) {
            return "--";
        }
        return String.format(Locale.US, "%.1f", intensity);
    }
}
