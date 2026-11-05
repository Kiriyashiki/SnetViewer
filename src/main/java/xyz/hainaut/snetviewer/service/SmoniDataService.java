package xyz.hainaut.snetviewer.service;

import xyz.hainaut.snetviewer.model.Station;
import xyz.hainaut.snetviewer.model.TargetTimeInfo;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for querying targetTimes.json, downloading tiles, and sampling marker colors.
 */
public class SmoniDataService {
    private static final String TARGET_TIMES_URL = "https://www.msil.go.jp/data/tiles/smoni/targetTimes.json";
    private static final String TILE_URL_FORMAT = "https://www.msil.go.jp/data/tiles/smoni/tileimage/%s/%s/7/%d/%d.png";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) SnetViewer/1.0";

    private final HttpClient httpClient;
    private final ExecutorService downloadExecutor;

    public SmoniDataService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.downloadExecutor = Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "TileDownloader");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Queries targetTimes.json and returns the latest TargetTimeInfo.
     */
    public TargetTimeInfo fetchLatestTargetTime() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TARGET_TIMES_URL))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Failed to fetch targetTimes.json, HTTP status: " + resp.statusCode());
        }

        String json = resp.body();
        Pattern objPattern = Pattern.compile("\"basetime\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"validtime\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = objPattern.matcher(json);

        String latestBasetime = null;
        String latestValidtime = null;

        while (matcher.find()) {
            String b = matcher.group(1).trim();
            String v = matcher.group(2).trim();
            if (latestBasetime == null || b.compareTo(latestBasetime) > 0) {
                latestBasetime = b;
                latestValidtime = v;
            }
        }

        if (latestBasetime == null || latestValidtime == null) {
            throw new IOException("No valid basetime/validtime entries found in targetTimes.json response.");
        }

        return new TargetTimeInfo(latestBasetime, latestValidtime);
    }

    /**
     * Downloads unique tile images concurrently and updates the colors of each station.
     *
     * @param stations list of stations to update
     * @param timeInfo target time info with basetime and validtime
     * @return map of tile coordinates (key: "x/y") to BufferedImage
     */
    public Map<String, BufferedImage> updateStationColors(List<Station> stations, TargetTimeInfo timeInfo) {
        // 1. Identify all unique tile coordinates
        Set<String> uniqueTileKeys = new HashSet<>();
        for (Station s : stations) {
            uniqueTileKeys.add(s.getTileKey());
        }

        // 2. Download tiles concurrently without duplicate requests
        Map<String, BufferedImage> tileMap = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String tileKey : uniqueTileKeys) {
            String[] parts = tileKey.split("/");
            int tileX = Integer.parseInt(parts[0]);
            int tileY = Integer.parseInt(parts[1]);

            String tileUrl = String.format(TILE_URL_FORMAT,
                    timeInfo.getBasetime(), timeInfo.getValidtime(), tileX, tileY);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    HttpRequest tileReq = HttpRequest.newBuilder()
                            .uri(URI.create(tileUrl))
                            .header("User-Agent", USER_AGENT)
                            .timeout(Duration.ofSeconds(15))
                            .GET()
                            .build();

                    HttpResponse<byte[]> tileResp = httpClient.send(tileReq, HttpResponse.BodyHandlers.ofByteArray());
                    if (tileResp.statusCode() == 200 && tileResp.body() != null && tileResp.body().length > 0) {
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(tileResp.body()));
                        if (img != null) {
                            tileMap.put(tileKey, img);
                        }
                    } else {
                        System.err.println("Warning: Tile " + tileKey + " returned HTTP " + tileResp.statusCode());
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Failed to load tile " + tileKey + ": " + e.getMessage());
                }
            }, downloadExecutor);

            futures.add(future);
        }

        // Wait for all tile downloads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 3. Sample colors for each station
        for (Station s : stations) {
            BufferedImage img = tileMap.get(s.getTileKey());
            if (img != null) {
                int px = s.getPixelX();
                int py = s.getPixelY();
                if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                    int argb = img.getRGB(px, py);
                    // Extract color, handle transparency if needed
                    s.setColor(new Color(argb, true));
                } else {
                    s.setColor(Color.GRAY);
                }
            } else {
                s.setColor(Color.DARK_GRAY);
            }
        }

        return tileMap;
    }

    public void shutdown() {
        downloadExecutor.shutdown();
    }
}
