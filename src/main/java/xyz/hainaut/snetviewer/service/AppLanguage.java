package xyz.hainaut.snetviewer.service;

/**
 * Supported UI languages. Japanese is the default.
 */
public enum AppLanguage {
    JAPANESE("ja", "日本語"),
    ENGLISH("en", "English");

    private final String code;
    private final String nativeName;

    AppLanguage(String code, String nativeName) {
        this.code = code;
        this.nativeName = nativeName;
    }

    public String getCode() {
        return code;
    }

    public String getNativeName() {
        return nativeName;
    }

    public static AppLanguage fromCode(String code) {
        if (code != null) {
            for (AppLanguage language : values()) {
                if (language.code.equalsIgnoreCase(code.trim())) {
                    return language;
                }
            }
        }
        return JAPANESE;
    }

    @Override
    public String toString() {
        return nativeName;
    }
}
