package xyz.hainaut.snetviewer.service;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Centralized user-visible text. The active language is stored in {@link AppSettings}.
 */
public final class I18n {
    public enum Key {
        // Main window / toolbar
        BUTTON_REFRESH,
        TOOLTIP_REFRESH,
        BUTTON_FIT,
        TOOLTIP_FIT,
        BUTTON_SETTINGS,
        TOOLTIP_SETTINGS,

        // Status messages
        STATUS_STARTING,
        STATUS_LOADING_MAP_LAYERS,
        STATUS_LOADING_STATION_METADATA,
        STATUS_LOADED_STATIONS,
        STATUS_QUERYING_TIMESTAMPS,
        STATUS_FETCHING_TIMESTAMPS,
        STATUS_DOWNLOADING_NEW_DATA,
        STATUS_DOWNLOADING_REFRESH,
        STATUS_READY,
        STATUS_UPDATED,
        STATUS_UP_TO_DATE,
        STATUS_FAILED_LOAD_MAP,
        STATUS_UPDATE_ERROR,
        STATUS_ERROR,

        // Map canvas / HUD
        MAP_EMPTY_INITIALIZING,
        HUD_FETCHING_DATA,
        HUD_TIME_SUBTITLE,
        FOOTER_DATA,

        // Station tooltip
        TOOLTIP_LAT_LNG,
        TOOLTIP_DEPTH,
        TOOLTIP_INTENSITY,
        TOOLTIP_TILE,
        TOOLTIP_COLOR,

        // Settings dialog
        SETTINGS_WINDOW_TITLE,
        SETTINGS_TITLE,
        SETTINGS_SUBTITLE,
        LANGUAGE,
        LANGUAGE_DESC,
        AUTO_REFRESH,
        AUTO_REFRESH_TOOLTIP,
        AUTO_REFRESH_DESC,
        STATION_IDS,
        STATION_IDS_TOOLTIP,
        STATION_IDS_DESC,
        DEV_INFO,
        DEV_INFO_TOOLTIP,
        DEV_INFO_DESC,
        MAP_ROTATION_LABEL,
        MAP_ROTATION_DEFAULT,
        ROTATE_90,
        ROTATE_TOOLTIP,
        ROTATION_DESC,
        CONFIG_PATH_TITLE,
        CONFIG_PATH_TOOLTIP,
        CLOSE
    }

    private static final Map<Key, String> JAPANESE = new EnumMap<>(Key.class);
    private static final Map<Key, String> ENGLISH = new EnumMap<>(Key.class);

