package com.taskmanager.view.theme;

/**
 * Utility responsible for resolving and normalizing application theme preferences.
 */
public final class ThemeSelector {

    public static final String THEME_LIGHT = "Light";
    public static final String THEME_DARK = "Dark";

    private ThemeSelector() {
        // Utility class
    }

    /**
     * Resolves the configured theme string to a normalized canonical theme name.
     * Defaults to {@link #THEME_LIGHT} if input is null, blank, or unrecognized.
     *
     * @param themePreference the raw theme preference string
     * @return {@link #THEME_DARK} if input matches "dark" (case-insensitive); otherwise {@link #THEME_LIGHT}
     */
    public static String resolveTheme(String themePreference) {
        if (themePreference == null || themePreference.trim().isEmpty()) {
            return THEME_LIGHT;
        }
        String trimmed = themePreference.trim();
        if (THEME_DARK.equalsIgnoreCase(trimmed)) {
            return THEME_DARK;
        }
        return THEME_LIGHT;
    }

    /**
     * Determines whether the given theme represents Dark mode.
     *
     * @param theme the theme name
     * @return true if dark mode, false otherwise
     */
    public static boolean isDark(String theme) {
        return THEME_DARK.equalsIgnoreCase(resolveTheme(theme));
    }
}
