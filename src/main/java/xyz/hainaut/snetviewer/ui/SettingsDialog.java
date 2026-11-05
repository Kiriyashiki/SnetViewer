package xyz.hainaut.snetviewer.ui;

import xyz.hainaut.snetviewer.service.AppLanguage;
import xyz.hainaut.snetviewer.service.AppSettings;
import xyz.hainaut.snetviewer.service.I18n;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

/**
 * Modern dark-themed settings dialog to configure display and auto-refresh options.
 */
public class SettingsDialog extends JDialog implements AppSettings.SettingsListener {
    private final JCheckBox autoRefreshCheckBox;
    private final JCheckBox labelsCheckBox;
    private final JCheckBox debugCheckBox;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JLabel languageLabel;
    private JLabel languageDescLabel;
    private JComboBox<AppLanguage> languageComboBox;
    private JLabel autoRefreshDescLabel;
    private JLabel stationIdsDescLabel;
    private JLabel devInfoDescLabel;
    private JLabel rotationLabel;
    private JButton rotateButton;
    private JLabel rotationDescLabel;
    private JLabel configPathTitleLabel;
    private JLabel configPathLabel;
    private JButton closeButton;
    private boolean isUpdatingUi = false;

    public SettingsDialog(Frame owner) {
        super(owner, I18n.t(I18n.Key.SETTINGS_WINDOW_TITLE), false);

        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(UITheme.BG_PANEL);

        // Root container
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(UITheme.BG_PANEL);
        contentPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. Header
        JPanel headerPanel = new JPanel(new BorderLayout(0, 4));
        headerPanel.setOpaque(false);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleLabel = new JLabel();
        titleLabel.setFont(UITheme.FONT_HEADER_TITLE);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);

        subtitleLabel = new JLabel();
        subtitleLabel.setFont(UITheme.FONT_HEADER_SUB);
        subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        contentPanel.add(headerPanel);

        contentPanel.add(Box.createVerticalStrut(16));

        // 2. Settings Items Container
        JPanel itemsContainer = new JPanel();
        itemsContainer.setLayout(new BoxLayout(itemsContainer, BoxLayout.Y_AXIS));
        itemsContainer.setOpaque(false);
        itemsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Language selector
        itemsContainer.add(createLanguageCard());
        itemsContainer.add(Box.createVerticalStrut(10));

        // Checkboxes
        autoRefreshCheckBox = new JCheckBox();
        autoRefreshDescLabel = createDescriptionLabel(24);
        JPanel autoRefreshCard = createSettingCard(autoRefreshCheckBox, autoRefreshDescLabel);
        autoRefreshCheckBox.addActionListener(e -> {
            if (!isUpdatingUi) {
                AppSettings.getInstance().setAutoRefresh(autoRefreshCheckBox.isSelected());
            }
        });
        itemsContainer.add(autoRefreshCard);
        itemsContainer.add(Box.createVerticalStrut(10));

        labelsCheckBox = new JCheckBox();
        stationIdsDescLabel = createDescriptionLabel(24);
        JPanel labelsCard = createSettingCard(labelsCheckBox, stationIdsDescLabel);
        labelsCheckBox.addActionListener(e -> {
            if (!isUpdatingUi) {
                AppSettings.getInstance().setShowStationLabels(labelsCheckBox.isSelected());
            }
        });
        itemsContainer.add(labelsCard);
        itemsContainer.add(Box.createVerticalStrut(10));

        debugCheckBox = new JCheckBox();
        devInfoDescLabel = createDescriptionLabel(24);
        JPanel debugCard = createSettingCard(debugCheckBox, devInfoDescLabel);
        debugCheckBox.addActionListener(e -> {
            if (!isUpdatingUi) {
                AppSettings.getInstance().setShowDevInfo(debugCheckBox.isSelected());
            }
        });
        itemsContainer.add(debugCard);
        itemsContainer.add(Box.createVerticalStrut(10));

        // Map Rotation Setting Card
        JPanel rotationCard = createRotationCard();
        itemsContainer.add(rotationCard);
        contentPanel.add(itemsContainer);

