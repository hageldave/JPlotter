package hageldave.jplotter.ui;

import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.ui.schemes.ColorScheme;
import hageldave.jplotter.ui.schemes.CustomColorScheme;
import hageldave.jplotter.ui.schemes.DarkColorScheme;
import hageldave.jplotter.ui.schemes.LightColorScheme;

import java.awt.*;

// TODO vereinen mit ColorScheme
/**
 * The ColorProvider is responsible for providing color information to the {@link CoordSysRenderer}.
 * The ColorProvider itself doesn't hold any color information,
 * those will be queried from its 'currentColorScheme' ({@link ColorScheme}) property.
 *
 * @author lucareichmann
 */
public class ColorProvider implements ColorScheme {
    protected ColorScheme currentColorScheme;

    public ColorProvider() {
        this.currentColorScheme = new LightColorScheme();
    }

    /**
     * Instanciates a Colorprovider with darkmode enabled by default, if
     * the parameter is 'true'.
     * @param enableDarkmode - enables darkmode, if enableDarkmode is true
     */
    public ColorProvider(final boolean enableDarkmode) {
        if (enableDarkmode) {
            this.currentColorScheme = new DarkColorScheme();
        } else {
            this.currentColorScheme = new LightColorScheme();
        }
    }

    /**
     * Switches the color scheme currently in use.
     */
    public void enableDarkmode() {
        this.currentColorScheme = new DarkColorScheme();
    }

    /**
     * Switches the color scheme currently in use.
     *
     * @param value darkmode will be set if value is true
     */
    public void enableDarkmode(final boolean value) {
        if (value) {
            this.currentColorScheme = new DarkColorScheme();
        }
        else {
            this.currentColorScheme = new LightColorScheme();
        }
    }

    /**
     * Sets a {@link CustomColorScheme} with the given parameters.
     * This enables setting custom colors in the CoordSysRenderer.
     *
     * @param primaryColor - primary color
     * @param secondaryColor - secondary color
     * @param tertiaryColor - tertiary color
     * @param fourthColor - fourth color
     */
    public void setCustomColors(final Color primaryColor, final Color secondaryColor,
                                final Color tertiaryColor, final Color fourthColor) {
        this.currentColorScheme = new CustomColorScheme(primaryColor, secondaryColor, tertiaryColor, fourthColor);
    }

    /**
     * Sets a {@link CustomColorScheme} with the given parameters and a custom text color.
     *
     * @param primaryColor - primary color
     * @param secondaryColor - secondary color
     * @param tertiaryColor - tertiary color
     * @param fourthColor - fourth color
     * @param textColor - custom text color
     */
    public void setCustomColors(final Color primaryColor, final Color secondaryColor,
                                final Color tertiaryColor, final Color fourthColor, final Color textColor) {
        this.currentColorScheme = new CustomColorScheme(primaryColor, secondaryColor, tertiaryColor, fourthColor, textColor);
    }

    public Color getPrimaryColor() {
        return this.currentColorScheme.getPrimaryColor();
    }

    public Color getSecondaryColor() {
        return this.currentColorScheme.getSecondaryColor();
    }

    public Color getTertiaryColor() {
        return this.currentColorScheme.getTertiaryColor();
    }

    public Color getFourthColor() {
        return this.currentColorScheme.getFourthColor();
    }

    public Color getTextColor() {
        return this.currentColorScheme.getTextColor();
    }
}
