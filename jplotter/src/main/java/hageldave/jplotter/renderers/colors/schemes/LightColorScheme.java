package hageldave.jplotter.renderers.colors.schemes;

import java.awt.*;

/**
 * The LightColorScheme defines darker colors which can be used in a light environment.
 *
 * @author lucareichmann
 */
public class LightColorScheme implements ColorScheme {
    private final static Color primaryColor = Color.BLACK;
    private final static Color secondaryColor = Color.GRAY;
    private final static Color tertiaryColor = Color.DARK_GRAY;
    private final static Color fourthColor = new Color(0xdddddd);
    private final static Color textColor = new Color(96,96,96);

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