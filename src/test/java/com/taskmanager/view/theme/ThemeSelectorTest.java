package com.taskmanager.view.theme;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ThemeSelector Normalization and Resolution Tests")
class ThemeSelectorTest {

    @ParameterizedTest
    @ValueSource(strings = {"Dark", "dark", "DARK", "  Dark  ", "daRK"})
    @DisplayName("Dark variations resolve to canonical Dark theme")
    void testDarkVariations(String input) {
        assertEquals(ThemeSelector.THEME_DARK, ThemeSelector.resolveTheme(input));
        assertTrue(ThemeSelector.isDark(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Light", "light", "LIGHT", "  Light  ", "Solarized", "HighContrast", "RandomUnknown"})
    @DisplayName("Light or unknown variations resolve to canonical Light theme")
    void testLightAndUnknownVariations(String input) {
        assertEquals(ThemeSelector.THEME_LIGHT, ThemeSelector.resolveTheme(input));
        assertFalse(ThemeSelector.isDark(input));
    }

    @Test
    @DisplayName("Null or empty/whitespace input safely defaults to Light theme")
    void testNullAndWhitespaceDefaults() {
        assertEquals(ThemeSelector.THEME_LIGHT, ThemeSelector.resolveTheme(null));
        assertFalse(ThemeSelector.isDark(null));

        assertEquals(ThemeSelector.THEME_LIGHT, ThemeSelector.resolveTheme(""));
        assertFalse(ThemeSelector.isDark(""));

        assertEquals(ThemeSelector.THEME_LIGHT, ThemeSelector.resolveTheme("    "));
        assertFalse(ThemeSelector.isDark("    "));
    }
}
