package lk.fraudshield.client.util;

public class LanguageManager {

    public enum Language {
        ENGLISH("en", "🌐 Eng"),
        SINHALA("si", "🌐 සිං");

        private final String code;
        private final String label;

        Language(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }

    private static Language currentLanguage = Language.ENGLISH;

    public static Language getCurrentLanguage() {
        return currentLanguage;
    }

    public static void setLanguage(Language language) {
        currentLanguage = language;
    }

    public static boolean isSinhala() {
        return currentLanguage == Language.SINHALA;
    }
}
