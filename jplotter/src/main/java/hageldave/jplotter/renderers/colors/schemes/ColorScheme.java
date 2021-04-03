package hageldave.jplotter.renderers.colors.schemes;

import hageldave.jplotter.renderers.CoordSysRenderer;

import java.awt.*;

/**
 * The ColorScheme interface defines the necessary colors stored in a custom ColorScheme,
 * which can then be used to define the colors in the {@link CoordSysRenderer} axis, axis-labels, guides and ticks.
 *
 * @author lucareichmann
 */
public interface ColorScheme {
    /**
     *
     * @return primary color
     */
    Color getPrimaryColor ();

    /**
     *
     * @return secondary color
     */
    Color getSecondaryColor();

    /**
     *
     * @return tertiary color
     */
    Color getTertiaryColor();

    /**
     *
     * @return
     */
    Color getFourthColor();

    /**
     *
     * @return the text color - used in
     */
    Color getTextColor();
}
