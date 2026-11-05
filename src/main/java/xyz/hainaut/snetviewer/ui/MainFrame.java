package xyz.hainaut.snetviewer.ui;

import xyz.hainaut.snetviewer.model.GeoJsonLayer;
import xyz.hainaut.snetviewer.model.Station;
import xyz.hainaut.snetviewer.model.TargetTimeInfo;
import xyz.hainaut.snetviewer.service.AppSettings;
import xyz.hainaut.snetviewer.service.GeoJsonLoader;
import xyz.hainaut.snetviewer.service.I18n;
import xyz.hainaut.snetviewer.service.SmoniDataService;
import xyz.hainaut.snetviewer.service.StationCsvLoader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main application window for SnetViewer.
 */
public class MainFrame extends JFrame {
    private final MapCanvas mapCanvas;
    private final SmoniDataService dataService;
    private final JLabel statusLabel;
    private JButton refreshButton;
    private JButton fitButton;
    private JButton settingsButton;
    private SettingsDialog settingsDialog;

    private List<Station> stations;
    private TargetTimeInfo lastTargetTimeInfo = null;
    private final Timer periodicTimer;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    // Last status message, kept as an i18n key so it can be re-rendered when language changes.
    private volatile I18n.Key statusKey = I18n.Key.STATUS_STARTING;
    private volatile Object[] statusArgs = new Object[0];
    private volatile boolean statusBusy = false;
    private volatile boolean statusError = false;

    public MainFrame() {
        super("SnetViewer");

        this.dataService = new SmoniDataService();
        this.mapCanvas = new MapCanvas();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(400, 400));
        setPreferredSize(new Dimension(700, 550));
        getContentPane().setBackground(UITheme.BG_MAP);

        // Top Toolbar
        JToolBar toolBar = createToolBar();

        // Bottom Status Bar
        this.statusLabel = new JLabel(I18n.t(I18n.Key.STATUS_STARTING));
        this.statusLabel.setFont(UITheme.FONT_STATUS);
        this.statusLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(UITheme.BG_PANEL);
        statusBar.setBorder(new EmptyBorder(6, 12, 6, 12));
        statusBar.add(statusLabel, BorderLayout.CENTER);

        // Layout
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(mapCanvas, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);

        // Periodic timer to query at exactly :30 of every minute
        this.periodicTimer = new Timer(60_000, e -> {
            if (AppSettings.getInstance().isAutoRefresh()) {
                refreshData(false);
            }
        });

        // Listen for settings changes to control periodic timer and refresh localized text
        AppSettings.getInstance().addListener(settings -> {
            if (settings.isAutoRefresh()) {
                scheduleNextPeriodicCheck();
            } else {
                periodicTimer.stop();
            }
            SwingUtilities.invokeLater(this::updateLocalizedTexts);
        });

        // Global Key Bindings (F5: Refresh, L: Toggle IDs, D: Toggle Dev Info)
        setupGlobalKeyBindings();

