package xyz.hainaut.snetviewer.model;

import java.awt.Color;
import java.util.Objects;

/**
 * Represents a seismic monitoring station loaded from CSV.
 */
public class Station {
    private final String name;
    private final double lat;
    private final double lng;
    private final double depth;
    private final int tileX;
    private final int tileY;
    private final int pixelX;
    private final int pixelY;

    // Projected coordinates (Web Mercator)
    private final double mercatorX;
    private final double mercatorY;

    // Dynamically sampled color from tile image
    private volatile Color color = Color.GRAY;

    public Station(String name, double lat, double lng, double depth, int tileX, int tileY, int pixelX, int pixelY) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.lat = lat;
        this.lng = lng;
        this.depth = depth;
        this.tileX = tileX;
        this.tileY = tileY;
        this.pixelX = pixelX;
        this.pixelY = pixelY;

        // Precompute Web Mercator coordinates in radians (EPSG:3857)
        // Longitude: x in radians
        this.mercatorX = Math.toRadians(lng);
        // Latitude: Web Mercator projection y in radians
        double latRad = Math.toRadians(Math.max(-85.05112878, Math.min(85.05112878, lat)));
        this.mercatorY = Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));
    }

    public String getName() {
        return name;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public double getDepth() {
        return depth;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public int getPixelX() {
        return pixelX;
    }

    public int getPixelY() {
        return pixelY;
    }

    public String getTileKey() {
        return tileX + "/" + tileY;
    }

    public double getMercatorX() {
        return mercatorX;
    }

    public double getMercatorY() {
        return mercatorY;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = (color != null) ? color : Color.GRAY;
    }

    public Double getIntensity() {
        return xyz.hainaut.snetviewer.service.KyoshinShindoColorMap.getInstance().getIntensity(color);
    }

    public String getFormattedIntensity() {
        return xyz.hainaut.snetviewer.service.KyoshinShindoColorMap.getInstance().getFormattedIntensity(color);
    }

    @Override
    public String toString() {
        return "Station{" +
                "name='" + name + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                ", depth=" + depth +
                ", tile=(" + tileX + "," + tileY + ")" +
                ", pixel=(" + pixelX + "," + pixelY + ")" +
                ", color=" + color +
                '}';
    }
}
