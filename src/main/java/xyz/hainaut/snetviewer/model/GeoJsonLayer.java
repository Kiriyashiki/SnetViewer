package xyz.hainaut.snetviewer.model;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a vector map layer containing paths precomputed in Web Mercator coordinates.
 */
public class GeoJsonLayer {
    public enum LayerType {
        POLYGON,
        LINE
    }

    private final String name;
    private final LayerType type;
    private final List<Path2D.Double> paths;
    private Color fillColor;
    private Color strokeColor;
    private float strokeWidth;
    private boolean visible = true;

    public GeoJsonLayer(String name, LayerType type, List<Path2D.Double> paths,
                        Color fillColor, Color strokeColor, float strokeWidth) {
        this.name = name;
        this.type = type;
        this.paths = (paths != null) ? new ArrayList<>(paths) : new ArrayList<>();
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
    }

    public String getName() {
        return name;
    }

    public LayerType getType() {
        return type;
    }

    public List<Path2D.Double> getPaths() {
        return Collections.unmodifiableList(paths);
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