        // Cleanup on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                periodicTimer.stop();
                dataService.shutdown();
                if (settingsDialog != null) {
                    settingsDialog.dispose();
                }
            }
        });

        // Initialize map layers, CSV, and live data
        initializeData();
    }

    private void setupGlobalKeyBindings() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        // F5 -> Refresh
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refreshData");
        am.put("refreshData", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshData(true);
            }
        });

        // D -> Toggle Dev Info
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "toggleDebug");
        am.put("toggleDebug", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppSettings.getInstance().setShowDevInfo(!AppSettings.getInstance().isShowDevInfo());
            }
        });

        // L -> Toggle Labels
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "toggleLabels");
        am.put("toggleLabels", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppSettings.getInstance().setShowStationLabels(!AppSettings.getInstance().isShowStationLabels());
            }
        });
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(UITheme.BG_PANEL);
        toolBar.setBorder(new EmptyBorder(8, 12, 8, 12));

        refreshButton = createStyledButton(I18n.t(I18n.Key.BUTTON_REFRESH), I18n.t(I18n.Key.TOOLTIP_REFRESH));
        refreshButton.addActionListener(e -> refreshData(true));
        toolBar.add(refreshButton);
        toolBar.add(Box.createHorizontalStrut(8));

        fitButton = createStyledButton(I18n.t(I18n.Key.BUTTON_FIT), I18n.t(I18n.Key.TOOLTIP_FIT));
        fitButton.addActionListener(e -> mapCanvas.resetView());
        toolBar.add(fitButton);
        toolBar.add(Box.createHorizontalStrut(8));

        settingsButton = createStyledButton(I18n.t(I18n.Key.BUTTON_SETTINGS), I18n.t(I18n.Key.TOOLTIP_SETTINGS));
        settingsButton.addActionListener(e -> openSettingsDialog());
        toolBar.add(settingsButton);

        return toolBar;
    }

    public void openSettingsDialog() {
        if (settingsDialog == null) {
            settingsDialog = new SettingsDialog(this);
        }
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setVisible(true);
        settingsDialog.toFront();
    }

    private JButton createStyledButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(UITheme.FONT_STATUS);
        btn.setForeground(UITheme.TEXT_PRIMARY);
        btn.setBackground(new Color(0x28, 0x2E, 0x3E));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_HUD, 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        btn.setToolTipText(tooltip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void initializeData() {
        // Load GeoJSON layers and CSV in background
        SwingWorker<List<Station>, Void> worker = new SwingWorker<>() {
            private GeoJsonLayer world;
            private GeoJsonLayer prefectures;
            private GeoJsonLayer faults;

            @Override
            protected List<Station> doInBackground() throws Exception {
                updateStatus(I18n.Key.STATUS_LOADING_MAP_LAYERS, true);

                // Load Prefectures GeoJSON (Japan)
                try {
                    prefectures = GeoJsonLoader.loadLayer(
                            "prefectures.geojson", "prefectures", GeoJsonLayer.LayerType.POLYGON,
                            UITheme.PREFECTURES_FILL, UITheme.PREFECTURES_STROKE, 0.8f);
                } catch (Exception e) {
                    System.err.println("Warning: prefectures.geojson could not be loaded: " + e.getMessage());
                }

                // Load Faults / Trench GeoJSON
                try {
                    faults = GeoJsonLoader.loadLayer(
                            "faults.geojson", "faults", GeoJsonLayer.LayerType.LINE,
                            null, UITheme.FAULTS_STROKE, 1.8f);
                } catch (Exception e) {
                    System.err.println("Warning: faults.geojson could not be loaded: " + e.getMessage());
                }

                updateStatus(I18n.Key.STATUS_LOADING_STATION_METADATA, true);
                return StationCsvLoader.loadStations("sta-mapped.csv");
            }

            @Override
            protected void done() {
                try {
                    if (world != null) mapCanvas.setWorldLayer(world);
                    if (prefectures != null) mapCanvas.setPrefecturesLayer(prefectures);
                    if (faults != null) mapCanvas.setFaultsLayer(faults);

                    stations = get();
                    mapCanvas.setStations(stations);
                    updateStatus(I18n.Key.STATUS_LOADED_STATIONS, true, stations.size());
                    refreshData(false);
                } catch (Exception e) {
                    showError(I18n.Key.STATUS_FAILED_LOAD_MAP, e.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Schedules the next periodic query to execute on the 30th second of the minute (e.g. HH:MM:30).
     */
    private void scheduleNextPeriodicCheck() {
        if (!AppSettings.getInstance().isAutoRefresh()) {
            return;
        }

        long now = System.currentTimeMillis();
        long secondOfMinute = (now / 1000) % 60;
        long millisOfSecond = now % 1000;

        long delaySeconds = (30 - secondOfMinute + 60) % 60;
        if (delaySeconds == 0 && millisOfSecond > 200) {
            delaySeconds = 60; // already past :30 for the current minute
        }
        long delayMillis = delaySeconds * 1000L - millisOfSecond;
        if (delayMillis <= 50) {
            delayMillis += 60000L;
        }

        periodicTimer.setInitialDelay((int) delayMillis);
        periodicTimer.setRepeats(false);
        periodicTimer.restart();
    }

    private static class RefreshResult {
        final TargetTimeInfo timeInfo;
        final boolean isNewData;

        RefreshResult(TargetTimeInfo timeInfo, boolean isNewData) {
            this.timeInfo = timeInfo;
            this.isNewData = isNewData;
        }
    }

    public void refreshData(boolean userInitiated) {
        if (!isRefreshing.compareAndSet(false, true)) {
            return;
        }

        refreshButton.setEnabled(false);
        mapCanvas.setStatusMessage(I18n.Key.STATUS_QUERYING_TIMESTAMPS, true);
        updateStatus(I18n.Key.STATUS_FETCHING_TIMESTAMPS, true);

        SwingWorker<RefreshResult, Void> worker = new SwingWorker<>() {
            @Override
            protected RefreshResult doInBackground() throws Exception {
                // 1. Query targetTimes.json for latest basetime
                TargetTimeInfo timeInfo = dataService.fetchLatestTargetTime();

                // 2. Check if a new basetime/validtime has been added
                boolean isNew = (lastTargetTimeInfo == null) || !timeInfo.equals(lastTargetTimeInfo);

                // 3. Update station colors only if new data arrived or user manually clicked Refresh
                if (userInitiated || isNew) {
                    if (stations != null && !stations.isEmpty()) {
                        updateStatus(isNew ? I18n.Key.STATUS_DOWNLOADING_NEW_DATA
                                        : I18n.Key.STATUS_DOWNLOADING_REFRESH,
                                true, timeInfo.getBasetime(), timeInfo.getFormattedJstTime());
                        dataService.updateStationColors(stations, timeInfo);
                    }
                    lastTargetTimeInfo = timeInfo;
                    return new RefreshResult(timeInfo, true);
                } else {
                    // No new basetime/validtime added - keep current station colors
                    return new RefreshResult(timeInfo, false);
                }
            }

            @Override
            protected void done() {
                try {
                    RefreshResult result = get();
                    mapCanvas.setTargetTimeInfo(result.timeInfo);
                    mapCanvas.setStatusMessage(I18n.Key.STATUS_READY, false);

                    if (result.isNewData) {
                        updateStatus(I18n.Key.STATUS_UPDATED, false,
                                result.timeInfo.getFormattedJstTime());
                        mapCanvas.repaint();
                    } else {
                        updateStatus(I18n.Key.STATUS_UP_TO_DATE, false,
                                result.timeInfo.getFormattedJstTime());
                    }
                } catch (Exception e) {
                    showError(I18n.Key.STATUS_UPDATE_ERROR, e.getMessage());
                } finally {
                    isRefreshing.set(false);
                    refreshButton.setEnabled(true);
                    scheduleNextPeriodicCheck();
                }
            }
        };
        worker.execute();
    }

    private void updateStatus(I18n.Key key, boolean isBusy, Object... args) {
        statusKey = key;
        statusArgs = (args != null) ? args : new Object[0];
        statusBusy = isBusy;
        statusError = false;
        SwingUtilities.invokeLater(this::applyStatus);
    }

    private void showError(I18n.Key messageKey, Object... args) {
        statusKey = messageKey;
        statusArgs = (args != null) ? args : new Object[0];
        statusBusy = false;
        statusError = true;
        SwingUtilities.invokeLater(this::applyStatus);
        mapCanvas.setStatusMessage(messageKey, false, args);
    }

    private void applyStatus() {
        String text = I18n.t(statusKey, statusArgs);
        if (statusError) {
            text = "⚠️ " + text;
        }
        statusLabel.setText(text);
        statusLabel.setForeground(statusError ? UITheme.STATUS_ERROR
                : statusBusy ? UITheme.STATUS_BUSY : UITheme.TEXT_SECONDARY);
    }

    private void updateLocalizedTexts() {
        if (refreshButton != null) {
            refreshButton.setText(I18n.t(I18n.Key.BUTTON_REFRESH));
            refreshButton.setToolTipText(I18n.t(I18n.Key.TOOLTIP_REFRESH));
        }
        if (fitButton != null) {
            fitButton.setText(I18n.t(I18n.Key.BUTTON_FIT));
            fitButton.setToolTipText(I18n.t(I18n.Key.TOOLTIP_FIT));
        }
        if (settingsButton != null) {
            settingsButton.setText(I18n.t(I18n.Key.BUTTON_SETTINGS));
            settingsButton.setToolTipText(I18n.t(I18n.Key.TOOLTIP_SETTINGS));
        }
        applyStatus();
    }
}
