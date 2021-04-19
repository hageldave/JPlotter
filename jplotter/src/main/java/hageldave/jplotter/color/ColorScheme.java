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
	protected final Color color1;
	protected final Color color2;
	protected final Color color3;
	protected final Color color4;
	protected final Color colorText;

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
		this.color1 = primaryColor;
		this.color2 = secondaryColor;
		this.color3 = tertiaryColor;
		this.color4 = quaternaryColor;
		this.colorText = textColor;
	}

	/**
	 * @return primary color of the color scheme
	 */
	public Color getColor1() {
		return this.color1;
	}

	/**
	 * @return secondary color of the color scheme
	 */
	public Color getColor2() {
		return this.color2;
	}

	/**
	 * @return tertiary color of the color scheme
	 */
	public Color getColor3() {
		return this.color3;
	}

	/**
	 * @return fourth color of the color scheme
	 */
	public Color getColor4 () {
		return this.color4;
	}

	/**
	 * @return text color of the color scheme
	 */
	public Color getColorText() {
		return this.colorText;
	}
}
