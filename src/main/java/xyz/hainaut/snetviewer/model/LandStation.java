package xyz.hainaut.snetviewer.model;

import java.util.Objects;

/**
 * Represents a fixed land-based seismic station.
 *
 * <p>Land stations are rendered as static markers and intentionally do not participate in the
 * bounds calculation used by the fit-to-screen / default view.</p>
 */
public class LandStation {
    private final String japaneseName;
    private final String englishName;
    private final double lat;
    private final double lng;

    // Precomputed Web Mercator coordinates in radians (EPSG:3857)
    private final double mercatorX;
    private final double mercatorY;

    public LandStation(String japaneseName, String englishName, double lat, double lng) {
        this.japaneseName = Objects.requireNonNull(japaneseName, "japaneseName must not be null");
        this.englishName = Objects.requireNonNull(englishName, "englishName must not be null");
        this.lat = lat;
        this.lng = lng;

        this.mercatorX = Math.toRadians(lng);
        double latRad = Math.toRadians(Math.max(-85.05112878, Math.min(85.05112878, lat)));
        this.mercatorY = Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));
    }

    public String getJapaneseName() {
        return japaneseName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getDisplayName() {
        return japaneseName + " " + englishName;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public double getMercatorX() {
        return mercatorX;
    }

    public double getMercatorY() {
        return mercatorY;
    }

    @Override
    public String toString() {
        return "LandStation{" +
                "name='" + getDisplayName() + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                '}';
    }
}
