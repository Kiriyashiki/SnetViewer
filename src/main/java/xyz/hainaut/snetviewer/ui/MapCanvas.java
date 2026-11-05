package xyz.hainaut.snetviewer.ui;

import xyz.hainaut.snetviewer.model.GeoJsonLayer;
import xyz.hainaut.snetviewer.model.LandStation;
import xyz.hainaut.snetviewer.model.Station;
import xyz.hainaut.snetviewer.model.TargetTimeInfo;
import xyz.hainaut.snetviewer.service.AppSettings;
import xyz.hainaut.snetviewer.service.I18n;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive map canvas that projects and renders seismic stations, GeoJSON layers,
 * real-time sampled colors, and the HUD / color scale.
 */
public class MapCanvas extends JPanel {
    private final List<Station> stations = new ArrayList<>();

    // Fixed land stations
    private static final List<LandStation> LAND_STATIONS = List.of(
            new LandStation("八戸陸上局", "Hachinohe Station", 40.540751, 141.565287),
            new LandStation("宮古陸上局", "Miyako Station", 39.684946, 141.955831),
            new LandStation("亘理陸上局", "Watari Station", 38.039498, 140.915106),
            new LandStation("鹿嶋陸上局", "Kashima Station", 35.997241, 140.643624),
            new LandStation("南房総陸上局", "Minamiboso Station", 34.910325, 139.860436)
    );

    private TargetTimeInfo targetTimeInfo;
    private I18n.Key statusMessageKey = I18n.Key.MAP_EMPTY_INITIALIZING;
    private Object[] statusMessageArgs = new Object[0];
    private boolean isUpdating = false;
    private boolean showLabels = false;
    private boolean showDebugInfo = false;
    private int rotationAngle = 0;

    // Vector GeoJSON Map Layers
    private GeoJsonLayer worldLayer;
    private GeoJsonLayer prefecturesLayer;
    private GeoJsonLayer faultsLayer;

    // Color Scale Image
    private BufferedImage scaleImage;

    // Geographic bounding box (Web Mercator)
    private double minMercX = 0;
    private double maxMercX = 1;
    private double minMercY = 0;
    private double maxMercY = 1;
    private double centerMercX = 0.5;
    private double centerMercY = 0.5;
    private double spanMercX = 1;
    private double spanMercY = 1;
    private boolean boundsCalculated = false;

    // View transformation state
    private double zoomFactor = 1.0;
    private double panOffsetX = 0.0;
    private double panOffsetY = 0.0;
    private static final double PADDING = 30.0;
    private static final double MARKER_RADIUS = 6.0;
    private static final double LAND_MARKER_SIZE = 6.0;

    // HUD / legend layout (kept in sync with drawTimeHud and the scale image placement)
    private static final int HUD_X = 16;
    private static final int HUD_Y = 16;
    private static final int HUD_WIDTH = 260;
    private static final int HUD_HEIGHT = 64;
    private static final int SCALE_X = 16;
    private static final int SCALE_Y = HUD_Y + HUD_HEIGHT + 10; // 90px from top
    private static final double HUD_CLEARANCE = 16.0;

    // Interaction state
    private Point lastMousePoint;
    private Station hoveredStation;
    private Point hoveredMousePoint;

    public MapCanvas() {
        setBackground(UITheme.BG_MAP);
        setDoubleBuffered(true);
        setFocusable(true);

        this.showLabels = AppSettings.getInstance().isShowStationLabels();
        this.showDebugInfo = AppSettings.getInstance().isShowDevInfo();
        this.rotationAngle = AppSettings.getInstance().getMapRotation();

        AppSettings.getInstance().addListener(settings -> {
            setShowLabels(settings.isShowStationLabels());
            setShowDebugInfo(settings.isShowDevInfo());
            setRotationAngle(settings.getMapRotation());
        });

        setupMouseListeners();
        setupKeyListeners();
        loadDefaultScaleImage();
    }

