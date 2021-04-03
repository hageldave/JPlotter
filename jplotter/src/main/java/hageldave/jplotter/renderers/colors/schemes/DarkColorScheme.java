package hageldave.jplotter.renderers.colors.schemes;

import java.awt.*;

/**
 * The DarkColorScheme defines lighter colors which can be used in a dark environment.
 *
 * @author lucareichmann
 */
public class DarkColorScheme implements ColorScheme {
    private final static Color primaryColor = Color.WHITE;
    private final static Color secondaryColor = Color.LIGHT_GRAY;
    private final static Color tertiaryColor = Color.LIGHT_GRAY;
    private final static Color fourthColor = Color.DARK_GRAY;
    private final static Color textColor = new Color(196, 196, 196);

    @Override
    public Color getPrimaryColor () {
        return primaryColor;
    }

    @Override
    public Color getSecondaryColor () {
        return secondaryColor;
    }

    @Override
    public Color getTertiaryColor () {
        return tertiaryColor;
    }

    @Override
    public Color getFourthColor () {
        return fourthColor;
    }

    @Override
    public Color getTextColor () {
        return textColor;
    }
}