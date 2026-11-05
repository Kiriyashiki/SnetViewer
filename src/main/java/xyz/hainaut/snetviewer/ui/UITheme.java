package xyz.hainaut.snetviewer.ui;

import java.awt.Color;
import java.awt.Font;

/**
 * UI Theme constants, colors, and typography for modern cross-platform styling.
 */
public final class UITheme {
    private UITheme() {}

    // Dark map & UI palette
    public static final Color BG_MAP = new Color(0x13, 0x16, 0x1F);
    public static final Color BG_PANEL = new Color(0x1B, 0x1F, 0x2B);
    public static final Color BG_HUD = new Color(0x10, 0x13, 0x1A, 220);
    public static final Color BORDER_HUD = new Color(0x35, 0x3E, 0x54, 200);

    public static final Color TEXT_PRIMARY = new Color(0xF0, 0xF3, 0xF8);
    public static final Color TEXT_SECONDARY = new Color(0x9E, 0xA8, 0xBF);
    public static final Color TEXT_ACCENT = new Color(0x4E, 0x9F, 0xFF);

    public static final Color STATUS_OK = new Color(0x2E, 0xC4, 0x7E);
    public static final Color STATUS_BUSY = new Color(0xFA, 0xAD, 0x14);
    public static final Color STATUS_ERROR = new Color(0xFF, 0x4D, 0x4F);

    public static final Color MARKER_BORDER = new Color(0x05, 0x07, 0x0B, 200);
    public static final Color MARKER_HOVER_RING = new Color(0xFF, 0xFF, 0xFF, 220);
    public static final Color MARKER_GLOW = new Color(0x4E, 0x9F, 0xFF, 120);

    public static final Color LAND_STATION = new Color(0xAA, 0xAA, 0xAA);

    // GeoJSON Map Layer styling
    public static final Color WORLD_FILL = new Color(0x1C, 0x22, 0x30);
    public static final Color WORLD_STROKE = new Color(0x2A, 0x33, 0x47);
    public static final Color PREFECTURES_FILL = new Color(0x25, 0x2D, 0x3E);
    public static final Color PREFECTURES_STROKE = new Color(0x3B, 0x47, 0x62);
    public static final Color FAULTS_STROKE = new Color(0xFF, 0x6B, 0x6B, 220);

    // Settings UI & Card styling
    public static final Color BG_CARD = new Color(0x22, 0x27, 0x36);
    public static final Color BG_CARD_HOVER = new Color(0x28, 0x2E, 0x40);
    public static final Color TEXT_MUTED = new Color(0x7A, 0x86, 0x9E);

    // Fonts
    public static final Font FONT_HEADER_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    public static final Font FONT_HEADER_SUB = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font FONT_SETTING_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final Font FONT_SETTING_DESC = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    public static final Font FONT_TIME_HUD = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    public static final Font FONT_TIME_SUB = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font FONT_TOOLTIP_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    public static final Font FONT_TOOLTIP_BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    public static final Font FONT_MARKER_LABEL = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    public static final Font FONT_STATUS = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
}
