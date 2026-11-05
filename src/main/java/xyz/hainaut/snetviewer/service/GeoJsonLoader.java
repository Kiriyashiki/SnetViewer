package xyz.hainaut.snetviewer.service;

import xyz.hainaut.snetviewer.model.GeoJsonLayer;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Fast, robust GeoJSON parser for loading vector polygon and line layers
 * with proper multi-ring and MultiPolygon geometry isolation.
 */
public class GeoJsonLoader {

    /**
     * Loads a GeoJSON layer from a file path or classpath resource.
     */
    public static GeoJsonLayer loadLayer(String fileName, String layerName,
                                         GeoJsonLayer.LayerType type,
                                         Color fillColor, Color strokeColor, float strokeWidth) throws IOException {
        String jsonContent = readContent(fileName);
        List<Path2D.Double> paths = parseGeoJson(jsonContent);
        return new GeoJsonLayer(layerName, type, paths, fillColor, strokeColor, strokeWidth);
    }

    private static String readContent(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        if (Files.exists(path)) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }

        String resPath = fileName.startsWith("/") ? fileName : "/" + fileName;
        InputStream in = GeoJsonLoader.class.getResourceAsStream(resPath);
        if (in != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        }

        throw new FileNotFoundException("GeoJSON file not found: " + fileName + " (or classpath: " + resPath + ")");
    }

    public static List<Path2D.Double> parseGeoJson(String json) {
        List<Path2D.Double> out = new ArrayList<>();
        int pos = 0;
        int len = json.length();

        while (pos < len) {
            int geomIdx = json.indexOf("\"geometry\"", pos);
            if (geomIdx == -1) break;

            int braceStart = json.indexOf('{', geomIdx);
            if (braceStart == -1) break;

            int braceEnd = findMatchingBrace(json, braceStart);
            if (braceEnd == -1) break;

            String geomStr = json.substring(braceStart, braceEnd + 1);
            parseFeatureGeometry(geomStr, out);

            pos = braceEnd + 1;
        }

        return out;
    }

    private static int findMatchingBrace(String s, int start) {
        int depth = 0;
        boolean inQuotes = false;
        int len = s.length();
        for (int i = start; i < len; i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private static void parseFeatureGeometry(String geomJson, List<Path2D.Double> out) {
        int typeIdx = geomJson.indexOf("\"type\"");
        if (typeIdx == -1) return;
        int colonIdx = geomJson.indexOf(':', typeIdx);
        int q1 = geomJson.indexOf('"', colonIdx);
        int q2 = geomJson.indexOf('"', q1 + 1);
        if (q1 == -1 || q2 == -1) return;
        String type = geomJson.substring(q1 + 1, q2).trim();

        int coordIdx = geomJson.indexOf("\"coordinates\"");
        if (coordIdx == -1) return;
        int arrStart = geomJson.indexOf('[', coordIdx);
        if (arrStart == -1) return;
        int arrEnd = findMatchingBracket(geomJson, arrStart);
        if (arrEnd == -1) return;

        String coordsStr = geomJson.substring(arrStart, arrEnd + 1);

        if ("Polygon".equalsIgnoreCase(type)) {
            // [ [ [lng, lat], ... ], ... ]
            parsePolygon(coordsStr, out);
        } else if ("MultiPolygon".equalsIgnoreCase(type)) {
            // [ [ [ [lng, lat], ... ], ... ], ... ]
            parseMultiPolygon(coordsStr, out);
        } else if ("LineString".equalsIgnoreCase(type)) {
            // [ [lng, lat], ... ]
            parseLineString(coordsStr, out);
        } else if ("MultiLineString".equalsIgnoreCase(type)) {
            // [ [ [lng, lat], ... ], ... ]
            parseMultiLineString(coordsStr, out);
        }
    }

    private static void parsePolygon(String s, List<Path2D.Double> out) {
        List<String> rings = extractSubArrays(s);
        if (rings.isEmpty()) return;

        Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        boolean hasRings = false;

        for (String ringStr : rings) {
            if (addRingToPath(ringStr, path, true)) {
                hasRings = true;
            }
        }

        if (hasRings) {
            out.add(path);
        }
    }

    private static void parseMultiPolygon(String s, List<Path2D.Double> out) {
        List<String> polys = extractSubArrays(s);
        for (String polyStr : polys) {
            parsePolygon(polyStr, out);
        }
    }

    private static void parseLineString(String s, List<Path2D.Double> out) {
        Path2D.Double path = new Path2D.Double();
        if (addRingToPath(s, path, false)) {
            out.add(path);
        }
    }

    private static void parseMultiLineString(String s, List<Path2D.Double> out) {
        List<String> lines = extractSubArrays(s);
        for (String lineStr : lines) {
            parseLineString(lineStr, out);
        }
    }

    private static List<String> extractSubArrays(String s) {
        List<String> result = new ArrayList<>();
        int i = 0;
        int len = s.length();
        while (i < len && s.charAt(i) != '[') i++;
        if (i >= len) return result;
        i++; // skip opening '['

        while (i < len) {
            char c = s.charAt(i);
            if (c == '[') {
                int end = findMatchingBracket(s, i);
                if (end == -1) break;
                result.add(s.substring(i, end + 1));
                i = end + 1;
            } else if (c == ']') {
                break;
            } else {
                i++;
            }
        }
        return result;
    }

    private static boolean addRingToPath(String ringStr, Path2D.Double path, boolean close) {
        int i = 0;
        int len = ringStr.length();
        boolean firstPointInRing = true;
        int count = 0;

        while (i < len) {
            int pStart = ringStr.indexOf('[', i);
            if (pStart == -1) break;
            if (pStart == 0) {
                i = 1;
                continue;
            }
            int pEnd = ringStr.indexOf(']', pStart);
            if (pEnd == -1) break;

            String pair = ringStr.substring(pStart + 1, pEnd).trim();
            int comma = pair.indexOf(',');
            if (comma != -1) {
                try {
                    double lng = Double.parseDouble(pair.substring(0, comma).trim());
                    double lat = Double.parseDouble(pair.substring(comma + 1).trim());

                    // Web Mercator in radians (EPSG:3857)
                    double mx = Math.toRadians(lng);
                    double latRad = Math.toRadians(Math.max(-85.05112878, Math.min(85.05112878, lat)));
                    double my = Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));

                    if (firstPointInRing) {
                        path.moveTo(mx, my);
                        firstPointInRing = false;
                    } else {
                        path.lineTo(mx, my);
                    }
                    count++;
                } catch (NumberFormatException ignored) {}
            }
            i = pEnd + 1;
        }

        if (count >= 2) {
            if (close) {
                path.closePath();
            }
            return true;
        }
        return false;
    }

    private static int findMatchingBracket(String s, int start) {
        int depth = 0;
        int len = s.length();
        for (int i = start; i < len; i++) {
            char c = s.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
}