    private void loadDefaultScaleImage() {
        try {
            File scaleFile = new File("scale.png");
            if (scaleFile.exists()) {
                this.scaleImage = ImageIO.read(scaleFile);
                return;
            }
            InputStream in = getClass().getResourceAsStream("/scale.png");
            if (in != null) {
                this.scaleImage = ImageIO.read(in);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load default scale.png: " + e.getMessage());
        }
    }

    public synchronized void setStations(List<Station> newStations) {
        this.stations.clear();
        if (newStations != null) {
            this.stations.addAll(newStations);
        }
        computeBounds();
        repaint();
    }

    public synchronized void setTargetTimeInfo(TargetTimeInfo targetTimeInfo) {
        this.targetTimeInfo = targetTimeInfo;
        repaint();
    }

    public synchronized void setStatusMessage(I18n.Key key, boolean isUpdating, Object... args) {
        this.statusMessageKey = (key != null) ? key : I18n.Key.MAP_EMPTY_INITIALIZING;
        this.statusMessageArgs = (args != null) ? args : new Object[0];
        this.isUpdating = isUpdating;
        repaint();
    }

    public synchronized void setWorldLayer(GeoJsonLayer worldLayer) {
        this.worldLayer = worldLayer;
        repaint();
    }

    public synchronized void setPrefecturesLayer(GeoJsonLayer prefecturesLayer) {
        this.prefecturesLayer = prefecturesLayer;
        repaint();
    }

    public synchronized void setFaultsLayer(GeoJsonLayer faultsLayer) {
        this.faultsLayer = faultsLayer;
        repaint();
    }

    public synchronized void setScaleImage(BufferedImage scaleImage) {
        this.scaleImage = scaleImage;
        repaint();
    }

    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
        repaint();
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public void setShowDebugInfo(boolean showDebugInfo) {
        this.showDebugInfo = showDebugInfo;
        repaint();
    }

    public boolean isShowDebugInfo() {
        return showDebugInfo;
    }

    public void setRotationAngle(int rotationAngle) {
        this.rotationAngle = AppSettings.normalizeRotation(rotationAngle);
        repaint();
    }

    public int getRotationAngle() {
        return rotationAngle;
    }

    public synchronized void resetView() {
        this.zoomFactor = 1.0;
        this.panOffsetX = 0.0;
        this.panOffsetY = 0.0;
        repaint();
    }

    private void computeBounds() {
        if (stations.isEmpty()) {
            boundsCalculated = false;
            return;
        }

        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (Station s : stations) {
            double mx = s.getMercatorX();
            double my = s.getMercatorY();
            if (mx < minX) minX = mx;
            if (mx > maxX) maxX = mx;
            if (my < minY) minY = my;
            if (my > maxY) maxY = my;
        }

        // Add slight margin if min equals max
        if (minX == maxX) { minX -= 0.01; maxX += 0.01; }
        if (minY == maxY) { minY -= 0.01; maxY += 0.01; }

        this.minMercX = minX;
        this.maxMercX = maxX;
        this.minMercY = minY;
        this.maxMercY = maxY;
        this.centerMercX = (minX + maxX) / 2.0;
        this.centerMercY = (minY + maxY) / 2.0;
        this.spanMercX = Math.max(0.0001, maxX - minX);
        this.spanMercY = Math.max(0.0001, maxY - minY);
        this.boundsCalculated = true;
    }

    private double computeBaseScale(int width, int height) {
        if (!boundsCalculated) return 1.0;
        double availW = availableWidth(width);
        double availH = availableHeight(height);
        if (rotationAngle == 90 || rotationAngle == 270) {
            return Math.min(availW / spanMercY, availH / spanMercX);
        } else {
            return Math.min(availW / spanMercX, availH / spanMercY);
        }
    }

    /**
     * Screen-space insets reserved for the overlaid HUD and legend. The default/fit view places
     * the map inside these bounds so markers are not hidden underneath the left column (legend)
     * or the top time card. Right and bottom only keep the regular padding.
     */
    private double viewLeftInset() {
        double legendWidth = (scaleImage != null) ? scaleImage.getWidth() : 0.0;
        return Math.max(PADDING, SCALE_X + legendWidth + HUD_CLEARANCE);
    }

    private double viewTopInset() {
        return Math.max(PADDING, HUD_Y + HUD_HEIGHT + HUD_CLEARANCE);
    }

    private double availableWidth(int width) {
        return Math.max(50.0, width - viewLeftInset() - PADDING);
    }

    private double availableHeight(int height) {
        return Math.max(50.0, height - viewTopInset() - PADDING);
    }

    private double viewCenterX(int width) {
        return viewLeftInset() + availableWidth(width) / 2.0;
    }

    private double viewCenterY(int height) {
        return viewTopInset() + availableHeight(height) / 2.0;
    }

    private Point2D.Double projectToScreen(Station s, int width, int height, double currentScale) {
        return projectToScreen(s.getMercatorX(), s.getMercatorY(), width, height, currentScale);
    }

    private Point2D.Double projectToScreen(double mercatorX, double mercatorY,
                                           int width, int height, double currentScale) {
        double dx = (mercatorX - centerMercX) * currentScale;
        double dy = -(mercatorY - centerMercY) * currentScale;

        if (rotationAngle != 0) {
            double rotRad = Math.toRadians(rotationAngle);
            double cos = Math.cos(rotRad);
            double sin = Math.sin(rotRad);
            double rotX = dx * cos - dy * sin;
            double rotY = dx * sin + dy * cos;
            double sx = viewCenterX(width) + panOffsetX + rotX;
            double sy = viewCenterY(height) + panOffsetY + rotY;
            return new Point2D.Double(sx, sy);
        } else {
            double sx = viewCenterX(width) + panOffsetX + dx;
            double sy = viewCenterY(height) + panOffsetY + dy;
            return new Point2D.Double(sx, sy);
        }
    }

    private void setupMouseListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                    lastMousePoint = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastMousePoint = null;
                updateCursor(e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastMousePoint != null) {
                    int dx = e.getX() - lastMousePoint.x;
                    int dy = e.getY() - lastMousePoint.y;
                    panOffsetX += dx;
                    panOffsetY += dy;
                    lastMousePoint = e.getPoint();
                    hoveredMousePoint = e.getPoint();
                    findHoveredStation(e.getPoint());
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                hoveredMousePoint = e.getPoint();
                findHoveredStation(e.getPoint());
                updateCursor(e.getPoint());
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int width = getWidth();
                int height = getHeight();
                if (width <= 0 || height <= 0 || !boundsCalculated) return;

                double baseScale = computeBaseScale(width, height);
                double oldScale = baseScale * zoomFactor;

                // Zoom factor calculation
                double zoomDelta = (e.getWheelRotation() < 0) ? 1.15 : (1.0 / 1.15);
                double newZoom = Math.max(0.2, Math.min(50.0, zoomFactor * zoomDelta));

                if (newZoom != zoomFactor) {
                    double sx = e.getX();
                    double sy = e.getY();
                    double cx = viewCenterX(width);
                    double cy = viewCenterY(height);

                    double relX = sx - cx - panOffsetX;
                    double relY = sy - cy - panOffsetY;

                    double unrotX, unrotY;
                    if (rotationAngle != 0) {
                        double rotRad = Math.toRadians(rotationAngle);
                        double cos = Math.cos(rotRad);
                        double sin = Math.sin(rotRad);
                        unrotX = relX * cos + relY * sin;
                        unrotY = -relX * sin + relY * cos;
                    } else {
                        unrotX = relX;
                        unrotY = relY;
                    }

                    // World point under cursor before zoom
                    double mercX = centerMercX + unrotX / oldScale;
                    double mercY = centerMercY - unrotY / oldScale;

                    zoomFactor = newZoom;
                    double newScale = baseScale * zoomFactor;

                    double newDx = (mercX - centerMercX) * newScale;
                    double newDy = -(mercY - centerMercY) * newScale;

                    double newRotX, newRotY;
                    if (rotationAngle != 0) {
                        double rotRad = Math.toRadians(rotationAngle);
                        double cos = Math.cos(rotRad);
                        double sin = Math.sin(rotRad);
                        newRotX = newDx * cos - newDy * sin;
                        newRotY = newDx * sin + newDy * cos;
                    } else {
                        newRotX = newDx;
                        newRotY = newDy;
                    }

                    // Keep world point under cursor after zoom
                    panOffsetX = sx - cx - newRotX;
                    panOffsetY = sy - cy - newRotY;

                    findHoveredStation(e.getPoint());
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    resetView();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoveredStation = null;
                hoveredMousePoint = null;
                repaint();
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        addMouseWheelListener(mouseAdapter);
    }

    private void setupKeyListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_R) {
                    resetView();
                } else if (e.getKeyCode() == KeyEvent.VK_L) {
                    AppSettings.getInstance().setShowStationLabels(!AppSettings.getInstance().isShowStationLabels());
                } else if (e.getKeyCode() == KeyEvent.VK_D) {
                    AppSettings.getInstance().setShowDevInfo(!AppSettings.getInstance().isShowDevInfo());
                } else if (e.getKeyChar() == '+' || e.getKeyCode() == KeyEvent.VK_EQUALS) {
                    zoomAroundCenter(1.2);
                } else if (e.getKeyChar() == '-' || e.getKeyCode() == KeyEvent.VK_MINUS) {
                    zoomAroundCenter(1.0 / 1.2);
                }
            }
        });
    }

    private void zoomAroundCenter(double factor) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0 || !boundsCalculated) return;

        double baseScale = computeBaseScale(width, height);
        double newZoom = Math.max(0.2, Math.min(50.0, zoomFactor * factor));
        if (newZoom != zoomFactor) {
            zoomFactor = newZoom;
            repaint();
        }
    }

    private void updateCursor(Point p) {
        if (hoveredStation != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private synchronized void findHoveredStation(Point mousePoint) {
        if (mousePoint == null || !boundsCalculated || stations.isEmpty()) {
            hoveredStation = null;
            return;
        }

        int width = getWidth();
        int height = getHeight();
        double currentScale = computeBaseScale(width, height) * zoomFactor;
        double hitThresholdSq = 14.0 * 14.0; // 14px hit radius

        Station closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Station s : stations) {
            Point2D.Double screenPt = projectToScreen(s, width, height, currentScale);
            double dx = screenPt.x - mousePoint.x;
            double dy = screenPt.y - mousePoint.y;
            double distSq = dx * dx + dy * dy;
            if (distSq <= hitThresholdSq && distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = s;
            }
        }

        this.hoveredStation = closest;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        int width = getWidth();
        int height = getHeight();

        // 1. Draw Map Canvas Background (Ocean)
        g2.setColor(UITheme.BG_MAP);
        g2.fillRect(0, 0, width, height);

        // 2. Draw GeoJSON Map Layers (world, prefectures, faults)
        if (boundsCalculated) {
            double currentScale = computeBaseScale(width, height) * zoomFactor;
            Graphics2D mapG2 = (Graphics2D) g2.create();
            mapG2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            mapG2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            AffineTransform tx = new AffineTransform();
            tx.translate(viewCenterX(width) + panOffsetX, viewCenterY(height) + panOffsetY);
            if (rotationAngle != 0) {
                tx.rotate(Math.toRadians(rotationAngle));
            }
            tx.scale(currentScale, -currentScale);
            tx.translate(-centerMercX, -centerMercY);
            mapG2.transform(tx);

            // 2a. World layer (without Japan)
            if (worldLayer != null && worldLayer.isVisible()) {
                mapG2.setColor(worldLayer.getFillColor());
                for (Path2D.Double path : worldLayer.getPaths()) {
                    mapG2.fill(path);
                }
                mapG2.setColor(worldLayer.getStrokeColor());
                mapG2.setStroke(new BasicStroke((float)(worldLayer.getStrokeWidth() / currentScale)));
                for (Path2D.Double path : worldLayer.getPaths()) {
                    mapG2.draw(path);
                }
            }

            // 2b. Japan Prefectures layer
            if (prefecturesLayer != null && prefecturesLayer.isVisible()) {
                mapG2.setColor(prefecturesLayer.getFillColor());
                for (Path2D.Double path : prefecturesLayer.getPaths()) {
                    mapG2.fill(path);
                }
                mapG2.setColor(prefecturesLayer.getStrokeColor());
                mapG2.setStroke(new BasicStroke((float)(prefecturesLayer.getStrokeWidth() / currentScale)));
                for (Path2D.Double path : prefecturesLayer.getPaths()) {
                    mapG2.draw(path);
                }
            }

            // 2c. Faults / Trench line layer
            if (faultsLayer != null && faultsLayer.isVisible()) {
                mapG2.setColor(faultsLayer.getStrokeColor());
                mapG2.setStroke(new BasicStroke(
                        (float)(faultsLayer.getStrokeWidth() / currentScale),
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                ));
                for (Path2D.Double path : faultsLayer.getPaths()) {
                    mapG2.draw(path);
                }
            }

            mapG2.dispose();
        }

        // 3. Draw fixed land stations above map layers but below live station markers
        if (boundsCalculated) {
            double currentScale = computeBaseScale(width, height) * zoomFactor;
            double landSize = Math.max(5.0, Math.min(14.0, LAND_MARKER_SIZE * Math.sqrt(zoomFactor)));
            for (LandStation landStation : LAND_STATIONS) {
                Point2D.Double pt = projectToScreen(
                        landStation.getMercatorX(), landStation.getMercatorY(),
                        width, height, currentScale);
                drawLandStation(g2, pt.x, pt.y, landSize);
            }
        }

        // 4. Draw Station Markers ABOVE GeoJSON layers
        synchronized (this) {
            if (boundsCalculated && !stations.isEmpty()) {
                double currentScale = computeBaseScale(width, height) * zoomFactor;
                double r = Math.max(4.0, Math.min(18.0, MARKER_RADIUS * Math.sqrt(zoomFactor)));

                // Draw non-hovered markers first
                for (Station s : stations) {
                    if (s == hoveredStation) continue;
                    Point2D.Double pt = projectToScreen(s, width, height, currentScale);
                    drawMarker(g2, s, pt.x, pt.y, r, false);
                }

                // Draw hovered marker on top with glow
                if (hoveredStation != null) {
                    Point2D.Double pt = projectToScreen(hoveredStation, width, height, currentScale);
                    drawMarker(g2, hoveredStation, pt.x, pt.y, r, true);
                }
            } else {
                drawEmptyState(g2, width, height);
            }
        }

        // 5. Draw Top-Left Time HUD Badge
        drawTimeHud(g2);

        // 6. Draw Scale Image aligned to left edge directly under the time HUD
        if (scaleImage != null) {
            g2.drawImage(scaleImage, SCALE_X, SCALE_Y, null);
        }

        // 7. Draw Floating Tooltip if hovering over a station
        if (hoveredStation != null && hoveredMousePoint != null) {
            drawTooltip(g2, hoveredStation, hoveredMousePoint.x, hoveredMousePoint.y, width, height);
        }

        g2.setFont(UITheme.FONT_TOOLTIP_BODY);
        g2.setColor(UITheme.TEXT_SECONDARY);
        g2.drawString(I18n.t(I18n.Key.FOOTER_DATA), 10, height - 6);

        g2.dispose();
    }

    private void drawMarker(Graphics2D g2, Station station, double x, double y, double radius, boolean isHovered) {
        Ellipse2D.Double circle = new Ellipse2D.Double(x - radius, y - radius, radius * 2.0, radius * 2.0);

        if (isHovered) {
            // Glow effect
            g2.setColor(UITheme.MARKER_GLOW);
            double glowRadius = radius + 6.0;
            g2.fill(new Ellipse2D.Double(x - glowRadius, y - glowRadius, glowRadius * 2.0, glowRadius * 2.0));

            // Outer highlight ring
            g2.setColor(UITheme.MARKER_HOVER_RING);
            g2.setStroke(new BasicStroke(2.0f));
            double outerRadius = radius + 2.5;
            g2.draw(new Ellipse2D.Double(x - outerRadius, y - outerRadius, outerRadius * 2.0, outerRadius * 2.0));
        }

        // Fill with sampled pixel color
        Color fillColor = station.getColor();
        g2.setColor(fillColor);
        g2.fill(circle);

        // Outline border
        g2.setColor(UITheme.MARKER_BORDER);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(circle);

        // Optional station labels
        if (showLabels || isHovered) {
            g2.setFont(UITheme.FONT_MARKER_LABEL);
            g2.setColor(isHovered ? UITheme.TEXT_PRIMARY : UITheme.TEXT_SECONDARY);
            FontMetrics fm = g2.getFontMetrics();
            String label = station.getName();
            int lx = (int) (x + radius + 4);
            int ly = (int) (y + fm.getAscent() / 2 - 1);
            g2.drawString(label, lx, ly);
        }
    }

    private void drawLandStation(Graphics2D g2, double x, double y, double size) {
        Rectangle2D.Double square = new Rectangle2D.Double(
                x - size / 2.0, y - size / 2.0, size, size);

        g2.setColor(UITheme.LAND_STATION);
        g2.fill(square);

        g2.setColor(UITheme.MARKER_BORDER);
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(square);
    }

    private void drawTimeHud(Graphics2D g2) {
        int x = HUD_X;
        int y = HUD_Y;
        int cardWidth = HUD_WIDTH;
        int cardHeight = HUD_HEIGHT;

        // Background card
        RoundRectangle2D.Double card = new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 12, 12);
        g2.setColor(UITheme.BG_HUD);
        g2.fill(card);
        g2.setColor(UITheme.BORDER_HUD);
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(card);

        // Time text
        g2.setColor(UITheme.TEXT_PRIMARY);
        g2.setFont(UITheme.FONT_TIME_HUD);

        String timeText = (targetTimeInfo != null)
                ? targetTimeInfo.getFormattedJstTime()
                : "--:--";

        g2.drawString(timeText, x + 14, y + 28);

        // Subtitle / Timezone & Status
        g2.setFont(UITheme.FONT_TIME_SUB);
        g2.setColor(UITheme.TEXT_SECONDARY);
        String subText = (targetTimeInfo != null)
                ? I18n.t(I18n.Key.HUD_TIME_SUBTITLE, stations.size())
                : I18n.t(I18n.Key.HUD_FETCHING_DATA);
        g2.drawString(subText, x + 14, y + 48);

        // Status Indicator Dot
        int dotX = x + cardWidth - 22;
        int dotY = y + 18;
        int dotSize = 8;
        Color statusColor = isUpdating ? UITheme.STATUS_BUSY : (targetTimeInfo != null ? UITheme.STATUS_OK : UITheme.STATUS_BUSY);
        g2.setColor(statusColor);
        g2.fillOval(dotX, dotY, dotSize, dotSize);
    }

    private void drawTooltip(Graphics2D g2, Station s, int mouseX, int mouseY, int viewW, int viewH) {
        int cardW = showDebugInfo ? 240 : 220;
        int cardH = showDebugInfo ? 142 : 106;
        int tipX = mouseX + 16;
        int tipY = mouseY + 16;

        // Prevent off-screen clipping
        if (tipX + cardW > viewW - 12) {
            tipX = mouseX - cardW - 12;
        }
        if (tipY + cardH > viewH - 12) {
            tipY = mouseY - cardH - 12;
        }

        RoundRectangle2D.Double tipCard = new RoundRectangle2D.Double(tipX, tipY, cardW, cardH, 10, 10);
        g2.setColor(UITheme.BG_HUD);
        g2.fill(tipCard);
        g2.setColor(UITheme.BORDER_HUD);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(tipCard);

        // Tooltip Content
        int textX = tipX + 12;
        int curY = tipY + 20;

        // Header: Station Name & Color Swatch
        g2.setFont(UITheme.FONT_TOOLTIP_TITLE);
        g2.setColor(UITheme.TEXT_PRIMARY);
        g2.drawString(s.getName(), textX, curY);

        // Color Swatch
        int swatchSize = 12;
        int swatchX = tipX + cardW - 12 - swatchSize;
        int swatchY = curY - 10;
        g2.setColor(s.getColor());
        g2.fillRoundRect(swatchX, swatchY, swatchSize, swatchSize, 4, 4);
        g2.setColor(UITheme.MARKER_BORDER);
        g2.drawRoundRect(swatchX, swatchY, swatchSize, swatchSize, 4, 4);

        // Separator line
        curY += 8;
        g2.setColor(UITheme.BORDER_HUD);
        g2.drawLine(textX, curY, tipX + cardW - 12, curY);

        // Standard Details
        g2.setFont(UITheme.FONT_TOOLTIP_BODY);
        curY += 16;
        g2.setColor(UITheme.TEXT_SECONDARY);
        g2.drawString(I18n.t(I18n.Key.TOOLTIP_LAT_LNG, s.getLat(), s.getLng()), textX, curY);

        curY += 16;
        g2.drawString(I18n.t(I18n.Key.TOOLTIP_DEPTH, s.getDepth()), textX, curY);

        curY += 16;
        g2.drawString(I18n.t(I18n.Key.TOOLTIP_INTENSITY, s.getFormattedIntensity()), textX, curY);

        // Dev/Debug Details (only if enabled)
        if (showDebugInfo) {
            curY += 16;
            g2.drawString(I18n.t(I18n.Key.TOOLTIP_TILE,
                    s.getTileX(), s.getTileY(), s.getPixelX(), s.getPixelY()), textX, curY);

            curY += 16;
            Color c = s.getColor();
            String hex = String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
            g2.drawString(I18n.t(I18n.Key.TOOLTIP_COLOR,
                    hex, c.getRed(), c.getGreen(), c.getBlue()), textX, curY);
        }
    }

    private void drawEmptyState(Graphics2D g2, int width, int height) {
        g2.setFont(UITheme.FONT_TIME_HUD);
        g2.setColor(UITheme.TEXT_SECONDARY);
        FontMetrics fm = g2.getFontMetrics();
        String msg = I18n.t(statusMessageKey, statusMessageArgs);
        int textW = fm.stringWidth(msg);
        g2.drawString(msg, (width - textW) / 2, height / 2);
    }
}
