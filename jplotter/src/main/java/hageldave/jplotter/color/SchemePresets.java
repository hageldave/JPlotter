package hageldave.jplotter.color;

import java.awt.*;

/**
 * The SchemePresets contains different color constants (Light, Dark),
 * which can be accessed by the Color Scheme.
 *
 * @author lucareichmann
 */
public enum SchemePresets {
    LIGHT(
            Color.BLACK,
            Color.GRAY,
            Color.DARK_GRAY,
            new Color(0xdddddd),
            new Color(96, 96, 96)
    ),
    DARK(
            Color.WHITE,
            Color.LIGHT_GRAY,
            Color.LIGHT_GRAY,
            Color.DARK_GRAY,
            new Color(196, 196, 196)
    );

    protected final Color primaryColor;
    protected final Color secondaryColor;
    protected final Color tertiaryColor;
    protected final Color fourthColor;
    protected final Color textColor;

    SchemePresets (final Color primaryColor, final Color secondaryColor,
                   final Color tertiaryColor, final Color fourthColor, final Color textColor) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.tertiaryColor = tertiaryColor;
        this.fourthColor = fourthColor;
        this.textColor = textColor;
    }
}