        contentPanel.add(Box.createVerticalStrut(14));

        // 3. Storage Location Info Card
        JPanel storageInfoPanel = createStorageInfoPanel();
        storageInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(storageInfoPanel);

        contentPanel.add(Box.createVerticalStrut(16));

        // 4. Bottom Button Panel
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footerPanel.setOpaque(false);
        footerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        closeButton = new JButton();
        closeButton.setFont(UITheme.FONT_STATUS);
        closeButton.setForeground(UITheme.TEXT_PRIMARY);
        closeButton.setBackground(new Color(0x28, 0x2E, 0x3E));
        closeButton.setFocusPainted(false);
        closeButton.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_HUD, 1),
                new EmptyBorder(6, 18, 6, 18)
        ));
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> setVisible(false));
        footerPanel.add(closeButton);
        contentPanel.add(footerPanel);

        setContentPane(contentPanel);

        // Sync initial state before packing so localized text determines the dialog size
        updateFromSettings(AppSettings.getInstance());
        pack();

        // Register ESC key to close dialog
        getRootPane().registerKeyboardAction(e -> setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Register settings listener
        AppSettings.getInstance().addListener(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                AppSettings.getInstance().removeListener(SettingsDialog.this);
            }
        });
    }

    private JPanel createLanguageCard() {
        JPanel card = createCard();
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 74));

        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setOpaque(false);

        languageLabel = new JLabel();
        languageLabel.setFont(UITheme.FONT_SETTING_TITLE);
        languageLabel.setForeground(UITheme.TEXT_PRIMARY);

        languageComboBox = new JComboBox<>(AppLanguage.values());
        languageComboBox.setFont(UITheme.FONT_STATUS);
        languageComboBox.setForeground(UITheme.TEXT_PRIMARY);
        languageComboBox.setBackground(new Color(0x28, 0x2E, 0x3E));
        languageComboBox.setFocusable(false);
        languageComboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        languageComboBox.addActionListener(e -> {
            if (!isUpdatingUi && languageComboBox.getSelectedItem() instanceof AppLanguage language) {
                AppSettings.getInstance().setLanguage(language);
            }
        });

        topRow.add(languageLabel, BorderLayout.WEST);
        topRow.add(languageComboBox, BorderLayout.EAST);

        languageDescLabel = createDescriptionLabel(0);

        card.add(topRow, BorderLayout.NORTH);
        card.add(languageDescLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createRotationCard() {
        JPanel card = createCard();
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 74));

        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setOpaque(false);

        rotationLabel = new JLabel();
        rotationLabel.setFont(UITheme.FONT_SETTING_TITLE);
        rotationLabel.setForeground(UITheme.TEXT_PRIMARY);

        rotateButton = new JButton();
        rotateButton.setFont(UITheme.FONT_STATUS);
        rotateButton.setForeground(UITheme.TEXT_PRIMARY);
        rotateButton.setBackground(new Color(0x28, 0x2E, 0x3E));
        rotateButton.setFocusPainted(false);
        rotateButton.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_HUD, 1),
                new EmptyBorder(4, 12, 4, 12)
        ));
        rotateButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rotateButton.addActionListener(e -> {
            AppSettings.getInstance().rotateMap90();
        });

        topRow.add(rotationLabel, BorderLayout.WEST);
        topRow.add(rotateButton, BorderLayout.EAST);

        rotationDescLabel = createDescriptionLabel(0);

        card.add(topRow, BorderLayout.NORTH);
        card.add(rotationDescLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createCard() {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(UITheme.BG_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_HUD, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        return card;
    }

    private JLabel createDescriptionLabel(int leftInset) {
        JLabel label = new JLabel();
        label.setFont(UITheme.FONT_SETTING_DESC);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setBorder(new EmptyBorder(0, leftInset, 0, 0));
        return label;
    }

    private JPanel createSettingCard(JCheckBox checkBox, JLabel descLabel) {
        JPanel card = createCard();
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 72));

        checkBox.setFont(UITheme.FONT_SETTING_TITLE);
        checkBox.setForeground(UITheme.TEXT_PRIMARY);
        checkBox.setOpaque(false);
        checkBox.setFocusPainted(false);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        card.add(checkBox, BorderLayout.NORTH);
        card.add(descLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createStorageInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 2));
        panel.setBackground(new Color(0x14, 0x17, 0x22));
        panel.setBorder(new CompoundBorder(
                new LineBorder(new Color(0x28, 0x30, 0x42), 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        panel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        configPathTitleLabel = new JLabel();
        configPathTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        configPathTitleLabel.setForeground(UITheme.TEXT_MUTED);

        Path configPath = AppSettings.getInstance().getConfigPath();
        configPathLabel = new JLabel(configPath.toString());
        configPathLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        configPathLabel.setForeground(UITheme.TEXT_SECONDARY);

        panel.add(configPathTitleLabel, BorderLayout.NORTH);
        panel.add(configPathLabel, BorderLayout.CENTER);
        return panel;
    }

    private void updateFromSettings(AppSettings settings) {
        isUpdatingUi = true;
        try {
            autoRefreshCheckBox.setSelected(settings.isAutoRefresh());
            labelsCheckBox.setSelected(settings.isShowStationLabels());
            debugCheckBox.setSelected(settings.isShowDevInfo());
            languageComboBox.setSelectedItem(settings.getLanguage());

            setTitle(I18n.t(I18n.Key.SETTINGS_WINDOW_TITLE));
            titleLabel.setText(I18n.t(I18n.Key.SETTINGS_TITLE));
            subtitleLabel.setText(I18n.t(I18n.Key.SETTINGS_SUBTITLE));

            languageLabel.setText(I18n.t(I18n.Key.LANGUAGE));
            languageDescLabel.setText(I18n.t(I18n.Key.LANGUAGE_DESC));

            autoRefreshCheckBox.setText(I18n.t(I18n.Key.AUTO_REFRESH));
            autoRefreshCheckBox.setToolTipText(I18n.t(I18n.Key.AUTO_REFRESH_TOOLTIP));
            autoRefreshDescLabel.setText(I18n.t(I18n.Key.AUTO_REFRESH_DESC));

            labelsCheckBox.setText(I18n.t(I18n.Key.STATION_IDS));
            labelsCheckBox.setToolTipText(I18n.t(I18n.Key.STATION_IDS_TOOLTIP));
            stationIdsDescLabel.setText(I18n.t(I18n.Key.STATION_IDS_DESC));

            debugCheckBox.setText(I18n.t(I18n.Key.DEV_INFO));
            debugCheckBox.setToolTipText(I18n.t(I18n.Key.DEV_INFO_TOOLTIP));
            devInfoDescLabel.setText(I18n.t(I18n.Key.DEV_INFO_DESC));

            rotateButton.setText(I18n.t(I18n.Key.ROTATE_90));
            rotateButton.setToolTipText(I18n.t(I18n.Key.ROTATE_TOOLTIP));
            rotationDescLabel.setText(I18n.t(I18n.Key.ROTATION_DESC));

            configPathTitleLabel.setText(I18n.t(I18n.Key.CONFIG_PATH_TITLE));
            configPathLabel.setToolTipText(
                    I18n.t(I18n.Key.CONFIG_PATH_TOOLTIP, settings.getConfigPath()));

            closeButton.setText(I18n.t(I18n.Key.CLOSE));

            updateRotationLabel(settings.getMapRotation());
        } finally {
            isUpdatingUi = false;
        }
        if (isDisplayable()) {
            pack();
        }
    }

    private void updateRotationLabel(int rotation) {
        String rotationText;
        switch (rotation) {
            case 90:
                rotationText = "90°";
                break;
            case 180:
                rotationText = "180°";
                break;
            case 270:
                rotationText = "270°";
                break;
            default:
                rotationText = I18n.t(I18n.Key.MAP_ROTATION_DEFAULT);
                break;
        }
        rotationLabel.setText(I18n.t(I18n.Key.MAP_ROTATION_LABEL, rotationText));
    }

    @Override
    public void onSettingsChanged(AppSettings settings) {
        SwingUtilities.invokeLater(() -> updateFromSettings(settings));
    }
}
