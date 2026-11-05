package xyz.hainaut.snetviewer;

import xyz.hainaut.snetviewer.model.GeoJsonLayer;
import xyz.hainaut.snetviewer.model.Station;
import xyz.hainaut.snetviewer.model.TargetTimeInfo;
import xyz.hainaut.snetviewer.service.GeoJsonLoader;
import xyz.hainaut.snetviewer.service.SmoniDataService;
import xyz.hainaut.snetviewer.service.StationCsvLoader;
import xyz.hainaut.snetviewer.ui.MapCanvas;
import xyz.hainaut.snetviewer.ui.UITheme;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Automated test and verification for SnetViewer components.
 */
public class SnetViewerTest {
    public static void main(String[] args) {
        System.out.println("=== Running SnetViewer Automated Tests ===");

        try {
            // Test 1: Load CSV
            List<Station> stations = StationCsvLoader.loadStations("sta-mapped.csv");
            assertCondition(stations.size() == 150, "Expected 150 stations loaded, got: " + stations.size());
            System.out.println("✓ Test 1 Passed: Loaded " + stations.size() + " stations from CSV.");

            // Verify first and last station data
            Station first = stations.get(0);
            assertCondition("N.S1N01".equals(first.getName()), "Expected name N.S1N01, got: " + first.getName());
            assertCondition(first.getTileX() == 114 && first.getTileY() == 50, "Tile mismatch on first station");
            assertCondition(first.getPixelX() == 39 && first.getPixelY() == 79, "Pixel mismatch on first station");

            Station last = stations.get(stations.size() - 1);
            assertCondition("N.S6N25".equals(last.getName()), "Expected name N.S6N25, got: " + last.getName());
            System.out.println("✓ Test 2 Passed: Station data fields parsed correctly.");

            // Test 3: Load GeoJSON Layers
            GeoJsonLayer prefectures = GeoJsonLoader.loadLayer(
                    "prefectures.geojson", "prefectures", GeoJsonLayer.LayerType.POLYGON,
                    UITheme.PREFECTURES_FILL, UITheme.PREFECTURES_STROKE, 0.8f);
            assertCondition(!prefectures.getPaths().isEmpty(), "Prefectures layer paths empty!");

            GeoJsonLayer faults = GeoJsonLoader.loadLayer(
                    "faults.geojson", "faults", GeoJsonLayer.LayerType.LINE,
                    null, UITheme.FAULTS_STROKE, 1.8f);
            assertCondition(!faults.getPaths().isEmpty(), "Faults layer paths empty!");
            System.out.println("✓ Test 3 Passed: Loaded GeoJSON layers (Prefectures: " + prefectures.getPaths().size() +
                    " paths, Faults: " + faults.getPaths().size() + " paths).");

            // Test 4: Load Scale Image
            BufferedImage scaleImg = ImageIO.read(new File("scale.png"));
            assertCondition(scaleImg != null && scaleImg.getWidth() > 0 && scaleImg.getHeight() > 0, "scale.png could not be loaded!");
            System.out.println("✓ Test 4 Passed: Loaded scale.png (" + scaleImg.getWidth() + "x" + scaleImg.getHeight() + ").");

            // Test 5: Fetch TargetTimes & check equality logic
            SmoniDataService service = new SmoniDataService();
            TargetTimeInfo timeInfo = service.fetchLatestTargetTime();
            assertCondition(timeInfo.getBasetime() != null && timeInfo.getBasetime().length() == 14,
                    "Invalid basetime: " + timeInfo.getBasetime());
            assertCondition(timeInfo.getValidtime() != null && timeInfo.getValidtime().length() == 14,
                    "Invalid validtime: " + timeInfo.getValidtime());

            TargetTimeInfo timeInfoCopy = new TargetTimeInfo(timeInfo.getBasetime(), timeInfo.getValidtime());
            assertCondition(timeInfo.equals(timeInfoCopy), "TargetTimeInfo equals failed!");
            assertCondition(timeInfo.hashCode() == timeInfoCopy.hashCode(), "TargetTimeInfo hashCode failed!");
            System.out.println("✓ Test 5 Passed: TargetTimes fetched (basetime=" + timeInfo.getBasetime() +
                    ", JST formatted=" + timeInfo.getFormattedJstTime() + ") & equality verified.");

            // Test 6: Download tiles & sample colors
            Map<String, BufferedImage> tiles = service.updateStationColors(stations, timeInfo);
            assertCondition(!tiles.isEmpty(), "No tiles were downloaded!");
            System.out.println("✓ Test 6 Passed: Downloaded " + tiles.size() + " unique tiles.");

            // Test 7: Verify sampled colors
            int coloredCount = 0;
            for (Station s : stations) {
                Color c = s.getColor();
                if (c != null && !Color.GRAY.equals(c) && !Color.DARK_GRAY.equals(c)) {
                    coloredCount++;
                }
            }
            assertCondition(coloredCount > 0, "No station colors were sampled!");
            System.out.println("✓ Test 7 Passed: Successfully sampled live colors for " + coloredCount + " stations.");

            // Test 8: Verify MapCanvas debug toggle
            MapCanvas canvas = new MapCanvas();
            assertCondition(!canvas.isShowDebugInfo(), "Default showDebugInfo should be false!");
            canvas.setShowDebugInfo(true);
            assertCondition(canvas.isShowDebugInfo(), "showDebugInfo should be true after setter!");
            System.out.println("✓ Test 8 Passed: Dev/Debug tooltip visibility flag verified.");

            // Test 9: Verify KyoshinShindoColorMap loading and intensity resolution
            xyz.hainaut.snetviewer.service.KyoshinShindoColorMap colorMap = xyz.hainaut.snetviewer.service.KyoshinShindoColorMap.getInstance();
            assertCondition(colorMap.getEntries().size() == 101, "Expected 101 entries in color map, got: " + colorMap.getEntries().size());

            assertCondition(Double.valueOf(-3.0).equals(colorMap.getIntensity(new Color(0, 0, 205))), "Expected -3.0 for (0,0,205)");
            assertCondition(Double.valueOf(0.0).equals(colorMap.getIntensity(new Color(63, 250, 54))), "Expected 0.0 for (63,250,54)");
            assertCondition(Double.valueOf(2.0).equals(colorMap.getIntensity(new Color(255, 255, 0))), "Expected 2.0 for (255,255,0)");
            assertCondition(Double.valueOf(7.0).equals(colorMap.getIntensity(new Color(170, 0, 0))), "Expected 7.0 for (170,0,0)");
            assertCondition(colorMap.getIntensity(Color.GRAY) == null, "Placeholder gray should return null");
            assertCondition("--".equals(colorMap.getFormattedIntensity(Color.GRAY)), "Placeholder gray should format to --");

            int validIntensityCount = 0;
            for (Station s : stations) {
                if (s.getIntensity() != null) {
                    validIntensityCount++;
                }
            }
            assertCondition(validIntensityCount > 0, "Expected stations to have resolved intensity, got: " + validIntensityCount);
            System.out.println("✓ Test 9 Passed: Color map loaded (" + colorMap.getEntries().size() + " entries) and resolved intensity for " + validIntensityCount + " stations.");

            service.shutdown();
            System.out.println("\n🎉 ALL TESTS PASSED SUCCESSFULLY! 🎉");
        } catch (Throwable t) {
            System.err.println("\n❌ TEST FAILED: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
