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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class RenderTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Testing full map rendering with GeoJSON layers and scale.png...");

        List<Station> stations = StationCsvLoader.loadStations("sta-mapped.csv");
        SmoniDataService service = new SmoniDataService();
        TargetTimeInfo timeInfo = service.fetchLatestTargetTime();
        service.updateStationColors(stations, timeInfo);

        MapCanvas canvas = new MapCanvas();
        canvas.setSize(new Dimension(1200, 900));

        // Load layers
        try {
            GeoJsonLayer world = GeoJsonLoader.loadLayer(
                    "world.geojson", "world", GeoJsonLayer.LayerType.POLYGON,
                    UITheme.WORLD_FILL, UITheme.WORLD_STROKE, 0.8f);
            canvas.setWorldLayer(world);
        } catch (Exception ignored) {}

        GeoJsonLayer prefectures = GeoJsonLoader.loadLayer(
                "prefectures.geojson", "prefectures", GeoJsonLayer.LayerType.POLYGON,
                UITheme.PREFECTURES_FILL, UITheme.PREFECTURES_STROKE, 0.8f);
        canvas.setPrefecturesLayer(prefectures);

        GeoJsonLayer faults = GeoJsonLoader.loadLayer(
                "faults.geojson", "faults", GeoJsonLayer.LayerType.LINE,
                null, UITheme.FAULTS_STROKE, 1.8f);
        canvas.setFaultsLayer(faults);

        canvas.setStations(stations);
        canvas.setTargetTimeInfo(timeInfo);

        BufferedImage output = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        canvas.paint(g2);
        g2.dispose();

        File outFile = new File("build/test_map_render.png");
        outFile.getParentFile().mkdirs();
        ImageIO.write(output, "PNG", outFile);

        System.out.println("✓ Map render successful! Output image written to: " + outFile.getAbsolutePath());
        service.shutdown();
    }
}