    static {
        // Main window / toolbar
        put(Key.BUTTON_REFRESH, "🔄 更新", "🔄 Refresh");
        put(Key.TOOLTIP_REFRESH,
                "最新の基準時刻を取得して観測点の色を更新します (F5)",
                "Query latest basetime and update station colors (F5)");
        put(Key.BUTTON_FIT, "⛶ 画面に合わせる", "⛶ Fit to Screen");
        put(Key.TOOLTIP_FIT,
                "ズームをリセットして観測点を中央に表示します (Space / R)",
                "Reset zoom and center markers (Space / R)");
        put(Key.BUTTON_SETTINGS, "⚙ 設定", "⚙ Settings");
        put(Key.TOOLTIP_SETTINGS,
                "アプリの設定と表示オプションを変更します",
                "Configure application preferences and display options");

        // Status messages
        put(Key.STATUS_STARTING,
                "SnetViewerを起動中...",
                "Starting SnetViewer...");
        put(Key.STATUS_LOADING_MAP_LAYERS,
                "地図レイヤーを読み込み中...",
                "Loading map layers...");
        put(Key.STATUS_LOADING_STATION_METADATA,
                "観測点メタデータを読み込み中...",
                "Loading station metadata...");
        put(Key.STATUS_LOADED_STATIONS,
                "%d個の観測点を読み込みました。リアルタイム監視データを取得中...",
                "Loaded %d stations. Querying live monitoring data...");
        put(Key.STATUS_QUERYING_TIMESTAMPS,
                "時刻情報を問い合わせ中...",
                "Querying timestamps...");
        put(Key.STATUS_FETCHING_TIMESTAMPS,
                "時刻情報を取得中...",
                "Fetching timestamps...");
        put(Key.STATUS_DOWNLOADING_NEW_DATA,
                "新しいデータがあります: %s (%s 日本時間)。タイル画像をダウンロード中...",
                "New data available: %s (%s JST). Downloading tile images...");
        put(Key.STATUS_DOWNLOADING_REFRESH,
                "データを更新中: %s (%s 日本時間)。タイル画像をダウンロード中...",
                "Refreshing data: %s (%s JST). Downloading tile images...");
        put(Key.STATUS_READY, "準備完了", "Ready");
        put(Key.STATUS_UPDATED, "更新完了: %s 日本時間", "Updated: %s JST");
        put(Key.STATUS_UP_TO_DATE, "最新です (%s 日本時間)", "Up to date (%s JST)");
        put(Key.STATUS_FAILED_LOAD_MAP,
                "地図データの読み込みに失敗しました: %s",
                "Failed to load map data: %s");
        put(Key.STATUS_UPDATE_ERROR, "更新エラー: %s", "Update error: %s");
        put(Key.STATUS_ERROR, "エラー: %s", "Error: %s");

        // Map canvas / HUD
        put(Key.MAP_EMPTY_INITIALIZING, "初期化中...", "Initializing...");
        put(Key.HUD_FETCHING_DATA, "データ取得中...", "Fetching data...");
        put(Key.HUD_TIME_SUBTITLE,
                "日本時間 (UTC+9) • %d観測点",
                "JST (UTC+9) • %d stations");
        put(Key.FOOTER_DATA, "データ: 海しる (msil.go.jp)", "Data: 海しる (msil.go.jp)");

        // Station tooltip
        put(Key.TOOLTIP_LAT_LNG,
                "緯度 / 経度:  %.4f°, %.4f°",
                "Lat / Lng:  %.4f°, %.4f°");
        put(Key.TOOLTIP_DEPTH, "深さ:  %.0f m", "Depth:  %.0f m");
        put(Key.TOOLTIP_INTENSITY, "計測震度:  %s", "Instrumental Intensity:  %s");
        put(Key.TOOLTIP_TILE,
                "タイル:  X=%d, Y=%d (px: %d, %d)",
                "Tile:  X=%d, Y=%d (px: %d, %d)");
        put(Key.TOOLTIP_COLOR,
                "色:  %s  (RGB: %d, %d, %d)",
                "Color:  %s  (RGB: %d, %d, %d)");

        // Settings dialog
        put(Key.SETTINGS_WINDOW_TITLE, "設定", "Settings");
        put(Key.SETTINGS_TITLE, "⚙ 設定", "⚙ Settings");
        put(Key.SETTINGS_SUBTITLE,
                "表示設定・地図の向き・自動監視をカスタマイズ",
                "Customize display preferences, orientation, and monitoring");
        put(Key.LANGUAGE, "表示言語", "Language");
        put(Key.LANGUAGE_DESC,
                "アプリ内のテキストに使用する言語を選択します。",
                "Choose the language used for text in the app.");
        put(Key.AUTO_REFRESH, "データを自動更新", "Auto-refresh data");
        put(Key.AUTO_REFRESH_TOOLTIP,
                "毎分30秒に新しいデータを自動確認します",
                "Automatically check for new data on the 30th second of every minute");
        put(Key.AUTO_REFRESH_DESC,
                "毎分:30に新しい地震監視データを自動取得します。",
                "Automatically queries new seismic monitoring data at :30 of every minute.");
        put(Key.STATION_IDS, "観測点ID (L)", "Station IDs (L)");
        put(Key.STATION_IDS_TOOLTIP,
                "地図上の観測点の名前とIDを表示します (Lキー)",
                "Display station names and IDs next to map markers (Press L)");
        put(Key.STATION_IDS_DESC,
                "地図上の観測点の名前とIDを表示します (ショートカット: L)。",
                "Display station names and IDs next to map markers (Hotkey: L).");
        put(Key.DEV_INFO, "開発情報 (D)", "Dev Info (D)");
        put(Key.DEV_INFO_TOOLTIP,
                "観測点のホバーツールチップにタイル座標とRGB色を表示します (Dキー)",
                "Show tile coordinates & color RGB in station hover tooltip (Press D)");
        put(Key.DEV_INFO_DESC,
                "ホバーツールチップにタイル座標・ピクセル位置・RGB色を表示します (ショートカット: D)。",
                "Show tile coordinates, pixel position, and RGB color in hover tooltips (Hotkey: D).");
        put(Key.MAP_ROTATION_LABEL, "🔄 地図の回転: %s", "🔄 Map Rotation: %s");
        put(Key.MAP_ROTATION_DEFAULT, "0° (デフォルト)", "0° (Default)");
        put(Key.ROTATE_90, "⟳ 90°回転", "⟳ Rotate 90°");
        put(Key.ROTATE_TOOLTIP,
                "地図を時計回りに90度回転します (0° → 90° → 180° → 270°)",
                "Rotate map clockwise by 90 degrees (cycles 0° → 90° → 180° → 270°)");
        put(Key.ROTATION_DESC,
                "ウィンドウサイズを変えずに地図レイヤーと観測点を回転します。",
                "Rotate map layers and station markers without resizing the window.");
        put(Key.CONFIG_PATH_TITLE, "📁 設定ファイルのパス:", "📁 Configuration file path:");
        put(Key.CONFIG_PATH_TOOLTIP,
                "設定はここに自動保存されます: %s",
                "Settings are automatically saved here: %s");
        put(Key.CLOSE, "閉じる", "Close");
    }

    private I18n() {
    }

    private static void put(Key key, String japanese, String english) {
        JAPANESE.put(key, japanese);
        ENGLISH.put(key, english);
    }

    public static String t(Key key, Object... args) {
        AppLanguage language = AppSettings.getInstance().getLanguage();
        Map<Key, String> strings = (language == AppLanguage.ENGLISH) ? ENGLISH : JAPANESE;
        String pattern = strings.getOrDefault(key, key.name());
        return (args.length == 0) ? pattern : String.format(Locale.ROOT, pattern, args);
    }
}
