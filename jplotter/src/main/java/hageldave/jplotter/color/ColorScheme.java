package hageldave.jplotter.color;

import java.awt.*;

/**
 * The ColorScheme is responsible for storing and providing color information,
 * which other components can use.
 * This enables an easy way to distribute color information to multiple components.
 * There is the option to use one of the predefined {@link ColorSchemePreset} or to define custom colors.
 * The color scheme contains five different color attributes,
 * which can be accessed by the respective components.
 *
 * @author lucareichmann
 */
public class ColorScheme {
    protected Color primaryColor;
    protected Color secondaryColor;
    protected Color tertiaryColor;
    protected Color quaternaryColor;
    protected Color textColor;

    /**
     * Defines the color scheme by using one of the predefined {@link ColorSchemePreset}.
     *
     * @param scheme - the used SchemePreset
     */
    public ColorScheme (final ColorSchemePreset scheme) {
        setColors(scheme.primaryColor, scheme.secondaryColor,
                scheme.tertiaryColor, scheme.quaternaryColor, scheme.textColor);
    }

    /**
     * This constructor is used for defining custom colors in the color scheme.
     *
     * @param primaryColor primary color of the color scheme
     * @param secondaryColor secondary color of the color scheme
     * @param tertiaryColor tertiary color of the color scheme
     * @param quaternaryColor quarternary color of the color scheme
     * @param textColor text color of the color scheme
     */
    public ColorScheme (final Color primaryColor, final Color secondaryColor,
                        final Color tertiaryColor, final Color quaternaryColor, final Color textColor) {
        setColors(primaryColor, secondaryColor, tertiaryColor, quaternaryColor, textColor);
    }

    /**
     * Helper method which sets colors of the scheme
     *
     * @param primaryColor primary color of the color scheme
     * @param secondaryColor secondary color of the color scheme
     * @param tertiaryColor tertiary color of the color scheme
     * @param quaternaryColor fourth color of the color scheme
     * @param textColor text color of the color scheme
     */
    protected void setColors (final Color primaryColor, final Color secondaryColor,
                                     final Color tertiaryColor, final Color quaternaryColor, final Color textColor) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.tertiaryColor = tertiaryColor;
        this.quaternaryColor = quaternaryColor;
        this.textColor = textColor;
    }

    /**
     * @return primary color of the color scheme
     */
    public Color getPrimaryColor() {
        return this.primaryColor;
    }

    /**
     * @return secondary color of the color scheme
     */
    public Color getSecondaryColor() {
        return this.secondaryColor;
    }

    /**
     * @return tertiary color of the color scheme
     */
    public Color getTertiaryColor() {
        return this.tertiaryColor;
    }

    /**
     * @return fourth color of the color scheme
     */
    public Color getQuaternary () {
        return this.quaternaryColor;
    }

    /**
     * @return text color of the color scheme
     */
    public Color getTextColor() {
        return this.textColor;
    }
}
