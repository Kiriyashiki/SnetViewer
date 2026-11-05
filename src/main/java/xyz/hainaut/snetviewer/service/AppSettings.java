package xyz.hainaut.snetviewer.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages application configuration and cross-platform persistence.
 * Settings are stored in platform-standard directories:
 * - Windows: %APPDATA%\SnetViewer\settings.properties
 * - macOS: ~/Library/Application Support/SnetViewer/settings.properties
 * - Linux / Unix: $XDG_CONFIG_HOME/SnetViewer/settings.properties (or ~/.config/SnetViewer/settings.properties)
 */
public class AppSettings {
    private static final String APP_NAME = "SnetViewer";
    private static final String CONFIG_FILE_NAME = "settings.properties";

    public static final String KEY_AUTO_REFRESH = "autoRefresh";
    public static final String KEY_SHOW_STATION_LABELS = "showStationLabels";
    public static final String KEY_SHOW_DEV_INFO = "showDevInfo";
    public static final String KEY_MAP_ROTATION = "mapRotation";
    public static final String KEY_LANGUAGE = "language";

    private static final AppSettings INSTANCE = new AppSettings();

    public static AppSettings getInstance() {
        return INSTANCE;
    }

    public interface SettingsListener {
        void onSettingsChanged(AppSettings settings);
    }

    private final Path configPath;
    private final List<SettingsListener> listeners = new CopyOnWriteArrayList<>();

    private boolean autoRefresh = true;
    private boolean showStationLabels = false;
    private boolean showDevInfo = false;
    private int mapRotation = 0;
    private AppLanguage language = AppLanguage.JAPANESE;

    private AppSettings() {
        this.configPath = resolveConfigPath();
        load();
    }

    /**
     * Resolves the OS-specific configuration path following platform standards.
     */
    public static Path resolveConfigPath() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Path baseDir;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.trim().isEmpty()) {
                baseDir = Paths.get(appData, APP_NAME);
            } else {
                baseDir = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", APP_NAME);
            }
        } else if (os.contains("mac") || os.contains("darwin")) {
            baseDir = Paths.get(System.getProperty("user.home"), "Library", "Application Support", APP_NAME);
        } else {
            // Linux / Unix / BSD (XDG specification)
            String xdgConfig = System.getenv("XDG_CONFIG_HOME");
            if (xdgConfig != null && !xdgConfig.trim().isEmpty()) {
                baseDir = Paths.get(xdgConfig, APP_NAME);
            } else {
                baseDir = Paths.get(System.getProperty("user.home"), ".config", APP_NAME);
            }
        }
        return baseDir.resolve(CONFIG_FILE_NAME);
    }

    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Loads settings from the configuration file. If the file does not exist, defaults are saved.
     */
    public synchronized void load() {
        if (!Files.exists(configPath)) {
            save();
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
            if (props.containsKey(KEY_AUTO_REFRESH)) {
                this.autoRefresh = Boolean.parseBoolean(props.getProperty(KEY_AUTO_REFRESH, "true"));
            }
            if (props.containsKey(KEY_SHOW_STATION_LABELS)) {
                this.showStationLabels = Boolean.parseBoolean(props.getProperty(KEY_SHOW_STATION_LABELS, "false"));
            }
            if (props.containsKey(KEY_SHOW_DEV_INFO)) {
                this.showDevInfo = Boolean.parseBoolean(props.getProperty(KEY_SHOW_DEV_INFO, "false"));
            }
            if (props.containsKey(KEY_MAP_ROTATION)) {
                try {
                    int rot = Integer.parseInt(props.getProperty(KEY_MAP_ROTATION, "0"));
                    this.mapRotation = normalizeRotation(rot);
                } catch (NumberFormatException ignored) {}
            }
            if (props.containsKey(KEY_LANGUAGE)) {
                this.language = AppLanguage.fromCode(props.getProperty(KEY_LANGUAGE));
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load settings from " + configPath + ": " + e.getMessage());
        }
    }

    /**
     * Persists settings to disk in the OS configuration directory.
     */
    public synchronized void save() {
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }
            Properties props = new Properties();
            props.setProperty(KEY_AUTO_REFRESH, String.valueOf(autoRefresh));
            props.setProperty(KEY_SHOW_STATION_LABELS, String.valueOf(showStationLabels));
            props.setProperty(KEY_SHOW_DEV_INFO, String.valueOf(showDevInfo));
            props.setProperty(KEY_MAP_ROTATION, String.valueOf(mapRotation));
            props.setProperty(KEY_LANGUAGE, language.getCode());

            try (OutputStream out = Files.newOutputStream(configPath)) {
                props.store(out, "SnetViewer Configuration");
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to save settings to " + configPath + ": " + e.getMessage());
        }
    }

    public static int normalizeRotation(int rotation) {
        int norm = ((rotation % 360) + 360) % 360;
        return (Math.round((float) norm / 90.0f) * 90) % 360;
    }

    public boolean isAutoRefresh() {
        return autoRefresh;
    }

    public void setAutoRefresh(boolean autoRefresh) {
        if (this.autoRefresh != autoRefresh) {
            this.autoRefresh = autoRefresh;
            save();
            notifyListeners();
        }
    }

    public boolean isShowStationLabels() {
        return showStationLabels;
    }

    public void setShowStationLabels(boolean showStationLabels) {
        if (this.showStationLabels != showStationLabels) {
            this.showStationLabels = showStationLabels;
            save();
            notifyListeners();
        }
    }

    public boolean isShowDevInfo() {
        return showDevInfo;
    }

    public void setShowDevInfo(boolean showDevInfo) {
        if (this.showDevInfo != showDevInfo) {
            this.showDevInfo = showDevInfo;
            save();
            notifyListeners();
        }
    }

    public int getMapRotation() {
        return mapRotation;
    }

    public void setMapRotation(int rotation) {
        int norm = normalizeRotation(rotation);
        if (this.mapRotation != norm) {
            this.mapRotation = norm;
            save();
            notifyListeners();
        }
    }

    public void rotateMap90() {
        setMapRotation((mapRotation + 90) % 360);
    }

    public AppLanguage getLanguage() {
        return language;
    }

    public void setLanguage(AppLanguage language) {
        AppLanguage next = (language != null) ? language : AppLanguage.JAPANESE;
        if (this.language != next) {
            this.language = next;
            save();
            notifyListeners();
        }
    }

    public void addListener(SettingsListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SettingsListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (SettingsListener listener : listeners) {
            try {
                listener.onSettingsChanged(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
