package hageldave.jplotter.ui.schemes;

import java.awt.*;

/**
 * A CustomColorScheme can be used for defining custom colors.
 *
 * @author lucareichmann
 */
public class CustomColorScheme implements ColorScheme {
    private final Color primaryColor;
    private final Color secondaryColor;
    private final Color tertiaryColor;
    private final Color fourthColor;
    private final Color textColor;

    public CustomColorScheme(final Color primary, final Color secondary,
                             final Color tertiary, final Color fourth) {
        this.primaryColor = primary;
        this.secondaryColor = secondary;
        this.tertiaryColor = tertiary;
        this.fourthColor = fourth;
        this.textColor = new Color(96, 96, 96);
    }

    public CustomColorScheme(final Color primary, final Color secondary,
                             final Color tertiary, final Color fourth, final Color textColor) {
        this.primaryColor = primary;
        this.secondaryColor = secondary;
        this.tertiaryColor = tertiary;
        this.fourthColor = fourth;
        this.textColor = textColor;
    }

    @Override
    public Color getPrimaryColor () {
        return this.primaryColor;
    }

    @Override
    public Color getSecondaryColor () {
        return this.secondaryColor;
    }

    @Override
    public Color getTertiaryColor () {
        return this.tertiaryColor;
    }

    @Override
    public Color getFourthColor () {
        return this.fourthColor;
    }

    @Override
    public Color getTextColor () {
        return this.textColor;
    }
}
